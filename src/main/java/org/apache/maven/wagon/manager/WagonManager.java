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

import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.manager.stats.WagonStatistics;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import java.util.Map;
import java.util.Set;

/**
 * WagonManager 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface WagonManager
{
    public static final String ROLE = WagonManager.class.getName();

    /**
     * Add a Proxy to the underlying manager.
     * 
     * @param protocol the protocol to map the proxy to.
     * @param proxyInfo the proxy information to store
     * @throws UnsupportedProtocolException 
     */
    void addProxy( String protocol, ProxyInfo proxyInfo );

    /**
     * Add a Proxy to the underlying manager.
     * 
     * @param protocol the protocol to map the proxy to.
     * @param host the hostname of the proxy.
     * @param port the port of the proxy.
     * @param username the optional username for the proxy.
     * @param password the optional password for the proxy.
     * @param nonProxyHosts the list of nonProxyHosts (comma delimited)
     */
    void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts );

    /**
     * Add a TransferListener to the underlying manager.
     * 
     * @param listener the transfer listener to add.
     */
    void addTransferListener( TransferListener listener );

    /**
     * List of protocol list.
     * 
     * @return a list of Strings representing the available protocols.
     */
    Set getProtocols();

    /**
     * Get a Map of {@link ProxyInfo} objects using the protocol as the key.
     * @return
     */
    Map /*<String, ProxyInfo>*/getProxies();

    /**
     * Get {@link ProxyInfo} object for specified protocol.
     * 
     * @param protocol
     * @return
     */
    ProxyInfo getProxy( String protocol );

    /**
     * Get a Wagon provider that understands the protocol passed as argument.
     * It doesn't configure the Wagon.
     * 
     * NOTE: This implementation creates a raw Wagon object, regardless of any registered Repositories.
     *
     * @param protocol the protocol the {@link Wagon} will handle
     * @return the {@link Wagon} instance able to handle the protocol provided
     * @throws UnsupportedProtocolException if there is no provider able to handle the protocol
     */
    Wagon getRawWagon( String protocol )
        throws UnsupportedProtocolException;

    /**
     * Get the RepositoryBinding for the specific repository id.
     * 
     * @param id the repository id to get the bindings for.
     * @return the {@link RepositoryBinding} for the id.
     * @throws RepositoryNotFoundException
     */
    RepositoryBinding getRepositoryBindings( String id )
        throws RepositoryNotFoundException;

    /**
     * Get a Wagon provider that understands the protocol defined within the specified Repository.
     * It doesn't configure the Wagon.
     *
     * @param repository the {@link Repository} the {@link Wagon} will handle
     * @return the {@link Wagon} instance able to handle the protocol provided
     * @throws UnsupportedProtocolException if there is no provider able to handle the protocol
     */
    Wagon getWagon( RepositoryBinding repository )
        throws UnsupportedProtocolException;

    /**
     * Get a Wagon provider that understands the protocol defined within the specified Repository.
     * It doesn't configure the Wagon.
     *
     * @param repository the {@link Repository} the {@link Wagon} will handle
     * @return the {@link Wagon} instance able to handle the protocol provided
     * @throws UnsupportedProtocolException if there is no provider able to handle the protocol
     * @throws RepositoryNotFoundException if the specified id cannot be found.
     */
    Wagon getWagon( String repositoryId )
        throws UnsupportedProtocolException, RepositoryNotFoundException;
    
    /**
     * Release a previously used wagon.
     * 
     * All tracking and statistics for this wagon are stopped.
     * 
     * @param wagon the wagon to release.
     */
    void releaseWagon(Wagon wagon);

    /**
     * Flag indicating that the interactivity of wagons.
     *  
     * @return true if interactive prompts are allowed, false if all transfers are non-interactive.
     */
    boolean isInteractive();

    /**
     * Flag indicating if the manager is online or offline.
     * 
     * @return true if the manager is online.
     */
    boolean isOnline();

    /**
     * Remove a specific TransferListener from WagonManager.
     * 
     * @param listener the listener to remove.
     */
    void removeTransferListener( TransferListener listener );

    /**
     * Flag that dictates the interactivity of the wagon providers.
     * 
     * True means that the wagon providers can prompt the user for input (such as usernames and passwords), false
     * means that the wagon operation is to be non-interactive.
     * 
     * @param interactive true enables the interactive nature of the wagons.
     */
    void setInteractive( boolean interactive );

    /**
     * Flag indicating if the manager is to treat requests for Wagon implementations in a online mode or not.
     * 
     * @param online true enables the online nature of the wagon manager.
     */
    void setOnline( boolean online );

    /**
     * Add a {@link Repository} to the underlying list of Wagon Repositories that are used as wagon sources. 
     * 
     * @param repository the repository to add, the {@link Repository#getId()} is used as a repository key.
     */
    public void addRepository( Repository repository );

    /**
     * Get a Map of {@link RepositoryBinding} objects being tracked.
     * 
     * @return Map of {@link Repository} keys to {@link RepositoryBinding} objects.
     */
    public Map getRepositories();

    /**
     * Get the Statistics gathered so far.
     * 
     * @return the statistics gathered so far.
     */
    public WagonStatistics getStatistics();

    /**
     * Remove the repository from the list of repositories being tracked.
     *  
     * @param repositoryBinding the repository to remove.
     * @throws RepositoryNotFoundException
     */
    public void removeRepository( RepositoryBinding repositoryBinding )
        throws RepositoryNotFoundException;

    /**
     * Remove the repository from the list of repositories being tracked.
     *  
     * @param repoId the repository to remove.
     * @throws RepositoryNotFoundException
     */
    public void removeRepository( String repoId )
        throws RepositoryNotFoundException;
}
