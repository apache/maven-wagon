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

import java.util.Properties;

import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.mortbay.jetty.Server;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class LightweightHttpWagonTest
    extends HttpWagonTestCase
{
    protected String getProtocol()
    {
        return "http";
    }

    protected String getTestRepositoryUrl()
    {
        return getProtocol() + "://localhost:10007/";
    }

    protected void setHttpHeaders( StreamingWagon wagon, Properties properties )
    {
        ( (LightweightHttpWagon) wagon ).setHttpHeaders( properties );
    }

    public void testProxyReset()
        throws Exception
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType( "http" );
        proxyInfo.setHost( "proxyhost" );
        proxyInfo.setPort( 1234 );
        proxyInfo.setNonProxyHosts( "non" );
        Server server = new Server( 0 );
        addConnectors( server );
        server.start();

        String proxyHost = System.getProperty( "http.proxyHost" );
        String proxyPort = System.getProperty( "http.proxyPort" );
        String nonProxyHosts = System.getProperty( "http.nonProxyHosts" );
        try
        {
            Repository repository = new Repository( "test", getRepositoryUrl( server ) );

            System.getProperties().remove( "http.proxyHost" );
            System.getProperties().remove( "http.proxyPort" );

            Wagon wagon = getWagon();

            wagon.connect( repository, proxyInfo );
            wagon.resourceExists( "/test"  );
            wagon.disconnect();

            assertNull( System.getProperty( "http.proxyHost" ) );
            assertNull( System.getProperty( "http.proxyPort" ) );

            System.setProperty( "http.proxyHost", "host" );
            System.setProperty( "http.proxyPort", "port" );
            System.setProperty( "http.nonProxyHosts", "hosts" );

            wagon = getWagon();

            wagon.connect( repository, proxyInfo );
            wagon.resourceExists( "/test"  );
            wagon.disconnect();

            assertEquals( "host", System.getProperty( "http.proxyHost" ) );
            assertEquals( "port", System.getProperty( "http.proxyPort" ) );
            assertEquals( "hosts", System.getProperty( "http.nonProxyHosts" ) );

            wagon = getWagon();

            wagon.connect( repository );
            wagon.resourceExists( "/test"  );
            wagon.disconnect();

            assertEquals( "host", System.getProperty( "http.proxyHost" ) );
            assertEquals( "port", System.getProperty( "http.proxyPort" ) );
            assertEquals( "hosts", System.getProperty( "http.nonProxyHosts" ) );
        }
        finally
        {
            server.stop();
        }

        if ( proxyHost != null )
        {
            System.setProperty( "http.proxyHost", proxyHost );
        }
        else
        {
            System.getProperties().remove( "http.proxyHost" );
        }
        if ( proxyPort != null )
        {
            System.setProperty( "http.proxyPort", proxyPort );
        }
        else
        {
            System.getProperties().remove( "http.proxyPort" );
        }
        if ( nonProxyHosts != null )
        {
            System.setProperty( "http.nonProxyHosts", nonProxyHosts );
        }
        else
        {
            System.getProperties().remove( "http.nonProxyHosts" );
        }
    }

    @Override
    protected boolean supportPreemptiveAuthentication()
    {
        return false;
    }
}
