package org.apache.maven.wagon.manager;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MirroredWagon - a wrapped wagon class that handles mirror lists. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MirroredWagon
    implements Wagon
{
    private WagonManager wagonManager;

    private String mirrorOfRepoId;

    private Wagon impl;

    private List sessionListeners = new ArrayList();

    private List transferListeners = new ArrayList();

    private List mirrors;

    private Iterator mirrorsIterator;

    private Logger logger;

    public MirroredWagon( WagonManager wagonManager, String mirrorOf, Wagon firstWagon, Logger logger )
    {
        this.wagonManager = wagonManager;
        this.mirrorOfRepoId = mirrorOf;
        this.logger = logger;
        this.impl = firstWagon;

        initMirrorsList();
    }

    public void addSessionListener( SessionListener listener )
    {
        sessionListeners.add( listener );
        this.impl.addSessionListener( listener );
    }

    public void addTransferListener( TransferListener listener )
    {
        transferListeners.add( listener );
        this.impl.addTransferListener( listener );
    }

    public void connect()
        throws ConnectionException, AuthenticationException
    {
        try
        {
            // Keep trying with .nextMirror() et al.
            while ( hasMirror() )
            {
                try
                {
                    this.impl.connect();
                    return;
                }
                catch ( ConnectionException e )
                {
                    logger.warn( "Unable to connect to mirror [" + this.impl.getRepository().getId() + "]" );
                    // This can result in an ExhaustedMirrorsException.
                    nextMirror();
                }
                catch ( AuthenticationException e )
                {
                    logger.warn( "Unable to authenticate to mirror [" + this.impl.getRepository().getId() + "]" );
                    // This can result in an ExhaustedMirrorsException.
                    nextMirror();
                }
            }
        }
        catch ( ExhaustedMirrorsException e )
        {
            logger.warn( e.getMessage() );
            throw new ConnectionException( e.getMessage(), e );
        }
    }

    public void connect( Repository repository )
        throws ConnectionException, AuthenticationException
    {
        throw new ConnectionException( "MirroredWagon.connect(Repository) "
            + "is not supported ( See Wagon.connect() )." );
    }

    public void connect( Repository repository, AuthenticationInfo authn )
        throws ConnectionException, AuthenticationException
    {
        throw new ConnectionException( "MirroredWagon.connect(Repository, AuthenticationInfo) "
            + "is not supported ( See Wagon.connect() )." );
    }

    public void connect( Repository repository, AuthenticationInfo authn, ProxyInfo proxy )
        throws ConnectionException, AuthenticationException
    {
        throw new ConnectionException( "MirroredWagon.connect(Repository, AuthenticationInfo, ProxyInfo) "
            + "is not supported ( See Wagon.connect() )." );
    }

    public void connect( Repository repository, ProxyInfo proxy )
        throws ConnectionException, AuthenticationException
    {
        throw new ConnectionException( "MirroredWagon.connect(Repository, ProxyInfo) "
            + "is not supported ( See Wagon.connect() )." );
    }

    public void disconnect()
        throws ConnectionException
    {
        this.impl.disconnect();
    }

    public void get( String resource, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        try
        {
            while ( hasMirror() )
            {
                try
                {
                    this.impl.get( resource, destination );
                    return;
                }
                catch ( TransferFailedException e )
                {
                    // This can result in an ExhaustedMirrorsException.
                    nextMirror();
                }
            }
        }
        catch ( ExhaustedMirrorsException e )
        {
            throw new TransferFailedException( "Unable to fetch resource " + resource + " from any mirror." );
        }

    }

    public AuthenticationInfo getAuthenticationInfo()
    {
        return this.impl.getAuthenticationInfo();
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return this.impl.getFileList( destinationDirectory );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return this.impl.getIfNewer( resourceName, destination, timestamp );
        // TODO: next mirror on failure?
    }

    public String getProtocol()
    {
        return this.impl.getProtocol();
    }

    public ProxyInfo getProxyInfo()
    {
        return this.impl.getProxyInfo();
    }

    public Repository getRepository()
    {
        return this.impl.getRepository();
    }

    public boolean hasSessionListener( SessionListener listener )
    {
        return this.impl.hasSessionListener( listener );
    }

    public boolean hasTransferListener( TransferListener listener )
    {
        return this.impl.hasTransferListener( listener );
    }

    public boolean isConnected()
    {
        return this.impl.isConnected();
    }

    public boolean isInteractive()
    {
        return this.impl.isInteractive();
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        this.impl.put( source, destination );
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        this.impl.putDirectory( sourceDirectory, destinationDirectory );
    }

    public void removeSessionListener( SessionListener listener )
    {
        sessionListeners.remove( listener );
        this.impl.removeSessionListener( listener );
    }

    public void removeTransferListener( TransferListener listener )
    {
        transferListeners.remove( listener );
        this.impl.removeTransferListener( listener );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        return this.impl.resourceExists( resourceName );
    }

    public void setAuthenticationInfo( AuthenticationInfo authn )
    {
        this.impl.setAuthenticationInfo( authn );
    }

    public void setInteractive( boolean interactive )
    {
        this.impl.setInteractive( interactive );
    }

    public void setProxyInfo( ProxyInfo proxy )
    {
        this.impl.setProxyInfo( proxy );
    }

    public void setRepository( Repository repository )
    {
        this.impl.setRepository( repository );
    }

    public boolean supportsDirectoryCopy()
    {
        return this.impl.supportsDirectoryCopy();
    }

    private void copyConfiguration( Wagon from, Wagon to )
    {
        Iterator it;

        it = sessionListeners.iterator();
        while ( it.hasNext() )
        {
            to.addSessionListener( (SessionListener) it.next() );
        }

        it = transferListeners.iterator();
        while ( it.hasNext() )
        {
            to.addTransferListener( (TransferListener) it.next() );
        }

        /* DO NOT COPY these values, as the wagonManager manages these values for you.
         * It is also quite possible for the next mirror wagon to use a different auth/proxy/repo setup.
         to.setAuthenticationInfo( from.getAuthenticationInfo() );
         to.setProxyInfo( from.getProxyInfo() );
         to.setRepository( from.getRepository() );
         */

        to.setInteractive( from.isInteractive() );
    }

    private void initMirrorsList()
    {
        mirrors = new ArrayList();

        RepositorySettings settings = wagonManager.getRepositorySettings( mirrorOfRepoId );

        Iterator it = settings.getMirrors().iterator();
        while ( it.hasNext() )
        {
            String mirrorId = (String) it.next();

            try
            {
                Wagon mirrorWagon = wagonManager.getWagon( mirrorId );
                RepositorySettings mirrorSettings = wagonManager.getRepositorySettings( mirrorId );

                if ( mirrorSettings.isBlacklisted() )
                {
                    logger.debug( "Skipping blacklisted mirror [" + mirrorId + "]" );
                    continue;
                }

                if ( !mirrorSettings.isEnabled() )
                {
                    logger.debug( "Skipping disabled mirror [" + mirrorId + "]" );
                    continue;
                }

                copyConfiguration( mirrorWagon, this.impl );
                mirrors.add( mirrorWagon );
            }
            catch ( UnsupportedProtocolException e )
            {
                blacklistMirror( mirrorId );
                logger.warn( "Unable to use mirror [" + mirrorId + "]: " + e.getMessage(), e );
            }
            catch ( RepositoryNotFoundException e )
            {
                blacklistMirror( mirrorId );
                logger.warn( "Unable to use mirror [" + mirrorId + "]: " + e.getMessage(), e );
            }
            catch ( WagonConfigurationException e )
            {
                blacklistMirror( mirrorId );
                logger.warn( "Unable to use mirror [" + mirrorId + "]: " + e.getMessage(), e );
            }
            catch ( NotOnlineException e )
            {
                logger.warn( "Unable to use mirror [" + mirrorId + "]: " + e.getMessage(), e );
            }
        }

        mirrorsIterator = mirrors.iterator();
    }

    private boolean hasMirror()
    {
        return mirrorsIterator.hasNext();
    }

    private void nextMirror()
        throws ExhaustedMirrorsException
    {
        Wagon previous = this.impl;

        boolean wasConnected = false;
        if ( previous != null )
        {
            previous.isConnected();
        }

        if ( !hasMirror() )
        {
            this.impl = null;
            throw new ExhaustedMirrorsException( "Exhausted all mirrors for repository [" + mirrorOfRepoId + "]." );
        }

        this.impl = (Wagon) mirrorsIterator.next();

        if ( wasConnected )
        {
            disconnectQuietly( previous );
            try
            {
                this.impl.connect();
            }
            catch ( ConnectionException e )
            {
                // Whoops! Gotta try the next one now.
                logger.warn( "Unable to connect to mirror [" + this.impl.getRepository().getId() + "]: "
                    + e.getMessage() );
                nextMirror();
            }
            catch ( AuthenticationException e )
            {
                // Whoops! Gotta try the next one now.
                logger.warn( "Unable to authenticate to mirror [" + this.impl.getRepository().getId() + "]: "
                    + e.getMessage() );
                nextMirror();
            }
        }
    }

    private void disconnectQuietly( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            logger.debug( "Unable to properly disconnect wagon: " + wagon, e );
        }
    }

    private void blacklistMirror( String mirrorId )
    {
        RepositorySettings settings = wagonManager.getRepositorySettings( mirrorId );
        Repository repo = wagonManager.getRepository( mirrorId );
        settings.setBlacklisted( true );
        logger.warn( "The mirror [" + mirrorId + "] - " + repo.getUrl()
            + " has been blacklisted, and will no longer be attempted." );
    }

    protected Wagon getCurrentWagon()
    {
        return this.impl;
    }

    public List getMirrors()
    {
        return mirrors;
    }

    /**
     * @deprecated Provided for backward compatibility. Use {@link MirroredWagon#connect()} instead.
     */
    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
    }
}
