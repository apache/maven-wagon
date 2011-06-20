package org.apache.maven.wagon.tck.http.util;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.IOUtil;

public final class TestUtil
{
    private static Logger logger = Logger.getLogger(TestUtil.class);


    private static final Map<String, File> bases = new HashMap<String, File>();

    private TestUtil()
    {
    }

    public static File getResource( final String path )
        throws URISyntaxException, IOException
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( path );
        if ( resource == null )
        {
            throw new IllegalStateException( "Cannot find classpath resource: " + path );
        }

        if ( resource.getProtocol().startsWith( "jar" ) )
        {
            // File f = new File( path );
            // f = File.createTempFile( f.getName() + ".", ".tmp" );

            String url = resource.toExternalForm();
            int startIdx = url.lastIndexOf( ':' ) + 1;
            int endIdx = url.indexOf( "!" );
            url = url.substring( startIdx, endIdx );

            File base = bases.get( url );
            if ( base == null )
            {
                File urlFile = new File( url );

                base = new File( "target/tck-resources/" + urlFile.getName() );
                base.getParentFile().mkdirs();

                logger.info("unpacking test resources in jar: " + url);
                JarFile jf = null;
                try
                {
                    jf = new JarFile( urlFile );

                    InputStream in = null;
                    OutputStream out = null;

                    for ( Enumeration<JarEntry> en = jf.entries(); en.hasMoreElements(); )
                    {
                        JarEntry je = en.nextElement();
                        File target = new File( base, je.getName() ).getAbsoluteFile();
                        if ( je.isDirectory() )
                        {
                            target.mkdirs();
                        }
                        else
                        {
                            target.getParentFile().mkdirs();

                            try
                            {
                                in = jf.getInputStream( je );
                                out = new FileOutputStream( target );

                                IOUtil.copy( in, out );
                            }
                            finally
                            {
                                IOUtil.close( in );
                                IOUtil.close( out );
                            }
                        }
                    }

                    bases.put( url, base );
                }
                finally
                {
                    if ( jf != null )
                    {
                        try
                        {
                            jf.close();
                        }
                        catch ( Exception e )
                        {
                        }
                    }
                }
            }

            return new File( base, path );
        }
        else
        {
            return new File( resource.toURI().normalize() );
        }
    }

}
