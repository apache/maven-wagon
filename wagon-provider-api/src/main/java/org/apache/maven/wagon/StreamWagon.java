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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;



public abstract class StreamWagon
    extends AbstractWagon
{
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public abstract InputStream getInputStream( String resource )
        throws TransferFailedException, ResourceDoesNotExistException;

    public abstract OutputStream getOutputStream( String resource )
        throws TransferFailedException;

    public abstract void openConnection()
        throws ConnectionException, AuthenticationException;

    public abstract void closeConnection()
        throws ConnectionException;

    // ----------------------------------------------------------------------
    // We take the artifact and create the resource from that so we can
    // just hand it off to get(String,File) below. So we might get an
    // Artifact where:
    //
    // groupId = maven
    // artifactId = wagon-api
    // type = pom
    // extension = pom
    // version = 1.0
    // layout = ${groupId}/{$type}s/${artifactId}-${version}.${extension}
    //
    // so the resource ends up looking like:
    //
    // maven/poms/wagon-api-1.0.pom
    //
    // ----------------------------------------------------------------------

    public void get( Artifact artifact, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        get( artifactPath( artifact ), destination );
    }

    public void put( File source, Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        put( source, artifactPath( artifact ) );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void get( String resource, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        
        if ( destination == null )
        {
           throw new ResourceDoesNotExistException( "get: Destination cannot be null" );
        }
        
        
        if ( !destination.getParentFile().exists() )
        {
            if ( !destination.getParentFile().mkdirs() )
            {
                throw new TransferFailedException( "Specified destination directory cannot be created: " + destination.getParentFile() );
            }
        }

        FileOutputStream os = null;

        try
        {
            os = new FileOutputStream( destination );
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Cannot write to specified destination: " + destination.getAbsolutePath() );
        }

        InputStream is;

        try
        {
            is = getInputStream( resource );
        }
        catch ( Exception e )
        {
            String msg = "Cannot create input stream for resource: " + resource;
            
            throw new TransferFailedException( msg, e );
        }

        getTransfer( resource, is, os );
    }

    // source doesn't exist exception
    public void put( File source, String resource )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !source.exists() )
        {
            throw new TransferFailedException( "Specified source file does not exist: " + source );
        }

        FileInputStream is = null;

        try
        {
            is = new FileInputStream( source );
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Cannot read from specified source: " + source.getAbsolutePath() );
        }

        OutputStream os;

        try
        {
            os = getOutputStream( resource );
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( "Cannot create input stream: ", e );
        }

        putTransfer( resource, is, os );
    }
}
