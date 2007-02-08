package org.apache.maven.wagon;

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

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.util.List;

public interface Wagon
{
    String ROLE = Wagon.class.getName();

    // ----------------------------------------------------------------------
    // File/File handling
    // ----------------------------------------------------------------------

    /**
     * Downloads specified resource from the repository to given file.
     *
     * @param resourceName
     * @param destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Downloads specified resource from the repository
     * if it was modfified since specified date.
     * The date is measured in milliseconds, between the current time and midnight, January 1, 1970 UTC
     * and aliged to GMT timezone.
     *
     * @param resourceName
     * @param destination
     * @param timestamp
     * @return <code>true</code> if newer resource has been downloaded, <code>false</code> if resource
     *         in the repository is older or has the same age.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     * @todo michal: I have to learn more about timezones!
     * Specifically how to convert time for UTC to time for GMT and if such conversioin is needed.
     */
    boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Copy a file from local system to remote
     * @param source the local file
     * @param destination the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Copy a directory from local system to remote
     * @param sourceDirectory the local directory
     * @param destinationDirectory the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Check if a remote resource exists
     * 
     * @param resourceName
     * @return whether the resource exists or not 
     * @throws TransferFailedException if there's an error trying to access the remote side
     * @throws AuthorizationException if not authorized to verify the existence of the resource
     */
    boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException;

    /**
     * <p>
     * Returns a {@link List} of strings naming the files and directories in the directory denoted by
     * this abstract pathname.
     * </p>
     * <p>
     * If this abstract pathname does not denote a directory, or does not exist, then this method throws
     * {@link ResourceDoesNotExistException}.
     * Otherwise a {@link List} of strings is returned, one for each file or directory in the directory.
     * Names denoting the directory itself and the directory's parent directory are not included in
     * the result. Each string is a file name rather than a complete path.
     * </p>
     * <p>
     * There is no guarantee that the name strings in the resulting list will appear in any specific
     * order; they are not, in particular, guaranteed to appear in alphabetical order.
     * </p> 
     * 
     * @param destinationDirectory directory to list contents of
     * @return A {@link List} of strings naming the files and directories in the directory denoted by
     * this abstract pathname. The {@link List} will be empty if the directory is empty.
     * @throws TransferFailedException if there's an error trying to access the remote side
     * @throws ResourceDoesNotExistException if destinationDirectory does not exist or is not a directory
     * @throws AuthorizationException if not authorized to list the contents of the directory
     */
    List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    // ----------------------------------------------------------------------
    // Settings / Configuration
    // ----------------------------------------------------------------------

    /**
     * Get the protocol for this wagon.
     * 
     * NOTE: This requires that the wagon only support 1 protocol.
     * 
     * @return the protocol supported by this wagon.
     */
    String getProtocol();

    /**
     * Flag indicating if this wagon supports directory copy operations.
     * 
     * @return whether if this wagon supports directory operations
     */
    boolean supportsDirectoryCopy();

    boolean isInteractive();

    void setInteractive( boolean interactive );

    Repository getRepository();
    
    void setRepository( Repository repository );

    AuthenticationInfo getAuthenticationInfo();

    void setAuthenticationInfo( AuthenticationInfo authnInfo );

    ProxyInfo getProxyInfo();

    void setProxyInfo( ProxyInfo proxyInfo );

    // ----------------------------------------------------------------------
    // Connection/Disconnection
    // ----------------------------------------------------------------------

    /**
     * Initiate the connection to the Repository.
     * 
     * @see #getAuthenticationInfo()
     * @see #getProxyInfo()
     * @see #getRepository()
     */
    void connect()
        throws ConnectionException, AuthenticationException;
    
    /**
     * Flag indicating if the wagon is currently connected (or not)
     * 
     * @return true if wagon is currently connected.
     */
    boolean isConnected();
    
    /**
     * @deprecated Replaced by calls to {@link #connect()}, uses {@link #getRepository()}
     */
    void connect( Repository source )
        throws ConnectionException, AuthenticationException;

    /**
     * @deprecated Replaced by calls to {@link #connect()}, uses {@link #getRepository()} and 
     *   {@link #getProxyInfo()}
     */
    void connect( Repository source, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException;

    /**
     * @deprecated Replaced by calls to {@link #connect()}, uses {@link #getRepository()} and 
     *   {@link #getAuthenticationInfo()}
     */
    void connect( Repository source, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException;

    /**
     * @deprecated Replaced by calls to {@link #connect()}, uses {@link #getRepository()} and 
     *   {@link #getAuthenticationInfo()} and {@link #getProxyInfo()}
     */
    void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException;

    /**
     * Close the connection. 
     * 
     * @throws ConnectionException
     */
    void disconnect()
        throws ConnectionException;

    // ----------------------------------------------------------------------
    //  Session listener
    // ----------------------------------------------------------------------

    void addSessionListener( SessionListener listener );

    void removeSessionListener( SessionListener listener );

    boolean hasSessionListener( SessionListener listener );

    // ----------------------------------------------------------------------
    // Transfer listener
    // ----------------------------------------------------------------------

    void addTransferListener( TransferListener listener );

    void removeTransferListener( TransferListener listener );

    boolean hasTransferListener( TransferListener listener );

}
