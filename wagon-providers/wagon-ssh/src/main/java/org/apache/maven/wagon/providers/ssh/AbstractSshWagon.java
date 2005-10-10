package org.apache.maven.wagon.providers.ssh;

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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Common SSH operations.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractSshWagon
    extends AbstractWagon
    implements SshCommandExecutor
{
    public static final int DEFAULT_SSH_PORT = 22;

    public static final int SOCKS5_PROXY_PORT = 1080;

    protected Session session;

    public static final String EXEC_CHANNEL = "exec";

    private static final int LINE_BUFFER_SIZE = 256;

    private static final byte LF = '\n';

    private KnownHostsProvider knownHostsProvider;

    private InteractiveUserInfo interactiveUserInfo;

    private JSch sch;

    public void openConnection()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            throw new IllegalArgumentException( "Authentication Credentials cannot be null for SSH protocol" );
        }

        sch = new JSch();

        int port = getRepository().getPort();

        if ( port == WagonConstants.UNKNOWN_PORT )
        {
            port = DEFAULT_SSH_PORT;
        }

        String host = getRepository().getHost();

        try
        {
            session = sch.getSession( authenticationInfo.getUserName(), host, port );
        }
        catch ( JSchException e )
        {
            fireSessionError( e );

            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }

        // If user don't define a password, he want to use a private key
        if ( authenticationInfo.getPassword() == null )
        {
            File privateKey;

            if ( authenticationInfo.getPrivateKey() != null )
            {
                privateKey = new File( authenticationInfo.getPrivateKey() );
            }
            else
            {
                privateKey = findPrivateKey();
            }

            if ( privateKey.exists() )
            {
                if ( authenticationInfo.getPassphrase() == null )
                {
                    authenticationInfo.setPassphrase( "" );
                }

                fireSessionDebug( "Using private key: " + privateKey );

                try
                {
                    sch.addIdentity( privateKey.getAbsolutePath(), authenticationInfo.getPassphrase() );
                }
                catch ( JSchException e )
                {
                    fireSessionError( e );

                    throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
                }
            }
            else
            {
                String msg = "Private key was not found. You must define a private key or a password for repo: " +
                    getRepository().getName();

                throw new AuthenticationException( msg );
            }
        }

        if ( proxyInfo != null && proxyInfo.getHost() != null )
        {
            Proxy proxy;

            int proxyPort = proxyInfo.getPort();

            // HACK: if port == 1080 we will use SOCKS5 Proxy, otherwise will use HTTP Proxy
            if ( proxyPort == SOCKS5_PROXY_PORT )
            {
                proxy = new ProxySOCKS5( proxyInfo.getHost() );
                ( (ProxySOCKS5) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
            }
            else
            {
                proxy = new ProxyHTTP( proxyInfo.getHost(), proxyPort );
                ( (ProxyHTTP) proxy ).setUserPasswd( proxyInfo.getUserName(), proxyInfo.getPassword() );
            }

            try
            {
                proxy.connect( session, host, port );
            }
            catch ( Exception e )
            {
                fireSessionError( e );

                throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
            }
        }

        Properties config = new Properties();

        // username and password will be given via UserInfo interface.
        UserInfo ui = new WagonUserInfo( authenticationInfo, interactiveUserInfo );

        if ( knownHostsProvider != null )
        {
            try
            {
                knownHostsProvider.addConfiguration( config );
                knownHostsProvider.addKnownHosts( sch, ui );
            }
            catch ( JSchException e )
            {
                fireSessionError( e );
                // continue without known_hosts
            }
        }

        session.setConfig( config );

        session.setUserInfo( ui );

        try
        {
            session.connect();
        }
        catch ( JSchException e )
        {
            fireSessionError( e );

            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }
    }

    private File findPrivateKey()
    {
        String privateKeyDirectory = System.getProperty( "wagon.privateKeyDirectory" );

        if ( privateKeyDirectory == null )
        {
            privateKeyDirectory = System.getProperty( "user.home" );
        }

        File privateKey = new File( privateKeyDirectory, ".ssh/id_dsa" );

        if ( !privateKey.exists() )
        {
            privateKey = new File( privateKeyDirectory, ".ssh/id_rsa" );
        }

        return privateKey;
    }

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        ChannelExec channel = null;

        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;
        try
        {
            fireTransferDebug( "Executing command: " + command );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command + "\n" );

            out = channel.getOutputStream();

            in = channel.getInputStream();

            err = channel.getErrStream();

            channel.connect();

            sendEom( out );

            String line = readLine( err );

            if ( line != null )
            {
                throw new CommandExecutionException( line );
            }
        }
        catch ( JSchException e )
        {
            throw new CommandExecutionException( "Cannot execute remote command: " + command, e );
        }
        catch ( IOException e )
        {
            throw new CommandExecutionException( "Cannot execute remote command: " + command, e );
        }
        finally
        {
            IOUtil.close( out );
            IOUtil.close( in );
            IOUtil.close( err );
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    protected String readLine( InputStream in )
        throws IOException
    {
        byte[] buf = new byte[LINE_BUFFER_SIZE];

        String result = null;
        for ( int i = 0; result == null; i++ )
        {
            if ( in.read( buf, i, 1 ) != 1 )
            {
                return null;
            }

            if ( buf[i] == LF )
            {
                result = new String( buf, 0, i );
            }
        }
        return result;
    }

    protected static void sendEom( OutputStream out )
        throws IOException
    {
        out.write( 0 );

        out.flush();
    }

    public void closeConnection()
    {
        if ( knownHostsProvider != null )
        {
            knownHostsProvider.storeKnownHosts( sch );
        }

        if ( session != null )
        {
            session.disconnect();
            session = null;
        }
        
        sch = null;
    }

    protected void handleGetException( Resource resource, Exception e, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        fireTransferError( resource, e, TransferEvent.REQUEST_GET );

        if ( destination.exists() )
        {
            boolean deleted = destination.delete();

            if ( !deleted )
            {
                destination.deleteOnExit();
            }
        }

        String msg = "Error occured while downloading '" + resource + "' from the remote repository:" + getRepository();

        if ( "No such file".equals( e.toString() ) )
        {
            // SFTP only
            throw new ResourceDoesNotExistException( msg, e );
        }
        else
        {
            throw new TransferFailedException( msg, e );
        }
    }

    // ----------------------------------------------------------------------
    // JSch user info
    // ----------------------------------------------------------------------
    // TODO: are the prompt values really right? Is there an alternative to UserInfo?

    private static class WagonUserInfo
        implements UserInfo
    {
        private final AuthenticationInfo authInfo;

        private final InteractiveUserInfo userInfo;

        WagonUserInfo( AuthenticationInfo authInfo, InteractiveUserInfo userInfo )
        {
            this.authInfo = authInfo;
            this.userInfo = userInfo;
        }

        public String getPassphrase()
        {
            return authInfo.getPassphrase();
        }

        public String getPassword()
        {
            return authInfo.getPassword();
        }

        public boolean promptPassphrase( String arg0 )
        {
            return true;
        }

        public boolean promptPassword( String arg0 )
        {
            return true;
        }

        public boolean promptYesNo( String arg0 )
        {
            if ( userInfo != null )
            {
                return userInfo.promptYesNo( arg0 );
            }
            else
            {
                return false;
            }
        }

        public void showMessage( String message )
        {
            if ( userInfo != null )
            {
                userInfo.showMessage( message );
            }
        }
    }

    public final KnownHostsProvider getKnownHostsProvider()
    {
        return knownHostsProvider;
    }

    public final void setKnownHostsProvider( KnownHostsProvider knownHostsProvider )
    {
        if ( knownHostsProvider == null )
        {
            throw new IllegalArgumentException( "knownHostsProvider can't be null" );
        }
        this.knownHostsProvider = knownHostsProvider;
    }

    public InteractiveUserInfo getInteractiveUserInfo()
    {
        return interactiveUserInfo;
    }

    public void setInteractiveUserInfo( InteractiveUserInfo interactiveUserInfo )
    {
        if ( interactiveUserInfo == null )
        {
            throw new IllegalArgumentException( "interactiveUserInfo can't be null" );
        }
        this.interactiveUserInfo = interactiveUserInfo;
    }
}
