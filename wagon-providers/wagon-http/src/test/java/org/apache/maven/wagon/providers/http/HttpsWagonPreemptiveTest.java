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

import org.apache.maven.wagon.Wagon;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;

public class HttpsWagonPreemptiveTest
    extends HttpWagonTest
{
    protected String getProtocol()
    {
        return "https";
    }

    protected void addConnectors( Server server )
    {
        System.setProperty( "javax.net.ssl.trustStore",
                            getTestFile( "src/test/resources/ssl/keystore" ).getAbsolutePath() );

        SslSocketConnector connector = new SslSocketConnector();
        connector.setPort( server.getConnectors()[0].getPort() );
        connector.setKeystore( getTestPath( "src/test/resources/ssl/keystore" ) );
        connector.setPassword( "wagonhttp" );
        connector.setKeyPassword( "wagonhttp" );
        connector.setTruststore( getTestPath( "src/test/resources/ssl/keystore" ) );
        connector.setTrustPassword( "wagonhttp" );
        server.setConnectors( new Connector[]{ connector } );
    }

    @Override
    protected Wagon getWagon()
        throws Exception
    {
        HttpWagon wagon = (HttpWagon) super.getWagon();
        wagon.setHttpConfiguration(
            new HttpConfiguration().setAll( new HttpMethodConfiguration().setUsePreemptive( true ) ) );
        return wagon;
    }

    @Override
    protected boolean supportPreemptiveAuthenticationPut()
    {
        return true;
    }

    @Override
    protected boolean supportPreemptiveAuthenticationGet()
    {
        return true;
    }

    @Override
    protected boolean supportProxyPreemptiveAuthentication()
    {
        return true;
    }

    @Override
    protected boolean assertOnTransferProgress()
    {
        return false;
    }
}
