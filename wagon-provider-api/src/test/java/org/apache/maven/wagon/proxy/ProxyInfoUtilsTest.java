package org.apache.maven.wagon.proxy;

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

/**
 * @author <a href="mailto:lafeuil@gmail.com">Thomas Champagne</a>
 */
public class ProxyInfoUtilsTest
    extends TestCase
{
    public ProxyInfoUtilsTest( final String name )
    {
        super( name );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    public void testValidateNonProxyHostsWithNullProxy()
    {
        assertFalse( "www.ibiblio.org", ProxyUtils.validateNonProxyHosts( null, "maven.apache.org" ) );
    }

    public void testValidateNonProxyHostsWithUniqueHost()

    {
        final ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setUserName( "username" );
        proxyInfo.setPassword( "password" );
        proxyInfo.setHost( "http://www.ibiblio.org" );
        proxyInfo.setPort( 0 );
        proxyInfo.setType( "SOCKSv4" );
        proxyInfo.setNonProxyHosts( "*.apache.org" );

        assertTrue( "maven.apache.org", ProxyUtils.validateNonProxyHosts( proxyInfo, "maven.apache.org" ) );

        assertFalse( "www.ibiblio.org", ProxyUtils.validateNonProxyHosts( proxyInfo, "www.ibiblio.org" ) );

        assertFalse( "null", ProxyUtils.validateNonProxyHosts( proxyInfo, null ) );

        proxyInfo.setNonProxyHosts( null );
        assertFalse( "NonProxyHosts = null", ProxyUtils.validateNonProxyHosts( proxyInfo, "maven.apache.org" ) );

        proxyInfo.setNonProxyHosts( "" );
        assertFalse( "NonProxyHosts = \"\"", ProxyUtils.validateNonProxyHosts( proxyInfo, "maven.apache.org" ) );
    }

    public void testValidateNonProxyHostsWithMultipleHost()

    {
        final ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setUserName( "username" );
        proxyInfo.setPassword( "password" );
        proxyInfo.setHost( "http://www.ibiblio.org" );
        proxyInfo.setPort( 0 );
        proxyInfo.setType( "SOCKSv4" );
        proxyInfo.setNonProxyHosts( "*.apache.org|*.codehaus.org" );

        assertTrue( "maven.apache.org", ProxyUtils.validateNonProxyHosts( proxyInfo, "maven.apache.org" ) );
        assertTrue( "wiki.codehaus.org", ProxyUtils.validateNonProxyHosts( proxyInfo, "wiki.codehaus.org" ) );

        assertFalse( "www.ibiblio.org", ProxyUtils.validateNonProxyHosts( proxyInfo, "www.ibiblio.org" ) );
    }
}
