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
package org.apache.maven.wagon.repository;

import org.apache.maven.wagon.WagonConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:jvanzyl@maven.org">Jason van Zyl</a>
 *
 */
public class RepositoryTest {

    @Test
    void repositoryProperties() {
        Repository repository = new Repository("fake", "http://localhost");

        repository.setBasedir("directory");

        assertEquals("directory", repository.getBasedir());

        repository.setName("name");

        assertEquals("name", repository.getName());

        repository.setPort(0);

        assertEquals(0, repository.getPort());

        assertEquals("localhost", repository.getHost());

        repository.setUrl("http://www.ibiblio.org");

        assertEquals("http://www.ibiblio.org", repository.getUrl());

        assertEquals("http", repository.getProtocol());

        assertEquals("www.ibiblio.org", repository.getHost());

        assertEquals("/", repository.getBasedir());

        assertEquals(WagonConstants.UNKNOWN_PORT, repository.getPort());

        repository.setUrl("https://www.ibiblio.org:100/maven");

        assertEquals("https://www.ibiblio.org:100/maven", repository.getUrl());

        assertEquals("https", repository.getProtocol());

        assertEquals("www.ibiblio.org", repository.getHost());

        assertEquals("/maven", repository.getBasedir());

        assertEquals(100, repository.getPort());

        assertEquals("www.ibiblio.org", repository.getHost());

        repository.setBasedir("basedir");

        assertEquals("basedir", repository.getBasedir());

        repository.setUrl("http://brett:porter@www.ibiblio.org");

        assertEquals("http://www.ibiblio.org", repository.getUrl());

        repository.setUrl("http://brett@www.ibiblio.org");

        assertEquals("http://www.ibiblio.org", repository.getUrl());
    }

    @Test
    void ipv6() {
        assertRepository(
                "http://user:password@[fff:::1]:7891/oo/rest/users",
                "http://[fff:::1]:7891/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "fff:::1",
                7891);
        assertRepository(
                "http://[fff:::1]:7891/oo/rest/users",
                "http://[fff:::1]:7891/oo/rest/users",
                "/oo/rest/users",
                null,
                null,
                "fff:::1",
                7891);
        assertRepository(
                "http://user:password@[fff:::1]/oo/rest/users",
                "http://[fff:::1]/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "fff:::1",
                -1);
        assertRepository(
                "http://user:password@[fff:::1]:7891",
                "http://[fff:::1]:7891",
                "/",
                "user",
                "password",
                "fff:::1",
                7891);

        assertRepository(
                "http://user:password@[fff:000::222:1111]:7891/oo/rest/users",
                "http://[fff:000::222:1111]:7891/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "fff:000::222:1111",
                7891);
        assertRepository(
                "http://[fff:000::222:1111]:7891/oo/rest/users",
                "http://[fff:000::222:1111]:7891/oo/rest/users",
                "/oo/rest/users",
                null,
                null,
                "fff:000::222:1111",
                7891);
        assertRepository(
                "http://user:password@[fff:000::222:1111]/oo/rest/users",
                "http://[fff:000::222:1111]/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "fff:000::222:1111",
                -1);
        assertRepository(
                "http://user:password@[fff:000::222:1111]:7891",
                "http://[fff:000::222:1111]:7891",
                "/",
                "user",
                "password",
                "fff:000::222:1111",
                7891);

        assertRepository(
                "http://user:password@16.60.56.58:7891/oo/rest/users",
                "http://16.60.56.58:7891/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "16.60.56.58",
                7891);
        assertRepository(
                "http://16.60.56.58:7891/oo/rest/users",
                "http://16.60.56.58:7891/oo/rest/users",
                "/oo/rest/users",
                null,
                null,
                "16.60.56.58",
                7891);
        assertRepository(
                "http://user:password@16.60.56.58/oo/rest/users",
                "http://16.60.56.58/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "16.60.56.58",
                -1);
        assertRepository(
                "http://user:password@16.60.56.58:7891",
                "http://16.60.56.58:7891",
                "/",
                "user",
                "password",
                "16.60.56.58",
                7891);

        assertRepository(
                "http://user:password@16.60.56.58:7891/oo/rest/users",
                "http://16.60.56.58:7891/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "16.60.56.58",
                7891);
        assertRepository(
                "http://16.60.56.58:7891/oo/rest/users",
                "http://16.60.56.58:7891/oo/rest/users",
                "/oo/rest/users",
                null,
                null,
                "16.60.56.58",
                7891);
        assertRepository(
                "http://user:password@16.60.56.58/oo/rest/users",
                "http://16.60.56.58/oo/rest/users",
                "/oo/rest/users",
                "user",
                "password",
                "16.60.56.58",
                -1);
        assertRepository(
                "http://user:password@16.60.56.58:7891",
                "http://16.60.56.58:7891",
                "/",
                "user",
                "password",
                "16.60.56.58",
                7891);
    }

    private void assertRepository(
            String url, String repoUrl, String baseDir, String user, String password, String host, int port) {
        Repository repo = new Repository(String.valueOf(System.currentTimeMillis()), url);
        assertEquals(repoUrl, repo.getUrl());
        assertEquals(baseDir, repo.getBasedir());
        assertEquals(host, repo.getHost());
        assertEquals(user, repo.getUsername());
        assertEquals(password, repo.getPassword());
        assertEquals(port, repo.getPort());
    }
}
