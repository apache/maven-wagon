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
    public abstract InputStream getInputStream( Artifact artifact )
        throws Exception;

    public abstract OutputStream getOutputStream( Artifact artifact )
        throws Exception;

    public abstract void openConnection()
        throws Exception;

    public abstract void closeConnection()
        throws Exception;

    //!! destination directory doesn't exist exception
    public void get( Artifact artifact, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String resource = artifactUrl( artifact );

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
            // This is taken care of above, if we cannot create the
            // parent directory then an exception will be thrown above.
        }

        InputStream is;

        try
        {
            is = getInputStream( artifact );
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( "Cannot create input stream: ", e );
        }

        getTransfer( resource, is, os );
    }

    // source doesn't exist exception
    public void put( File source, Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String resource = artifactUrl( artifact );

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
            // This is taken care of above, if we cannot create the
            // parent directory then an exception will be thrown above.
        }

        OutputStream os;

        try
        {
            os = getOutputStream( artifact );
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( "Cannot create input stream: ", e );
        }

        putTransfer( resource, is, os );
    }
}
