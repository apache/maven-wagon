package org.apache.maven.wagon;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.wagon.artifact.Artifact;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;

public interface Wagon
{
    String ROLE = Wagon.class.getName();

    // ----------------------------------------------------------------------
    // File/File handling
    // ----------------------------------------------------------------------

    void get( String source, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    // ----------------------------------------------------------------------
    // Artifact/File handling
    // ----------------------------------------------------------------------

    void get( Artifact artifact, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;

    void put( File source, Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;


    // ----------------------------------------------------------------------
    // /Stream handling
    // ----------------------------------------------------------------------

    

    Repository getRepository();

    // ----------------------------------------------------------------------
    // Connection/Disconnection
    // ----------------------------------------------------------------------

    void connect( Repository source )
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
}
