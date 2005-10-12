package org.apache.maven.wagon;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;

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

    void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    boolean supportsDirectoryCopy();

    Repository getRepository();

    // ----------------------------------------------------------------------
    // Connection/Disconnection
    // ----------------------------------------------------------------------

    void connect( Repository source )
        throws ConnectionException, AuthenticationException;

    void connect( Repository source, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException;

    void connect( Repository source, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException;

    void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException;

    void openConnection()
        throws ConnectionException, AuthenticationException;

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

    boolean isInteractive();

    void setInteractive( boolean interactive );
}
