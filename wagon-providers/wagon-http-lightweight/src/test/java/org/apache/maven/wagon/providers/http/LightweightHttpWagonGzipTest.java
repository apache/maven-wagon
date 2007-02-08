package org.apache.maven.wagon.providers.http;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.FileUtils;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;

import junit.framework.TestCase;

public class LightweightHttpWagonGzipTest
    extends TestCase
{
    public void testGzipGet()
        throws Exception
    {
        HttpServer server = new HttpServer();
        SocketListener listener = new SocketListener();
        listener.setPort( 10008 );
        server.addListener( listener );

        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        HttpContext context = new HttpContext();
        context.setContextPath( "/" );
        context.setResourceBase( localRepositoryPath );
        ResourceHandler rh = new ResourceHandler();
        rh.setMinGzipLength( 1 );
        context.addHandler( rh );
        server.addContext( context );
        server.start();

        try
        {
            Wagon wagon = new LightweightHttpWagon();

            Repository testRepository = new Repository( "testrepo", "http://localhost:10008" );

            wagon.setRepository( testRepository );

            File sourceFile = new File( localRepositoryPath + "/gzip" );

            sourceFile.deleteOnExit();

            String resName = "gzip-res.txt";
            String sourceContent = writeTestFileGzip( sourceFile, resName );

            wagon.connect();

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );

            destFile.deleteOnExit();

            wagon.get( "gzip/" + resName, destFile );

            wagon.disconnect();

            String destContent = FileUtils.fileRead( destFile );

            assertEquals( sourceContent, destContent );
        }
        finally
        {
            server.stop();
        }
    }

    private String writeTestFileGzip( File parent, String child )
        throws IOException
    {
        File file = new File( parent, child );
        file.getParentFile().mkdirs();
        file.deleteOnExit();
        OutputStream out = new FileOutputStream( file );
        out.write( child.getBytes() );
        out.close();

        file = new File( parent, child + ".gz" );
        file.deleteOnExit();
        out = new FileOutputStream( file );
        out = new GZIPOutputStream( out );
        //write out different data than non-gz file, so we can
        //assert the gz version was returned
        String content = file.getAbsolutePath();
        out.write( content.getBytes() );
        out.close();
        return content;
    }
}
