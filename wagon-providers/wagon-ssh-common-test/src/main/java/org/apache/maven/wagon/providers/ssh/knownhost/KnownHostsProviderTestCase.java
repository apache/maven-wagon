package org.apache.maven.wagon.providers.ssh.knownhost;

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
import org.apache.maven.wagon.providers.ssh.SshWagon;
import org.apache.maven.wagon.providers.ssh.TestData;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;

/**
 * 
 */
public class KnownHostsProviderTestCase
    extends PlexusTestCase
{
    protected KnownHostsProvider okHostsProvider;

    protected KnownHostsProvider failHostsProvider;

    protected KnownHostsProvider changedHostsProvider;

    private SshWagon wagon;

    private Repository source;

    private static final String CORRECT_KEY = TestData.getHostKey();

    private static final String CHANGED_KEY =
        "AAAAB3NzaC1yc2EAAAABIwAAAQEA8VLKkfHl2CNqW+m0603z07dyweWzzdVGQlMPUX4z1264E7M/h+6lPKiOo+u49CL7eQVA+FtW"
        + "TZoJ3oBAMABcKnHx41TnSpQUkbdR6rzyC6IG1lXiVtEjG2w7DUnxpCtVo5PaQuJobwoXv5NNL3vx03THPgcDJquLPWvGnDWhnXoEh"
        + "3/6c7rprwT+PrjZ6LIT35ZCUGajoehhF151oNbFMQHllfR6EAiZIP0z0nIVI+Jiv6g+XZapumVPVYjdOfxvLKQope1H9HJamT3bDI"
        + "m8mkebUB10DzQJYxFt4/0wiNH3L4jsIFn+CiW1/IQm5yyff1CUO87OqVbtp9BlaXZNmw==";

    /**
     * tests what happens if the remote host has a different key than the one
     * we expect
     *
     * @throws Exception on error
     */
    public void testIncorrectKey()
        throws Exception
    {
        wagon.setKnownHostsProvider( failHostsProvider );

        try
        {
            wagon.connect( source );

            fail( "Should not have successfully connected - host is not known" );
        }
        catch ( UnknownHostException e )
        {
            // ok
        }
    }

    /**
     * tests what happens if the remote host has changed since being recorded.
     *
     * @throws Exception on error
     */
    public void testChangedKey()
        throws Exception
    {
        wagon.setKnownHostsProvider( changedHostsProvider );

        try
        {
            wagon.connect( source );

            fail( "Should not have successfully connected - host is changed" );
        }
        catch ( KnownHostChangedException e )
        {
            // ok
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
        wagon.setKnownHostsProvider( okHostsProvider );

        wagon.connect( source );

        assertTrue( true );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        source =
            new Repository( "test", "scp://" + TestData.getUserName() + "@" + TestData.getHostname() + "/tmp/foo" );

        wagon = (SshWagon) lookup( Wagon.ROLE, "scp" );
        wagon.setInteractive( false );

        this.okHostsProvider = new SingleKnownHostProvider( TestData.getHostname(), CORRECT_KEY );
        this.failHostsProvider = new SingleKnownHostProvider( "beaver.codehaus.org", CORRECT_KEY );
        this.changedHostsProvider = new SingleKnownHostProvider( TestData.getHostname(), CHANGED_KEY );
    }
}
