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

import javax.servlet.http.HttpServletResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.http.HttpWagonTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class LightweightHttpWagonTest extends HttpWagonTestCase {
    protected String getProtocol() {
        return "http";
    }

    protected String getTestRepositoryUrl() {
        return getProtocol() + "://localhost:" + getTestRepositoryPort() + "/";
    }

    protected void setHttpConfiguration(StreamingWagon wagon, Properties headers, Properties params) {
        ((LightweightHttpWagon) wagon).setHttpHeaders(headers);
    }

    @Override
    protected boolean supportPreemptiveAuthenticationGet() {
        return false;
    }

    @Override
    protected boolean supportPreemptiveAuthenticationPut() {
        return false;
    }

    @Override
    protected boolean supportProxyPreemptiveAuthentication() {
        return false;
    }

    @Override
    protected void verifyWagonExceptionMessage(Exception e, int forStatusCode, String forUrl, String forReasonPhrase) {

        // HttpUrlConnection prevents direct API access to the response code or reasonPhrase for any
        // status code >= 400. So all we can do is check WagonException wraps the HttpUrlConnection
        // thrown IOException / FileNotFoundException as a cause, if cause is not null

        assertNotNull(e);
        try {
            assertTrue(e instanceof WagonException, "only verify instances of WagonException");

            String assertMessageForBadMessage = "exception message not described properly: ";
            switch (forStatusCode) {
                case HttpServletResponse.SC_GONE:
                case HttpServletResponse.SC_NOT_FOUND:
                    assertTrue(
                            e instanceof ResourceDoesNotExistException,
                            "404 or 410 should throw ResourceDoesNotExistException");

                    if (e.getCause() != null) {
                        assertTrue(
                                e.getCause() instanceof FileNotFoundException,
                                "ResourceDoesNotExistException should have the expected cause");
                        // the status code and reason phrase cannot always be learned due to implementation limitations
                        // which means the message may not include them
                        assertEquals("resource missing at " + forUrl, e.getMessage(), assertMessageForBadMessage);
                    } else {
                        assertEquals(
                                "resource missing at " + forUrl + ", status: " + forStatusCode + " " + forReasonPhrase,
                                e.getMessage(),
                                assertMessageForBadMessage);
                    }

                    break;

                case HttpServletResponse.SC_FORBIDDEN:
                    assertTrue(e instanceof AuthorizationException, "403 Forbidden throws AuthorizationException");

                    assertEquals(
                            "authorization failed for " + forUrl + ", status: 403"
                                    + ((forReasonPhrase == null || forReasonPhrase.isEmpty())
                                            ? " Forbidden"
                                            : (" " + forReasonPhrase)),
                            e.getMessage(),
                            assertMessageForBadMessage);
                    break;

                case HttpServletResponse.SC_UNAUTHORIZED:
                    assertTrue(e instanceof AuthorizationException, "401 Unauthorized throws AuthorizationException");

                    assertEquals(
                            "authentication failed for " + forUrl + ", status: 401"
                                    + ((forReasonPhrase == null || forReasonPhrase.isEmpty())
                                            ? " Unauthorized"
                                            : (" " + forReasonPhrase)),
                            e.getMessage(),
                            assertMessageForBadMessage);
                    break;

                default:
                    assertTrue(
                            e instanceof TransferFailedException, "general exception must be TransferFailedException");
                    assertTrue(
                            forStatusCode >= HttpServletResponse.SC_BAD_REQUEST,
                            "expected status code for transfer failures should be >= 400, but none of "
                                    + " the already handled codes");

                    if (e.getCause() != null) {
                        assertTrue(
                                e.getCause() instanceof IOException,
                                "TransferFailedException should have the original cause for diagnosis");
                    }

                    // the status code and reason phrase cannot always be learned due to implementation limitations
                    // so the message may not include them, but the implementation should use a consistent format
                    assertTrue(
                            e.getMessage().startsWith("transfer failed for " + forUrl),
                            "message should always include url");

                    if (e.getMessage().length() > ("transfer failed for " + forUrl).length()) {
                        assertTrue(
                                e.getMessage()
                                        .startsWith("transfer failed for " + forUrl + ", status: " + forStatusCode),
                                "message should include url and status code");
                    }

                    if (e.getMessage().length()
                            > ("transfer failed for " + forUrl + ", status: " + forStatusCode).length()) {
                        assertEquals(
                                "transfer failed for " + forUrl + ", status: " + forStatusCode + " " + forReasonPhrase,
                                e.getMessage(),
                                "message should include url and status code and reason phrase");
                    }

                    break;
            }

        } catch (AssertionError assertionError) {
            logger.error("Exception which failed assertions: ", e);
            throw assertionError;
        }
    }
}
