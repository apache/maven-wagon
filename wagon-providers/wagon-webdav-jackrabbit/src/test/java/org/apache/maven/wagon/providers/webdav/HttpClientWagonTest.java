package org.apache.maven.wagon.providers.webdav;

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

import junit.framework.TestCase;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

public class HttpClientWagonTest
    extends TestCase
{

    public void testSetPreemptiveAuthParamViaConfig()
    {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        methodConfig.addParam( HttpClientParams.PREEMPTIVE_AUTHENTICATION, "%b,true" );

        HttpConfiguration config = new HttpConfiguration();
        config.setAll( methodConfig );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HeadMethod method = new HeadMethod();
        wagon.setParameters( method );

        HttpMethodParams params = method.getParams();
        assertNotNull( params );
        assertTrue( params.isParameterTrue( HttpClientParams.PREEMPTIVE_AUTHENTICATION ) );
    }

    public void testSetMaxRedirectsParamViaConfig()
    {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        int maxRedirects = 2;
        methodConfig.addParam( HttpClientParams.MAX_REDIRECTS, "%i," + maxRedirects );

        HttpConfiguration config = new HttpConfiguration();
        config.setAll( methodConfig );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HeadMethod method = new HeadMethod();
        wagon.setParameters( method );

        HttpMethodParams params = method.getParams();
        assertNotNull( params );
        assertEquals( maxRedirects, params.getIntParameter( HttpClientParams.MAX_REDIRECTS, -1 ) );
    }

    public void testDefaultHeadersUsedByDefault()
    {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll( new HttpMethodConfiguration() );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HeadMethod method = new HeadMethod();
        wagon.setHeaders( method );

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Cache-store", "no-store" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // method.addRequestHeader( "Expires", "0" );
        // method.addRequestHeader( "Accept-Encoding", "gzip" );

        Header header = method.getRequestHeader( "Cache-control" );
        assertNotNull( header );
        assertEquals( "no-cache", header.getValue() );

        header = method.getRequestHeader( "Cache-store" );
        assertNotNull( header );
        assertEquals( "no-store", header.getValue() );

        header = method.getRequestHeader( "Pragma" );
        assertNotNull( header );
        assertEquals( "no-cache", header.getValue() );

        header = method.getRequestHeader( "Expires" );
        assertNotNull( header );
        assertEquals( "0", header.getValue() );

        header = method.getRequestHeader( "Accept-Encoding" );
        assertNotNull( header );
        assertEquals( "gzip", header.getValue() );

        header = method.getRequestHeader( "User-Agent" );
        assertNotNull( header );
        // during test-phase /META-INF/maven/org.apache.maven.wagon/*/pom.properties hasn't been created yet
        assertTrue( header.getValue().startsWith( "Apache-Maven-Wagon/unknown-version (Java " ) );
    }

    public void testTurnOffDefaultHeaders()
    {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll( new HttpMethodConfiguration().setUseDefaultHeaders( false ) );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HeadMethod method = new HeadMethod();
        wagon.setHeaders( method );

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Cache-store", "no-store" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // method.addRequestHeader( "Expires", "0" );
        // method.addRequestHeader( "Accept-Encoding", "gzip" );

        Header header = method.getRequestHeader( "Cache-control" );
        assertNull( header );

        header = method.getRequestHeader( "Cache-store" );
        assertNull( header );

        header = method.getRequestHeader( "Pragma" );
        assertNull( header );

        header = method.getRequestHeader( "Expires" );
        assertNull( header );

        header = method.getRequestHeader( "Accept-Encoding" );
        assertNull( header );
    }

    public void testNTCredentialsWithUsernameNull()
        throws AuthenticationException, ConnectionException
    {
        TestWagon wagon = new TestWagon();

        Repository repository = new Repository( "mockRepoId", "mockRepoURL" );
        wagon.connect( repository );

        wagon.openConnection();

        assertNull( wagon.getAuthenticationInfo().getUserName() );
        assertNull( wagon.getAuthenticationInfo().getPassword() );

        assertFalse( wagon.getClient().getState().getCredentials( new AuthScope( null, 0 ) ) instanceof NTCredentials );
    }

    public void testNTCredentialsNoNTDomain()
        throws AuthenticationException, ConnectionException
    {
        TestWagon wagon = new TestWagon();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        String myUsernameNoNTDomain = "myUserNameNoNTDomain";
        authenticationInfo.setUserName( myUsernameNoNTDomain );

        String myPassword = "myPassword";
        authenticationInfo.setPassword( myPassword );

        Repository repository = new Repository( "mockRepoId", "mockRepoURL" );

        wagon.connect( repository, authenticationInfo, (ProxyInfo) null );

        wagon.openConnection();

        assertEquals( myUsernameNoNTDomain, wagon.getAuthenticationInfo().getUserName() );
        assertEquals( myPassword, wagon.getAuthenticationInfo().getPassword() );

        assertFalse( wagon.getClient().getState().getCredentials( new AuthScope( null, 0 ) ) instanceof NTCredentials );
    }

    public void testNTCredentialsWithNTDomain()
        throws AuthenticationException, ConnectionException
    {
        TestWagon wagon = new TestWagon();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        String myNTDomain = "myNTDomain";
        String myUsername = "myUsername";
        String myNTDomainAndUser = myNTDomain + "\\" + myUsername;
        authenticationInfo.setUserName( myNTDomainAndUser );

        String myPassword = "myPassword";
        authenticationInfo.setPassword( myPassword );

        Repository repository = new Repository( "mockRepoId", "mockRepoURL" );

        wagon.connect( repository, authenticationInfo, (ProxyInfo) null );

        wagon.openConnection();

        assertEquals( myNTDomainAndUser, wagon.getAuthenticationInfo().getUserName() );
        assertEquals( myPassword, wagon.getAuthenticationInfo().getPassword() );

        Credentials credentials = wagon.getClient().getState().getCredentials( new AuthScope( null, 0 ) );
        assertTrue( credentials instanceof NTCredentials );

        NTCredentials ntCredentials = (NTCredentials) credentials;
        assertEquals( myNTDomain, ntCredentials.getDomain() );
        assertEquals( myUsername, ntCredentials.getUserName() );
        assertEquals( myPassword, ntCredentials.getPassword() );
    }

    private static final class TestWagon
        extends AbstractHttpClientWagon
    {
        @Override
        public void fillOutputData( OutputData outputData )
            throws TransferFailedException
        {

        }
    }

}
