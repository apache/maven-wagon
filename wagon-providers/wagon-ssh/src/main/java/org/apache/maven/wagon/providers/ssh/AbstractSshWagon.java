/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.wagon.providers.ssh;

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
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;

/**
 * TODO: describe
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

    public void openConnection()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            throw new IllegalArgumentException( "Authentication Credentials cannot be null for SSH protocol" );
        }

        JSch jsch = new JSch();

        int port = getRepository().getPort();

        if ( port == WagonConstants.UNKNOWN_PORT )
        {
            port = DEFAULT_SSH_PORT;
        }

        String host = getRepository().getHost();

        try
        {
            session = jsch.getSession( authenticationInfo.getUserName(), host, port );
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
                    jsch.addIdentity( privateKey.getAbsolutePath(), authenticationInfo.getPassphrase() );
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

        // username and password will be given via UserInfo interface.
        UserInfo ui = new ScpWagon.WagonUserInfo( authenticationInfo );

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
        throws TransferFailedException
    {
        ChannelExec channel = null;

        try
        {
            fireTransferDebug( "Executing command: " + command );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            channel.connect();
        }
        catch ( JSchException e )
        {
            throw new TransferFailedException( "Cannot execute remote command: " + command, e );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    public void closeConnection()
    {
        if ( session != null )
        {
            session.disconnect();
        }
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

        String msg = "Error occured while downloading from the remote repository:" + getRepository();

        // TODO: this might be too hokey?
        if ( "No such file".equals( e.getMessage() ) )
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
        private AuthenticationInfo authInfo;

        WagonUserInfo( AuthenticationInfo authInfo )
        {
            this.authInfo = authInfo;
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
            return true;
        }

        public void showMessage( String message )
        {
            // TODO: is this really debug?
            //fireTransferDebug( message );
        }
    }
}
