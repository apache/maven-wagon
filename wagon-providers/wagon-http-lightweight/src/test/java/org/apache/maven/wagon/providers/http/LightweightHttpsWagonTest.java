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
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;

public class LightweightHttpsWagonTest
    extends LightweightHttpWagonTest
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
        server.setConnectors( new Connector[] { connector } );
    }

    public void testHttpsProxyReset()
        throws Exception
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType( "https" );
        proxyInfo.setHost( "proxyhost" );
        proxyInfo.setPort( 1234 );
        proxyInfo.setNonProxyHosts( "non" );

        Repository repository = new Repository();

        String proxyHost = System.getProperty( "https.proxyHost" );
        String proxyPort = System.getProperty( "https.proxyPort" );
        String nonProxyHosts = System.getProperty( "https.nonProxyHosts" );

        System.getProperties().remove( "https.proxyHost" );
        System.getProperties().remove( "https.proxyPort" );

        Wagon wagon = getWagon();

        wagon.connect( repository, proxyInfo );

        assertEquals( "proxyhost", System.getProperty( "https.proxyHost" ) );
        assertEquals( "1234", System.getProperty( "https.proxyPort" ) );
        assertEquals( "non", System.getProperty( "https.nonProxyHosts" ) );

        wagon.disconnect();

        assertNull( System.getProperty( "https.proxyHost" ) );
        assertNull( System.getProperty( "https.proxyPort" ) );

        System.setProperty( "https.proxyHost", "host" );
        System.setProperty( "https.proxyPort", "port" );
        System.setProperty( "https.nonProxyHosts", "hosts" );

        wagon = getWagon();

        wagon.connect( repository, proxyInfo );

        assertEquals( "proxyhost", System.getProperty( "https.proxyHost" ) );
        assertEquals( "1234", System.getProperty( "https.proxyPort" ) );
        assertEquals( "non", System.getProperty( "https.nonProxyHosts" ) );

        wagon.disconnect();

        assertEquals( "host", System.getProperty( "https.proxyHost" ) );
        assertEquals( "port", System.getProperty( "https.proxyPort" ) );
        assertEquals( "hosts", System.getProperty( "https.nonProxyHosts" ) );

        wagon = getWagon();

        wagon.connect( repository );

        assertNull( System.getProperty( "https.proxyHost" ) );
        assertNull( System.getProperty( "https.proxyPort" ) );

        wagon.disconnect();

        assertEquals( "host", System.getProperty( "https.proxyHost" ) );
        assertEquals( "port", System.getProperty( "https.proxyPort" ) );
        assertEquals( "hosts", System.getProperty( "https.nonProxyHosts" ) );

        if ( proxyHost != null )
        {
            System.setProperty( "https.proxyHost", proxyHost );
        }
        else
        {
            System.getProperties().remove( "https.proxyHost" );
        }
        if ( proxyPort != null )
        {
            System.setProperty( "https.proxyPort", proxyPort );
        }
        else
        {
            System.getProperties().remove( "https.proxyPort" );
        }
        if ( nonProxyHosts != null )
        {
            System.setProperty( "https.nonProxyHosts", nonProxyHosts );
        }
        else
        {
            System.getProperties().remove( "https.nonProxyHosts" );
        }
    }

}
