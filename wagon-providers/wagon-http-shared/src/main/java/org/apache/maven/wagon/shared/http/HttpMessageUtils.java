package org.apache.maven.wagon.shared.http;

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
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.util.StringUtils;

/**
 * Helper for HTTP related messages.
 *
 * @since 3.3.4
 */
public class HttpMessageUtils
{
    // status codes here to avoid checkstyle magic number and not have have hard depend on non-wagon classes
    private static final int SC_UNAUTHORIZED = 401;
    private static final int SC_FORBIDDEN = 403;
    private static final int SC_NOT_FOUND = 404;
    private static final int SC_PROXY_AUTH_REQUIRED = 407;
    private static final int SC_GONE = 410;

    /**
     * A HTTP status code used to indicate that the actual response status code is not known at time of message
     * generation.
     */
    public static final int UNKNOWN_STATUS_CODE = -1;

    /**
     * Format a consistent HTTP transfer debug message combining url, status code, status line reason phrase and HTTP
     * proxy server info.
     * <p>
     * Url will always be included in the message. A status code other than {@link #UNKNOWN_STATUS_CODE} will be
     * included. A reason phrase will only be included if non-empty and status code is not {@link #UNKNOWN_STATUS_CODE}.
     * Proxy information will only be included if not null.
     *
     * @param url          the required non-null URL associated with the message
     * @param statusCode   an HTTP response status code
     * @param reasonPhrase an HTTP status line reason phrase
     * @param proxyInfo    proxy server used during the transfer, may be null if none used
     * @return a formatted debug message combining the parameters of this method
     * @throws NullPointerException if url is null
     */
    public static String formatTransferDebugMessage( String url, int statusCode, String reasonPhrase,
                                                     ProxyInfo proxyInfo )
    {
        String msg = url;
        if ( statusCode != UNKNOWN_STATUS_CODE )
        {
            msg += " -- status code: " + statusCode;
            if ( StringUtils.isNotEmpty( reasonPhrase ) )
            {
                msg += ", reason phrase: " + reasonPhrase;
            }
        }
        if ( proxyInfo != null )
        {
            msg += " -- " + proxyInfo.toString();
        }
        return msg;
    }

    /**
     * Format a consistent message for HTTP related {@link TransferFailedException}.
     * <p>
     * This variation typically used in cases where there is no HTTP transfer response data to extract status code and
     * reason phrase from. Equivalent to calling {@link #formatTransferFailedMessage(String, int, String, ProxyInfo)}
     * with {@link #UNKNOWN_STATUS_CODE} and null reason phrase.
     *
     * @param url the URL associated with the message
     * @param proxyInfo proxy server used during the transfer, may be null if none used
     * @return a formatted failure message combining the parameters of this method
     */
    public static String formatTransferFailedMessage( String url, ProxyInfo proxyInfo )
    {
        return formatTransferFailedMessage( url, UNKNOWN_STATUS_CODE, null,
                proxyInfo );
    }

    /**
     * Format a consistent message for HTTP related {@link TransferFailedException}.
     *
     * @param url          the URL associated with the message
     * @param statusCode   an HTTP response status code or {@link #UNKNOWN_STATUS_CODE}
     * @param reasonPhrase an HTTP status line reason phrase or null if the reason phrase unknown
     * @param proxyInfo    proxy server used during the transfer, may be null if none used
     * @return a formatted failure message combining the parameters of this method
     */
    public static String formatTransferFailedMessage( String url, int statusCode, String reasonPhrase,
                                                      ProxyInfo proxyInfo )
    {
        String msg = "Transfer failed for " + url;
        if ( statusCode != UNKNOWN_STATUS_CODE )
        {
            msg += " " + statusCode;
            // deliberately a null check instead of empty check so that we avoid having to handle
            // all conceivable default status code messages
            if ( reasonPhrase != null )
            {
                msg += " " + reasonPhrase;
            }
        }
        if ( proxyInfo != null )
        {
            msg += " " + proxyInfo.toString();
        }
        return msg;
    }

    /**
     * Format a consistent message for HTTP related {@link AuthorizationException}.
     * <p>
     * The message will always include the URL and status code provided. If empty, the reason phrase is substituted with
     * a common reason based on status code. {@link ProxyInfo} is only included in the message if not null.
     *
     * @param url          the URL associated with the message
     * @param statusCode   an HTTP response status code related to auth
     * @param reasonPhrase an HTTP status line reason phrase
     * @param proxyInfo    proxy server used during the transfer, may be null if none used
     * @return a consistent message for a HTTP related {@link AuthorizationException}
     */
    public static String formatAuthorizationMessage( String url, int statusCode, String reasonPhrase,
                                                     ProxyInfo proxyInfo )
    {
        switch ( statusCode )
        {
            case SC_UNAUTHORIZED: // no credentials or auth was not valid
                return "Authentication failed for " + url + " " + statusCode
                        + ( StringUtils.isEmpty( reasonPhrase ) ? " Unauthorized" : " " + reasonPhrase );

            case SC_FORBIDDEN: // forbidden based on permissions usually
                return "Authorization failed for " + url + " " + statusCode
                        + ( StringUtils.isEmpty( reasonPhrase ) ? " Forbidden" : " " + reasonPhrase );

            case SC_PROXY_AUTH_REQUIRED:
                return "HTTP proxy server authentication failed for " + url + " " + statusCode
                        + ( StringUtils.isEmpty( reasonPhrase ) ? " Proxy Authentication Required"
                        : " " + reasonPhrase );
            default:
                break;
        }
        String msg = "Authorization failed for " + url;
        if ( statusCode != UNKNOWN_STATUS_CODE )
        {
            msg += " " + statusCode;
            if ( StringUtils.isNotEmpty( reasonPhrase ) )
            {
                msg += " " + reasonPhrase;
            }
        }
        if ( proxyInfo != null )
        {
            msg += " " + proxyInfo.toString();
        }
        return msg;

    }

    /**
     * Format a consistent message for HTTP related {@link ResourceDoesNotExistException}.
     * <p>
     * The message will always include the URL and status code provided. If empty, the reason phrase is substituted with
     * the commonly used reason phrases per status code. {@link ProxyInfo} is only included if not null.
     *
     * @param url          the URL associated with the message
     * @param statusCode   an HTTP response status code related to resources not being found
     * @param reasonPhrase an HTTP status line reason phrase
     * @param proxyInfo    proxy server used during the transfer, may be null if none used
     * @return a consistent message for a HTTP related {@link ResourceDoesNotExistException}
     */
    public static String formatResourceDoesNotExistMessage( String url, int statusCode, String reasonPhrase,
                                                            ProxyInfo proxyInfo )
    {
        String msg = "Resource missing at " + url;

        if ( statusCode != UNKNOWN_STATUS_CODE )
        {
            msg += " " + statusCode;

            if ( StringUtils.isNotEmpty( reasonPhrase ) )
            {
                msg += " " + reasonPhrase;
            }
            else
            {
                if ( statusCode == SC_NOT_FOUND )
                {
                    msg += " Not Found";
                }
                else if ( statusCode == SC_GONE )
                {
                    msg += " Gone";
                }
            }
        }
        if ( proxyInfo != null )
        {
            msg += " " + proxyInfo.toString();
        }
        return msg;
    }

}
