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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.tck.http.fixture.ErrorCodeServlet;
import org.apache.maven.wagon.tck.http.fixture.LatencyServlet;
import org.apache.maven.wagon.tck.http.fixture.ProxyConnectionVerifierFilter;
import org.apache.maven.wagon.tck.http.fixture.RedirectionServlet;
import org.apache.maven.wagon.tck.http.fixture.ServerFixture;
import org.apache.maven.wagon.tck.http.fixture.ServletExceptionServlet;
import org.apache.maven.wagon.tck.http.util.ValueHolder;
import org.awaitility.Awaitility;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.maven.wagon.tck.http.Assertions.NO_RESPONSE_STATUS_CODE;
import static org.apache.maven.wagon.tck.http.Assertions.assertFileContentsFromResource;
import static org.apache.maven.wagon.tck.http.Assertions.assertWagonExceptionMessage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class GetWagonTests extends HttpWagonTests {
    private static final int TWO_SECONDS = 2000;
    private static final int ONE_MINUTE = 60000;

    @Test
    public void basic() throws Exception {
        testSuccessfulGet("base.txt");
    }

    @Test
    @Disabled("FIX ME!")
    public void proxied() throws Exception {
        getServerFixture().addFilter("*", new ProxyConnectionVerifierFilter());

        ProxyInfo info = newProxyInfo();
        if (!initTest(null, info)) {
            return;
        }

        File target = newTempFile();
        getWagon().get("base.txt", target);

        assertFileContentsFromResource(
                ServerFixture.SERVER_ROOT_RESOURCE_PATH, "base.txt", target, "Downloaded file doesn't match original.");
    }

    @Test
    public void highLatencyHighTimeout() throws Exception {
        getServerFixture().addServlet("/slow/*", new LatencyServlet(TWO_SECONDS));
        testSuccessfulGet("slow/large.txt", "large.txt");
    }

    @Test
    @Disabled
    public void highLatencyLowTimeout() throws Exception {
        Servlet servlet = new LatencyServlet(TWO_SECONDS);
        getServerFixture().addServlet("/slow/*", servlet);
        testSuccessfulGet("slow/large.txt", "large.txt");
    }

    @Test
    @Disabled
    public void inifiniteLatencyTimeout() throws Exception {
        if (!isSupported()) {
            return;
        }

        final ValueHolder<TransferFailedException> holder = new ValueHolder<>(null);

        Runnable r = () -> {
            Servlet servlet = new LatencyServlet(-1);
            addNotificationTarget(servlet);

            getServerFixture().addServlet("/infinite/*", servlet);
            try {
                if (!initTest(null, null)) {
                    return;
                }

                if (getWagon() instanceof StreamWagon) {
                    logger.info("Connection timeout is: " + getWagon().getTimeout());
                }

                File target = newTempFile();
                getWagon().get("infinite/", target);

                fail("Should have failed to transfer due to transaction timeout.");
            } catch (ConnectionException
                    | AuthenticationException
                    | ResourceDoesNotExistException
                    | AuthorizationException
                    | IOException
                    | ComponentConfigurationException e) {
                throw new IllegalStateException(e);
            } catch (TransferFailedException e) {
                // expected
                holder.setValue(e);
            }
        };

        Thread t = new Thread(r);
        t.start();

        //        try {
        //            logger.info("Waiting 60 seconds for wagon timeout.");
        //            t.join(ONE_MINUTE);
        //        } catch (InterruptedException e) {
        //            e.printStackTrace();
        //        }
        //        logger.info("Interrupting thread.");
        //        t.interrupt();

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> holder.getValue() != null);

        assertNotNull(holder.getValue(), "TransferFailedException should have been thrown.");
        assertWagonExceptionMessage(holder.getValue(), NO_RESPONSE_STATUS_CODE, getBaseUrl() + "infinite/", "", null);
    }

    @Test
    public void nonExistentHost()
            throws ConnectionException, AuthenticationException, IOException, ComponentConfigurationException {
        // we use a invalid localhost URL since some Internet Service Providers lately
        // use funny 'search-DNS' which don't handle explicitly marked testing DNS properly.
        // According to RFC-2606 .test, .invalid TLDs etc should work, but in practice it doesn't :(
        if (!initTest("http://localhost:65520", null, null)) {
            return;
        }

        File target = newTempFile();

        TransferFailedException e =
                assertThrows(TransferFailedException.class, () -> getWagon().get("base.txt", target));
        assertWagonExceptionMessage(e, NO_RESPONSE_STATUS_CODE, "http://localhost:65520/base.txt", null, null);
    }

    @Test
    public void oneLevelPermanentMove() throws Exception {
        getServerFixture()
                .addServlet(
                        "/moved.txt", new RedirectionServlet(HttpServletResponse.SC_MOVED_PERMANENTLY, "/base.txt"));

        testSuccessfulGet("moved.txt");
    }

    @Test
    public void oneLevelTemporaryMove() throws Exception {
        getServerFixture()
                .addServlet(
                        "/moved.txt", new RedirectionServlet(HttpServletResponse.SC_MOVED_TEMPORARILY, "/base.txt"));

        testSuccessfulGet("moved.txt");
    }

    @Test
    public void sixLevelPermanentMove() throws Exception {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath + "/*",
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_PERMANENTLY, myPath, targetPath, 6));

        testSuccessfulGet(myPath);
    }

    @Test
    public void sixLevelTemporaryMove() throws Exception {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath + "/*",
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_TEMPORARILY, myPath, targetPath, 6));

        testSuccessfulGet(myPath);
    }

    @Test
    public void infinitePermanentMove() {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath,
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_PERMANENTLY, myPath, targetPath, -1));

        assertThrows(TransferFailedException.class, () -> testSuccessfulGet(myPath));
    }

    @Test
    public void infiniteTemporaryMove() {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath,
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_TEMPORARILY, myPath, targetPath, -1));

        assertThrows(TransferFailedException.class, () -> testSuccessfulGet(myPath));
    }

    /**
     * NOTE: This test depends on a {@link WagonTestCaseConfigurator} configuration to limit redirects to 20. In the
     * case of the Sun HTTP implementation, this is the default limit.
     */
    @Test
    @SuppressWarnings("checkstyle:methodname")
    public void permanentMove_TooManyRedirects_limit20() {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath,
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_PERMANENTLY, myPath, targetPath, -1));

        assertThrows(TransferFailedException.class, () -> testSuccessfulGet(myPath));
    }

    /**
     * NOTE: This test depends on a {@link WagonTestCaseConfigurator} configuration to limit redirects to 20. In the
     * case of the Sun HTTP implementation, this is the default limit.
     */
    @Test
    @SuppressWarnings("checkstyle:methodname")
    public void temporaryMove_TooManyRedirects_limit20() {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture()
                .addServlet(
                        "/" + myPath,
                        new RedirectionServlet(HttpServletResponse.SC_MOVED_TEMPORARILY, myPath, targetPath, -1));

        assertThrows(TransferFailedException.class, () -> testSuccessfulGet(myPath));
    }

    @Test
    public void missing() throws Exception {
        if (!initTest(null, null)) {
            return;
        }

        File target = newTempFile();
        assertThrows(ResourceDoesNotExistException.class, () -> getWagon().get("404.txt", target));
    }

    @Test
    public void error() throws Exception {
        testErrorHandling(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void proxyTimeout() throws Exception {
        testErrorHandling(HttpServletResponse.SC_GATEWAY_TIMEOUT);
    }

    @Test
    public void forbidden() throws Exception {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName("user");
        info.setPassword("password");

        getServerFixture().addUser(info.getUserName(), "password");

        getServerFixture()
                .addServlet("/403.txt", new ErrorCodeServlet(HttpServletResponse.SC_FORBIDDEN, "Expected 403"));

        testAuthFailure("403.txt", info);
    }

    @Test
    public void successfulAuthentication() throws Exception {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName("user");
        info.setPassword("password");

        getServerFixture().addUser(info.getUserName(), info.getPassword());

        if (!initTest(info, null)) {
            return;
        }

        File target = newTempFile();
        getWagon().get("protected/base.txt", target);

        assertFileContentsFromResource(
                ServerFixture.SERVER_ROOT_RESOURCE_PATH, "base.txt", target, "Downloaded file doesn't match original.");
    }

    @Test
    public void unsuccessfulAuthentication() throws Exception {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName("user");
        info.setPassword("password");

        getServerFixture().addUser(info.getUserName(), "anotherPassword");

        testAuthFailure("protected/base.txt", info);
    }

    protected void testAuthFailure(final String path, final AuthenticationInfo info) throws Exception {
        boolean authFailure = false;
        try {
            if (!initTest(info, null)) {
                return;
            }
        } catch (AuthenticationException e) {
            // expected
            authFailure = true;
        }

        File target = newTempFile();
        try {
            getWagon().get(path, target);
        } catch (AuthorizationException e) {
            // expected
            authFailure = true;
        }

        assertTrue(authFailure, "Authentication/Authorization should have failed.");
    }

    protected void testSuccessfulGet(final String path) throws Exception {
        testSuccessfulGet(path, "base.txt");
    }

    protected void testSuccessfulGet(final String path, final String checkPath) throws Exception {
        if (!initTest(null, null)) {
            return;
        }

        if (getWagon() instanceof StreamWagon) {
            logger.info("Connection timeout is: " + getWagon().getTimeout());
        }

        File target = newTempFile();
        getWagon().get(path, target);

        assertFileContentsFromResource(
                ServerFixture.SERVER_ROOT_RESOURCE_PATH, checkPath, target, "Downloaded file doesn't match original.");
    }

    protected void testErrorHandling(final int code) throws Exception {
        if (code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            getServerFixture().addServlet("/" + code + ".txt", new ServletExceptionServlet("Expected " + code));
        } else {
            getServerFixture().addServlet("/" + code + ".txt", new ErrorCodeServlet(code, "Expected " + code));
        }

        if (!initTest(null, null)) {
            return;
        }
        File target = newTempFile();
        assertThrows(TransferFailedException.class, () -> getWagon().get(code + ".txt", target));
    }
}
