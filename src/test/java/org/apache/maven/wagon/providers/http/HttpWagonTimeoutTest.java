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
 
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpContext;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;

/**
 * User: jdumay
 * Date: 24/01/2008
 * Time: 17:17:34
 */
public class HttpWagonTimeoutTest extends HttpWagonHttpServerTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
        ServletHandler servlets = new ServletHandler();
        servlets.addServlet( "/", "org.apache.maven.wagon.providers.http.WaitForeverServlet" );
        context.addHandler( servlets );
        startServer();
    }

    public void testGetTimeout() throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );
            
            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/timeoutfile", destFile );

            wagon.disconnect();
        }
        catch (Exception e)
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
    
    public void testResourceExits() throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            wagon.resourceExists( "/timeoutfile" );

            wagon.disconnect();
        }
        catch (Exception e)
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

    public void testGetFileList() throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            wagon.getFileList( "/timeoutfile" );

            wagon.disconnect();
        }
        catch (Exception e)
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

    public void testPutTimeout() throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();
            wagon.setTimeout( 1000 );

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = File.createTempFile( "Hello", null );
            destFile.deleteOnExit();

            wagon.put( destFile, "/timeoutfile" );

            wagon.disconnect();
        }
        catch (Exception e)
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
