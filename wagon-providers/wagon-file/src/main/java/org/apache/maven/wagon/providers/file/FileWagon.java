package org.apache.maven.wagon.providers.file;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Wagon Provider for Local File System
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="file" instantiation-strategy="per-lookup"
 */
public class FileWagon
    extends StreamWagon
{
    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        if ( getRepository().getBasedir() == null )
        {
            throw new TransferFailedException( "Unable to operate with a null basedir." );
        }

        Resource resource = inputData.getResource();

        File file = new File( getRepository().getBasedir(), resource.getName() );

        if ( !file.exists() )
        {
            throw new ResourceDoesNotExistException( "File: " + file + " does not exist" );
        }

        try
        {
            InputStream in = new BufferedInputStream( new FileInputStream( file ) );

            inputData.setInputStream( in );

            resource.setContentLength( file.length() );

            resource.setLastModified( file.lastModified() );
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Could not read from file: " + file.getAbsolutePath(), e );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        if ( getRepository().getBasedir() == null )
        {
            throw new TransferFailedException( "Unable to operate with a null basedir." );
        }

        Resource resource = outputData.getResource();

        File file = new File( getRepository().getBasedir(), resource.getName() );

        createParentDirectories( file );

        OutputStream outputStream = new BufferedOutputStream( new LazyFileOutputStream( file ) );

        outputData.setOutputStream( outputStream );
    }

    protected void openConnectionInternal()
        throws ConnectionException
    {
        if ( getRepository() == null )
        {
            throw new ConnectionException( "Unable to operate with a null repository." );
        }

        if ( getRepository().getBasedir() == null )
        {
            // This condition is possible when using wagon-file under integration testing conditions.
            fireSessionDebug( "Using a null basedir." );
            return;
        }

        // Check the File repository exists
        File basedir = new File( getRepository().getBasedir() );
        if ( !basedir.exists() )
        {
            if ( !basedir.mkdirs() )
            {
                throw new ConnectionException( "Repository path " + basedir + " does not exist, and cannot be created." );
            }
        }

        if ( !basedir.canRead() )
        {
            throw new ConnectionException( "Repository path " + basedir + " cannot be read" );
        }
    }

    public void closeConnection()
    {
    }

    public boolean supportsDirectoryCopy()
    {
        // TODO: should we test for null basedir here?
        return true;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( getRepository().getBasedir() == null )
        {
            throw new TransferFailedException( "Unable to putDirectory() with a null basedir." );
        }

        File path = resolveDestinationPath( destinationDirectory );

        try
        {
            /*
             * Done to address issue found in HP-UX with regards to "." directory references. Details found in ..
             * WAGON-30 - wagon-file failed when used by maven-site-plugin WAGON-33 - FileWagon#putDirectory() fails in
             * HP-UX if destinationDirectory is "."
             * http://www.nabble.com/With-maven-2.0.2-site%3Adeploy-doesn%27t-work-t934716.html for details. Using
             * path.getCanonicalFile() ensures that the path is fully resolved before an attempt to create it. TODO:
             * consider moving this to FileUtils.mkdirs()
             */
            File realFile = path.getCanonicalFile();
            realFile.mkdirs();
        }
        catch ( IOException e )
        {
            // Fall back to standard way if getCanonicalFile() fails.
            path.mkdirs();
        }

        if ( !path.exists() || !path.isDirectory() )
        {
            String emsg = "Could not make directory '" + path.getAbsolutePath() + "'.";

            // Add assistive message in case of failure.
            File basedir = new File( getRepository().getBasedir() );
            if ( !basedir.canWrite() )
            {
                emsg += "  The base directory " + basedir + " is read-only.";
            }

            throw new TransferFailedException( emsg );
        }

        try
        {
            FileUtils.copyDirectoryStructure( sourceDirectory, path );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error copying directory structure", e );
        }
    }

    private File resolveDestinationPath( String destinationPath )
    {
        String basedir = getRepository().getBasedir();

        destinationPath = StringUtils.replace( destinationPath, "\\", "/" );

        File path;

        if ( destinationPath.equals( "." ) )
        {
            path = new File( basedir );
        }
        else
        {
            path = new File( basedir, destinationPath );
        }

        return path;
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( getRepository().getBasedir() == null )
        {
            throw new TransferFailedException( "Unable to getFileList() with a null basedir." );
        }

        File path = resolveDestinationPath( destinationDirectory );

        if ( !path.exists() )
        {
            throw new ResourceDoesNotExistException( "Directory does not exist: " + destinationDirectory );
        }

        if ( !path.isDirectory() )
        {
            throw new ResourceDoesNotExistException( "Path is not a directory: " + destinationDirectory );
        }

        String files[] = path.list();

        return Arrays.asList( files );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        if ( getRepository().getBasedir() == null )
        {
            throw new TransferFailedException( "Unable to getFileList() with a null basedir." );
        }

        File file = resolveDestinationPath( resourceName );

        return file.exists();
    }
}
