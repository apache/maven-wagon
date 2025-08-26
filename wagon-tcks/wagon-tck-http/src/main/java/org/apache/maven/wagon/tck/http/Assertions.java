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

import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class Assertions {

    public static final int NO_RESPONSE_STATUS_CODE = -1;

    protected static final Logger LOGGER = LoggerFactory.getLogger(Assertions.class);

    public static void assertFileContentsFromResource(
            final String resourceBase, final String resourceName, final File output, final String whyWouldItFail)
            throws IOException {
        String content = readResource(resourceBase, resourceName);
        String test = new String(Files.readAllBytes(output.toPath()));

        assertEquals(content, test, whyWouldItFail);
    }

    private static String readResource(final String base, final String name) throws IOException {
        String url = base;
        if (!url.endsWith("/") && !name.startsWith("/")) {
            url += "/";
        }
        url += name;

        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        InputStream stream = cloader.getResourceAsStream(url);

        if (stream == null) {
            return null;
        }

        final String resource = IOUtil.toString(stream);
        stream.close();
        return resource;
    }

    /**
     * Assert a WagonException message contains required format and context based on the status code we expected to
     * trigger it in the first place.
     * <p>
     * This implementation represents the most desired assertions, but HttpWagonTestCase sub-classes could override
     * this method if a specific wagon representation makes it impossible to meet these assertions.
     *
     * @param e               an instance of {@link WagonException}
     * @param forStatusCode   the response status code that triggered the exception
     * @param forUrl          the url that triggered the exception
     * @param forReasonPhrase the optional status line reason phrase the server returned
     */
    public static void assertWagonExceptionMessage(
            Exception e, int forStatusCode, String forUrl, String forReasonPhrase, ProxyInfo proxyInfo) {
        // TODO: handle AuthenticationException for Wagon.connect() calls
        assertNotNull(e);
        try {
            assertInstanceOf(WagonException.class, e, "only verify instances of WagonException");

            String reasonPhrase;
            String assertMessageForBadMessage = "exception message not described properly";

            if (proxyInfo != null) {
                assertTrue(
                        e.getMessage().endsWith(proxyInfo.toString()),
                        "message should end with proxy information if proxy was used");
            }

            switch (forStatusCode) {
                case HttpServletResponse.SC_NOT_FOUND:
                    // TODO: add test for 410: Gone?
                    assertInstanceOf(
                            ResourceDoesNotExistException.class,
                            e,
                            "404 not found response should throw ResourceDoesNotExistException");
                    reasonPhrase = (forReasonPhrase == null || forReasonPhrase.isEmpty())
                            ? " Not Found"
                            : (" " + forReasonPhrase);
                    assertEquals(
                            assertMessageForBadMessage,
                            "resource missing at " + forUrl + ", status: 404" + reasonPhrase,
                            e.getMessage());
                    break;

                case HttpServletResponse.SC_UNAUTHORIZED:
                    // FIXME assumes Wagon.get()/put() returning 401 instead of Wagon.connect()
                    assertInstanceOf(
                            AuthorizationException.class,
                            e,
                            "401 Unauthorized should throw AuthorizationException since "
                                    + " AuthenticationException is not explicitly declared as thrown from wagon "
                                    + "methods");
                    reasonPhrase = (forReasonPhrase == null || forReasonPhrase.isEmpty())
                            ? " Unauthorized"
                            : (" " + forReasonPhrase);
                    assertEquals(
                            assertMessageForBadMessage,
                            "authentication failed for " + forUrl + ", status: 401" + reasonPhrase,
                            e.getMessage());
                    break;

                case HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED:
                    assertInstanceOf(
                            AuthorizationException.class,
                            e,
                            "407 Proxy authentication required should throw AuthorizationException");
                    reasonPhrase = (forReasonPhrase == null || forReasonPhrase.isEmpty())
                            ? " Proxy Authentication Required"
                            : (" " + forReasonPhrase);
                    assertEquals(
                            assertMessageForBadMessage,
                            "proxy authentication failed for " + forUrl + ", status: 407" + reasonPhrase,
                            e.getMessage());
                    break;

                case HttpServletResponse.SC_FORBIDDEN:
                    assertInstanceOf(
                            AuthorizationException.class, e, "403 Forbidden should throw AuthorizationException");
                    reasonPhrase = (forReasonPhrase == null || forReasonPhrase.isEmpty())
                            ? " Forbidden"
                            : (" " + forReasonPhrase);
                    assertEquals(
                            assertMessageForBadMessage,
                            "authorization failed for " + forUrl + ", status: 403" + reasonPhrase,
                            e.getMessage());
                    break;

                default:
                    assertInstanceOf(
                            TransferFailedException.class,
                            e,
                            "transfer failures should at least be wrapped in a TransferFailedException");

                    // the status code and reason phrase cannot always be learned due to implementation limitations
                    // so the message may not include them, but the implementation should use a consistent format
                    assertTrue(
                            e.getMessage().startsWith("transfer failed for " + forUrl),
                            "message should always include url tried: " + e.getMessage());

                    String statusCodeStr = forStatusCode == NO_RESPONSE_STATUS_CODE ? "" : ", status: " + forStatusCode;
                    if (forStatusCode != NO_RESPONSE_STATUS_CODE) {

                        assertTrue(
                                forStatusCode >= HttpServletResponse.SC_BAD_REQUEST,
                                "if there was a response status line, the status code should be >= 400");

                        if (e.getMessage().length() > ("transfer failed for " + forUrl).length()) {
                            assertTrue(
                                    e.getMessage().startsWith("transfer failed for " + forUrl + statusCodeStr),
                                    "message should include url and status code: " + e.getMessage());
                        }

                        reasonPhrase = forReasonPhrase == null ? "" : " " + forReasonPhrase;

                        if (!reasonPhrase.isEmpty()
                                && e.getMessage().length()
                                        > ("transfer failed for " + forUrl + statusCodeStr).length()) {
                            assertTrue(
                                    e.getMessage()
                                            .startsWith("transfer failed for " + forUrl + statusCodeStr + reasonPhrase),
                                    "message should include url and status code and reason phrase: " + e.getMessage());
                        }
                    }

                    break;
            }
        } catch (AssertionError assertionError) {
            LOGGER.error("Exception which failed assertions: ", e);
            throw assertionError;
        }
    }
}
