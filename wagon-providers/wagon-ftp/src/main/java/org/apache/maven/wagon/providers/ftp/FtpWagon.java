package org.apache.maven.wagon.providers.ftp;

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

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FtpWagon
    extends StreamWagon
{
    private FTPClient ftp;

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        AuthenticationInfo authInfo = getRepository().getAuthenticationInfo();

        if ( authInfo == null )
        {
            throw new IllegalArgumentException( "Authentication Credentials cannot be null for FTP protocol" );
        }

        String username = authInfo.getUserName();

        String password = authInfo.getPassword();

        String host = getRepository().getHost();

        ftp = new FTPClient();

        ftp.addProtocolCommandListener( new PrintCommandListener( this ) );

        try
        {
            if ( getRepository().getPort() != WagonConstants.UNKNOWN_PORT )
            {
                ftp.connect( host, getRepository().getPort() );
            }
            else
            {
                ftp.connect( host );
            }

            // After connection attempt, you should check the reply code to
            // verify
            // success.
            int reply = ftp.getReplyCode();

            if ( !FTPReply.isPositiveCompletion( reply ) )
            {
                fireSessionConnectionRefused();

                fireSessionDisconnecting();

                ftp.disconnect();

                fireSessionDisconnected();

                throw new AuthenticationException( "FTP server refused connection." );
            }
        }
        catch ( IOException e )
        {
            if ( ftp.isConnected() )
            {
                try
                {
                    fireSessionError( e );

                    fireSessionDisconnecting();

                    ftp.disconnect();

                    fireSessionDisconnected();
                }
                catch ( IOException f )
                {
                    // do nothing
                }
            }

            throw new AuthenticationException( "Could not connect to server." );
        }

        try
        {
            if ( ftp.login( username.trim(), password.trim() ) == false )
            {
                fireSessionConnectionRefused();

                throw new AuthenticationException( "Cannot login to remote system" );
            }

            fireSessionDebug( "Remote system is " + ftp.getSystemName() );

            // Set to binary mode.
            ftp.setFileType( FTP.BINARY_FILE_TYPE );


            // Use passive mode as default because most of us are
            // behind firewalls these days.
            ftp.enterLocalPassiveMode();

            boolean dirChanged = ftp.changeWorkingDirectory( getRepository().getBasedir() );

            if ( !dirChanged )
            {
                throw new ConnectionException( "Required directories: '" + getRepository().getBasedir() + "' are missing" );
            }
        }
        catch ( IOException e )
        {
            throw new ConnectionException( "Cannot login to remote system" );
        }
    }

    public void closeConnection()
        throws ConnectionException
    {
        if ( ftp.isConnected() )
        {
            
            try
            {
                // This is a NPE rethink shutting down the streams
                ftp.disconnect();
            }
            catch ( IOException e)
            {
                throw new ConnectionException( "Failed to close connection to FTP repository", e );
            }
        }
    }

    

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public OutputStream getOutputStream( String resource )
        throws TransferFailedException
    {
        OutputStream os;

        try
        {
            String[] dirs = PathUtils.dirnames( resource );

            for ( int i = 0; i < dirs.length; i++ )
            {
                ftp.makeDirectory( dirs[i] );

                boolean dirChanged = ftp.changeWorkingDirectory( dirs[i] );

                if ( !dirChanged )
                {
                    String msg = " Resource " + resource + " not found. Directory " + dirs[i] + " does not exist";

                    throw new ResourceDoesNotExistException( msg );
                }
            }

            // we come back to orginal basedir so
            // FTP wagon is ready for next requests
            for ( int i = 0; i < dirs.length; i++ )
            {
                ftp.changeWorkingDirectory( ".." );
            }


            os = ftp.storeFileStream( resource );

            if ( os == null)
            {
                 fireTransferDebug( "REPLY STRING: " + ftp.getReplyString() );

                 fireTransferDebug( "REPLY: " + ftp.getReply() );

                 fireTransferDebug( "REPLY CODE: " + ftp.getReplyCode() );
            }

            fireTransferDebug( "resource = " + resource );
            
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( "Cannot transfer: ", e );
        }

        return os;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public InputStream getInputStream( String resource )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        InputStream is;

        try
        {
            String[] dirs = PathUtils.dirnames( resource );

            for ( int i = 0; i < dirs.length; i++ )
            {
                boolean dirChanged = ftp.changeWorkingDirectory( dirs[i] );

                if ( !dirChanged )
                {
                    String msg = "Resource " + resource + " not found. Directory " + dirs[i] + " does not exist";

                    throw new ResourceDoesNotExistException( msg );
                }
            }

            is = ftp.retrieveFileStream( PathUtils.filename( resource ) );

            for ( int i = 0; i < dirs.length; i++ )
            {
                ftp.changeWorkingDirectory( ".." );
            }
        }
        catch ( Exception e )
        {
            throw new TransferFailedException( e.getMessage() );
        }

        return is;
    }

    
    public class PrintCommandListener
        implements ProtocolCommandListener
    {
        FtpWagon wagon;

        public PrintCommandListener( FtpWagon wagon )
        {
            this.wagon = wagon;
        }

        public void protocolCommandSent( ProtocolCommandEvent event )
        {
            wagon.fireSessionDebug( "Command sent: " + event.getMessage() );

        }

        public void protocolReplyReceived( ProtocolCommandEvent event )
        {
            wagon.fireSessionDebug( "Reply received: " + event.getMessage() );
        }
    }

    protected void fireSessionDebug( String msg )
    {
        super.fireSessionDebug( msg );
    }
}
