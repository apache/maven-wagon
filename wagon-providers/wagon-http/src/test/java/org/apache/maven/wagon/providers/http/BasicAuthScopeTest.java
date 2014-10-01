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

import org.apache.http.auth.AuthScope;
import org.junit.Assert;
import org.junit.Test;

public class BasicAuthScopeTest
{

    /**
     * Test AuthScope override with no overriding values set. Nothing should
     * change in original host/port.
     */
    @Test
    public void testGetScopeNothingOverridden()
    {
        BasicAuthScope scope = new BasicAuthScope();

        AuthScope authScope = scope.getScope( "original.host.com", 3456 );
        Assert.assertEquals( "original.host.com", authScope.getHost() );
        Assert.assertEquals( 3456, authScope.getPort() );
        Assert.assertEquals( AuthScope.ANY_REALM, authScope.getRealm() );
    }

    /**
     * Test AuthScope override for all values overridden
     */
    @Test
    public void testGetScopeAllOverridden()
    {
        BasicAuthScope scope = new BasicAuthScope();
        scope.setHost( "override.host.com" );
        scope.setPort( "1234" );
        scope.setRealm( "override-realm" );
        AuthScope authScope = scope.getScope( "original.host.com", 3456 );
        Assert.assertEquals( "override.host.com", authScope.getHost() );
        Assert.assertEquals( 1234, authScope.getPort() );
        Assert.assertEquals( "override-realm", authScope.getRealm() );
    }

    /**
     * Test AuthScope override for all values overridden with "ANY"
     */
    @Test
    public void testGetScopeAllAny()
    {
        BasicAuthScope scope = new BasicAuthScope();
        scope.setHost( "ANY" );
        scope.setPort( "ANY" );
        scope.setRealm( "ANY" );
        AuthScope authScope = scope.getScope( "original.host.com", 3456 );
        Assert.assertEquals( AuthScope.ANY_HOST, authScope.getHost() );
        Assert.assertEquals( AuthScope.ANY_PORT, authScope.getPort() );
        Assert.assertEquals( AuthScope.ANY_REALM, authScope.getRealm() );
    }

    /**
     * Test AuthScope override for realm value overridden
     */
    @Test
    public void testGetScopeRealmOverridden()
    {
        BasicAuthScope scope = new BasicAuthScope();
        scope.setRealm( "override-realm" );
        AuthScope authScope = scope.getScope( "original.host.com", 3456 );
        Assert.assertEquals( "original.host.com", authScope.getHost() );
        Assert.assertEquals( 3456, authScope.getPort() );
        Assert.assertEquals( "override-realm", authScope.getRealm() );
    }

    /**
     * Test AuthScope where original port is -1, which should result in ANY
     */
    @Test
    public void testGetScopeOriginalPortIsNegativeOne()
    {
        BasicAuthScope scope = new BasicAuthScope();
        AuthScope authScope = scope.getScope( "original.host.com", -1 );
        Assert.assertEquals( AuthScope.ANY_PORT, authScope.getPort() );
    }
}
