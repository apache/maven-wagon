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
package org.apache.maven.wagon.providers.ssh.jsch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.testing.PlexusTestConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@PlexusTest
public class ScpWagonWithProxyTest implements PlexusTestConfiguration {
    @Override
    public void customizeConfiguration(ContainerConfiguration configuration) {
        // the providers are @Named beans now, so the container has to read META-INF/sisu
        configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    }

    // injected by the container so the wagon keeps the collaborators AbstractJschWagon
    // declares with @Inject; constructing it directly would leave them null
    @Inject
    @Named("scp")
    private Wagon wagon;

    private boolean handled;

    @Test
    public void testHttpProxy() throws Exception {
        handled = false;
        Handler handler = new AbstractHandler() {
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                assertEquals("CONNECT", request.getMethod());

                handled = true;
                baseRequest.setHandled(true);
            }
        };

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();

        int port = connector.getLocalPort();
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost("localhost");
        proxyInfo.setPort(port);
        proxyInfo.setType("http");
        proxyInfo.setNonProxyHosts(null);

        try {
            wagon.connect(new Repository("id", "scp://localhost/tmp"), proxyInfo);
            fail();
        } catch (AuthenticationException e) {
            assertTrue(handled);
        } finally {
            wagon.disconnect();
            server.stop();
        }
    }

    @Test
    public void testSocksProxy() throws Exception {
        handled = false;

        int port = (Math.abs(new Random().nextInt()) % 2048) + 1024;

        SocksServer s = new SocksServer(port);
        Thread t = new Thread(s);
        t.setDaemon(true);
        t.start();

        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost("localhost");
        proxyInfo.setPort(port);
        proxyInfo.setType("socks_5");
        proxyInfo.setNonProxyHosts(null);

        try {
            wagon.connect(new Repository("id", "scp://localhost/tmp"), proxyInfo);
            fail();
        } catch (AuthenticationException e) {
            assertTrue(handled);
        } finally {
            wagon.disconnect();
            t.interrupt();
        }
    }

    private final class SocksServer implements Runnable {

        private final int port;

        private String userAgent;

        private SocksServer(int port) {
            this.port = port;
        }

        public void run() {
            ServerSocket ssock = null;
            try {
                try {
                    ssock = new ServerSocket(port);
                    ssock.setSoTimeout(2000);
                } catch (IOException e) {
                    return;
                }

                while (!Thread.currentThread().isInterrupted()) {
                    Socket sock = null;
                    try {
                        try {
                            sock = ssock.accept();
                        } catch (SocketTimeoutException e) {
                            continue;
                        }

                        handled = true;
                    } catch (IOException e) {
                    } finally {
                        if (sock != null) {
                            try {
                                sock.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            } finally {
                if (ssock != null) {
                    try {
                        ssock.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
