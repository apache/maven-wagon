package org.apache.maven.wagon.shared.http;

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

import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class EncodingUtilTest
    extends TestCase
{
    public void testEncodeURLWithSpaces()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/path with spaces" );

        assertEquals( "file://host:1/path%20with%20spaces", encodedURL );
    }

    public void testEncodeURLWithSpacesInPath()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1", "path with spaces" );

        assertEquals( "file://host:1/path%20with%20spaces", encodedURL );
    }

    public void testEncodeURLWithSpacesInBothBaseAndPath()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/with%20a", "path with spaces" );

        assertEquals( "file://host:1/with%20a/path%20with%20spaces", encodedURL );
    }

    public void testEncodeURLWithSlashes1()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath", "a", "b", "c" );

        assertEquals( "file://host:1/basePath/a/b/c", encodedURL );

        encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath", "a/b/c" );

        assertEquals( "file://host:1/basePath/a/b/c", encodedURL );
    }

    public void testEncodeURLWithSlashes2()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath/", "a", "b", "c" );

        assertEquals( "file://host:1/basePath/a/b/c", encodedURL );

        encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath/", "a/b/c" );

        assertEquals( "file://host:1/basePath/a/b/c", encodedURL );
    }

    public void testEncodeURLWithSlashes3()
            throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath/", new String[0] );

        assertEquals( "file://host:1/basePath/", encodedURL );
    }

    public void testEncodeURLWithSlashes4()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath", new String[0] );

        assertEquals( "file://host:1/basePath", encodedURL );
    }

    public void testEncodeURLWithSlashes5()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/basePath",
                                                            "a/1", "b/1", "c/1" );

        assertEquals( "file://host:1/basePath/a%2F1/b%2F1/c%2F1", encodedURL );
    }

    public void testEncodeURLWithSlashes6()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1/", new String[0] );

        assertEquals( "file://host:1/", encodedURL );
    }

    public void testEncodeURLWithSlashes7()
        throws URISyntaxException, MalformedURLException
    {
        String encodedURL = EncodingUtil.encodeURLToString( "file://host:1", new String[0] );

        assertEquals( "file://host:1", encodedURL );
    }
}
