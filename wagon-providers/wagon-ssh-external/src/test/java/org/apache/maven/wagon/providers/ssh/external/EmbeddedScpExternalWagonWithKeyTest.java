package org.apache.maven.wagon.providers.ssh.external;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.AbstractEmbeddedScpWagonWithKeyTest;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class EmbeddedScpExternalWagonWithKeyTest
    extends AbstractEmbeddedScpWagonWithKeyTest
{


    @Override
    protected Wagon getWagon()
        throws Exception
    {
        ScpExternalWagon scpWagon = (ScpExternalWagon) super.getWagon();
        scpWagon.setInteractive( false );
        File dummyKnowHostsFile = new File( "target/dummy_knowhost" );
        if ( dummyKnowHostsFile.exists() )
        {
            dummyKnowHostsFile.delete();
        }
        scpWagon.setScpArgs(
            "-o StrictHostKeyChecking=no -o UserKnownHostsFile=" + dummyKnowHostsFile.getCanonicalPath() );
        scpWagon.setSshArgs(
            "-o StrictHostKeyChecking=no -o UserKnownHostsFile=" + dummyKnowHostsFile.getCanonicalPath() );
        dummyKnowHostsFile.deleteOnExit();
        return scpWagon;
    }


    protected String getProtocol()
    {
        return "scpexe";
    }


    protected AuthenticationInfo getAuthInfo()
    {
        try
        {
            AuthenticationInfo authInfo = super.getAuthInfo();
            // user : guest/guest123 -  passphrase : toto01
            authInfo.setUserName( "guest" );
            File sshKeysTarget = new File( "target/ssh-keys" );
            FileUtils.copyDirectory( new File( "src/test/ssh-keys" ), sshKeysTarget );
            // ssh keys need to 700 permissions
            // to prevent WARNING: UNPROTECTED PRIVATE KEY FILE!
            Commandline commandline = new Commandline( "chmod" );
            commandline.createArg().setValue( "-R" );
            commandline.createArg().setValue( "700" );
            commandline.createArg().setValue( sshKeysTarget.getCanonicalPath() );
            CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( commandline, out, err );
            Streams streams = new Streams();
            streams.setOut( out.getOutput() );
            streams.setErr( err.getOutput() );
            if ( exitCode != 0 )
            {
                throw new RuntimeException(
                    "fail to chmod exit code " + exitCode + ", error" + streams.getErr() + ", out "
                        + streams.getOut() );
            }

            authInfo.setPrivateKey( new File( sshKeysTarget, "id_rsa" ).getCanonicalPath() );

            return authInfo;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );

        }
    }

    public void testFailedGetToStream()
        throws Exception
    {
        // ignore this test as it need a stream wagon
    }


}
