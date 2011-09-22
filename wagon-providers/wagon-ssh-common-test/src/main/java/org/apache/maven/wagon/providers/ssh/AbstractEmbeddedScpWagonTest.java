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

import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.util.Arrays;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public abstract class AbstractEmbeddedScpWagonTest
    extends StreamingWagonTestCase
{

    SshServerEmbedded sshServerEmbedded;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        String sshKeyResource = "ssh-keys/id_rsa";

        sshServerEmbedded = new SshServerEmbedded( getProtocol(), Arrays.asList( sshKeyResource ), false );

        sshServerEmbedded.start();
        System.out.println( "sshd on port " + sshServerEmbedded.getPort() );
    }

    @Override
    protected void tearDownWagonTestingFixtures()
        throws Exception
    {

        for ( TestPasswordAuthenticator.PasswordAuthenticatorRequest passwordAuthenticatorRequest : sshServerEmbedded.passwordAuthenticator.passwordAuthenticatorRequests )
        {
            assertEquals( TestData.getUserName(), passwordAuthenticatorRequest.username );
            assertEquals( TestData.getUserPassword(), passwordAuthenticatorRequest.password );
        }
        sshServerEmbedded.stop();
    }

    protected abstract String getProtocol();

    @Override
    protected int getTestRepositoryPort()
    {
        return sshServerEmbedded.getPort();
    }


    public String getTestRepositoryUrl()
    {
        return TestData.getTestRepositoryUrl( sshServerEmbedded.getPort() );
    }

    protected AuthenticationInfo getAuthInfo()
    {
        AuthenticationInfo authInfo = super.getAuthInfo();

        authInfo.setUserName( TestData.getUserName() );
        authInfo.setPassword( TestData.getUserPassword() );

        return authInfo;
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        return new File( repository.getBasedir(), resource.getName() ).lastModified();
    }


    @Override
    protected abstract boolean supportsGetIfNewer();

}
