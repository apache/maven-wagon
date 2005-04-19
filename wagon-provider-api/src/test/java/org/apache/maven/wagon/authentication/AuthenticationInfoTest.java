package org.apache.maven.wagon.authentication;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

/**
 * @author <a href="mailto:jvanzyl@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo test defaults
 */
public class AuthenticationInfoTest
    extends TestCase
{
    public AuthenticationInfoTest( final String name )
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

    public void testAuthenticationInfoProperties()
    {
        final AuthenticationInfo authenticationInfo = new AuthenticationInfo();

        authenticationInfo.setUserName( "username" );

        assertEquals( "username", authenticationInfo.getUserName() );

        authenticationInfo.setPassword( "password" );

        assertEquals( "password", authenticationInfo.getPassword() );

        authenticationInfo.setPassphrase( "passphrase" );

        assertEquals( "passphrase", authenticationInfo.getPassphrase() );

        authenticationInfo.setPrivateKey( "privatekey" );

        assertEquals( "privatekey", authenticationInfo.getPrivateKey() );
    }
}
