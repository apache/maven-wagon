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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;

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
     * @param host
     *            the host to set
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
     * @param realm
     *            the realm to set
     */
    public void setRealm(String realm) {
	this.realm = realm;
    }

    /**
     * Create an authScope given the /repository/host and /repository/password
     * and the /server/basicAuth or /server/proxyBasicAuth host, port and realm
     * settings. The basicAuth setting should override the repository settings
     * host and/or port if host, port or realm is set to "ANY".
     * <p>
     * Realm can also be set to a specific string and will be set if
     * /server/basicAuthentication/realm is non-null
     * 
     * @param host
     *            The server setting's /server/host value
     * @param port
     *            The server setting's /server/port value
     * @return
     */
    public AuthScope getScope(String host, int port) {
	if (getHost() != null && "ANY".compareTo(getHost()) == 0
		&& getPort() != null && "ANY".compareTo(getPort()) == 0
		&& getRealm() != null && "ANY".compareTo(getRealm()) == 0) {
	    return AuthScope.ANY;
	}
	String scopeHost = host;
	if (getHost() != null) {
	    if ("ANY".compareTo(getHost()) == 0) {
		scopeHost = AuthScope.ANY_HOST;
	    } else {
		scopeHost = getHost();
	    }
	}

	int scopePort = port > -1 ? port : AuthScope.ANY_PORT;
	// -1 for server/port settings does this, but providing an override here
	// in
	// the BasicAuthScope config
	if (getPort() != null) {
	    if ("ANY".compareTo(getPort()) == 0) {
		scopePort = AuthScope.ANY_PORT;
	    } else {
		scopePort = Integer.parseInt(getPort());
	    }
	}

	String scopeRealm = AuthScope.ANY_REALM;
	if (getRealm() != null) {
	    if ("ANY".compareTo(getRealm()) != 0) {
		scopeRealm = getRealm();
	    } else {
		scopeRealm = getRealm();
	    }
	}

	return new AuthScope(scopeHost, scopePort, scopeRealm);
    }

    /**
     * @return the port
     */
    public String getPort() {
	return port;
    }

    /**
     * @param port
     *            the port to set
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
