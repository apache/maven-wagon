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
package org.apache.maven.wagon.providers.webdav;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.params.HttpParams;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpClientWagonTest {

    @Test
    public void testSetPreemptiveAuthParamViaConfig() {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        methodConfig.setUsePreemptive(true);

        HttpConfiguration config = new HttpConfiguration();
        config.setAll(methodConfig);

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration(config);

        HttpHead method = new HttpHead();
        wagon.setHeaders(method);

        HttpParams params = method.getParams();
        assertNotNull(params);
    }

    @Test
    public void testDefaultHeadersUsedByDefault() {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll(new HttpMethodConfiguration());

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration(config);

        HttpHead method = new HttpHead();
        wagon.setHeaders(method);

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // "Accept-Encoding" is automatically set by HttpClient at runtime

        Header header = method.getFirstHeader("Cache-control");
        assertNotNull(header);
        assertEquals("no-cache", header.getValue());

        header = method.getFirstHeader("Pragma");
        assertNotNull(header);
        assertEquals("no-cache", header.getValue());
    }

    @Test
    public void testTurnOffDefaultHeaders() {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll(new HttpMethodConfiguration().setUseDefaultHeaders(false));

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration(config);

        HttpHead method = new HttpHead();
        wagon.setHeaders(method);

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Pragma", "no-cache" );

        Header header = method.getFirstHeader("Cache-control");
        assertNull(header);

        header = method.getFirstHeader("Pragma");
        assertNull(header);
    }

    @Test
    public void testNTCredentialsWithUsernameNull() throws AuthenticationException, ConnectionException {
        TestWagon wagon = new TestWagon();

        Repository repository = new Repository("mockRepoId", "mockRepoURL");
        wagon.connect(repository);

        wagon.openConnection();

        assertNull(wagon.getAuthenticationInfo().getUserName());
        assertNull(wagon.getAuthenticationInfo().getPassword());
    }

    @Test
    public void testNTCredentialsNoNTDomain() throws AuthenticationException, ConnectionException {
        TestWagon wagon = new TestWagon();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        String myUsernameNoNTDomain = "myUserNameNoNTDomain";
        authenticationInfo.setUserName(myUsernameNoNTDomain);

        String myPassword = "myPassword";
        authenticationInfo.setPassword(myPassword);

        Repository repository = new Repository("mockRepoId", "mockRepoURL");

        wagon.connect(repository, authenticationInfo, (ProxyInfo) null);

        wagon.openConnection();

        assertEquals(myUsernameNoNTDomain, wagon.getAuthenticationInfo().getUserName());
        assertEquals(myPassword, wagon.getAuthenticationInfo().getPassword());
    }

    @Test
    public void testNTCredentialsWithNTDomain() throws AuthenticationException, ConnectionException {
        TestWagon wagon = new TestWagon();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        String myNTDomain = "myNTDomain";
        String myUsername = "myUsername";
        String myNTDomainAndUser = myNTDomain + "\\" + myUsername;
        authenticationInfo.setUserName(myNTDomainAndUser);

        String myPassword = "myPassword";
        authenticationInfo.setPassword(myPassword);

        Repository repository = new Repository("mockRepoId", "mockRepoURL");

        wagon.connect(repository, authenticationInfo, (ProxyInfo) null);

        wagon.openConnection();

        assertEquals(myNTDomainAndUser, wagon.getAuthenticationInfo().getUserName());
        assertEquals(myPassword, wagon.getAuthenticationInfo().getPassword());
    }

    private static final class TestWagon extends WebDavWagon {
        @Override
        public void fillOutputData(OutputData outputData) throws TransferFailedException {}
    }
}
