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

import java.io.File;
import java.util.Properties;

import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.FileUtils;

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

        Repository repository = new Repository();

        String proxyHost = System.getProperty( "http.proxyHost" );
        String proxyPort = System.getProperty( "http.proxyPort" );
        String nonProxyHosts = System.getProperty( "http.nonProxyHosts" );

        System.getProperties().remove( "http.proxyHost" );
        System.getProperties().remove( "http.proxyPort" );

        Wagon wagon = getWagon();

        wagon.connect( repository, proxyInfo );

        assertEquals( "proxyhost", System.getProperty( "http.proxyHost" ) );
        assertEquals( "1234", System.getProperty( "http.proxyPort" ) );
        assertEquals( "non", System.getProperty( "http.nonProxyHosts" ) );

        wagon.disconnect();

        assertNull( System.getProperty( "http.proxyHost" ) );
        assertNull( System.getProperty( "http.proxyPort" ) );

        System.setProperty( "http.proxyHost", "host" );
        System.setProperty( "http.proxyPort", "port" );
        System.setProperty( "http.nonProxyHosts", "hosts" );

        wagon = getWagon();

        wagon.connect( repository, proxyInfo );

        assertEquals( "proxyhost", System.getProperty( "http.proxyHost" ) );
        assertEquals( "1234", System.getProperty( "http.proxyPort" ) );
        assertEquals( "non", System.getProperty( "http.nonProxyHosts" ) );

        wagon.disconnect();

        assertEquals( "host", System.getProperty( "http.proxyHost" ) );
        assertEquals( "port", System.getProperty( "http.proxyPort" ) );
        assertEquals( "hosts", System.getProperty( "http.nonProxyHosts" ) );

        wagon = getWagon();

        wagon.connect( repository );

        assertNull( System.getProperty( "http.proxyHost" ) );
        assertNull( System.getProperty( "http.proxyPort" ) );

        wagon.disconnect();

        assertEquals( "host", System.getProperty( "http.proxyHost" ) );
        assertEquals( "port", System.getProperty( "http.proxyPort" ) );
        assertEquals( "hosts", System.getProperty( "http.nonProxyHosts" ) );

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

    /**
     * This unit test will verify the behaviour of WAGON-314.
     * Handling a HTTP redirect (301) properly
     *
     * It is disabled by default because it needs an online connection to maven.central
     */
    public void disabledtestHttpRedirect() throws Exception
    {
        Repository repository = new Repository( "apache.releases",
                                        "https://repository.apache.org/service/local/repositories/releases/content/" );

        Wagon wagon = getWagon();

        wagon.connect( repository );

        File destinationDir = new File( "./target/" );
        assertTrue( destinationDir.exists() );

        File downloadedFile = new File( destinationDir, "downloadedFile.pom" );

        if ( downloadedFile.exists() )
        {
            downloadedFile.delete();
            assertFalse( downloadedFile.exists() );
        }

        wagon.get( "org/apache/maven/maven-parent/19/maven-parent-19.pom", downloadedFile );

        assertTrue( downloadedFile.exists() );

        String artifactContent = FileUtils.fileRead( downloadedFile, "UTF-8" );
        assertNotNull( artifactContent );
        assertTrue( artifactContent.contains( "<artifactId>maven-parent</artifactId>" ) );

        wagon.disconnect();
    }
}
