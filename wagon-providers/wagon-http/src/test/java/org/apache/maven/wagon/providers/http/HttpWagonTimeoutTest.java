package org.apache.maven.wagon.providers.http;

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

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.File;
import java.util.Random;

/**
 * User: jdumay Date: 24/01/2008 Time: 17:17:34
 */
public class HttpWagonTimeoutTest
    extends HttpWagonHttpServerTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
        ServletHolder servlets = new ServletHolder( new WaitForeverServlet() );
        context.addServlet( servlets, "/*" );
        startServer();
    }

    public void testGetTimeout()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setReadTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/timeoutfile", destFile );

            wagon.disconnect();
        }
        catch ( Exception e )
        {
            thrown = e;
        }
        finally
        {
            stopServer();
        }

        assertNotNull( thrown );
        assertEquals( TransferFailedException.class, thrown.getClass() );
    }

    public void testResourceExits()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setReadTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            wagon.resourceExists( "/timeoutfile" );

            wagon.disconnect();
        }
        catch ( Exception e )
        {
            thrown = e;
        }
        finally
        {
            stopServer();
        }

        assertNotNull( thrown );
        assertEquals( TransferFailedException.class, thrown.getClass() );
    }

    public void testGetFileList()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setReadTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            wagon.getFileList( "/timeoutfile" );

            wagon.disconnect();
        }
        catch ( Exception e )
        {
            thrown = e;
        }
        finally
        {
            stopServer();
        }

        assertNotNull( thrown );
        assertEquals( TransferFailedException.class, thrown.getClass() );
    }

    public void testPutTimeout()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setReadTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = File.createTempFile( "Hello", null );
            destFile.deleteOnExit();

            wagon.put( destFile, "/timeoutfile" );

            wagon.disconnect();
        }
        catch ( Exception e )
        {
            thrown = e;
        }
        finally
        {
            stopServer();
        }

        assertNotNull( thrown );
        assertEquals( TransferFailedException.class, thrown.getClass() );
    }

    public void testConnectionTimeout()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            HttpWagon wagon = (HttpWagon) getWagon();
            wagon.setHttpConfiguration(
                new HttpConfiguration().setAll( new HttpMethodConfiguration().setConnectionTimeout( 500 ) ) );

            Repository testRepository = new Repository();
            Random random = new Random( );
            testRepository.setUrl( "http://localhost:" + random.nextInt( 2048 ));

            wagon.connect( testRepository );

            long start = System.currentTimeMillis();
            wagon.getFileList( "/foobar" );
            long end = System.currentTimeMillis();

            // validate we have a default time out 60000
            assertTrue( (end - start) >= 500 && (end - start) < 1000 );

        }
        catch ( Exception e )
        {
            thrown = e;
        }
        finally
        {
            stopServer();
        }

        assertNotNull( thrown );
        assertEquals( TransferFailedException.class, thrown.getClass() );
    }

}
