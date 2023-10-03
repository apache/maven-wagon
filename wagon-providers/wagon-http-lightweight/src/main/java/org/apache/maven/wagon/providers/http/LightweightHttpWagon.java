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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.EncodingUtil;
import org.codehaus.plexus.util.Base64;

import static java.lang.Integer.parseInt;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.UNKNOWN_STATUS_CODE;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatAuthorizationMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatResourceDoesNotExistMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatTransferFailedMessage;

/**
 * LightweightHttpWagon, using JDK's HttpURLConnection.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="http" instantiation-strategy="per-lookup"
 * @see HttpURLConnection
 */
public class LightweightHttpWagon extends StreamWagon {
    private boolean preemptiveAuthentication;

    private HttpURLConnection putConnection;

    private Proxy proxy = Proxy.NO_PROXY;

    private static final Pattern IOEXCEPTION_MESSAGE_PATTERN =
            Pattern.compile("Server returned HTTP response code: " + "(\\d\\d\\d) for URL: (.*)");

    public static final int MAX_REDIRECTS = 10;

    /**
     * Whether to use any proxy cache or not.
     *
     * @plexus.configuration default="false"
     */
    private boolean useCache;

    /**
     * @plexus.configuration
     */
    private Properties httpHeaders;

    /**
     * @plexus.requirement
     */
    private volatile LightweightHttpWagonAuthenticator authenticator;

    /**
     * Builds a complete URL string from the repository URL and the relative path of the resource passed.
     *
     * @param resource the resource to extract the relative path from.
     * @return the complete URL
     */
    private String buildUrl(Resource resource) {
        return EncodingUtil.encodeURLToString(getRepository().getUrl(), resource.getName());
    }

    public void fillInputData(InputData inputData)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = inputData.getResource();

        String visitingUrl = buildUrl(resource);

        List<String> visitedUrls = new ArrayList<>();

        for (int redirectCount = 0; redirectCount < MAX_REDIRECTS; redirectCount++) {
            if (visitedUrls.contains(visitingUrl)) {
                // TODO add a test for this message
                throw new TransferFailedException("Cyclic http redirect detected. Aborting! " + visitingUrl);
            }
            visitedUrls.add(visitingUrl);

            URL url = null;
            try {
                url = new URL(visitingUrl);
            } catch (MalformedURLException e) {
                // TODO add test for this
                throw new ResourceDoesNotExistException("Invalid repository URL: " + e.getMessage(), e);
            }

            HttpURLConnection urlConnection = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection(this.proxy);
            } catch (IOException e) {
                // TODO: add test for this
                String message = formatTransferFailedMessage(visitingUrl, UNKNOWN_STATUS_CODE, null, getProxyInfo());
                // TODO include e.getMessage appended to main message?
                throw new TransferFailedException(message, e);
            }

