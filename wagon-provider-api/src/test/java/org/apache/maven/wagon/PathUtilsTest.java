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
package org.apache.maven.wagon;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class PathUtilsTest extends TestCase {
    public void testFilenameResolving() {
        assertEquals("filename", PathUtils.filename("dir/filename"));

        assertEquals("filename", PathUtils.filename("filename"));

        assertEquals("filename", PathUtils.filename("dir1/dir2/filename"));
    }

    public void testDirResolving() {
        assertEquals("dir", PathUtils.dirname("dir/filename"));

        assertEquals("", PathUtils.dirname("filename"));

        assertEquals("dir1/dir2", PathUtils.dirname("dir1/dir2/filename"));
    }

    // A characterization test that demonstrates the existing behavior does not
    // match the Unix dirname function when a trailing slash is present.
    public void testDirnameDoesNotStripTrailingSlash() {
        assertEquals("dir1/dir2/filename", PathUtils.dirname("dir1/dir2/filename/"));
    }

    // A characterization test that demonstrates the existing behavior does not
    // match the Unix dirname function when a trailing slash is present.
    public void testFilenameDoesNotStripTrailingSlash() {
        assertEquals("", PathUtils.filename("dir1/dir2/filename/"));
    }

    public void testDirSplitting() {
        final String path = "a/b/c";

        final String[] dirs = PathUtils.dirnames(path);

        assertNotNull(dirs);

        assertEquals(2, dirs.length);

        assertEquals("a", dirs[0]);

        assertEquals("b", dirs[1]);
    }

    public void testHostResolving() {
        assertEquals("www.codehaus.org", PathUtils.host("http://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("HTTP://www.codehaus.org"));

        assertEquals("localhost", PathUtils.host(null));
        assertEquals("localhost", PathUtils.host("file:///c:/temp"));
        assertEquals("localhost", PathUtils.host("FILE:///c:/temp"));
    }

    public void testScmHostResolving() {
        assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org/repos/module"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org/repos/module"));
        assertEquals("www.codehaus.org", PathUtils.host("scm:cvs:pserver:anoncvs@www.codehaus.org:/root"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:CVS:pserver:anoncvs@www.codehaus.org:/root"));
    }

    public void testProtocolResolving() {
        assertEquals("http", PathUtils.protocol("http://www.codehause.org"));
        assertEquals("HTTP", PathUtils.protocol("HTTP://www.codehause.org"));
        assertEquals("file", PathUtils.protocol("file:///c:/temp"));
        assertEquals("scm", PathUtils.protocol("scm:svn:http://localhost/repos/module"));
        assertEquals("scm", PathUtils.protocol("scm:cvs:pserver:anoncvs@cvs.apache.org:/home/cvspublic"));
    }

    public void testUserInfo() {
        String urlWithUsername = "http://brett@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsername));
        assertNull(PathUtils.password(urlWithUsername));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "http://brett:porter@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));
    }

    public void testSubversionUserInfo() {
        String urlWithUsername = "scm:svn:http://brett@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsername));
        assertNull(PathUtils.password(urlWithUsername));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "scm:svn:http://brett:porter@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));

        String urlWithUpperCaseProtocol = "SCM:SVN:HTTP://brett@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUpperCaseProtocol));
        assertNull(PathUtils.password(urlWithUpperCaseProtocol));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUpperCaseProtocol));
        assertEquals("/", PathUtils.basedir(urlWithUpperCaseProtocol));
    }

    public void testCvsUserInfo() {
        String urlWithUsername = "scm:cvs:pserver:brett@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsername));
        assertNull(PathUtils.password(urlWithUsername));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "scm:cvs:pserver:brett:porter@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));

        String urlWithUpperCaseProtocol = "SCM:CVS:pserver:brett@www.codehaus.org";
        assertEquals("brett", PathUtils.user(urlWithUpperCaseProtocol));
        assertNull(PathUtils.password(urlWithUpperCaseProtocol));
        assertEquals("www.codehaus.org", PathUtils.host(urlWithUpperCaseProtocol));
        assertEquals("/", PathUtils.basedir(urlWithUpperCaseProtocol));
    }

    public void testFileBasedir() {
        // see http://www.mozilla.org/quality/networking/testing/filetests.html

        // strict forms
        assertEquals("c:/temp", PathUtils.basedir("file:///c|/temp"));
        assertEquals("localhost", PathUtils.host("file:///c|/temp"));
        assertEquals("c:/temp", PathUtils.basedir("file://localhost/c|/temp"));
        assertEquals("localhost", PathUtils.host("file://localhost/c|/temp"));
        assertEquals("/temp", PathUtils.basedir("file:///temp"));
        assertEquals("localhost", PathUtils.host("file:///temp"));
        assertEquals("/temp", PathUtils.basedir("file://localhost/temp"));
        assertEquals("localhost", PathUtils.host("file://localhost/temp"));

        // strict form, with : for drive separator
        assertEquals("c:/temp", PathUtils.basedir("file:///c:/temp"));
        assertEquals("localhost", PathUtils.host("file:///c:/temp"));
        assertEquals("c:/temp", PathUtils.basedir("file://localhost/c:/temp"));
        assertEquals("localhost", PathUtils.host("file://localhost/c:/temp"));

        // convenience forms
        assertEquals("c:/temp", PathUtils.basedir("file://c:/temp"));
        assertEquals("c:/temp", PathUtils.basedir("file://c|/temp"));
        assertEquals("c:/temp", PathUtils.basedir("file:c:/temp"));
        assertEquals("c:/temp", PathUtils.basedir("file:c|/temp"));
        assertEquals("/temp", PathUtils.basedir("file:/temp"));

        // URL decoding
        assertEquals("c:/my docs", PathUtils.basedir("file:///c:/my docs"));
        assertEquals("c:/my docs", PathUtils.basedir("file:///c:/my%20docs"));
        assertEquals(
                "c:/name #%20?{}[]<>.txt", PathUtils.basedir("file:///c:/name%20%23%2520%3F%7B%7D%5B%5D%3C%3E.txt"));

        assertEquals("c:/temp", PathUtils.basedir("FILE:///c:/temp"));
        assertEquals("localhost", PathUtils.host("FILE:///c:/temp"));
    }

    public void testEmptyBasedir() {
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80/"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org/"));
        assertEquals("/", PathUtils.basedir("HTTP://www.codehaus.org/"));
    }

    public void testEmptyProtocol() {
        assertEquals("", PathUtils.protocol("placeholder-only"));
        assertEquals("", PathUtils.protocol("placeholder-only/module-a"));

        assertEquals("placeholder-only", PathUtils.authorization("placeholder-only"));
        assertEquals("placeholder-only", PathUtils.authorization("placeholder-only/module-a"));

        assertEquals(-1, PathUtils.port("placeholder-only"));
        assertEquals(-1, PathUtils.port("placeholder-only/module-a"));

        assertEquals("/", PathUtils.basedir("placeholder-only"));
        assertEquals("/module-a", PathUtils.basedir("placeholder-only/module-a"));
    }

    public void testPortResolving() {
        assertEquals(80, PathUtils.port("http://www.codehause.org:80/maven"));
        assertEquals(80, PathUtils.port("HTTP://www.codehause.org:80/maven"));
        assertEquals(WagonConstants.UNKNOWN_PORT, PathUtils.port("http://localhost/temp"));

        assertEquals(10, PathUtils.port("ftp://localhost:10"));
        assertEquals(10, PathUtils.port("FTP://localhost:10"));
    }

    public void testScmPortResolving() {
        assertEquals(80, PathUtils.port("scm:svn:http://www.codehaus.org:80/maven"));
        assertEquals(80, PathUtils.port("SCM:SVN:HTTP://www.codehaus.org:80/maven"));
        assertEquals(WagonConstants.UNKNOWN_PORT, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:/temp:module"));

        assertEquals(2402, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:2402/temp:module"));
        assertEquals(2402, PathUtils.port("SCM:CVS:pserver:anoncvs@localhost:2402/temp:module"));
    }

    public void testScmBasedir() {
        assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org/maven"));
        assertEquals("/maven", PathUtils.basedir("SCM:SVN:HTTP://www.codehause.org/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org:80/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven"));
        assertEquals("/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven:module"));
        assertEquals("/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven:module"));
        assertEquals("/maven/module", PathUtils.basedir("SCM:CVS:pserver:anoncvs@www.codehause.org:/maven:module"));
    }

    public void testPortBasedir() {
        assertEquals("/maven", PathUtils.basedir("http://www.codehause.org:80/maven"));
        assertEquals("/temp", PathUtils.basedir("http://localhost/temp"));

        assertEquals("c:/temp", PathUtils.basedir("file://c:/temp"));
        assertEquals("/", PathUtils.basedir("http://localhost:80/"));
        assertEquals("/", PathUtils.basedir("http://localhost/"));
    }

    public void testIpV4() {
        assertUrl("http://127.0.0.1", "http", null, null, "127.0.0.1", -1, "/");
        assertUrl("http://127.0.0.1:8080", "http", null, null, "127.0.0.1", 8080, "/");
        assertUrl("http://127.0.0.1/oo/rest/users", "http", null, null, "127.0.0.1", -1, "/oo/rest/users");
        assertUrl("http://127.0.0.1:8080/oo/rest/users", "http", null, null, "127.0.0.1", 8080, "/oo/rest/users");

        assertUrl("http://user:password@127.0.0.1", "http", "user", "password", "127.0.0.1", -1, "/");
        assertUrl("http://user:password@127.0.0.1:8080", "http", "user", "password", "127.0.0.1", 8080, "/");
        assertUrl(
                "http://user:password@127.0.0.1/oo/rest/users",
                "http",
                "user",
                "password",
                "127.0.0.1",
                -1,
                "/oo/rest/users");
        assertUrl(
                "http://user:password@127.0.0.1:8080/oo/rest/users",
                "http",
                "user",
                "password",
                "127.0.0.1",
                8080,
                "/oo/rest/users");

        assertUrl(
                "scm:svn:http://user:password@127.0.0.1:8080/oo/rest/users",
                "scm",
                "user",
                "password",
                "127.0.0.1",
                8080,
                "/oo/rest/users");
    }

    public void testIPv6() {
        assertUrl(
                "http://user:password@[fff:::1]:7891/oo/rest/users",
                "http",
                "user",
                "password",
                "fff:::1",
                7891,
                "/oo/rest/users");
        assertUrl("http://[fff:::1]:7891/oo/rest/users", "http", null, null, "fff:::1", 7891, "/oo/rest/users");
        assertUrl(
                "http://user:password@[fff:::1]/oo/rest/users",
                "http",
                "user",
                "password",
                "fff:::1",
                -1,
                "/oo/rest/users");
        assertUrl("http://user:password@[fff:::1]:7891", "http", "user", "password", "fff:::1", 7891, "/");

        assertUrl(
                "http://user:password@[fff:000::222:1111]:7891/oo/rest/users",
                "http",
                "user",
                "password",
                "fff:000::222:1111",
                7891,
                "/oo/rest/users");
        assertUrl(
                "http://[fff:000::222:1111]:7891/oo/rest/users",
                "http",
                null,
                null,
                "fff:000::222:1111",
                7891,
                "/oo/rest/users");
        assertUrl(
                "http://user:password@[fff:000::222:1111]/oo/rest/users",
                "http",
                "user",
                "password",
                "fff:000::222:1111",
                -1,
                "/oo/rest/users");
        assertUrl(
                "http://user:password@[fff:000::222:1111]:7891",
                "http",
                "user",
                "password",
                "fff:000::222:1111",
                7891,
                "/");

        assertUrl(
                "http://user:password@16.60.56.58:7891/oo/rest/users",
                "http",
                "user",
                "password",
                "16.60.56.58",
                7891,
                "/oo/rest/users");
        assertUrl("http://16.60.56.58:7891/oo/rest/users", "http", null, null, "16.60.56.58", 7891, "/oo/rest/users");
        assertUrl(
                "http://user:password@16.60.56.58/oo/rest/users",
                "http",
                "user",
                "password",
                "16.60.56.58",
                -1,
                "/oo/rest/users");
        assertUrl("http://user:password@16.60.56.58:7891", "http", "user", "password", "16.60.56.58", 7891, "/");

        assertUrl(
                "http://user:password@16.60.56.58:7891/oo/rest/users",
                "http",
                "user",
                "password",
                "16.60.56.58",
                7891,
                "/oo/rest/users");
        assertUrl("http://16.60.56.58:7891/oo/rest/users", "http", null, null, "16.60.56.58", 7891, "/oo/rest/users");
        assertUrl(
                "http://user:password@16.60.56.58/oo/rest/users",
                "http",
                "user",
                "password",
                "16.60.56.58",
                -1,
                "/oo/rest/users");
        assertUrl("http://user:password@16.60.56.58:7891", "http", "user", "password", "16.60.56.58", 7891, "/");
    }

    private void assertUrl(
            String url, String protocol, String user, String password, String host, int port, String basedir) {
        assertEquals(protocol, PathUtils.protocol(url));
        assertEquals(user, PathUtils.user(url));
        assertEquals(password, PathUtils.password(url));
        assertEquals(host, PathUtils.host(url));
        assertEquals(port, PathUtils.port(url));
        assertEquals(basedir, PathUtils.basedir(url));
    }

    public void testToRelative() {
        assertEquals(
                "dir",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user/dir").getAbsolutePath()));
        assertEquals(
                "dir",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(), new File("C:/home/user/dir").getAbsolutePath()));

        assertEquals(
                "dir/subdir",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user/dir/subdir").getAbsolutePath()));
        assertEquals(
                "dir/subdir",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(),
                        new File("C:/home/user/dir/subdir").getAbsolutePath()));

        assertEquals(
                ".",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user").getAbsolutePath()));
        assertEquals(
                ".",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(), new File("C:/home/user").getAbsolutePath()));
    }
}
