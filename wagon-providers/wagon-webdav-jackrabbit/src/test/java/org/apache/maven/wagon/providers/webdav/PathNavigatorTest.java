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
package org.apache.maven.wagon.providers.webdav;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class PathNavigatorTest extends TestCase {
    private static final String TEST_PATH = "foo/bar/baz";

    public void testBackAndForward() {
        PathNavigator navigator = new PathNavigator(TEST_PATH);

        assertEquals("foo/bar/baz/", navigator.getPath());

        // Nav backward
        assertTrue(navigator.backward());
        assertEquals("foo/bar/", navigator.getPath());

        assertTrue(navigator.backward());
        assertEquals("foo/", navigator.getPath());

        assertTrue(navigator.backward());
        assertEquals("", navigator.getPath());

        assertFalse(navigator.backward());
        assertEquals("", navigator.getPath());

        // Nav forward
        assertTrue(navigator.forward());
        assertEquals("foo/", navigator.getPath());

        assertTrue(navigator.forward());
        assertEquals("foo/bar/", navigator.getPath());

        assertTrue(navigator.forward());
        assertEquals("foo/bar/baz/", navigator.getPath());

        assertFalse(navigator.forward());
        assertEquals("foo/bar/baz/", navigator.getPath());
    }
}
