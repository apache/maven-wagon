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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.CommandExecutorStreamProcessor;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.providers.ssh.SshWagon;
import org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo;
import org.apache.maven.wagon.providers.ssh.interactive.NullInteractiveUserInfo;
import org.apache.maven.wagon.providers.ssh.jsch.interactive.UserInfoUIKeyboardInteractiveProxy;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostChangedException;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostEntry;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.apache.maven.wagon.providers.ssh.knownhost.UnknownHostException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

/**
 * AbstractJschWagon
 */
public abstract class AbstractJschWagon
    extends StreamWagon
    implements SshWagon, CommandExecutor
{
    protected ScpHelper sshTool = new ScpHelper( this );

    protected Session session;

    private String strictHostKeyChecking;

    /**
     * @plexus.requirement role-hint="file"
     */
    private volatile KnownHostsProvider knownHostsProvider;

    /**
     * @plexus.requirement
     */
    private volatile InteractiveUserInfo interactiveUserInfo;

    /**
     * @plexus.configuration
     */
    private volatile String preferredAuthentications;

    /**
     * @plexus.requirement
     */
    private volatile UIKeyboardInteractive uIKeyboardInteractive;

    private static final int SOCKS5_PROXY_PORT = 1080;

    protected static final String EXEC_CHANNEL = "exec";

    public void openConnectionInternal()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            authenticationInfo = new AuthenticationInfo();
        }

        if ( !interactive )
        {
            uIKeyboardInteractive = null;
            setInteractiveUserInfo( new NullInteractiveUserInfo() );
        }

        JSch sch = new JSch();

        File privateKey;
        try
        {
            privateKey = ScpHelper.getPrivateKey( authenticationInfo );
        }
        catch ( FileNotFoundException e )
        {
            throw new AuthenticationException( e.getMessage() );
        }

        //can only pick one method of authentication
        if ( privateKey != null && privateKey.exists() )
        {
            fireSessionDebug( "Using private key: " + privateKey );
            try
            {
                sch.addIdentity( privateKey.getAbsolutePath(), authenticationInfo.getPassphrase() );
            }
            catch ( JSchException e )
            {
                throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
            }
        }
        else
        {
            try
            {
                Connector connector = ConnectorFactory.getDefault().createConnector();
                if ( connector != null )
                {
                    IdentityRepository repo = new RemoteIdentityRepository( connector );
                    sch.setIdentityRepository( repo );
                }
            }
            catch ( AgentProxyException e )
            {
                fireSessionDebug( "Unable to connect to agent: " + e.toString() );
            }

        }

        String host = getRepository().getHost();
        int port =
            repository.getPort() == WagonConstants.UNKNOWN_PORT ? ScpHelper.DEFAULT_SSH_PORT : repository.getPort();
        try
        {
            String userName = authenticationInfo.getUserName();
            if ( userName == null )
            {
                userName = System.getProperty( "user.name" );
            }
            session = sch.getSession( userName, host, port );
            session.setTimeout( getTimeout() );
        }
        catch ( JSchException e )
        {
            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }

        Proxy proxy = null;
        ProxyInfo proxyInfo = getProxyInfo( ProxyInfo.PROXY_SOCKS5, getRepository().getHost() );
        if ( proxyInfo != null && proxyInfo.getHost() != null )
        {
            proxy = new ProxySOCKS5( proxyInfo.getHost(), proxyInfo.getPort() );
            ( (ProxySOCKS5) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
        }
        else
        {
            proxyInfo = getProxyInfo( ProxyInfo.PROXY_HTTP, getRepository().getHost() );
            if ( proxyInfo != null && proxyInfo.getHost() != null )
            {
                proxy = new ProxyHTTP( proxyInfo.getHost(), proxyInfo.getPort() );
                ( (ProxyHTTP) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
            }
            else
            {
                // Backwards compatibility
                proxyInfo = getProxyInfo( getRepository().getProtocol(), getRepository().getHost() );
                if ( proxyInfo != null && proxyInfo.getHost() != null )
                {
                    // if port == 1080 we will use SOCKS5 Proxy, otherwise will use HTTP Proxy
                    if ( proxyInfo.getPort() == SOCKS5_PROXY_PORT )
                    {
                        proxy = new ProxySOCKS5( proxyInfo.getHost(), proxyInfo.getPort() );
                        ( (ProxySOCKS5) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
                    }
                    else
                    {
                        proxy = new ProxyHTTP( proxyInfo.getHost(), proxyInfo.getPort() );
                        ( (ProxyHTTP) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
                    }
                }
            }
        }
        session.setProxy( proxy );

        // username and password will be given via UserInfo interface.
        UserInfo ui = new WagonUserInfo( authenticationInfo, getInteractiveUserInfo() );

        if ( uIKeyboardInteractive != null )
        {
            ui = new UserInfoUIKeyboardInteractiveProxy( ui, uIKeyboardInteractive );
        }

        Properties config = new Properties();
        if ( getKnownHostsProvider() != null )
        {
            try
            {
                String contents = getKnownHostsProvider().getContents();
                if ( contents != null )
                {
                    sch.setKnownHosts( new ByteArrayInputStream( contents.getBytes() ) );
                }
            }
            catch ( JSchException e )
            {
                // continue without known_hosts
            }
            if ( strictHostKeyChecking == null )
            {
                strictHostKeyChecking = getKnownHostsProvider().getHostKeyChecking();
            }
            config.setProperty( "StrictHostKeyChecking", strictHostKeyChecking );
        }

        if ( preferredAuthentications != null )
        {
            config.setProperty( "PreferredAuthentications", preferredAuthentications );
        }

        config.setProperty( "BatchMode", interactive ? "no" : "yes" );

        session.setConfig( config );

        session.setUserInfo( ui );

        try
        {
            session.connect();
        }
        catch ( JSchException e )
        {
            if ( e.getMessage().startsWith( "UnknownHostKey:" ) || e.getMessage().startsWith( "reject HostKey:" ) )
            {
                throw new UnknownHostException( host, e );
            }
            else if ( e.getMessage().contains( "HostKey has been changed" ) )
            {
                throw new KnownHostChangedException( host, e );
            }
            else
            {
                throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
            }
        }

        if ( getKnownHostsProvider() != null )
        {
            HostKeyRepository hkr = sch.getHostKeyRepository();

            HostKey[] hk = hkr.getHostKey( host, null );
            try
            {
                if ( hk != null )
                {
                    for ( HostKey hostKey : hk )
                    {
                        KnownHostEntry knownHostEntry = new KnownHostEntry( hostKey.getHost(), hostKey.getType(),
                            hostKey.getKey() );
                        getKnownHostsProvider().addKnownHost( knownHostEntry );
                    }
                }
            }
            catch ( IOException e )
            {
                closeConnection();

                throw new AuthenticationException(
                    "Connection aborted - failed to write to known_hosts. Reason: " + e.getMessage(), e );
            }
        }
    }

    public void closeConnection()
    {
        if ( session != null )
        {
            session.disconnect();
            session = null;
        }
    }

    public Streams executeCommand( String command, boolean ignoreStdErr, boolean ignoreNoneZeroExitCode )
        throws CommandExecutionException
    {
        ChannelExec channel = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;
        Streams streams = null;
        try
        {
            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            fireSessionDebug( "Executing: " + command );
            channel.setCommand( command + "\n" );

            stdoutReader = new BufferedReader( new InputStreamReader( channel.getInputStream() ) );
            stderrReader = new BufferedReader( new InputStreamReader( channel.getErrStream() ) );

            channel.connect();

            streams = CommandExecutorStreamProcessor.processStreams( stderrReader, stdoutReader );

            stdoutReader.close();
            stdoutReader = null;

            stderrReader.close();
            stderrReader = null;

            int exitCode = channel.getExitStatus();

            if ( streams.getErr().length() > 0 && !ignoreStdErr )
            {
                throw new CommandExecutionException( "Exit code: " + exitCode + " - " + streams.getErr() );
            }

            if ( exitCode != 0 && !ignoreNoneZeroExitCode )
            {
                throw new CommandExecutionException( "Exit code: " + exitCode + " - " + streams.getErr() );
            }

            return streams;
        }
        catch ( IOException e )
        {
            throw new CommandExecutionException( "Cannot execute remote command: " + command, e );
        }
        catch ( JSchException e )
        {
            throw new CommandExecutionException( "Cannot execute remote command: " + command, e );
        }
        finally
        {
            if ( streams != null )
            {
                fireSessionDebug( "Stdout results:" + streams.getOut() );
                fireSessionDebug( "Stderr results:" + streams.getErr() );
            }

            IOUtil.close( stdoutReader );
            IOUtil.close( stderrReader );
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    protected void handleGetException( Resource resource, Exception e )
        throws TransferFailedException
    {
        fireTransferError( resource, e, TransferEvent.REQUEST_GET );

        String msg =
            "Error occurred while downloading '" + resource + "' from the remote repository:" + getRepository() + ": "
                + e.getMessage();

        throw new TransferFailedException( msg, e );
    }

    public List<String> getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return sshTool.getFileList( destinationDirectory, repository );
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        sshTool.putDirectory( this, sourceDirectory, destinationDirectory );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        return sshTool.resourceExists( resourceName, repository );
    }

    public boolean supportsDirectoryCopy()
    {
        return true;
    }

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        fireTransferDebug( "Executing command: " + command );

        //backward compatible with wagon 2.10
        executeCommand( command, false, true );
    }

    public Streams executeCommand( String command, boolean ignoreFailures )
            throws CommandExecutionException
    {
        fireTransferDebug( "Executing command: " + command );

        //backward compatible with wagon 2.10
        return executeCommand( command, ignoreFailures, true );
    }

    public InteractiveUserInfo getInteractiveUserInfo()
    {
        return this.interactiveUserInfo;
    }

    public KnownHostsProvider getKnownHostsProvider()
    {
        return this.knownHostsProvider;
    }

    public void setInteractiveUserInfo( InteractiveUserInfo interactiveUserInfo )
    {
        this.interactiveUserInfo = interactiveUserInfo;
    }

    public void setKnownHostsProvider( KnownHostsProvider knownHostsProvider )
    {
        this.knownHostsProvider = knownHostsProvider;
    }

    public void setUIKeyboardInteractive( UIKeyboardInteractive uIKeyboardInteractive )
    {
        this.uIKeyboardInteractive = uIKeyboardInteractive;
    }

    public String getPreferredAuthentications()
    {
        return preferredAuthentications;
    }

    public void setPreferredAuthentications( String preferredAuthentications )
    {
        this.preferredAuthentications = preferredAuthentications;
    }

    public String getStrictHostKeyChecking()
    {
        return strictHostKeyChecking;
    }

    public void setStrictHostKeyChecking( String strictHostKeyChecking )
    {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    /** {@inheritDoc} */
    // This method will be removed as soon as JSch issue #122 is resolved
    @Override
    protected void transfer( Resource resource, InputStream input, OutputStream output, int requestType, long maxSize )
        throws IOException
    {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, requestType );
        transferEvent.setTimestamp( System.currentTimeMillis() );

        long remaining = maxSize;
        while ( remaining > 0L )
        {
            // let's safely cast to int because the min value will be lower than the buffer size.
            int n = input.read( buffer, 0, (int) Math.min( buffer.length, remaining ) );

            if ( n == -1 )
            {
                break;
            }

            fireTransferProgress( transferEvent, buffer, n );

            output.write( buffer, 0, n );

            remaining -= n;
        }
        output.flush();
    }
}
