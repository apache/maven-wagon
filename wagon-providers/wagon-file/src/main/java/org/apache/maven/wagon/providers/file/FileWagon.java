package org.apache.maven.wagon.providers.file;

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

import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wagon Provider for Local File System
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class FileWagon
    extends StreamWagon
{

    // get
    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {

        Resource resource = inputData.getResource();

        File file = new File( getRepository().getBasedir(), resource.getName() );

        if ( !file.exists() )
        {
            throw new ResourceDoesNotExistException( "File: " + file + " does not exist" );
        }

        try
        {
            InputStream in = new FileInputStream( file );

            inputData.setInputStream( in );

            resource.setContentLength( file.length() );

            resource.setLastModified( file.lastModified() );
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Could not read from file: " + file.getAbsolutePath(), e );
        }
    }

    // put
    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {

        Resource resource = outputData.getResource();

        File file = new File( getRepository().getBasedir(), resource.getName() );

        File dir = file.getParentFile();

        if ( dir != null && !dir.exists() )
        {

            if ( !dir.mkdirs() )
            {
                String msg = "Some of the requied directories do not exist and could not be create. " +
                    "Requested path:  " + file.getAbsolutePath();

                throw new TransferFailedException( msg );
            }
        }

        OutputStream outputStream = new LazyFileOutputStream( file );

        outputData.setOutputStream( outputStream );

        resource.setContentLength( file.length() );

        resource.setLastModified( file.lastModified() );

    }

    public void openConnection()
    {
    }

    public void closeConnection()
    {
    }
}
