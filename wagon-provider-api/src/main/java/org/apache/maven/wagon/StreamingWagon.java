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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.wagon.authorization.AuthorizationException;

public interface StreamingWagon
    extends Wagon
{
    /**
     * Downloads specified resource from the repository to given output stream.
     * 
     * @param resourceName
     * @param destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException 
     * @throws AuthorizationException
     */
    void getToStream( String resourceName, OutputStream stream )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException;

    /**
     * Downloads specified resource from the repository if it was modified since specified date. The date is measured in
     * milliseconds, between the current time and midnight, January 1, 1970 UTC and aligned to GMT timezone.
     * 
     * @param resourceName
     * @param destination
     * @param timestamp
     * @return <code>true</code> if newer resource has been downloaded, <code>false</code> if resource in the
     *         repository is older or has the same age.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException 
     * @throws AuthorizationException
     */
    boolean getIfNewerToStream( String resourceName, OutputStream stream, long timestamp )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException;

    /**
     * Copy from a local input stream to remote.
     * 
     * @param source the local file
     * @param destination the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void putFromStream( InputStream stream, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    /**
     * Copy from a local input stream to remote.
     * 
     * @param source the local file
     * @param destination the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    void putFromStream( InputStream stream, String destination, long contentLength, long lastModified )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;
}
