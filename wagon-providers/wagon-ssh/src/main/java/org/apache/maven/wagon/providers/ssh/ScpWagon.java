package org.apache.maven.wagon.providers.ssh;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A base class for deployers and fetchers using protocols from SSH2 family and
 * JSch library for underlining implmenetation
 * <p/>
 * This is responsible for authentification stage of the process.
 * <p/>
 * We will first try to use public keys for authentication and if that doesn't
 * work then we fall back to using the login and password
 *
 * @todo [BP] add compression flag
 *
 * @version $Id$
 */
public class ScpWagon
        extends AbstractWagon implements SshCommandExecutor
{
    public static String EXEC_CHANNEL = "exec";

    public static int DEFAULT_SSH_PORT = 22;

    public static int SOCKS5_PROXY_PORT = 1080;

    protected Session session = null;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void openConnection()
            throws AuthenticationException
    {
        try
        {
            final AuthenticationInfo authInfo = getRepository().getAuthenticationInfo();

            if ( authInfo == null )
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

            session = jsch.getSession( authInfo.getUserName(), host, port );

            // If user don't define a password, he want to use a private key
            if ( authInfo.getPassword() == null )
            {
                File privateKey;

                if ( authInfo.getPrivateKey() != null )
                {
                    privateKey = new File( authInfo.getPrivateKey() );
                }
                else
                {
                    privateKey = findPrivateKey();
                }

                if ( privateKey.exists() )
                {
                    if ( authInfo.getPassphrase() == null )
                    {
                        authInfo.setPassphrase( "" );
                    }

                    fireSessionDebug( "Using private key: " + privateKey );

                    jsch.addIdentity( privateKey.getAbsolutePath(), authInfo.getPassphrase() );
                }
                else
                {
                    String msg = "You must define a private key or a password for repo: " + getRepository().getName();

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
                    ( ( ProxySOCKS5 ) proxy ).setUserPasswd( proxyInfo.getUserName(),
                            proxyInfo.getPassword() );
                }
                else
                {
                    proxy = new ProxyHTTP( proxyInfo.getHost(), proxyPort );
                    ( ( ProxyHTTP ) proxy ).setUserPasswd( proxyInfo.getUserName(),
                            proxyInfo.getPassword() );
                }

                proxy.connect( session, host, port );
            }

            // username and password will be given via UserInfo interface.
            UserInfo ui = new WagonUserInfo( authInfo );

            session.setUserInfo( ui );

            session.connect();
        }
        catch ( Exception e )
        {
            fireSessionError( e );

            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void closeConnection()
    {
        if ( session != null )
        {
            session.disconnect();
        }
    }

    public void executeCommand( String command )
            throws TransferFailedException
    {
        ChannelExec channel = null;

        try
        {
            fireTransferDebug( "Executing command: " + command );

            channel = ( ChannelExec ) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            channel.connect();
        }
        catch ( Exception e )
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

    public void put( File source, String resourceName )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {

        String basedir = getRepository().getBasedir();

        String dir = PathUtils.dirname( resourceName  );

        if ( dir != null && dir.length() > 0)
        {
            String mkdirCmd = "mkdir -p " + basedir + "/"  + dir  + "\n";

            executeCommand( mkdirCmd );
        }

        ChannelExec channel = null;

        //I/O streams for remote scp
        OutputStream out = null;

        InputStream in;

        try
        {
            // exec 'scp -t rfile' remotely
            String command = "scp -t " + basedir + "/"  + resourceName;

            fireTransferDebug( "Executing command: " + command );

            channel = ( ChannelExec ) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            in = channel.getInputStream();

            channel.connect();

            byte[] tmp = new byte[ 1 ];

            if ( checkAck( in ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = source.length();

            command = "C0644 " + filesize + " " + resourceName;

            command += "\n";

            out.write( command.getBytes() );

            out.flush();

            if ( checkAck( in ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }


            Resource resource = new Resource( resourceName );

            putTransfer( resource, source, out, false );

            byte[] buf = new byte[ 1024 ];

            // send '\0'
            buf[ 0 ] = 0;

            out.write( buf, 0, 1 );

            out.flush();

            if ( checkAck( in ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }
        }
        catch ( Exception e )
        {
            String msg = "Error occured while deploying '" + resourceName + "' to remote repository: " + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }

        RepositoryPermissions permissions = getRepository().getPermissions();

        if ( permissions != null && permissions.getGroup() != null )
        {
            executeCommand( "chgrp " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n" );
        }

        if ( permissions != null && permissions.getFileMode() != null )
        {
            executeCommand( "chmod " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n" );
        }

        if ( channel != null )
        {
            shutdownStream( out );

            channel.disconnect();
        }
    }

    public void get( String resourceName, File destination )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        ChannelExec channel = null;

        //I/O streams for remote scp
        OutputStream out = null;

        InputStream in = null;

        createParentDirectories( destination );

        LazyFileOutputStream outputStream = new LazyFileOutputStream( destination );

        String basedir = getRepository().getBasedir();

        //@todo get content lenght and last modified
        Resource resource = new Resource( resourceName );


        try
        {
            String cmd = "scp -f " + basedir + "/" + resourceName;

            fireTransferDebug( "Executing command: " + cmd );

            channel = ( ChannelExec ) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( cmd );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            in = channel.getInputStream();

            channel.connect();

            byte[] buf = new byte[ 1024 ];

            // send '\0'
            buf[ 0 ] = 0;

            out.write( buf, 0, 1 );

            out.flush();

            while ( true )
            {
                // TODO: is this really an ACK, or just an in.read()? If the latter, change checkAck method to not return a value, but throw an exception on non-zero result
                int c = checkAck( in );

                if ( c != 'C' )
                {
                    break;
                }

                // read '0644 '
                in.read( buf, 0, 5 );

                int filesize = 0;

                // get file size
                while ( true )
                {
                    in.read( buf, 0, 1 );

                    if ( buf[ 0 ] == ' ' )
                    {
                        break;
                    }

                    filesize = filesize * 10 + ( buf[ 0 ] - '0' );
                }

                resource.setContentLength(  filesize );


                for ( int i = 0; ; i++ )
                {
                    in.read( buf, i, 1 );

                    if ( buf[ i ] == ( byte ) 0x0a )
                    {
                        break;
                    }
                }

                // send '\0'
                buf[ 0 ] = 0;

                out.write( buf, 0, 1 );

                out.flush();

                fireGetStarted( resource, destination );

                TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_GET );

                try
                {
                    while ( true )
                    {
                        int len = Math.min( buf.length, filesize );

                        in.read( buf, 0, len );

                        outputStream.write( buf, 0, len );

                        fireTransferProgress( transferEvent, buf, len );

                        filesize -= len;

                        if ( filesize == 0 )
                        {
                            break;
                        }
                    }
                }
                catch ( IOException e )
                {
                    fireTransferError( resource, e );

                    shutdownStream( outputStream );

                    if ( destination.exists() )
                    {
                        boolean deleted = destination.delete();

                        if ( !deleted )
                        {
                            destination.deleteOnExit();
                        }
                    }

                    String msg = "GET request of: " + resource + " from " + repository.getName() + "failed";

                    throw new TransferFailedException( msg, e );

                }

                fireGetCompleted( resource, destination );

                if ( checkAck( in ) != 0 )
                {
                    throw new TransferFailedException( "Wrong ACK" );
                }
                else
                {
                    fireTransferDebug( "ACK check: OK" );
                }

                // send '\0'
                buf[ 0 ] = 0;

                out.write( buf, 0, 1 );

                out.flush();
            }
        }
        catch ( Exception e )
        {
            fireTransferError( resource, e );

            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( !deleted )
                {
                    destination.deleteOnExit();
                }
            }

            String msg = "Error occured while deploying to remote repository:" + getRepository();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( out != null )
            {
                shutdownStream( out );
            }
            if ( channel != null )
            {
                channel.disconnect();
            }

            shutdownStream( outputStream );
        }
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
    {
        throw new UnsupportedOperationException( "getIfNewer is scp wagon must be still implemented" );
    }

// ----------------------------------------------------------------------
// JSch user info
// ----------------------------------------------------------------------
// TODO: are the prompt values really right? Is there an alternative to UserInfo?
    public static class WagonUserInfo
            implements UserInfo
    {
        AuthenticationInfo authInfo;

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

    static int checkAck( InputStream in ) throws IOException
    {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //         -1

        if ( b == 0 || b == -1 )
        {
            return b;
        }

        if ( b == 1 || b == 2 )
        {
            StringBuffer sb = new StringBuffer();

            int c;

            do
            {
                c = in.read();

                sb.append( ( char ) c );
            }
            while ( c != '\n' );

            if ( b == 1 )
            {
                // TODO: log (throw exception?)
                // error
                System.out.print( sb.toString() );
            }
            if ( b == 2 )
            {
                // TODO: throw exception
                // fatal error
                System.out.print( sb.toString() );
            }
        }

        return b;
    }
}
