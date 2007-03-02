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
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.manager.stats.WagonStatistics;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DefaultWagonManager 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.wagon.manager.WagonManager"
 *   role-hint="default"
 */
public class DefaultWagonManager
    extends AbstractLogEnabled
    implements WagonManager, Initializable, Contextualizable
{
    /**
     * @plexus.configuration default-value="true" 
     */
    private boolean interactive = true;

    /**
     * @plexus.configuration default-value="true" 
     */
    private boolean online = true;

    /**
     * @plexus.requirement
     */
    private WagonStatistics stats;

    /**
     * @plexus.requirement role-hint="basic"
     */
    private ComponentConfigurator componentConfigurator;

    private PlexusContainer container;

    /**
     * Map of wagon hints to PlexusContainers.
     */
    private Map availableWagons = new HashMap();

    private Map proxies = new HashMap();

    private Map repositories = new HashMap();

    private Map settings = new HashMap();

    private List listeners = new ArrayList();

    // this is a reverse-mapping of className to hint, to make it simpler to release legacy wagons. 
    private Map availableWagonHintsByClassName = new HashMap();

    public void addProxy( String protocol, ProxyInfo proxyInfo )
    {
        if ( StringUtils.isEmpty( protocol ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.protocol.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        if ( proxyInfo == null )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.proxy.information.may.not.be.null" ) ); //$NON-NLS-1$
        }

        if ( proxies.containsKey( protocol ) )
        {
            getLogger().debug( Messages.getString( "wagon.manager.warn.overwrite.proxy.mapping", protocol ) ); //$NON-NLS-1$
        }

        proxies.put( protocol, proxyInfo );
    }

    public void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setType( protocol );
        proxyInfo.setPort( port );
        proxyInfo.setNonProxyHosts( nonProxyHosts );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        addProxy( protocol, proxyInfo );
    }

    public void addRepository( Repository repository )
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.repository.object.may.not.be.null" ) ); //$NON-NLS-1$
        }

        if ( StringUtils.isEmpty( repository.getId() ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.repository.id.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        String id = repository.getId();

        if ( repositories.containsKey( id ) )
        {
            getLogger().debug( Messages.getString( "wagon.manager.warn.overwrite.repository", id ) ); //$NON-NLS-1$
        }

        // Store repository, configure later.
        repositories.put( id, repository );
    }

    public void addRepositoryMirror( String repositoryIdToMirror, String mirrorId, String urlOfMirror )
    {
        // TODO: Add support for WILDCARD "*" repositoryIdToMirror

        if ( StringUtils.isEmpty( mirrorId ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.repository.mirror.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        if ( StringUtils.isEmpty( repositoryIdToMirror ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.repository.id.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        if ( StringUtils.equals( repositoryIdToMirror, mirrorId ) )
        {
            throw new IllegalArgumentException( Messages
                .getString( "wagon.manager.repository.mirror.may.not.equal.repository", mirrorId ) ); //$NON-NLS-1$
        }
        
        // repositoryIdToMirror can't point to a mirror repository.
        RepositorySettings repoSettings = getRepositorySettings( repositoryIdToMirror );
        if ( repoSettings.isMirror() )
        {
            throw new IllegalArgumentException(
                                                "You can't mirror a mirror.  Requested to mirror repository [{0}], which itself is already defined as mirror to [{1}]." );
        }

        // Save mirror id into repository settings for repo.
        repoSettings.addMirror( mirrorId );

        // Setup the mirror settings.
        RepositorySettings mirrorSettings = getRepositorySettings( mirrorId );
        mirrorSettings.setMirrorOf( repositoryIdToMirror );

        // Create the mirror repository.
        Repository mirrorRepository = new Repository( mirrorId, urlOfMirror );

        // Store mirror repository, configure later.
        repositories.put( mirrorId, mirrorRepository );
    }

    public Repository getRepository( String repositoryId )
    {
        Repository repository = (Repository) repositories.get( repositoryId );
        RepositorySettings settings = getRepositorySettings( repository.getId() );

        repository.setPermissions( settings.getPermissions() );

        return repository;
    }

    public void addTransferListener( TransferListener listener )
    {
        listeners.add( listener );
    }

    public Set getProtocols()
    {
        return Collections.unmodifiableSet( availableWagons.keySet() );
    }

    public Map getProxies()
    {
        return proxies;
    }

    public ProxyInfo getProxy( String protocol )
    {
        if ( StringUtils.isEmpty( protocol ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.protocol.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        return (ProxyInfo) proxies.get( protocol );
    }

    private Wagon getRawWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( StringUtils.isEmpty( protocol ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.protocol.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        if ( !availableWagons.containsKey( protocol ) )
        {
            throw new UnsupportedProtocolException( Messages
                .getString( "wagon.manager.protocol.not.supported", protocol ) ); //$NON-NLS-1$
        }

        try
        {
            Wagon wagon = (Wagon) getWagonContainer( protocol ).lookup( Wagon.ROLE, protocol );
            wagon.addSessionListener( stats );
            wagon.addTransferListener( stats );

            for ( Iterator i = listeners.iterator(); i.hasNext(); )
            {
                TransferListener transferListener = (TransferListener) i.next();

                wagon.addTransferListener( transferListener );
            }

            wagon.setProxyInfo( getProxy( protocol ) );
            wagon.setInteractive( isInteractive() );

            return wagon;
        }
        catch ( ComponentLookupException e )
        {
            throw new UnsupportedProtocolException( "Unable to create " + protocol + " Wagon.", e );
        }
    }

    public Map getRepositories()
    {
        return Collections.unmodifiableMap( repositories );
    }

    public RepositorySettings getRepositorySettings( String id )
    {
        RepositorySettings ret = (RepositorySettings) settings.get( id );

        if ( ret == null )
        {
            ret = new RepositorySettings( id );
            settings.put( id, ret );
        }

        return ret;
    }

    public Wagon getWagon( String repositoryId )
        throws WagonConfigurationException, UnsupportedProtocolException, RepositoryNotFoundException, 
        NotOnlineException
    {
        RepositorySettings settings = getRepositorySettings( repositoryId );
        
        String fetchId = repositoryId;
        boolean hasMirror = settings.hasMirror();
        Wagon wagon = null;

        if ( hasMirror )
        {
            // Fetch the first mirror id.
            fetchId = (String) settings.getMirrors().get( 0 );
        }

        Repository repository = getRepository( fetchId );
        
        String protocol = repository.getProtocol();
        
        if ( !protocol.equals( "file" ) && !isOnline() )
        {
            throw new NotOnlineException( "Unable to honor request for " + protocol
                + " Wagon, as WagonManager has been flagged as offline." );
            // TODO: Return NullWagon instead?
        }

        // Get Wagon
        if ( hasMirror )
        {
            Wagon subwagon = getRawWagon( protocol );
            wagon = new MirroredWagon( this, repositoryId, subwagon, getLogger() );
        }
        else
        {
            wagon = getRawWagon( protocol );
        }

        // Configure Wagon; make a defensive copy of the repository, in case the wagon alters it.
        wagon.setRepository( new Repository( repository.getId(), repository.getUrl() ) );
        
        wagon.setAuthenticationInfo( settings.getAuthentication() );

        ProxyInfo proxy = getProxy( protocol );
        if ( proxy != null )
        {
            wagon.setProxyInfo( proxy );
        }

        if ( settings.getConfiguration() != null )
        {
            try
            {
                componentConfigurator.configureComponent( wagon, settings.getConfiguration(), (ClassRealm) container
                    .getContainerRealm() );
            }
            catch ( ComponentConfigurationException e )
            {
                throw new WagonConfigurationException( "Unable to configure wagon [" + repositoryId
                    + "] from defined configuration.", e );
            }
        }
        
        // Return Wagon
        return wagon;
    }

    public void removeRepository( String repoId )
        throws RepositoryNotFoundException
    {
        repositories.remove( repoId );
    }

    public void removeTransferListener( TransferListener listener )
    {
        listeners.add( listener );
    }

    public boolean isInteractive()
    {
        return interactive;
    }

    public void setInteractive( boolean interactive )
    {
        this.interactive = interactive;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setOnline( boolean online )
    {
        this.online = online;
    }

    public PlexusContainer getWagonContainer( String protocol )
        throws UnsupportedProtocolException
    {
        PlexusContainer wagonContainer = (PlexusContainer) this.availableWagons.get( protocol );
        if ( wagonContainer == null )
        {
            throw new UnsupportedProtocolException( "Unable to find wagon for protocol [" + protocol + "]" );
        }

        return wagonContainer;
    }

    public void initialize()
        throws InitializationException
    {
        try
        {
            registerAvailableWagons( container );
        }
        catch ( ComponentLookupException e )
        {
            throw new InitializationException( "Unable to initialize: " + e.getMessage(), e );
        }
    }

    public void registerAvailableWagons( PlexusContainer searchContainer )
        throws ComponentLookupException
    {
        Map discoveredWagonProviders;
        try
        {
            discoveredWagonProviders = searchContainer.lookupMap( Wagon.ROLE );
            Iterator it = discoveredWagonProviders.entrySet().iterator();

            while ( it.hasNext() )
            {
                Map.Entry entry = (Map.Entry) it.next();
                
                String wagonHint = (String) entry.getKey();
                Object wagonInstance = entry.getValue();
                
                getLogger().debug( "Registering wagon for: " + wagonHint );
                
                Wagon wagon = (Wagon) searchContainer.lookup( Wagon.ROLE, wagonHint );
                
                // TODO: Remove this null check once we're clear of backward compat issues with
                // wagons <= 1.0-beta-2.
                if ( wagon.getProtocol() != null && !StringUtils.equals( wagon.getProtocol(), wagonHint ) )
                {
                    throw new IllegalStateException( "Plexus Hint [" + wagonHint + "] and Wagon.getProtocol() ["
                        + wagon.getProtocol() + "] do not agree." );
                }

                // TODO: need to address what to do when an extension container becomes unavailable.
                // TODO: need to figure out how to remove a container.
                // TODO: need to restore parent wagons if extension container overwrites them.

                this.availableWagons.put( wagonHint, searchContainer );
                this.availableWagonHintsByClassName.put( wagonInstance.getClass().getName(), wagonHint );
            }
        }
        catch ( ComponentLookupException e )
        {
            throw new ComponentLookupException( "Unable to find list of available Wagons.", e );
        }
    }

    public void registerExtensionContainer( PlexusContainer extContainer )
        throws ComponentLookupException
    {
        registerAvailableWagons( extContainer );
    }

    public WagonStatistics getStatistics()
    {
        return stats;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void releaseWagon( Wagon wagon )
    {
        // Safe to do this here, since WagonManager is the only place a MirroredWagon is created.
        if ( wagon instanceof MirroredWagon )
        {
            wagon = ( (MirroredWagon) wagon ).getCurrentWagon();
        }

        if ( wagon != null )
        {
            if ( wagon.isConnected() )
            {
                try
                {
                    wagon.disconnect();
                }
                catch ( ConnectionException e )
                {
                    getLogger().warn( "Unable to disconnect wagon (" + wagon.getClass().getName() + ")", e );
                }
            }

            String hint = findWagonHint( wagon );
            
            if ( hint != null )
            {
                try
                {
                    getWagonContainer( hint ).release( wagon );
                }
                catch ( ComponentLifecycleException e )
                {
                    getLogger().warn( "Unable to release wagon (" + wagon.getClass().getName() + ")", e );
                }
                catch ( UnsupportedProtocolException e )
                {
                    getLogger().warn( "Unable to release wagon (" + wagon.getClass().getName() + "): " + e.getMessage(), e );
                }
            }
        }
    }

    private String findWagonHint( Wagon wagon )
    {
        String result = null;
        
        result = wagon.getProtocol();
        
        if ( result == null )
        {
            getLogger().debug( "Looking for key of wagon-class: " + wagon.getClass().getName() + " in availableWagons:\n" + String.valueOf( availableWagonHintsByClassName ).replace( ',', '\n' ) );
            
            result = (String) availableWagonHintsByClassName.get( wagon.getClass().getName() );
        }
        
        getLogger().debug( "Found wagon-class: " + wagon.getClass().getName() + " under key: " + result );
        
        return result;
    }
}
