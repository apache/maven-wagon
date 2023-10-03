/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.wagon;

import java.io.File;
import java.util.List;

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

/**
 *
 */
public interface Wagon {
    String ROLE = Wagon.class.getName();

    /**
     * default 60s approximately 1 minute
     */
    int DEFAULT_CONNECTION_TIMEOUT = 60000;

    /**
     * default 1800s approximately 30 minutes
     *
     * @since 2.2
     */
    int DEFAULT_READ_TIMEOUT = 1800000;

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
    void get(String resourceName, File destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Downloads specified resource from the repository
     * if it was modified since specified date.
     * The date is measured in milliseconds, between the current time and midnight, January 1, 1970 UTC
     * and aligned to GMT timezone.
     *
     * @param resourceName
     * @param destination
     * @param timestamp
     * @return <code>true</code> if newer resource has been downloaded, <code>false</code> if resource
     *         in the repository is older or has the same age.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    boolean getIfNewer(String resourceName, File destination, long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Copy a file from local system to remote
     *
     * @param source      the local file
     * @param destination the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void put(File source, String destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Copy a directory from local system to remote
     *
     * @param sourceDirectory      the local directory
     * @param destinationDirectory the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void putDirectory(File sourceDirectory, String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Check if a remote resource exists
     *
     * @param resourceName
     * @return whether the resource exists or not
     * @throws TransferFailedException if there's an error trying to access the remote side
     * @throws AuthorizationException  if not authorized to verify the existence of the resource
     */
    boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException;

    /**
     * <p/>
     * Returns a {@link List} of strings naming the files and directories in the directory denoted by
     * this abstract pathname.
     * </p>
     * <p/>
     * If this abstract pathname does not denote a directory, or does not exist, then this method throws
     * {@link ResourceDoesNotExistException}.
     * Otherwise a {@link List} of strings is returned, one for each file or directory in the directory.
     * Names denoting the directory itself and the directory's parent directory are not included in
     * the result. Each string is a file name rather than a complete path.
     * </p>
     * <p/>
     * There is no guarantee that the name strings in the resulting list will appear in any specific
     * order; they are not, in particular, guaranteed to appear in alphabetical order.
     * </p>
     *
     * @param destinationDirectory directory to list contents of
     * @return A {@link List} of strings naming the files and directories in the directory denoted by
     *         this abstract pathname. The {@link List} will be empty if the directory is empty.
     * @throws TransferFailedException       if there's an error trying to access the remote side
     * @throws ResourceDoesNotExistException if destinationDirectory does not exist or is not a directory
     * @throws AuthorizationException        if not authorized to list the contents of the directory
     */
    List<String> getFileList(String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Flag indicating if this wagon supports directory copy operations.
     *
     * @return whether if this wagon supports directory operations
     */
    boolean supportsDirectoryCopy();

    Repository getRepository();

    // ----------------------------------------------------------------------
    // Connection/Disconnection
    // ----------------------------------------------------------------------

    /**
     * Initiate the connection to the repository.
     *
     * @param source the repository to connect to
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source) throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @param source the repository to connect to
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @param source the repository to connect to
     * @param proxyInfoProvider  the provider to obtain a network proxy to use to connect to the remote repository
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @param source             the repository to connect to
     * @param authenticationInfo authentication credentials for connecting
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source, AuthenticationInfo authenticationInfo)
            throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @param source             the repository to connect to
     * @param authenticationInfo authentication credentials for connecting
     * @param proxyInfo          the network proxy to use to connect to the remote repository
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo)
            throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @param source             the repository to connect to
     * @param authenticationInfo authentication credentials for connecting
     * @param proxyInfoProvider  the provider to obtain a network proxy to use to connect to the remote repository
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if the credentials for connecting are not sufficient
     */
    void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException;

    /**
     * Initiate the connection to the repository.
     *
     * @throws ConnectionException if there is a problem connecting
     * @throws org.apache.maven.wagon.authentication.AuthenticationException
     *                             if ther credentials for connecting are not sufficient
     * @todo delegate this to a truly internal connection method
     * @deprecated connect using the {@link #connect(org.apache.maven.wagon.repository.Repository)} or related methods
     *             - this is an internal method
     */
    void openConnection() throws ConnectionException, AuthenticationException;

    /**
     * Disconnect from the repository.
     *
     * @throws ConnectionException if there is a problem disconnecting
     */
    void disconnect() throws ConnectionException;

    /**
     * Set the connection timeout limit in milliseconds
     */
    void setTimeout(int timeoutValue);

    /**
     * Get the connection timeout limit in milliseconds
     */
    int getTimeout();

    /**
     * Set the read timeout limit in milliseconds
     * @since 2.2
     */
    void setReadTimeout(int timeoutValue);

    /**
     * Get the read timeout limit in milliseconds
     * @since 2.2
     */
    int getReadTimeout();

    // ----------------------------------------------------------------------
    //  Session listener
    // ----------------------------------------------------------------------

    void addSessionListener(SessionListener listener);

    void removeSessionListener(SessionListener listener);

    boolean hasSessionListener(SessionListener listener);

    // ----------------------------------------------------------------------
    // Transfer listener
    // ----------------------------------------------------------------------

    void addTransferListener(TransferListener listener);

    void removeTransferListener(TransferListener listener);

    boolean hasTransferListener(TransferListener listener);

    boolean isInteractive();

    void setInteractive(boolean interactive);
}
