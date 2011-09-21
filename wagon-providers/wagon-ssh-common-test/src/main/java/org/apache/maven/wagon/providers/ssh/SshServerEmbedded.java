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

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.keyprovider.ResourceKeyPairProvider;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.filesystem.NativeSshFile;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class SshServerEmbedded
{
    private String wagonProtocol;

    private int port;

    private SshServer sshd;

    private List<String> sshKeysResources = new ArrayList<String>();

    /**
     * @param wagonProtocol    scp scpexe
     * @param sshKeysResources paths in the classlaoder with ssh keys
     */
    public SshServerEmbedded( String wagonProtocol, List<String> sshKeysResources )
    {
        this.wagonProtocol = wagonProtocol;

        this.sshKeysResources = sshKeysResources;

        this.sshd = SshServer.setUpDefaultServer();


    }

    /**
     * @return random port used
     */
    public int start()
        throws IOException
    {
        sshd.setPort( 0 );

        sshd.setUserAuthFactories( Arrays.asList( new UserAuthPublicKey.Factory(), new UserAuthPassword.Factory() ) );

        sshd.setPublickeyAuthenticator( new PublickeyAuthenticator()
        {
            public boolean authenticate( String s, PublicKey publicKey, ServerSession serverSession )
            {
                return true;
            }
        } );

        ResourceKeyPairProvider resourceKeyPairProvider =
            new ResourceKeyPairProvider( sshKeysResources.toArray( new String[sshKeysResources.size()] ) );

        sshd.setKeyPairProvider( resourceKeyPairProvider );
        SessionFactory sessionFactory = new SessionFactory()
        {
            @Override
            protected AbstractSession doCreateSession( IoSession ioSession )
                throws Exception
            {
                System.out.println( "doCreateSession" );
                return super.doCreateSession( ioSession );
            }
        };
        sshd.setSessionFactory( sessionFactory );

        //sshd.setFileSystemFactory(  );

        final ProcessShellFactory processShellFactory =
            new ProcessShellFactory( new String[]{ "/bin/sh", "-i", "-l" } );
        sshd.setShellFactory( processShellFactory );

        CommandFactory delegateCommandFactory = new CommandFactory()
        {
            public Command createCommand( String command )
            {
                return new ShellCommand( command );
            }
        };

        ScpCommandFactory commandFactory = new ScpCommandFactory( delegateCommandFactory );
        sshd.setCommandFactory( commandFactory );

        FileSystemFactory fileSystemFactory = new FileSystemFactory()
        {
            public FileSystemView createFileSystemView( Session session )
                throws IOException
            {
                return new FileSystemView()
                {
                    public SshFile getFile( String file )
                    {
                        file = file.replace( "\\", "" );
                        file = file.replace( "\"", "" );
                        File f = new File( FileUtils.normalize( file ) );

                        return new SshServerEmbedded.TestSshFile( f.getAbsolutePath(), f,
                                                                  System.getProperty( "user.name" ) );
                    }

                    public SshFile getFile( SshFile baseDir, String file )
                    {
                        file = file.replace( "\\", "" );
                        file = file.replace( "\"", "" );
                        File f = new File( FileUtils.normalize( file ) );
                        return new SshServerEmbedded.TestSshFile( f.getAbsolutePath(), f,
                                                                  System.getProperty( "user.name" ) );
                    }
                };
            }
        };
        sshd.setNioWorkers( 0 );
        //sshd.setScheduledExecutorService(  );
        sshd.setFileSystemFactory( fileSystemFactory );
        sshd.start();
        this.port = sshd.getPort();
        return this.port;
    }


    public void stop( boolean immediately )
        throws InterruptedException
    {
        sshd.stop( immediately );
    }

    public int getPort()
    {
        return port;
    }

    public static class TestSshFile
        extends NativeSshFile
    {
        public TestSshFile( String fileName, File file, String userName )
        {

            super( FileUtils.normalize( fileName ), file, userName );
        }
    }
}
