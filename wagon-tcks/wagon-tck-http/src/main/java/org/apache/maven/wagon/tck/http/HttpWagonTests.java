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
package org.apache.maven.wagon.tck.http;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.tck.http.fixture.ServerFixture;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.wagon.tck.http.util.TestUtil.getResource;

public abstract class HttpWagonTests {

    private ServerFixture serverFixture;

    private static PlexusContainer container;

    private Wagon wagon;

    private static WagonTestCaseConfigurator configurator;

    private String baseUrl;

    private Repository repo;

    private final Set<Object> notificationTargets = new HashSet<>();

    // CHECKSTYLE_OFF: ConstantName
    protected static final Logger logger = LoggerFactory.getLogger(HttpWagonTests.class);
    // CHECKSTYLE_ON: ConstantName

    @Before
    public void beforeEach() throws Exception {
        serverFixture = new ServerFixture(isSsl());
        serverFixture.start();
        wagon = (Wagon) container.lookup(Wagon.ROLE, configurator.getWagonHint());
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        File keystore = getResource(ServerFixture.SERVER_SSL_KEYSTORE_RESOURCE_PATH);

        System.setProperty("javax.net.ssl.keyStore", keystore.getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", ServerFixture.SERVER_SSL_KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStore", keystore.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", ServerFixture.SERVER_SSL_KEYSTORE_PASSWORD);

        container = new DefaultPlexusContainer();

        configurator = (WagonTestCaseConfigurator) container.lookup(WagonTestCaseConfigurator.class.getName());
    }

    @After
    public void afterEach() {
        try {
            wagon.disconnect();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        for (Object obj : notificationTargets) {
            synchronized (obj) {
                obj.notify();
            }
        }

        if (serverFixture != null) {
            try {
                serverFixture.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            container.release(wagon);
        } catch (ComponentLifecycleException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void afterAll() {
        if (container != null) {
            try {
                container.release(configurator);
            } catch (ComponentLifecycleException e) {
                e.printStackTrace();
            }

            container.dispose();
        }
    }

    protected void addNotificationTarget(final Object target) {
        notificationTargets.add(target);
    }

    protected File newTempFile() throws IOException {
        File f = File.createTempFile("wagon-target.", ".file");
        f.deleteOnExit();

        return f;
    }

    protected boolean isSsl() {
        return false;
    }

    protected ProxyInfo newProxyInfo() {
        ProxyInfo info = new ProxyInfo();
        info.setType(isSsl() ? "https" : "http");
        info.setHost(ServerFixture.SERVER_HOST);
        info.setPort(getPort());

        return info;
    }

    protected boolean isSupported() {
        StackTraceElement[] elements = new Throwable().getStackTrace();
        String testCaseId = null;
        String lastMethodName = null;
        for (StackTraceElement e : elements) {
            if (!e.getClassName().startsWith(getClass().getPackage().getName())) {
                testCaseId = lastMethodName;
                break;
            } else {
                lastMethodName = e.getMethodName();
            }
        }

        if (testCaseId == null || !configurator.isSupported(testCaseId)) {
            logger.error("Cannot run test: " + testCaseId + ". Wagon under test does not support this test case.");
            return false;
        }

        return true;
    }

    protected boolean initTest(final AuthenticationInfo auth, final ProxyInfo proxy)
            throws ComponentConfigurationException, ConnectionException, AuthenticationException {
        return initTest(getBaseUrl(), auth, proxy);
    }

    protected boolean initTest(final String baseUrl, final AuthenticationInfo auth, final ProxyInfo proxy)
            throws ComponentConfigurationException, ConnectionException, AuthenticationException {
        StackTraceElement[] elements = new Throwable().getStackTrace();
        String testCaseId = null;
        String lastMethodName = null;
        for (StackTraceElement e : elements) {
            if (!e.getClassName().startsWith(getClass().getPackage().getName())) {
                testCaseId = lastMethodName;
                break;
            } else {
                lastMethodName = e.getMethodName();
            }
        }

        if (testCaseId == null || !configurator.configureWagonForTest(wagon, testCaseId)) {
            logger.error("Cannot run test: " + testCaseId + ". Wagon under test does not support this test case.");

            return false;
        }

        try {
            serverFixture.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start: " + e.getMessage(), e);
        }

        repo = new Repository("test", baseUrl);

        wagon.connect(repo, auth, proxy);

        return true;
    }

    protected int getPort() {
        return serverFixture.getHttpPort();
    }

    protected String getBaseUrl() {
        if (baseUrl == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(isSsl() ? "https" : "http");
            sb.append("://" + ServerFixture.SERVER_HOST + ":");
            sb.append(getPort());

            baseUrl = sb.toString();
        }

        return baseUrl;
    }

    protected ServerFixture getServerFixture() {
        return serverFixture;
    }

    protected static PlexusContainer getContainer() {
        return container;
    }

    protected Wagon getWagon() {
        return wagon;
    }
}
