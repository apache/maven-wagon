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
package org.apache.maven.wagon.providers.http;

import java.io.File;

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.getName;

/**
 * User: jdumay Date: 24/01/2008 Time: 17:17:34
 */
public class HttpWagonErrorTest extends HttpWagonHttpServerTestCase {
    private int serverPort;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ServletHolder servlets = new ServletHolder(new ErrorWithMessageServlet());
        context.addServlet(servlets, "/*");
        startServer();
        serverPort = getPort();
    }

    @Test
    public void testGet401() throws Exception {
        Exception thrown = null;

        try {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl("http://localhost:" + serverPort);

            wagon.connect(testRepository);

            File destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            wagon.get("401", destFile);

            wagon.disconnect();
        } catch (Exception e) {
            thrown = e;
        } finally {
            stopServer();
        }

        assertNotNull(thrown);
        assertEquals(AuthorizationException.class, thrown.getClass());
        assertEquals(
                "authentication failed for http://localhost:" + serverPort + "/401, status: 401 "
                        + ErrorWithMessageServlet.MESSAGE,
                thrown.getMessage());
    }

    @Test
    public void testGet403() throws Exception {
        Exception thrown = null;

        try {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl("http://localhost:" + serverPort);

            wagon.connect(testRepository);

            File destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            wagon.get("403", destFile);

            wagon.disconnect();
        } catch (Exception e) {
            thrown = e;
        } finally {
            stopServer();
        }

        assertNotNull(thrown);
        assertEquals(AuthorizationException.class, thrown.getClass());
        assertEquals(
                "authorization failed for http://localhost:" + serverPort + "/403, status: 403 "
                        + ErrorWithMessageServlet.MESSAGE,
                thrown.getMessage());
    }

    @Test
    public void testGet404() throws Exception {
        Exception thrown = null;

        try {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl("http://localhost:" + serverPort);

            wagon.connect(testRepository);

            File destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            wagon.get("404", destFile);

            wagon.disconnect();
        } catch (Exception e) {
            thrown = e;
        } finally {
            stopServer();
        }

        assertNotNull(thrown);
        assertEquals(ResourceDoesNotExistException.class, thrown.getClass());
        assertEquals(
                "resource missing at http://localhost:" + serverPort + "/404, status: 404 "
                        + ErrorWithMessageServlet.MESSAGE,
                thrown.getMessage());
    }

    @Test
    public void testGet407() throws Exception {
        Exception thrown = null;

        try {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl("http://localhost:" + getPort());

            wagon.connect(testRepository);

            File destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            wagon.get("407", destFile);

            wagon.disconnect();
        } catch (Exception e) {
            thrown = e;
        } finally {
            stopServer();
        }

        assertNotNull(thrown);
        assertEquals(AuthorizationException.class, thrown.getClass());
        assertEquals(
                "proxy authentication failed for http://localhost:" + serverPort + "/407, status: 407 "
                        + ErrorWithMessageServlet.MESSAGE,
                thrown.getMessage());
    }

    @Test
    public void testGet500() throws Exception {
        Exception thrown = null;

        try {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository();
            testRepository.setUrl("http://localhost:" + serverPort);

            wagon.connect(testRepository);

            File destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            wagon.get("500", destFile);

            wagon.disconnect();
        } catch (Exception e) {
            thrown = e;
        } finally {
            stopServer();
        }

        assertNotNull(thrown);
        assertEquals(TransferFailedException.class, thrown.getClass());
        assertEquals(
                "transfer failed for http://localhost:" + serverPort + "/500, status: 500 "
                        + ErrorWithMessageServlet.MESSAGE,
                thrown.getMessage());
    }
}
