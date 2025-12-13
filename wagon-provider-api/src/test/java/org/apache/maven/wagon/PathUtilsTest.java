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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
class PathUtilsTest {
    @Test
    void filenameResolving() {
        assertEquals("filename", PathUtils.filename("dir/filename"));

        assertEquals("filename", PathUtils.filename("filename"));

        assertEquals("filename", PathUtils.filename("dir1/dir2/filename"));
    }

    @Test
    void dirResolving() {
        assertEquals("dir", PathUtils.dirname("dir/filename"));

        assertEquals("", PathUtils.dirname("filename"));

        assertEquals("dir1/dir2", PathUtils.dirname("dir1/dir2/filename"));
    }

    @Test
    void dirSpliting() {
        final String path = "a/b/c";

        final String[] dirs = PathUtils.dirnames(path);

        assertNotNull(dirs);

        assertEquals(2, dirs.length);

        assertEquals("a", dirs[0]);

        assertEquals("b", dirs[1]);
    }

    @Test
    void hostResolving() {
        assertEquals("www.codehaus.org", PathUtils.host("http://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("HTTP://www.codehaus.org"));

        assertEquals("localhost", PathUtils.host(null));
        assertEquals("localhost", PathUtils.host("file:///c:/temp"));
        assertEquals("localhost", PathUtils.host("FILE:///c:/temp"));
    }

    @Test
    void scmHostResolving() {
        assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org"));
        assertEquals("www.codehaus.org", PathUtils.host("scm:svn:http://www.codehaus.org/repos/module"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:SVN:HTTP://www.codehaus.org/repos/module"));
        assertEquals("www.codehaus.org", PathUtils.host("scm:cvs:pserver:anoncvs@www.codehaus.org:/root"));
        assertEquals("www.codehaus.org", PathUtils.host("SCM:CVS:pserver:anoncvs@www.codehaus.org:/root"));
    }

    @Test
    void protocolResolving() {
        assertEquals("http", PathUtils.protocol("http://www.codehause.org"));
        assertEquals("HTTP", PathUtils.protocol("HTTP://www.codehause.org"));
        assertEquals("file", PathUtils.protocol("file:///c:/temp"));
        assertEquals("scm", PathUtils.protocol("scm:svn:http://localhost/repos/module"));
        assertEquals("scm", PathUtils.protocol("scm:cvs:pserver:anoncvs@cvs.apache.org:/home/cvspublic"));
    }

    @Test
    void userInfo() {
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

    @Test
    void subversionUserInfo() {
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

    @Test
    void cvsUserInfo() {
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

    @Test
    void fileBasedir() {
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

    @Test
    void emptyBasedir() {
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org:80/"));
        assertEquals("/", PathUtils.basedir("http://www.codehaus.org/"));
        assertEquals("/", PathUtils.basedir("HTTP://www.codehaus.org/"));
    }

    @Test
    void emptyProtocol() {
        assertEquals("", PathUtils.protocol("placeholder-only"));
        assertEquals("", PathUtils.protocol("placeholder-only/module-a"));

        assertEquals("placeholder-only", PathUtils.authorization("placeholder-only"));
        assertEquals("placeholder-only", PathUtils.authorization("placeholder-only/module-a"));

        assertEquals(-1, PathUtils.port("placeholder-only"));
        assertEquals(-1, PathUtils.port("placeholder-only/module-a"));

        assertEquals("/", PathUtils.basedir("placeholder-only"));
        assertEquals("/module-a", PathUtils.basedir("placeholder-only/module-a"));
    }

    @Test
    void portResolving() {
        assertEquals(80, PathUtils.port("http://www.codehause.org:80/maven"));
        assertEquals(80, PathUtils.port("HTTP://www.codehause.org:80/maven"));
        assertEquals(WagonConstants.UNKNOWN_PORT, PathUtils.port("http://localhost/temp"));

        assertEquals(10, PathUtils.port("ftp://localhost:10"));
        assertEquals(10, PathUtils.port("FTP://localhost:10"));
    }

    @Test
    void scmPortResolving() {
        assertEquals(80, PathUtils.port("scm:svn:http://www.codehaus.org:80/maven"));
        assertEquals(80, PathUtils.port("SCM:SVN:HTTP://www.codehaus.org:80/maven"));
        assertEquals(WagonConstants.UNKNOWN_PORT, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:/temp:module"));

        assertEquals(2402, PathUtils.port("scm:cvs:pserver:anoncvs@localhost:2402/temp:module"));
        assertEquals(2402, PathUtils.port("SCM:CVS:pserver:anoncvs@localhost:2402/temp:module"));
    }

    @Test
    void scmBasedir() {
        assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org/maven"));
        assertEquals("/maven", PathUtils.basedir("SCM:SVN:HTTP://www.codehause.org/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:svn:http://www.codehause.org:80/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven"));
        assertEquals("/maven", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven"));
        assertEquals("/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:80/maven:module"));
        assertEquals("/maven/module", PathUtils.basedir("scm:cvs:pserver:anoncvs@www.codehause.org:/maven:module"));
        assertEquals("/maven/module", PathUtils.basedir("SCM:CVS:pserver:anoncvs@www.codehause.org:/maven:module"));
    }

    @Test
    void portBasedir() {
        assertEquals("/maven", PathUtils.basedir("http://www.codehause.org:80/maven"));
        assertEquals("/temp", PathUtils.basedir("http://localhost/temp"));

        assertEquals("c:/temp", PathUtils.basedir("file://c:/temp"));
        assertEquals("/", PathUtils.basedir("http://localhost:80/"));
        assertEquals("/", PathUtils.basedir("http://localhost/"));
    }

    @Test
    void ipV4() {
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
    void ipv6() {
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

    @Test
    void toRelative() {
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
