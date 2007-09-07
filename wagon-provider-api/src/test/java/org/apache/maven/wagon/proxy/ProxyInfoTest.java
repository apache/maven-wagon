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
 * @author <a href="mailto:jvanzyl@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProxyInfoTest
    extends TestCase
{
    public ProxyInfoTest( final String name )
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

    public void testProxyInfoProperties()

    {
        final ProxyInfo proxyInfo = new ProxyInfo();

        proxyInfo.setUserName( "username" );

        assertEquals( "username", proxyInfo.getUserName() );

        proxyInfo.setPassword( "password" );

        assertEquals( "password", proxyInfo.getPassword() );

        proxyInfo.setHost( "http://www.ibiblio.org" );

        assertEquals( "http://www.ibiblio.org", proxyInfo.getHost() );

        proxyInfo.setPort( 0 );

        assertEquals( 0, proxyInfo.getPort() );

        proxyInfo.setType( "SOCKSv4" );

        assertEquals( "SOCKSv4", proxyInfo.getType() );
    }
}
