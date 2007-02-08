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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
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
import java.util.HashSet;
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

    private PlexusContainer container;

    private Set protocols = new HashSet();

    private Map proxies = new HashMap();

    private Map repositories = new HashMap();

    private List listeners = new ArrayList();

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

        RepositoryBinding repoBinding = new RepositoryBinding( id, repository );
        repositories.put( id, repoBinding );
    }

    public void addTransferListener( TransferListener listener )
    {
        listeners.add( listener );
    }

    public Set getProtocols()
    {
        return Collections.unmodifiableSet( protocols );
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

    public Wagon getRawWagon( String protocol )
        throws UnsupportedProtocolException
    {
        if ( StringUtils.isEmpty( protocol ) )
        {
            throw new IllegalArgumentException( Messages.getString( "wagon.manager.protocol.may.not.be.empty" ) ); //$NON-NLS-1$
        }

        if ( !protocols.contains( protocol ) )
        {
            throw new UnsupportedProtocolException( Messages
                .getString( "wagon.manager.protocol.not.supported", protocol ) ); //$NON-NLS-1$
        }

        try
        {
            Wagon wagon = (Wagon) container.lookup( Wagon.ROLE, protocol );
            wagon.addSessionListener( stats );
            wagon.addTransferListener( stats );
            wagon.setProxyInfo( getProxy( protocol ) );

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

    public RepositoryBinding getRepositoryBindings( String id )
        throws RepositoryNotFoundException
    {
        if ( !repositories.containsKey( id ) )
        {
            throw new RepositoryNotFoundException( Messages.getString( "wagon.manager.repository.id.not.found", id ) ); //$NON-NLS-1$
        }

        return (RepositoryBinding) repositories.get( id );
    }

    public Wagon getWagon( RepositoryBinding repositoryBinding )
        throws UnsupportedProtocolException
    {
        String protocol = repositoryBinding.getRepository().getProtocol();
        Wagon wagon = getRawWagon( protocol );

        wagon.setRepository( repositoryBinding.getRepository() );
        wagon.setAuthenticationInfo( repositoryBinding.getAuthenticationInfo() );
        wagon.setProxyInfo( repositoryBinding.getProxyInfo() );

        return wagon;
    }

    public Wagon getWagon( String repositoryId )
        throws UnsupportedProtocolException, RepositoryNotFoundException
    {
        RepositoryBinding repositoryBinding = getRepositoryBindings( repositoryId );
        return getWagon( repositoryBinding );
    }

    public void removeRepository( RepositoryBinding repositoryBinding )
        throws RepositoryNotFoundException
    {
        removeRepository( repositoryBinding.getId() );
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

    public void initialize()
        throws InitializationException
    {
        List availableWagonProviders;
        try
        {
            availableWagonProviders = container.lookupList( Wagon.ROLE );
            Iterator it = availableWagonProviders.iterator();

            while ( it.hasNext() )
            {
                Wagon wagon = (Wagon) it.next();
                protocols.add( wagon.getProtocol() );
            }
        }
        catch ( ComponentLookupException e )
        {
            throw new InitializationException( "Unable to find list of available Wagons.", e );
        }
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

            try
            {
                container.release( wagon );
            }
            catch ( ComponentLifecycleException e )
            {
                getLogger().warn( "Unable to release wagon (" + wagon.getClass().getName() + ")", e );
            }
        }
    }
}
