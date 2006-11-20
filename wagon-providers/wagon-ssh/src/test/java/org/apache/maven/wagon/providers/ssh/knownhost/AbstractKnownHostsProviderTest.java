package org.apache.maven.wagon.providers.ssh.knownhost;

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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import junit.framework.TestCase;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.apache.maven.wagon.providers.ssh.TestData;

/**
 * Generic Unit test for <code>KnownHostsProvider</code>
 *
 * @author Juan F. Codagnone
 * @see org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider
 * @since Sep 12, 2005
 */
public abstract class AbstractKnownHostsProviderTest
    extends TestCase
{
    protected KnownHostsProvider okHostsProvider;

    protected KnownHostsProvider failHostsProvider;

    protected final String user = TestData.getUserName();

    protected String host;

    /**
     * tests what happens if the remote host has a different key than the one
     * we expect
     *
     * @throws Exception on error
     */
    public void testIncorrectKey()
        throws Exception
    {
        final JSch sch = new JSch();

        failHostsProvider.addKnownHosts( sch, null );
        try
        {
            sch.getSession( user, host ).connect();

            fail( "Should not have successfully connected - host is not known" );
        }
        catch ( JSchException e )
        {
            assertTrue( e.getMessage().startsWith( "UnknownHostKey:" ) );
        }
    }

    /**
     * tests what happens if the remote host has the expected key
     *
     * @throws Exception on error
     */
    public void testCorrectKey()
        throws Exception
    {
        final JSch sch = new JSch();
        sch.addIdentity( TestData.getPrivateKey().getAbsolutePath(), "" );

        okHostsProvider.addKnownHosts( sch, null );

        final Session session = sch.getSession( user, host );

        try
        {
            session.connect();
        }
        catch ( JSchException e )
        {
            assertFalse( e.getMessage().indexOf( "UnknownHostKey") >= 0 );
        }
    }
}
