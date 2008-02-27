package org.apache.maven.wagon.providers.http.dav;

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
import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.codehaus.plexus.util.FileUtils;

/**
 * MkColMethodTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MkColMethodTest
    extends AbstractDavTestCase
{
    public void testMkColSimple()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "target/dav-tests/mkcol-simple" );
        FileUtils.deleteDirectory( serverRoot );
        serverRoot.mkdirs();
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "quotes" );
            MkColMethod method = new MkColMethod( testURI );
            HttpClient client = new HttpClient();
            int status = client.executeMethod( method );

            // Verify Results
            assertEquals( "Status", HttpStatus.SC_CREATED, status );
            assertTrue( "Created Directory", new File( serverRoot, "quotes" ).exists() );
        }
        finally
        {
            stopServer();
        }
    }

    public void testMkColMultipleDeep()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "target/dav-tests/mkcol-multideep" );
        FileUtils.deleteDirectory( serverRoot );
        serverRoot.mkdirs();
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            String paths[] = new String[] {
                "quotes",
                "quotes/twain",
                "quotes/twain/mark",
                "quotes/franklin",
                "quotes/franklin/benjamin" };

            HttpClient client = new HttpClient();
            for ( int i = 0; i < paths.length; i++ )
            {
                URI testURI = davServerURI.resolve( paths[i] );
                MkColMethod method = new MkColMethod( testURI );
                int status = client.executeMethod( method );
                // Verify Results
                assertEquals( "Status (" + paths[i] + ")", HttpStatus.SC_CREATED, status );
                assertTrue( "Created Directory (" + paths[i] + ")", new File( serverRoot, paths[i] ).exists() );
            }
        }
        finally
        {
            stopServer();
        }
    }

    public void testMkColTwice()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "target/dav-tests/mkcol-twice" );
        FileUtils.deleteDirectory( serverRoot );
        serverRoot.mkdirs();
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "quotes" );
            HttpClient client = new HttpClient();

            MkColMethod method;
            int status;

            // Create first one normally
            method = new MkColMethod( testURI );
            status = client.executeMethod( method );

            // Verify Results
            assertEquals( "Status: first create", HttpStatus.SC_CREATED, status );
            assertTrue( "Created Directory: first create", new File( serverRoot, "quotes" ).exists() );

            // Create second time (with error)
            method = new MkColMethod( testURI );
            status = client.executeMethod( method );

            // Verify Results (should have been a 405/Method Not Allowed result.
            assertEquals( "Status: second create", HttpStatus.SC_METHOD_NOT_ALLOWED, status );
            assertTrue( "Created Directory: second create", new File( serverRoot, "quotes" ).exists() );
        }
        finally
        {
            stopServer();
        }
    }

    public void testMkColDeepNoParent()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "target/dav-tests/mkcol-deep-noparent" );
        FileUtils.deleteDirectory( serverRoot );
        serverRoot.mkdirs();
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "quotes/franklin/benjamin" );
            HttpClient client = new HttpClient();

            // Create deep collection (should fail as 'quotes/franklin' do not exist)
            MkColMethod method = new MkColMethod( testURI );
            int status = client.executeMethod( method );

            // Verify Results (should have been a conflict)
            assertEquals( "Status:", HttpStatus.SC_CONFLICT, status );
            assertFalse( "Directory should not have been created.", new File( serverRoot, "quotes" ).exists() );
        }
        finally
        {
            stopServer();
        }
    }
}
