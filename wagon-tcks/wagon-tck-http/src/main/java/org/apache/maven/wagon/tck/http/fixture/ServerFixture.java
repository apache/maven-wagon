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
package org.apache.maven.wagon.tck.http.fixture;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.wagon.tck.http.util.TestUtil.getResource;

/**
 *
 */
public class ServerFixture {
    private static Logger logger = LoggerFactory.getLogger(ServerFixture.class);

    public static final String SERVER_ROOT_RESOURCE_PATH = "default-server-root";

    // it seems that some JDKs have a problem if you use different key stores
    // so we gonna reuse the keystore which is is used in the wagon implementations already
    public static final String SERVER_SSL_KEYSTORE_RESOURCE_PATH = "ssl/keystore";

    public static final String SERVER_SSL_KEYSTORE_PASSWORD = "wagonhttp";

    public static final String SERVER_HOST = "localhost";

    private final Server server;
    private ServerConnector serverConnector;

    private final WebAppContext webappContext;

    private final HashLoginService loginService;

    private final ConstraintSecurityHandler securityHandler;

    private int filterCount = 0;

    private int httpPort;

    public ServerFixture(final boolean ssl) throws URISyntaxException, IOException {
        server = new Server();
        if (ssl) {
            String keystore = getResource(SERVER_SSL_KEYSTORE_RESOURCE_PATH).getAbsolutePath();

            LoggerFactory.getLogger(ServerFixture.class).info("TCK Keystore path: " + keystore);
            System.setProperty("javax.net.ssl.keyStore", keystore);
            System.setProperty("javax.net.ssl.trustStore", keystore);

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keystore);
            sslContextFactory.setKeyStorePassword(SERVER_SSL_KEYSTORE_PASSWORD);
            sslContextFactory.setKeyManagerPassword(SERVER_SSL_KEYSTORE_PASSWORD);
            serverConnector = new ServerConnector(server, sslContextFactory);
            server.addConnector(serverConnector);
        } else {
            serverConnector = new ServerConnector(server);
            serverConnector.setHost("localhost");
            // connector.setPort( port );
            server.addConnector(serverConnector);
        }

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);

        constraint.setRoles(new String[] {"allowed"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/protected/*");

        securityHandler = new ConstraintSecurityHandler();

        loginService = new HashLoginService("Test Server");

        securityHandler.setLoginService(loginService);
        securityHandler.setConstraintMappings(new ConstraintMapping[] {cm});

        webappContext = new WebAppContext();
        webappContext.setContextPath("/");

        File base = getResource(SERVER_ROOT_RESOURCE_PATH);
        logger.info("docroot: " + base);
        webappContext.setWar(base.getAbsolutePath());
        webappContext.setHandler(securityHandler);

        SessionHandler sessionHandler = webappContext.getSessionHandler();
        ((AbstractSessionManager) sessionHandler.getSessionManager()).setUsingCookies(false);

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {webappContext, new DefaultHandler()});

        server.setHandler(handlers);
    }

    public void addFilter(final String pathSpec, final Filter filter) {
        String name = "filter" + filterCount++;

        FilterMapping fm = new FilterMapping();
        fm.setPathSpec(pathSpec);
        fm.setFilterName(name);

        FilterHolder fh = new FilterHolder(filter);
        fh.setName(name);

        webappContext.getServletHandler().addFilter(fh, fm);
    }

    public void addServlet(final String pathSpec, final Servlet servlet) {
        webappContext.getServletHandler().addServletWithMapping(new ServletHolder(servlet), pathSpec);
    }

    public void addUser(final String user, final String password) {
        loginService.putUser(user, new Password(password), new String[] {"allowed"});
    }

    public Server getServer() {
        return server;
    }

    public WebAppContext getWebappContext() {
        return webappContext;
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    public void start() throws Exception {
        if (server.isStarted() || server.isRunning()) {
            return;
        }
        server.start();

        int total = 0;
        while (total < 3 * 1000 && !server.isStarted()) {
            server.wait(10);
            total += 10;
        }

        if (!server.isStarted()) {
            throw new IllegalStateException("Server didn't start in: " + total + "ms.");
        }
        this.httpPort = serverConnector.getLocalPort();
    }

    public int getHttpPort() {
        return httpPort;
    }
}
