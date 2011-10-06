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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import junit.framework.TestCase;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.TransferFailedException;

public class AbstractHttpClientWagonTest
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
