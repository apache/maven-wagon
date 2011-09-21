package org.apache.maven.wagon.providers.ssh;
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

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class TestPublickeyAuthenticator
    implements PublickeyAuthenticator
{
    public List<PublickeyAuthenticatorRequest> publickeyAuthenticatorRequests =
        new ArrayList<PublickeyAuthenticatorRequest>();

    public boolean keyAuthz;

    public TestPublickeyAuthenticator( boolean keyAuthz )
    {
        this.keyAuthz = keyAuthz;
    }

    public boolean authenticate( String username, PublicKey key, ServerSession session )
    {
        if ( !keyAuthz )
        {
            return false;
        }
        publickeyAuthenticatorRequests.add( new PublickeyAuthenticatorRequest( username, key ) );
        return true;
    }

    public static class PublickeyAuthenticatorRequest
    {
        public String username;

        public PublicKey publicKey;

        public PublickeyAuthenticatorRequest( String username, PublicKey publicKey )
        {
            this.username = username;
            this.publicKey = publicKey;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "PublickeyAuthenticatorRequest" );
            sb.append( "{username='" ).append( username ).append( '\'' );
            sb.append( ", publicKey=" ).append( publicKey );
            sb.append( '}' );
            return sb.toString();
        }
    }
}
