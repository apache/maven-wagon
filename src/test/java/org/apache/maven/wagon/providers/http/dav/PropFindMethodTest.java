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

/**
 * PropFindMethodTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PropFindMethodTest
    extends AbstractDavTestCase
{
    public void testPropFindOnDir()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "src/test/resources" );
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "links" );
            PropFindMethod method = new PropFindMethod( testURI );
            HttpClient client = new HttpClient();
            int status = client.executeMethod( method );

            // Verify Results
            assertEquals( "Status", HttpStatus.SC_MULTI_STATUS, status );
            MultiStatus multistatus = method.getMultiStatus();
            assertNotNull( "MultiStatus", multistatus );

            File expected[] = new File( serverRoot, "links" ).listFiles();
            // expectation is the files/directories + directory itself.
            int expectedResourceCount = expected.length + 1;

            assertEquals( "MultiStatus.resources.size", expectedResourceCount, multistatus.getResources().size() );
            for ( int i = 0; i < expected.length; i++ )
            {
                File expectation = expected[i];
                String href = "/dav/links/" + expectation.getName();
                if ( expectation.isDirectory() )
                {
                    href += "/";
                }
                DavResource resource = multistatus.getResource( href );
                assertNotNull( "MultiStatus.resource[" + href + "] should not be null", resource );
                assertEquals( "Resource[" + href + "].href", href, resource.getHref() );
                assertEquals( "Resource[" + href + "].collection", expectation.isDirectory(), resource.isCollection() );
                assertEquals( "Resource[" + href + "].status", 200, resource.getStatus() );
            }
        }
        finally
        {
            stopServer();
        }
    }

    public void testPropFindOnFile()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "src/test/resources" );
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "links/commons-lang.html" );
            PropFindMethod method = new PropFindMethod( testURI );
            HttpClient client = new HttpClient();
            int status = client.executeMethod( method );

            // Verify Results
            assertEquals( "Status", HttpStatus.SC_MULTI_STATUS, status );
            MultiStatus multistatus = method.getMultiStatus();
            assertNotNull( "MultiStatus", multistatus );

            assertEquals( "MultiStatus.resources.size", 1, multistatus.getResources().size() );

            String href = "/dav/links/commons-lang.html";
            DavResource resource = multistatus.getResource( "/dav/links/commons-lang.html" );
            assertNotNull( "MultiStatus.resource[" + href + "] should not be null", resource );
            assertEquals( "Resource[" + href + "].href", href, resource.getHref() );
            assertEquals( "Resource[" + href + "].collection", false, resource.isCollection() );
            assertEquals( "Resource[" + href + "].status", 200, resource.getStatus() );
        }
        finally
        {
            stopServer();
        }
    }

    public void testPropFindOnInvalid()
        throws Exception
    {
        // Setup
        File serverRoot = new File( "src/test/resources" );
        URI davServerURI = startDavServer( serverRoot );

        try
        {
            // Execute Test
            URI testURI = davServerURI.resolve( "invalid/resource.txt" );
            PropFindMethod method = new PropFindMethod( testURI );
            HttpClient client = new HttpClient();
            int status = client.executeMethod( method );

            // Verify Results
            assertEquals( "Status", HttpStatus.SC_NOT_FOUND, status );
            assertNull( "MultiStatus should be null", method.getMultiStatus() );
        }
        finally
        {
            stopServer();
        }
    }
}
