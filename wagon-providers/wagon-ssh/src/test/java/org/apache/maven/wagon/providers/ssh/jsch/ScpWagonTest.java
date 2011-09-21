package org.apache.maven.wagon.providers.ssh.jsch;

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

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.TestData;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
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
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class ScpWagonTest
    extends StreamingWagonTestCase
{

    final SshServer sshd = SshServer.setUpDefaultServer();

    @Override
    protected Wagon getWagon()
        throws Exception
    {
        ScpWagon scpWagon = (ScpWagon) super.getWagon();
        scpWagon.setInteractive( false );
        scpWagon.setKnownHostsProvider( new KnownHostsProvider()
        {
            public void storeKnownHosts( String contents )
                throws IOException
            {

            }

            public void setHostKeyChecking( String hostKeyChecking )
            {
            }

            public String getHostKeyChecking()
            {
                return "no";
            }

            public String getContents()
            {
                return null;
            }
        } );
        return scpWagon;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        sshd.setPort( 0 );

        sshd.setUserAuthFactories( Arrays.asList( new UserAuthPublicKey.Factory(), new UserAuthPassword.Factory() ) );

        sshd.setPublickeyAuthenticator( new PublickeyAuthenticator()
        {
            public boolean authenticate( String s, PublicKey publicKey, ServerSession serverSession )
            {
                return true;
            }
        } );

        FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider();
        File sshKey = new File( System.getProperty( "sshKeysPath", "src/test/ssh-keys" ), "id_rsa" );
        fileKeyPairProvider.setFiles( Arrays.asList( sshKey.getPath() ).toArray( new String[1] ) );

        sshd.setKeyPairProvider( fileKeyPairProvider );
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
                    // Executing command: scp -t "/Users/olamy/dev/sources/maven/maven-wagon/wagon-providers/wagon-ssh/target/classes/wagon-ssh-test/olamy/test-resource"
                    public SshFile getFile( String file )
                    {
                        file = file.replace( "\\", "" );
                        file = file.replace( "\"", "" );
                        File f = new File( FileUtils.normalize( file ) );

                        return new TestSshFile( f.getAbsolutePath(), f, System.getProperty( "user.name" ) );
                    }

                    public SshFile getFile( SshFile baseDir, String file )
                    {
                        file = file.replace( "\\", "" );
                        file = file.replace( "\"", "" );
                        File f = new File( FileUtils.normalize( file ) );
                        return new TestSshFile( f.getAbsolutePath(), f, System.getProperty( "user.name" ) );
                    }
                };
            }
        };
        sshd.setNioWorkers( 0 );
        //sshd.setScheduledExecutorService(  );
        sshd.setFileSystemFactory( fileSystemFactory );
        sshd.start();
        System.out.println( "sshd on port " + sshd.getPort() );
    }

    @Override
    protected void tearDownWagonTestingFixtures()
        throws Exception
    {
        sshd.stop( true );
    }

    protected String getProtocol()
    {
        return "scp";
    }

    @Override
    protected int getTestRepositoryPort()
    {
        return sshd.getPort();
    }


    public String getTestRepositoryUrl()
    {
        return TestData.getTestRepositoryUrl( sshd.getPort() );
    }

    protected AuthenticationInfo getAuthInfo()
    {
        AuthenticationInfo authInfo = super.getAuthInfo();

        authInfo.setUserName( TestData.getUserName() );

        File privateKey = TestData.getPrivateKey();

        if ( privateKey.exists() )
        {
            authInfo.setPrivateKey( privateKey.getAbsolutePath() );

            authInfo.setPassphrase( "" );
        }

        return authInfo;
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        return new File( repository.getBasedir(), resource.getName() ).lastModified();
    }


    protected static class ShellCommand
        implements Command
    {

        protected static final int OK = 0;

        protected static final int WARNING = 1;

        protected static final int ERROR = 2;

        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private Thread thread;

        private String commandLine;

        ShellCommand( String commandLine )
        {
            this.commandLine = commandLine;
        }

        public void setInputStream( InputStream in )
        {
            this.in = in;
        }

        public void setOutputStream( OutputStream out )
        {
            this.out = out;
        }

        public void setErrorStream( OutputStream err )
        {
            this.err = err;
        }

        public void setExitCallback( ExitCallback callback )
        {
            this.callback = callback;
        }

        public void start( Environment env )
            throws IOException
        {
            File tmpFile = File.createTempFile( "wagon", "test-sh" );
            tmpFile.deleteOnExit();
            int exitValue = 0;
            SystemLogOutputStream systemOut = new SystemLogOutputStream( 1 );
            SystemLogOutputStream errOut = new SystemLogOutputStream( 1 );
            CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();
            try
            {

                Executor exec = new DefaultExecutor();
                exec.setStreamHandler( new PumpStreamHandler( systemOut, errOut ) );
                // hackhish defaut commandline tools not support ; or && so write a file with the script
                // and "/bin/sh -e " + tmpFile.getPath();

                FileUtils.fileWrite( tmpFile, commandLine );

                Commandline cl = new Commandline();
                cl.setExecutable( "/bin/sh" );
                //cl.createArg().setValue( "-e" );
                //cl.createArg().setValue( tmpFile.getPath() );
                cl.createArg().setFile( tmpFile );

                exitValue = CommandLineUtils.executeCommandLine( cl, stdout, stderr );
                System.out.println( "exit value " + exitValue );
                /*
                if ( exitValue == 0 )
                {
                    out.write( stdout.getOutput().getBytes() );
                    out.write( '\n' );
                    out.flush();

                }
                else
                {
                    out.write( stderr.getOutput().getBytes() );
                    out.write( '\n' );
                    out.flush();

                }*/

            }
            catch ( Exception e )
            {
                exitValue = ERROR;
                e.printStackTrace();
            }
            finally
            {
                deleteQuietly( tmpFile );
                if ( exitValue != 0 )
                {
                    err.write( stderr.getOutput().getBytes() );
                    err.write( '\n' );
                    err.flush();
                    callback.onExit( exitValue, stderr.getOutput() );
                }
                else
                {
                    out.write( stdout.getOutput().getBytes() );
                    out.write( '\n' );
                    out.flush();
                    callback.onExit( exitValue, stdout.getOutput() );
                }

            }
            /*
            out.write( exitValue );
            out.write( '\n' );

            */
            out.flush();
        }

        public void destroy()
        {

        }

        private void deleteQuietly( File f )
        {

            try
            {
                f.delete();
            }
            catch ( Exception e )
            {
                // ignore
            }
        }
    }


    static class TestSshFile
        extends NativeSshFile
    {
        public TestSshFile( String fileName, File file, String userName )
        {

            super( FileUtils.normalize( fileName ), file, userName );
        }
    }

    static class SystemLogOutputStream
        extends LogOutputStream
    {

        private List<String> lines = new ArrayList<String>();

        private SystemLogOutputStream( int level )
        {
            super( level );
        }

        protected void processLine( String line, int level )
        {
            lines.add( line );
        }
    }

    // file lastModified not return so don't support
    protected boolean supportsGetIfNewer()
    {
        return false;
    }

    public void testWagon() throws Exception
    {
        super.testWagon();
    }


}