            try {

                urlConnection.setRequestProperty("Accept-Encoding", "gzip,deflate");
                if (!useCache) {
                    urlConnection.setRequestProperty("Pragma", "no-cache");
                }

                addHeaders(urlConnection);

                // TODO: handle all response codes
                int responseCode = urlConnection.getResponseCode();
                String reasonPhrase = urlConnection.getResponseMessage();

                // TODO Move 401/407 to AuthenticationException after WAGON-587
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN
                        || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
                        || responseCode == HttpURLConnection.HTTP_PROXY_AUTH) {
                    throw new AuthorizationException(
                            formatAuthorizationMessage(buildUrl(resource), responseCode, reasonPhrase, getProxyInfo()));
                }
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    visitingUrl = urlConnection.getHeaderField("Location");
                    continue;
                }

                InputStream is = urlConnection.getInputStream();
                String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                boolean isGZipped = contentEncoding != null && "gzip".equalsIgnoreCase(contentEncoding);
                if (isGZipped) {
                    is = new GZIPInputStream(is);
                }
                boolean isDeflated = contentEncoding != null && "deflate".equalsIgnoreCase(contentEncoding);
                if (isDeflated) {
                    is = new DeflaterInputStream(is);
                }
                inputData.setInputStream(is);
                resource.setLastModified(urlConnection.getLastModified());
                resource.setContentLength(urlConnection.getContentLength());
                break;

            } catch (FileNotFoundException e) {
                // this could be 404 Not Found or 410 Gone - we don't have access to which it was.
                // TODO: 2019-10-03 url used should list all visited/redirected urls, not just the original
                throw new ResourceDoesNotExistException(
                        formatResourceDoesNotExistMessage(
                                buildUrl(resource), UNKNOWN_STATUS_CODE, null, getProxyInfo()),
                        e);
            } catch (IOException originalIOException) {
                throw convertHttpUrlConnectionException(originalIOException, urlConnection, buildUrl(resource));
            }
        }
    }

    private void addHeaders(HttpURLConnection urlConnection) {
        if (httpHeaders != null) {
            for (Object header : httpHeaders.keySet()) {
                urlConnection.setRequestProperty((String) header, httpHeaders.getProperty((String) header));
            }
        }
        setAuthorization(urlConnection);
    }

    private void setAuthorization(HttpURLConnection urlConnection) {
        if (preemptiveAuthentication && authenticationInfo != null && authenticationInfo.getUserName() != null) {
            String credentials = authenticationInfo.getUserName() + ":" + authenticationInfo.getPassword();
            String encoded = new String(Base64.encodeBase64(credentials.getBytes()));
            urlConnection.setRequestProperty("Authorization", "Basic " + encoded);
        }
    }

    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        Resource resource = outputData.getResource();
        try {
            URL url = new URL(buildUrl(resource));
            putConnection = (HttpURLConnection) url.openConnection(this.proxy);

            addHeaders(putConnection);

            putConnection.setRequestMethod("PUT");
            putConnection.setDoOutput(true);
            outputData.setOutputStream(putConnection.getOutputStream());
        } catch (IOException e) {
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    protected void finishPutTransfer(Resource resource, InputStream input, OutputStream output)
            throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {
        try {
            String reasonPhrase = putConnection.getResponseMessage();
            int statusCode = putConnection.getResponseCode();

            switch (statusCode) {
                    // Success Codes
                case HttpURLConnection.HTTP_OK: // 200
                case HttpURLConnection.HTTP_CREATED: // 201
                case HttpURLConnection.HTTP_ACCEPTED: // 202
                case HttpURLConnection.HTTP_NO_CONTENT: // 204
                    break;

                    // TODO Move 401/407 to AuthenticationException after WAGON-587
                case HttpURLConnection.HTTP_FORBIDDEN:
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_PROXY_AUTH:
                    throw new AuthorizationException(
                            formatAuthorizationMessage(buildUrl(resource), statusCode, reasonPhrase, getProxyInfo()));

                case HttpURLConnection.HTTP_NOT_FOUND:
                case HttpURLConnection.HTTP_GONE:
                    throw new ResourceDoesNotExistException(formatResourceDoesNotExistMessage(
                            buildUrl(resource), statusCode, reasonPhrase, getProxyInfo()));

                    // add more entries here
                default:
                    throw new TransferFailedException(
                            formatTransferFailedMessage(buildUrl(resource), statusCode, reasonPhrase, getProxyInfo()));
            }
        } catch (IOException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
            throw convertHttpUrlConnectionException(e, putConnection, buildUrl(resource));
        }
    }

    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        final ProxyInfo proxyInfo = getProxyInfo("http", getRepository().getHost());
        if (proxyInfo != null) {
            this.proxy = getProxy(proxyInfo);
            this.proxyInfo = proxyInfo;
        }
        authenticator.setWagon(this);

        boolean usePreemptiveAuthentication = Boolean.getBoolean("maven.wagon.http.preemptiveAuthentication")
                || Boolean.parseBoolean(repository.getParameter("preemptiveAuthentication"))
                || this.preemptiveAuthentication;

        setPreemptiveAuthentication(usePreemptiveAuthentication);
    }

    @SuppressWarnings("deprecation")
    public PasswordAuthentication requestProxyAuthentication() {
        if (proxyInfo != null && proxyInfo.getUserName() != null) {
            String password = "";
            if (proxyInfo.getPassword() != null) {
                password = proxyInfo.getPassword();
            }
            return new PasswordAuthentication(proxyInfo.getUserName(), password.toCharArray());
        }
        return null;
    }

    public PasswordAuthentication requestServerAuthentication() {
        if (authenticationInfo != null && authenticationInfo.getUserName() != null) {
            String password = "";
            if (authenticationInfo.getPassword() != null) {
                password = authenticationInfo.getPassword();
            }
            return new PasswordAuthentication(authenticationInfo.getUserName(), password.toCharArray());
        }
        return null;
    }

    private Proxy getProxy(ProxyInfo proxyInfo) {
        return new Proxy(getProxyType(proxyInfo), getSocketAddress(proxyInfo));
    }

    private Type getProxyType(ProxyInfo proxyInfo) {
        if (ProxyInfo.PROXY_SOCKS4.equals(proxyInfo.getType()) || ProxyInfo.PROXY_SOCKS5.equals(proxyInfo.getType())) {
            return Type.SOCKS;
        } else {
            return Type.HTTP;
        }
    }

    public SocketAddress getSocketAddress(ProxyInfo proxyInfo) {
        return InetSocketAddress.createUnresolved(proxyInfo.getHost(), proxyInfo.getPort());
    }

    public void closeConnection() throws ConnectionException {
        // FIXME WAGON-375 use persistent connection feature provided by the jdk
        if (putConnection != null) {
            putConnection.disconnect();
        }
        authenticator.resetWagon();
    }

    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        HttpURLConnection headConnection;

        try {
            Resource resource = new Resource(resourceName);
            URL url = new URL(buildUrl(resource));
            headConnection = (HttpURLConnection) url.openConnection(this.proxy);

            addHeaders(headConnection);

            headConnection.setRequestMethod("HEAD");

            int statusCode = headConnection.getResponseCode();
            String reasonPhrase = headConnection.getResponseMessage();

            switch (statusCode) {
                case HttpURLConnection.HTTP_OK:
                    return true;

                case HttpURLConnection.HTTP_NOT_FOUND:
                case HttpURLConnection.HTTP_GONE:
                    return false;

                    // TODO Move 401/407 to AuthenticationException after WAGON-587
                case HttpURLConnection.HTTP_FORBIDDEN:
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_PROXY_AUTH:
                    throw new AuthorizationException(
                            formatAuthorizationMessage(buildUrl(resource), statusCode, reasonPhrase, getProxyInfo()));

                default:
                    throw new TransferFailedException(
                            formatTransferFailedMessage(buildUrl(resource), statusCode, reasonPhrase, getProxyInfo()));
            }
        } catch (IOException e) {
            throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
        }
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public Properties getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Properties httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    void setSystemProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.getProperties().remove(key);
        }
    }

    public void setPreemptiveAuthentication(boolean preemptiveAuthentication) {
        this.preemptiveAuthentication = preemptiveAuthentication;
    }

    public LightweightHttpWagonAuthenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(LightweightHttpWagonAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Convert the IOException that is thrown for most transfer errors that HttpURLConnection encounters to the
     * equivalent {@link TransferFailedException}.
     * <p>
     * Details are extracted from the error stream if possible, either directly or indirectly by way of supporting
     * accessors. The returned exception will include the passed IOException as a cause and a message that is as
     * descriptive as possible.
     *
     * @param originalIOException an IOException thrown from an HttpURLConnection operation
     * @param urlConnection       instance that triggered the IOException
     * @param url                 originating url that triggered the IOException
     * @return exception that is representative of the original cause
     */
    private TransferFailedException convertHttpUrlConnectionException(
            IOException originalIOException, HttpURLConnection urlConnection, String url) {
        // javadoc of HttpUrlConnection, HTTP transfer errors throw IOException
        // In that case, one may attempt to get the status code and reason phrase
        // from the errorstream. We do this, but by way of the following code path
        // getResponseCode()/getResponseMessage() - calls -> getHeaderFields()
        // getHeaderFields() - calls -> getErrorStream()
        try {
            // call getResponseMessage first since impl calls getResponseCode as part of that anyways
            String errorResponseMessage = urlConnection.getResponseMessage(); // may be null
            int errorResponseCode = urlConnection.getResponseCode(); // may be -1 if the code cannot be discerned
            String message = formatTransferFailedMessage(url, errorResponseCode, errorResponseMessage, getProxyInfo());
            return new TransferFailedException(message, originalIOException);

        } catch (IOException errorStreamException) {
            // there was a problem using the standard methods, need to fall back to other options
        }

        // Attempt to parse the status code and URL which can be included in an IOException message
        // https://github.com/AdoptOpenJDK/openjdk-jdk11/blame/999dbd4192d0f819cb5224f26e9e7fa75ca6f289/src/java
        // .base/share/classes/sun/net/www/protocol/http/HttpURLConnection.java#L1911L1913
        String ioMsg = originalIOException.getMessage();
        if (ioMsg != null) {
            Matcher matcher = IOEXCEPTION_MESSAGE_PATTERN.matcher(ioMsg);
            if (matcher.matches()) {
                String codeStr = matcher.group(1);
                String urlStr = matcher.group(2);

                int code = UNKNOWN_STATUS_CODE;
                try {
                    code = parseInt(codeStr);
                } catch (NumberFormatException nfe) {
                    // if here there is a regex problem
                }

                String message = formatTransferFailedMessage(urlStr, code, null, getProxyInfo());
                return new TransferFailedException(message, originalIOException);
            }
        }

        String message = formatTransferFailedMessage(url, UNKNOWN_STATUS_CODE, null, getProxyInfo());
        return new TransferFailedException(message, originalIOException);
    }
}
