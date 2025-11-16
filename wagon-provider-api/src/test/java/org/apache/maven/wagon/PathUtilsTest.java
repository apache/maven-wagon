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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class PathUtilsTest {
    @Test
    public void testFilenameResolving() {
        Assertions.assertEquals("filename", PathUtils.filename("dir/filename"));

        Assertions.assertEquals("filename", PathUtils.filename("filename"));

        Assertions.assertEquals("filename", PathUtils.filename("dir1/dir2/filename"));
    }

    @Test
    public void testDirResolving() {
        Assertions.assertEquals("dir", PathUtils.dirname("dir/filename"));

        Assertions.assertEquals("", PathUtils.dirname("filename"));

        Assertions.assertEquals("dir1/dir2", PathUtils.dirname("dir1/dir2/filename"));
    }

    // A characterization test that demonstrates the existing behavior does not
    // match the Unix dirname function when a trailing slash is present.
    @Test
    public void testDirnameDoesNotStripTrailingSlash() {
        Assertions.assertEquals("dir1/dir2/filename", PathUtils.dirname("dir1/dir2/filename/"));
    }

    // A characterization test that demonstrates the existing behavior does not
    // match the Unix dirname function when a trailing slash is present.
    @Test
    public void testFilenameDoesNotStripTrailingSlash() {
        Assertions.assertEquals("", PathUtils.filename("dir1/dir2/filename/"));
    }

    @Test
    public void testDirSplitting() {
        final String path = "a/b/c";

        final String[] dirs = PathUtils.dirnames(path);

        Assertions.assertNotNull(dirs);

        Assertions.assertEquals(2, dirs.length);

        Assertions.assertEquals("a", dirs[0]);

        Assertions.assertEquals("b", dirs[1]);
    }

    @Test
    public void testHostResolving() {
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("http://www.codehaus.org"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("HTTP://www.codehaus.org"));

        Assertions.assertEquals("localhost", PathUtils.host(null));
        Assertions.assertEquals("localhost", PathUtils.host("file:///c:/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("FILE:///c:/temp"));
    }

    @Test
    public void testScmHostResolving() {
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org/repos/module"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org/repos/module"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("scm:cvs:pserver:anoncvs@www.codehaus.org:/root"));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host("SCM:CVS:pserver:anoncvs@www.codehaus.org:/root"));
    }

    @Test
    public void testProtocolResolving() {
        Assertions.assertEquals("http", PathUtils.protocol("http://www.codehause.org"));
        Assertions.assertEquals("HTTP", PathUtils.protocol("HTTP://www.codehause.org"));
        Assertions.assertEquals("file", PathUtils.protocol("file:///c:/temp"));
        Assertions.assertEquals("scm", PathUtils.protocol("scm:svn:http://localhost/repos/module"));
        Assertions.assertEquals("scm", PathUtils.protocol("scm:cvs:pserver:anoncvs@cvs.apache.org:/home/cvspublic"));
    }

    @Test
    public void testUserInfo() {
        String urlWithUsername = "http://brett@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsername));
        Assertions.assertNull(PathUtils.password(urlWithUsername));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "http://brett:porter@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        Assertions.assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));
    }

    @Test
    public void testSubversionUserInfo() {
        String urlWithUsername = "scm:svn:http://brett@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsername));
        Assertions.assertNull(PathUtils.password(urlWithUsername));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "scm:svn:http://brett:porter@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        Assertions.assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));

        String urlWithUpperCaseProtocol = "SCM:SVN:HTTP://brett@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUpperCaseProtocol));
        Assertions.assertNull(PathUtils.password(urlWithUpperCaseProtocol));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUpperCaseProtocol));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUpperCaseProtocol));
    }

    @Test
    public void testCvsUserInfo() {
        String urlWithUsername = "scm:cvs:pserver:brett@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsername));
        Assertions.assertNull(PathUtils.password(urlWithUsername));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsername));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsername));

        String urlWithUsernamePassword = "scm:cvs:pserver:brett:porter@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUsernamePassword));
        Assertions.assertEquals("porter", PathUtils.password(urlWithUsernamePassword));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUsernamePassword));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUsernamePassword));

        String urlWithUpperCaseProtocol = "SCM:CVS:pserver:brett@www.codehaus.org";
        Assertions.assertEquals("brett", PathUtils.user(urlWithUpperCaseProtocol));
        Assertions.assertNull(PathUtils.password(urlWithUpperCaseProtocol));
        Assertions.assertEquals("www.codehaus.org", PathUtils.host(urlWithUpperCaseProtocol));
        Assertions.assertEquals("/", PathUtils.basedir(urlWithUpperCaseProtocol));
    }

    @Test
    public void testFileBasedir() {
        // see http://www.mozilla.org/quality/networking/testing/filetests.html

        // strict forms
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file:///c|/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file:///c|/temp"));
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file://localhost/c|/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file://localhost/c|/temp"));
        Assertions.assertEquals("/temp", PathUtils.basedir("file:///temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file:///temp"));
        Assertions.assertEquals("/temp", PathUtils.basedir("file://localhost/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file://localhost/temp"));

        // strict form, with : for drive separator
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file:///c:/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file:///c:/temp"));
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file://localhost/c:/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("file://localhost/c:/temp"));

        // convenience forms
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file://c:/temp"));
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file://c|/temp"));
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file:c:/temp"));
        Assertions.assertEquals("c:/temp", PathUtils.basedir("file:c|/temp"));
        Assertions.assertEquals("/temp", PathUtils.basedir("file:/temp"));

        // URL decoding
        Assertions.assertEquals("c:/my docs", PathUtils.basedir("file:///c:/my docs"));
        Assertions.assertEquals("c:/my docs", PathUtils.basedir("file:///c:/my%20docs"));
        Assertions.assertEquals(
                "c:/name #%20?{}[]<>.txt", PathUtils.basedir("file:///c:/name%20%23%2520%3F%7B%7D%5B%5D%3C%3E.txt"));

        Assertions.assertEquals("c:/temp", PathUtils.basedir("FILE:///c:/temp"));
        Assertions.assertEquals("localhost", PathUtils.host("FILE:///c:/temp"));
    }

    @Test
    public void testEmptyBasedir() {
        Assertions.assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80"));
        Assertions.assertEquals("/", PathUtils.basedir("http://www.codehaus.org"));
        Assertions.assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80/"));
        Assertions.assertEquals("/", PathUtils.basedir("http://www.codehaus.org/"));
        Assertions.assertEquals("/", PathUtils.basedir("HTTP://www.codehaus.org/"));
    }

    @Test
    public void testEmptyProtocol() {
        Assertions.assertEquals("", PathUtils.protocol("placeholder-only"));
        Assertions.assertEquals("", PathUtils.protocol("placeholder-only/module-a"));

        Assertions.assertEquals("placeholder-only", PathUtils.authorization("placeholder-only"));
        Assertions.assertEquals("placeholder-only", PathUtils.authorization("placeholder-only/module-a"));

        Assertions.assertEquals(-1, PathUtils.port("placeholder-only"));
        Assertions.assertEquals(-1, PathUtils.port("placeholder-only/module-a"));

        Assertions.assertEquals("/", PathUtils.basedir("placeholder-only"));
        Assertions.assertEquals("/module-a", PathUtils.basedir("placeholder-only/module-a"));
    }

    @Test
    public void testPortResolving() {
        Assertions.assertEquals(80, PathUtils.port("http://www.codehause.org:80/maven"));
        Assertions.assertEquals(80, PathUtils.port("HTTP://www.codehause.org:80/maven"));
        Assertions.assertEquals(WagonConstants.UNKNOWN_PORT, PathUtils.port("http://localhost/temp"));

        Assertions.assertEquals(10, PathUtils.port("ftp://localhost:10"));
        Assertions.assertEquals(10, PathUtils.port("FTP://localhost:10"));
    }

    @Test
    public void testScmPortResolving() {
        Assertions.assertEquals(80, PathUtils.port("scm:svn:http://www.codehaus.org:80/maven"));
        Assertions.assertEquals(80, PathUtils.port("SCM:SVN:HTTP://www.codehaus.org:80/maven"));
        Assertions.assertEquals(
                WagonConstants.UNKNOWN_PORT, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:/temp:module"));

        Assertions.assertEquals(2402, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:2402/temp:module"));
        Assertions.assertEquals(2402, PathUtils.port("SCM:CVS:pserver:anoncvs@localhost:2402/temp:module"));
    }

    @Test
    public void testScmBasedir() {
        Assertions.assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org/maven"));
        Assertions.assertEquals("/maven", PathUtils.basedir("SCM:SVN:HTTP://www.codehause.org/maven"));
        Assertions.assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org:80/maven"));
        Assertions.assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven"));
        Assertions.assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven"));
        Assertions.assertEquals(
                "/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven:module"));
        Assertions.assertEquals(
                "/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven:module"));
        Assertions.assertEquals(
                "/maven/module", PathUtils.basedir("SCM:CVS:pserver:anoncvs@www.codehause.org:/maven:module"));
    }

    @Test
    public void testPortBasedir() {
        Assertions.assertEquals("/maven", PathUtils.basedir("http://www.codehause.org:80/maven"));
        Assertions.assertEquals("/temp", PathUtils.basedir("http://localhost/temp"));

        Assertions.assertEquals("c:/temp", PathUtils.basedir("file://c:/temp"));
        Assertions.assertEquals("/", PathUtils.basedir("http://localhost:80/"));
        Assertions.assertEquals("/", PathUtils.basedir("http://localhost/"));
    }

    @Test
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

    @Test
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
        Assertions.assertEquals(protocol, PathUtils.protocol(url));
        Assertions.assertEquals(user, PathUtils.user(url));
        Assertions.assertEquals(password, PathUtils.password(url));
        Assertions.assertEquals(host, PathUtils.host(url));
        Assertions.assertEquals(port, PathUtils.port(url));
        Assertions.assertEquals(basedir, PathUtils.basedir(url));
    }

    @Test
    public void testToRelative() {
        Assertions.assertEquals(
                "dir",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user/dir").getAbsolutePath()));
        Assertions.assertEquals(
                "dir",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(), new File("C:/home/user/dir").getAbsolutePath()));

        Assertions.assertEquals(
                "dir/subdir",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user/dir/subdir").getAbsolutePath()));
        Assertions.assertEquals(
                "dir/subdir",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(),
                        new File("C:/home/user/dir/subdir").getAbsolutePath()));

        Assertions.assertEquals(
                ".",
                PathUtils.toRelative(
                        new File("/home/user").getAbsoluteFile(), new File("/home/user").getAbsolutePath()));
        Assertions.assertEquals(
                ".",
                PathUtils.toRelative(
                        new File("C:/home/user").getAbsoluteFile(), new File("C:/home/user").getAbsolutePath()));
    }
}
