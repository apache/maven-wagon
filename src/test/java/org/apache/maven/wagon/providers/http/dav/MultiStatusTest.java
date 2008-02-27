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
import java.io.FileInputStream;
import java.util.List;

import junit.framework.TestCase;

/**
 * MultiStatusTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MultiStatusTest
    extends TestCase
{
    public void testParseMulti3()
        throws Exception
    {
        File multifile = new File( "src/test/resources/dav/multistatus-3.xml" );
        FileInputStream stream = new FileInputStream( multifile );
        MultiStatus multistatus = MultiStatus.parse( stream );
        assertNotNull( "Should not be null.", multistatus );
        assertEquals( "Resources.size", 7, multistatus.getResources().size() );

        DavResource resource;
        String resourceRoot = "/archiva/repository/commons-httpclient/commons-httpclient/commons-httpclient/commons-httpclient/";

        resource = multistatus.getResource( resourceRoot );
        assertNotNull( "Resource root should not be null.", resource );
        assertTrue( "Resource root should be a collection.", resource.isCollection() );

        resource = multistatus.getResource( resourceRoot + "2.0.2/" );
        assertNotNull( "Resource '2.0.2/' should not be null.", resource );
        assertTrue( "Resource '2.0.2/' should be a collection.", resource.isCollection() );

        resource = multistatus.getResource( resourceRoot + "commons-httpclient-2.0.2.pom" );
        assertNotNull( "Resource 'commons-httpclient-2.0.2.pom' should not be null.", resource );
        assertFalse( "Resource 'commons-httpclient-2.0.2.pom' should not be a collection.", resource.isCollection() );
        assertEquals( "Resource 'commons-httpclient-2.0.2.pom' status", 200, resource.getStatus() );

        resource = multistatus.getResource( resourceRoot + "invalid.txt" );
        assertNull( "Resource 'invalid.txt' should be null.", resource );

        List/*<DavResource>*/files = multistatus.getFileResources();
        assertNotNull( "Files should not be null", files );
        assertEquals( "Files.size", 4, files.size() );

        List/*<DavResource>*/dirs = multistatus.getCollectionResources();
        assertNotNull( "Dirs should not be null", dirs );
        assertEquals( "Dirs.size", 3, dirs.size() );
    }
}
