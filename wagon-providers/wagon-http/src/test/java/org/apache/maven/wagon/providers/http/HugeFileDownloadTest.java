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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 */
@PlexusTest
public class HugeFileDownloadTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HugeFileDownloadTest.class);

    private static final long HUGE_FILE_SIZE =
            Integer.valueOf(Integer.MAX_VALUE).longValue()
                    + Integer.valueOf(Integer.MAX_VALUE).longValue();

    private Server server;
    private ServerConnector connector;

    @Test
    public void testDownloadHugeFileWithContentLength() throws Exception {
        final File hugeFile = new File("target/hugefile.txt");
        if (!hugeFile.exists() || hugeFile.length() < HUGE_FILE_SIZE) {
            makeHugeFile(hugeFile);
        }

        server = new Server();
        connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        server.addConnector(connector);

        ServletContextHandler root = new ServletContextHandler(ServletContextHandler.SESSIONS);
        root.setResourceBase(new File("target").getAbsolutePath());
        ServletHolder servletHolder = new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                FileInputStream fis = new FileInputStream(hugeFile);

                resp.addHeader("Content-Length", String.valueOf(hugeFile.length()));
                IOUtil.copy(fis, resp.getOutputStream());
                fis.close();
            }
        });
        root.addServlet(servletHolder, "/*");
        server.setHandler(root);

        server.start();

        File dest = null;
        try {
            Wagon wagon = getWagon();
            wagon.connect(new Repository("id", "http://localhost:" + connector.getLocalPort()));

            dest = File.createTempFile("huge", "txt");

            LOGGER.info("Fetching 'hugefile.txt' with content length");
            wagon.get("hugefile.txt", dest);

            assertTrue(dest.length() >= HUGE_FILE_SIZE);
            LOGGER.info("The file was successfully fetched");

            wagon.disconnect();
        } finally {
            server.stop();
            dest.delete();
            hugeFile.delete();
        }
    }

    @Test
    public void testDownloadHugeFileWithChunked() throws Exception {
        final File hugeFile = new File("target/hugefile.txt");
        if (!hugeFile.exists() || hugeFile.length() < HUGE_FILE_SIZE) {
            makeHugeFile(hugeFile);
        }

        server = new Server();
        connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        server.addConnector(connector);

        ServletContextHandler root = new ServletContextHandler(ServletContextHandler.SESSIONS);
        root.setResourceBase(new File("target").getAbsolutePath());
        ServletHolder servletHolder = new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                FileInputStream fis = new FileInputStream(hugeFile);

                IOUtil.copy(fis, resp.getOutputStream());
                fis.close();
            }
        });
        root.addServlet(servletHolder, "/*");
        server.setHandler(root);

        server.start();

        File dest = null;
        try {
            Wagon wagon = getWagon();
            wagon.connect(new Repository("id", "http://localhost:" + connector.getLocalPort()));

            dest = File.createTempFile("huge", "txt");

            LOGGER.info("Fetching 'hugefile.txt' in chunks");
            wagon.get("hugefile.txt", dest);

            assertTrue(dest.length() >= HUGE_FILE_SIZE);
            LOGGER.info("The file was successfully fetched");

            wagon.disconnect();
        } finally {
            server.stop();
            dest.delete();
            hugeFile.delete();
        }
    }

    protected Wagon getWagon() throws Exception {
        Wagon wagon = new HttpWagon();

        Debug debug = new Debug();

        wagon.addSessionListener(debug);

        return wagon;
    }

    private void makeHugeFile(File hugeFile) throws Exception {
        LOGGER.info("Creating test file");
        final ByteBuffer buf = ByteBuffer.allocate(4).putInt(2);
        buf.rewind();

        final OpenOption[] options = {StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE
        };

        try (SeekableByteChannel channel = Files.newByteChannel(hugeFile.toPath(), options)) {
            channel.position(HUGE_FILE_SIZE);
            channel.write(buf);
        }
        LOGGER.info("Test file created");
    }
}
