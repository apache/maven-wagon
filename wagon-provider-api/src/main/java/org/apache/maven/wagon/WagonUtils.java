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

import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 * @version $Id$
 * @deprecated
 */
public final class WagonUtils
{
    private WagonUtils()
    {
    }

    public static String toString( String resource, Wagon wagon )
        throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {

        File file = null;

        try
        {
            file = File.createTempFile( "wagon", "tmp" );

            wagon.get( resource, file );

            return FileUtils.fileRead( file );
        }
        finally
        {
            if ( file != null )
            {
                boolean deleted = file.delete();

                if ( !deleted )
                {
                    file.deleteOnExit();
                }
            }
        }

    }


    public static void putDirectory( File dir, Wagon wagon, boolean includeBasdir )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException
    {

        LinkedList queue = new LinkedList();

        if ( includeBasdir )
        {
            queue.add( dir.getName() );
        }
        else
        {
            queue.add( "" );
        }

        while ( !queue.isEmpty() )
        {
            String path = (String) queue.removeFirst();

            File currentDir = new File( dir, path );

            File[] files = currentDir.listFiles();

            for ( int i = 0; i < files.length; i++ )
            {
                File file = files[i];

                String resource;

                if ( path.length() > 0 )
                {
                    resource = path + "/" + file.getName();
                }
                else
                {
                    resource = file.getName();
                }

                if ( file.isDirectory() )
                {
                    queue.add( resource );
                }
                else
                {
                    wagon.put( file, resource );
                }

            }

        }

    }
}
