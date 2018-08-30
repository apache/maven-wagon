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
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;

/**
 * User: jdumay Date: 24/01/2008 Time: 17:17:34
 */
public class HttpWagonErrorTest
    extends HttpWagonHttpServerTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
        ServletHolder servlets = new ServletHolder( new ErrorWithMessageServlet() );
        context.addServlet( servlets, "/*" );
        startServer();
    }

    public void testGetReasonPhase401()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/401", destFile );

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
        assertEquals( AuthorizationException.class, thrown.getClass() );
    }

    public void testGetReasonPhase403()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/403", destFile );

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
        assertEquals( AuthorizationException.class, thrown.getClass() );
    }


    public void testGetReasonPhase407()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/407", destFile );

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
        assertEquals( AuthorizationException.class, thrown.getClass() );
    }

    public void testGetReasonPhase500()
        throws Exception
    {
        Exception thrown = null;

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl( "http://localhost:" + httpServerPort );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );
            destFile.deleteOnExit();

            wagon.get( "/500", destFile );

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
}
