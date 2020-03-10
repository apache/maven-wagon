package org.apache.maven.wagon.providers.http;

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

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.codehaus.plexus.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class LightweightHttpWagonTest
    extends HttpWagonTestCase
{
    protected String getProtocol()
    {
        return "http";
    }

    protected String getTestRepositoryUrl()
    {
        return getProtocol() + "://localhost:" + getTestRepositoryPort() + "/";
    }

    protected void setHttpConfiguration( StreamingWagon wagon, Properties headers, Properties params )
    {
        ( (LightweightHttpWagon) wagon ).setHttpHeaders( headers );
    }

    @Override
    protected boolean supportPreemptiveAuthenticationGet()
    {
        return false;
    }

    @Override
    protected boolean supportPreemptiveAuthenticationPut()
    {
        return false;
    }

    @Override
    protected boolean supportProxyPreemptiveAuthentication()
    {
        return false;
    }

    @Override
    protected void verifyWagonExceptionMessage( Exception e, int forStatusCode, String forUrl, String forReasonPhrase )
    {

        // HttpUrlConnection prevents direct API access to the response code or reasonPhrase for any
        // status code >= 400. So all we can do is check WagonException wraps the HttpUrlConnection
        // thrown IOException / FileNotFoundException as a cause, if cause is not null

        assertNotNull( e );
        try
        {
            assertTrue( "only verify instances of WagonException", e instanceof WagonException );

            String assertMessageForBadMessage = "exception message not described properly: ";
            switch ( forStatusCode )
            {
                case HttpServletResponse.SC_GONE:
                case HttpServletResponse.SC_NOT_FOUND:
                    assertTrue( "404 or 410 should throw ResourceDoesNotExistException",
                            e instanceof ResourceDoesNotExistException );

                    if ( e.getCause() != null )
                    {
                        assertTrue( "ResourceDoesNotExistException should have the expected cause",
                                e.getCause() instanceof FileNotFoundException );
                        // the status code and reason phrase cannot always be learned due to implementation limitations
                        // which means the message may not include them
                        assertEquals( assertMessageForBadMessage, "Resource missing at " + forUrl, e.getMessage() );
                    }
                    else
                    {
                        assertEquals( assertMessageForBadMessage, "Resource missing at " + forUrl
                                + " " + forStatusCode + " " + forReasonPhrase, e.getMessage() );
                    }

                    break;

                case HttpServletResponse.SC_FORBIDDEN:
                    assertTrue( "403 Forbidden throws AuthorizationException",
                            e instanceof AuthorizationException );

                    assertEquals( assertMessageForBadMessage, "Authorization failed for " + forUrl + " 403"
                            + ( StringUtils.isEmpty( forReasonPhrase ) ? " Forbidden" : ( " " + forReasonPhrase ) ),
                            e.getMessage() );
                    break;

                case HttpServletResponse.SC_UNAUTHORIZED:
                    assertTrue( "401 Unauthorized throws AuthorizationException",
                            e instanceof AuthorizationException );

                    assertEquals( assertMessageForBadMessage, "Authentication failed for " + forUrl + " 401"
                                    + ( StringUtils.isEmpty( forReasonPhrase ) ? " Unauthorized" :
                                    ( " " + forReasonPhrase ) ),
                            e.getMessage() );
                    break;

                default:
                    assertTrue( "general exception must be TransferFailedException",
                            e instanceof TransferFailedException );
                    assertTrue( "expected status code for transfer failures should be >= 400, but none of "
                                    + " the already handled codes",
                            forStatusCode >= HttpServletResponse.SC_BAD_REQUEST );

                    if ( e.getCause() != null )
                    {
                        assertTrue( "TransferFailedException should have the original cause for diagnosis",
                                    e.getCause() instanceof IOException );
                    }

                    // the status code and reason phrase cannot always be learned due to implementation limitations
                    // so the message may not include them, but the implementation should use a consistent format
                    assertTrue( "message should always include url",
                            e.getMessage().startsWith( "Transfer failed for " + forUrl ) );

                    if ( e.getMessage().length() > ( "Transfer failed for " + forUrl ).length() )
                    {
                        assertTrue( "message should include url and status code",
                                e.getMessage().startsWith( "Transfer failed for " + forUrl + " " + forStatusCode ) );
                    }

                    if ( e.getMessage().length() > ( "Transfer failed for " + forUrl + " " + forStatusCode ).length() )
                    {
                        assertEquals( "message should include url and status code and reason phrase",
                                "Transfer failed for " + forUrl + " " + forStatusCode + " " + forReasonPhrase,
                                e.getMessage() );
                    }

                    break;
            }

        }
        catch ( AssertionError assertionError )
        {
            logger.error( "Exception which failed assertions: ", e );
            throw assertionError;
        }
    }

}
