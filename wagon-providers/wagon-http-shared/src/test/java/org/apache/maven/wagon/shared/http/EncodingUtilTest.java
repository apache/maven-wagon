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
package org.apache.maven.wagon.shared.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodingUtilTest {
    @Test
    public void testEncodeURLWithTrailingSlash() {
        String encodedURL = EncodingUtil.encodeURLToString("https://host:1234/test", "demo/");

        assertEquals("https://host:1234/test/demo/", encodedURL);
    }

    @Test
    public void testEncodeURLWithSpaces() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/path with spaces");

        assertEquals("file://host:1/path%20with%20spaces", encodedURL);
    }

    @Test
    public void testEncodeURLWithSpacesInPath() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1", "path with spaces");

        assertEquals("file://host:1/path%20with%20spaces", encodedURL);
    }

    @Test
    public void testEncodeURLWithSpacesInBothBaseAndPath() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/with%20a", "path with spaces");

        assertEquals("file://host:1/with%20a/path%20with%20spaces", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes1() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath", "a", "b", "c");

        assertEquals("file://host:1/basePath/a/b/c", encodedURL);

        encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath", "a/b/c");

        assertEquals("file://host:1/basePath/a/b/c", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes2() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath/", "a", "b", "c");

        assertEquals("file://host:1/basePath/a/b/c", encodedURL);

        encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath/", "a/b/c");

        assertEquals("file://host:1/basePath/a/b/c", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes3() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath/", new String[0]);

        assertEquals("file://host:1/basePath/", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes4() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath", new String[0]);

        assertEquals("file://host:1/basePath", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes5() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/basePath", "a/1", "b/1", "c/1");

        assertEquals("file://host:1/basePath/a%2F1/b%2F1/c%2F1", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes6() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1/", new String[0]);

        assertEquals("file://host:1/", encodedURL);
    }

    @Test
    public void testEncodeURLWithSlashes7() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1", new String[0]);

        assertEquals("file://host:1", encodedURL);
    }

    @Test
    public void testEncodeURLWithNonLatin() {
        String encodedURL = EncodingUtil.encodeURLToString("file://host:1", "пипец/");

        assertEquals("file://host:1/%D0%BF%D0%B8%D0%BF%D0%B5%D1%86/", encodedURL);
    }
}
