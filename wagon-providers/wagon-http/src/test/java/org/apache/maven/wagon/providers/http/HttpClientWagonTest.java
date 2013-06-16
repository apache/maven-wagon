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


import junit.framework.TestCase;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.params.HttpParams;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.TransferFailedException;

public class HttpClientWagonTest
    extends TestCase
{

    public void testSetPreemptiveAuthParamViaConfig()
    {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        //X TODO methodConfig.addParam( HttpClientParams.PREEMPTIVE_AUTHENTICATION, "%b,true" );

        HttpConfiguration config = new HttpConfiguration();
        config.setAll( methodConfig );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HttpHead method = new HttpHead();
        wagon.setParameters( method );

        HttpParams params = method.getParams();
        assertNotNull( params );
        //X TODO assertTrue( params.isParameterTrue( HttpClientParams.PREEMPTIVE_AUTHENTICATION ) );
    }

    public void testSetMaxRedirectsParamViaConfig()
    {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        int maxRedirects = 2;
        methodConfig.addParam( ClientPNames.MAX_REDIRECTS, "%i," + maxRedirects );

        HttpConfiguration config = new HttpConfiguration();
        config.setAll( methodConfig );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HttpHead method = new HttpHead();
        wagon.setParameters( method );

        HttpParams params = method.getParams();
        assertNotNull( params );
        assertEquals( maxRedirects, params.getIntParameter( ClientPNames.MAX_REDIRECTS, -1 ) );
    }

    public void testDefaultHeadersUsedByDefault()
    {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll( new HttpMethodConfiguration() );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HttpHead method = new HttpHead();
        wagon.setHeaders( method );

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Cache-store", "no-store" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // method.addRequestHeader( "Expires", "0" );
        // method.addRequestHeader( "Accept-Encoding", "gzip" );

        Header header = method.getFirstHeader( "Cache-control" );
        assertNotNull( header );
        assertEquals( "no-cache", header.getValue() );

        header = method.getFirstHeader( "Cache-store" );
        assertNotNull( header );
        assertEquals( "no-store", header.getValue() );

        header = method.getFirstHeader( "Pragma" );
        assertNotNull( header );
        assertEquals( "no-cache", header.getValue() );

        header = method.getFirstHeader( "Expires" );
        assertNotNull( header );
        assertEquals( "0", header.getValue() );

        header = method.getFirstHeader( "Accept-Encoding" );
        assertNotNull( header );
        assertEquals( "gzip", header.getValue() );
    }

    public void testTurnOffDefaultHeaders()
    {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll( new HttpMethodConfiguration().setUseDefaultHeaders( false ) );

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration( config );

        HttpHead method = new HttpHead();
        wagon.setHeaders( method );

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Cache-store", "no-store" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // method.addRequestHeader( "Expires", "0" );
        // method.addRequestHeader( "Accept-Encoding", "gzip" );

        Header header = method.getFirstHeader( "Cache-control" );
        assertNull( header );

        header = method.getFirstHeader( "Cache-store" );
        assertNull( header );

        header = method.getFirstHeader( "Pragma" );
        assertNull( header );

        header = method.getFirstHeader( "Expires" );
        assertNull( header );

        header = method.getFirstHeader( "Accept-Encoding" );
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
