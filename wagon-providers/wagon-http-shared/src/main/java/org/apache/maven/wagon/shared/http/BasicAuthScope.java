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

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.core5.http.HttpHost;

/**
 * @since 2.8
 */
public class BasicAuthScope {
    private String host;

    private String port;

    private String realm;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @param realm the realm to set
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Create an {@link AuthScope} from the given host and port, applying the host, port and
     * realm overrides configured on this object.
     * <p>
     * These overrides are configured per server. {@code AbstractHttpClientWagon} holds two of
     * them: {@code basicAuth} for the target host and {@code proxyAuth} for the proxy, settable
     * through {@code setBasicAuthScope} and {@code setProxyBasicAuthScope} respectively.
     * <p>
     * A non-null {@link #getHost() host}, {@link #getPort() port}, or {@link #getRealm() realm} replaces the value passed
     * in; the literal string {@code "ANY"} means no restriction on that component in HttpClient 5. A passed-in port of
     * {@code -1} also means any port. When host, port and realm are all {@code "ANY"}, an unrestricted
     * {@link AuthScope} is returned.
     *
     * @param host the host to scope credentials to, before any override is applied
     * @param port the port to scope credentials to, before any override is applied; -1 means
     *             any port
     * @return the {@link AuthScope} under which credentials should be registered
     */
    public AuthScope getScope(String host, int port) {
        if (getHost() != null //
                && "ANY".compareTo(getHost()) == 0 //
                && getPort() != null //
                && "ANY".compareTo(getPort()) == 0 //
                && getRealm() != null //
                && "ANY".compareTo(getRealm()) == 0) {
            return new AuthScope(null, -1);
        }
        // HttpClient 5 has no ANY_HOST / ANY_PORT / ANY_REALM constants: null host and realm,
        // and a negative port, express "any" in the 5.x AuthScope model.
        String scopeHost = host;
        if (getHost() != null) {
            if ("ANY".compareTo(getHost()) == 0) {
                scopeHost = null;
            } else {
                scopeHost = getHost();
            }
        }

        int scopePort = port > -1 ? port : -1;
        if (getPort() != null) {
            if ("ANY".compareTo(getPort()) == 0) {
                scopePort = -1;
            } else {
                scopePort = Integer.parseInt(getPort());
            }
        }

        String scopeRealm = null;
        if (getRealm() != null && "ANY".compareTo(getRealm()) != 0) {
            scopeRealm = getRealm();
        }

        return new AuthScope(null, scopeHost, scopePort, scopeRealm, null);
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Given a HttpHost, return scope with overrides from appropriate basicAuth
     * configuration.
     * <p>
     * Note: Protocol is ignored. AuthScope impl ignores it as well, but if that
     * changed, there could be a problem.
     * </p>
     *
     * @param targetHost
     * @return
     */
    public AuthScope getScope(HttpHost targetHost) {
        return getScope(targetHost.getHostName(), targetHost.getPort());
    }
}
