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
package org.apache.maven.wagon.shared.http;

import java.util.Objects;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * Helper for HTTP related messages.
 * <p>
 * <b>Important notice on Reason Phrase</b>:
 * <ul>
 * <li>reason phrase was <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html">defined by initial HTTP/1.1
 * RFC 2616</a>: <cite>The Reason-Phrase is intended to give a short textual description of the Status-Code. The
 * Status-Code is intended for use by automata and the Reason-Phrase is intended for the human user. The client is not
 * required to examine or display the Reason- Phrase.</cite></li>
 * <li>it has been later largely deprecated in <a href="https://tools.ietf.org/html/rfc7230#section-3.1.2">the updated
 * HTTP/1.1 in RFC 7230</a>: <cite>The reason-phrase element exists for the sole purpose of providing a textual
 * description associated with the numeric status code, mostly out of deference to earlier Internet application
 * protocols that were more frequently used with interactive text clients. A client SHOULD ignore the reason-phrase
 * content.</cite></li>
 * <li>it has been removed from <a href="https://tools.ietf.org/html/rfc7540#section-8.1.2.4">HTTP/2 in RFC 7540</a>:
 * <cite>HTTP/2 does not define a way to carry the version or reason phrase that is included in an HTTP/1.1 status
 * line.</cite>.</li>
 * </ul>
 * The use of Reason Phrase done here to improve the message to the end-user (particularly in case of failures) will
 * disappear while HTTP/2 is deployed: a new mechanism to provide such a message needs to be defined...
 *
 * @since 3.3.4
 */
public class HttpMessageUtils {
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
     * Format a consistent HTTP transfer debug message combining URL, status code, reason phrase and HTTP
     * proxy server info.
     * <p>
     * URL will always be included in the message. A status code other than {@link #UNKNOWN_STATUS_CODE} will be
     * included. A reason phrase will only be included if non-empty and status code is not {@link #UNKNOWN_STATUS_CODE}.
     * Proxy information will only be included if not null.
     *
     * @param url          the required non-null URL associated with the message
     * @param statusCode   an HTTP response status code
     * @param reasonPhrase an HTTP reason phrase
     * @param proxyInfo    proxy server used during the transfer, may be null if none used
     * @return a formatted debug message combining the parameters of this method
     * @throws NullPointerException if url is null
     */
    public static String formatTransferDebugMessage(
            String url, int statusCode, String reasonPhrase, ProxyInfo proxyInfo) {
        Objects.requireNonNull(url, "url cannot be null");
        String msg = url;
        if (statusCode != UNKNOWN_STATUS_CODE) {
            msg += " -- status code: " + statusCode;
            if (reasonPhrase != null && !reasonPhrase.isEmpty()) {
                msg += ", reason phrase: " + reasonPhrase;
            }
        }
        if (proxyInfo != null) {
            msg += " -- proxy: " + proxyInfo;
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
    public static String formatTransferFailedMessage(String url, ProxyInfo proxyInfo) {
        return formatTransferFailedMessage(url, UNKNOWN_STATUS_CODE, null, proxyInfo);
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
    public static String formatTransferFailedMessage(
            String url, int statusCode, String reasonPhrase, ProxyInfo proxyInfo) {
        return formatMessage("transfer failed for ", url, statusCode, reasonPhrase, proxyInfo);
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
    // TODO Split when WAGON-568 is implemented
    public static String formatAuthorizationMessage(
            String url, int statusCode, String reasonPhrase, ProxyInfo proxyInfo) {
        switch (statusCode) {
            case SC_UNAUTHORIZED: // no credentials or auth was not valid
                return formatMessage("authentication failed for ", url, statusCode, reasonPhrase, null);

            case SC_FORBIDDEN: // forbidden based on permissions usually
                return formatMessage("authorization failed for ", url, statusCode, reasonPhrase, null);

            case SC_PROXY_AUTH_REQUIRED:
                return formatMessage("proxy authentication failed for ", url, statusCode, reasonPhrase, null);

            default:
                break;
        }

        return formatMessage("authorization failed for ", url, statusCode, reasonPhrase, proxyInfo);
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
    public static String formatResourceDoesNotExistMessage(
            String url, int statusCode, String reasonPhrase, ProxyInfo proxyInfo) {
        return formatMessage("resource missing at ", url, statusCode, reasonPhrase, proxyInfo);
    }

    private static String formatMessage(
            String message, String url, int statusCode, String reasonPhrase, ProxyInfo proxyInfo) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(url, "url cannot be null");
        String msg = message + url;
        if (statusCode != UNKNOWN_STATUS_CODE) {
            msg += ", status: " + statusCode;

            if (reasonPhrase != null && !reasonPhrase.isEmpty()) {
                msg += " " + reasonPhrase;
            } else {
                switch (statusCode) {
                    case SC_UNAUTHORIZED:
                        msg += " Unauthorized";
                        break;

                    case SC_FORBIDDEN:
                        msg += " Forbidden";
                        break;

                    case SC_NOT_FOUND:
                        msg += " Not Found";
                        break;

                    case SC_PROXY_AUTH_REQUIRED:
                        msg += " Proxy Authentication Required";
                        break;

                    case SC_GONE:
                        msg += " Gone";
                        break;

                    default:
                        break;
                }
            }
        }
        if (proxyInfo != null) {
            msg += ", proxy: " + proxyInfo;
        }
        return msg;
    }
}
