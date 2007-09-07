package org.apache.maven.wagon.providers.ssh.ganymed;

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

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.HTTPProxyData;
import ch.ethz.ssh2.ProxyData;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.providers.ssh.AbstractSshWagon;
import org.apache.maven.wagon.providers.ssh.CommandExecutorStreamProcessor;
import org.apache.maven.wagon.providers.ssh.SshWagon;
import org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * AbstractGanymedWagon 
 *
 * @version $Id$
 */
public abstract class AbstractGanymedWagon
    extends AbstractSshWagon
    implements SshWagon
{
    /**
     * @plexus.requirement role-hint="file"
     */
    private KnownHostsProvider knownHostsProvider;
    
    /**
     * @plexus.requirement
     */
    private InteractiveUserInfo interactiveUserInfo;
    
    protected Connection connection;

    public void openConnection()
        throws AuthenticationException
    {
        super.openConnection();

        String host = getRepository().getHost();
        int port = getPort();

        File privateKey = getPrivateKey();

        /*
        if ( !interactive )
        {
            uIKeyboardInteractive = null;
            setInteractiveUserInfo( new NullInteractiveUserInfo() );
        }
        */

        connection = new Connection( host, port );

        if ( proxyInfo != null && proxyInfo.getHost() != null )
        {
            ProxyData proxy = new HTTPProxyData( proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUserName(),
                                                 proxyInfo.getPassword() );

            connection.setProxyData( proxy );
        }

        /* TODO! need to create a custom ServerHostKeyVerifier, and then pass that to connect later on.
          Note: the verifier will also need to add to ~/.ssh/known_hosts if it happens to be updated

        if ( getKnownHostsProvider() != null )
        {
            try
            {
                String contents = getKnownHostsProvider().getContents();
                if ( contents != null )
                {
                    sch.setKnownHosts( new StringInputStream( contents ) );
                }
            }
            catch ( JSchException e )
            {
                fireSessionError( e );
                // continue without known_hosts
            }
        }
        */

        try
        {
            // TODO: connection timeout?
            connection.connect();
        }
        catch ( IOException e )
        {
            fireSessionError( e );
            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }

        try
        {
            boolean authenticated;

            if ( privateKey != null && privateKey.exists() )
            {
                authenticated = connection.authenticateWithPublicKey( authenticationInfo.getUserName(), privateKey,
                                                                      authenticationInfo.getPassphrase() );
            }
            else
            {
                authenticated = connection.authenticateWithPassword( authenticationInfo.getUserName(),
                                                                     authenticationInfo.getPassword() );
            }
            // TODO! keyboard interactive

            if ( !authenticated )
            {
                throw new AuthenticationException( "Authentication failed." );
            }
        }
        catch ( IOException e )
        {
            closeConnection();
            fireSessionError( e );
            throw new AuthenticationException( "Cannot authenticate. Reason: " + e.getMessage(), e );
        }
    }

    // TODO! factor out into a separate class?
    public Streams executeCommand( String command, boolean ignoreFailures )
        throws CommandExecutionException
    {
        fireTransferDebug( "Executing command: " + command );

        Session session;
        try
        {
            session = connection.openSession();
        }
        catch ( IOException e )
        {
            throw new CommandExecutionException( "Cannot open session. Reason: " + e.getMessage(), e );
        }

        try
        {
            session.execCommand( command );
        }
        catch ( IOException e )
        {
            throw new CommandExecutionException( "Cannot execute remote command: " + command, e );
        }

        InputStream stdout = new StreamGobbler( session.getStdout() );
        InputStream stderr = new StreamGobbler( session.getStderr() );

        BufferedReader stdoutReader = new BufferedReader( new InputStreamReader( stdout ) );
        BufferedReader stderrReader = new BufferedReader( new InputStreamReader( stderr ) );

        try
        {
            Streams streams = CommandExecutorStreamProcessor.processStreams( stderrReader, stdoutReader );

            if ( streams.getErr().length() > 0 )
            {
                int exitCode = session.getExitStatus().intValue();
                throw new CommandExecutionException( "Exit code: " + exitCode + " - " + streams.getErr() );
            }

            return streams;
        }
        catch ( IOException e )
        {
            throw new CommandExecutionException( "Cannot read streams after remote command: " + command, e );
        }
        finally
        {
            IOUtil.close( stdoutReader );
            IOUtil.close( stderrReader );
        }
    }

    public void closeConnection()
    {
        if ( connection != null )
        {
            connection.close();
            connection = null;
        }
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
}
