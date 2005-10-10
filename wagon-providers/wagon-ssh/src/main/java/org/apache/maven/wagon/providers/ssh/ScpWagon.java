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
import com.jcraft.jsch.JSchException;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.util.IoUtils;
import org.codehaus.plexus.util.StringUtils;

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
 * @version $Id$
 * @todo [BP] add compression flag
 */
public class ScpWagon
    extends AbstractSshWagon
{
    private static final byte LF = '\n';

    private static final char PATH_SEPARATOR = '/';

    private static final int BUFFER_SIZE = 1024;

    private static final char ACK_CHAR = 'C';

    private static final char ACK_SEPARATOR = ' ';

    private static final char ZERO_CHAR = '0';

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        resourceName = StringUtils.replace( resourceName, "\\", "/" );
        String dir = PathUtils.dirname( resourceName );
        dir = StringUtils.replace( dir, "\\", "/" );

        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        String umaskCmd = "";

        if ( getRepository().getPermissions() != null )
        {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if ( dirPerms != null )
            {
                umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms ) + "\n";
            }
        }

        String mkdirCmd = umaskCmd + "mkdir -p " + basedir + "/" + dir + "\n";

        executeCommand( mkdirCmd );

        ChannelExec channel = null;

        //I/O streams for remote scp
        OutputStream out = null;

        InputStream in;

        try
        {
            // exec 'scp -t rfile' remotely
            String command = "scp -t " + basedir + "/" + resourceName;

            fireTransferDebug( "Executing command: " + command );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            in = channel.getInputStream();

            channel.connect();

            if ( checkAck( in, false ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = source.length();

            command = "C0644 " + filesize + " ";

            if ( resourceName.lastIndexOf( PATH_SEPARATOR ) > 0 )
            {
                command += resourceName.substring( resourceName.lastIndexOf( PATH_SEPARATOR ) + 1 );
            }
            else
            {
                command += resourceName;
            }

            command += "\n";

            out.write( command.getBytes() );

            out.flush();

            if ( checkAck( in, false ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }

            putTransfer( resource, source, out, false );

            byte[] buf = new byte[BUFFER_SIZE];

            // send '\0'
            buf[0] = 0;

            out.write( buf, 0, 1 );

            out.flush();

            if ( checkAck( in, false ) != 0 )
            {
                throw new TransferFailedException( "ACK check failed" );
            }
        }
        catch ( IOException e )
        {
            String msg = "Error occured while deploying '" + resourceName + "' to remote repository: " +
                getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        catch ( JSchException e )
        {
            String msg = "Error occured while deploying '" + resourceName + "' to remote repository: " +
                getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( channel != null )
            {
                IoUtils.close( out );

                channel.disconnect();
            }
        }

        RepositoryPermissions permissions = getRepository().getPermissions();

        if ( permissions != null && permissions.getGroup() != null )
        {
            executeCommand( "chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n" );
        }

        if ( permissions != null && permissions.getFileMode() != null )
        {
            executeCommand( "chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n" );
        }
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        ChannelExec channel = null;

        //I/O streams for remote scp
        OutputStream out = null;

        InputStream in;

        createParentDirectories( destination );

        LazyFileOutputStream outputStream = new LazyFileOutputStream( destination );

        String basedir = getRepository().getBasedir();

        //@todo get content lenght and last modified

        try
        {
            String cmd = "scp -f " + basedir + "/" + resourceName;

            fireTransferDebug( "Executing command: " + cmd );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( cmd );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            in = channel.getInputStream();

            channel.connect();

            byte[] buf = new byte[BUFFER_SIZE];

            // send '\0'
            buf[0] = 0;

            out.write( buf, 0, 1 );

            out.flush();

            while ( true )
            {
                // TODO: is this really an ACK, or just an in.read()? If the latter, change checkAck method to not return a value, but throw an exception on non-zero result
                int c = checkAck( in, true );

                if ( c != ACK_CHAR )
                {
                    break;
                }

                // read '0644 '
                if ( in.read( buf, 0, 5 ) != 5 )
                {
                    throw new TransferFailedException( "Unexpected end of data." );
                }

                int filesize = 0;

                // get file size
                while ( true )
                {
                    if ( in.read( buf, 0, 1 ) != 1 )
                    {
                        throw new TransferFailedException( "Unexpected end of data." );
                    }

                    if ( buf[0] == ACK_SEPARATOR )
                    {
                        break;
                    }

                    filesize = filesize * 10 + ( buf[0] - ZERO_CHAR );
                }

                resource.setContentLength( filesize );

                for ( int i = 0; ; i++ )
                {
                    if ( in.read( buf, i, 1 ) != 1 )
                    {
                        throw new TransferFailedException( "Unexpected end of data." );
                    }

                    if ( buf[i] == LF )
                    {
                        break;
                    }
                }

                // send '\0'
                buf[0] = 0;

                out.write( buf, 0, 1 );

                out.flush();

                fireGetStarted( resource, destination );

                TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS,
                                                                 TransferEvent.REQUEST_GET );

                try
                {
                    while ( true )
                    {
                        int len = Math.min( buf.length, filesize );

                        if ( in.read( buf, 0, len ) != len )
                        {
                            throw new TransferFailedException( "Unexpected end of data." );
                        }

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
                    fireTransferError( resource, e, TransferEvent.REQUEST_GET );

                    IoUtils.close( outputStream );

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

                if ( checkAck( in, true ) != 0 )
                {
                    throw new TransferFailedException( "Wrong ACK" );
                }
                else
                {
                    fireTransferDebug( "ACK check: OK" );
                }

                // send '\0'
                buf[0] = 0;

                out.write( buf, 0, 1 );

                out.flush();
            }
        }
        catch ( JSchException e )
        {
            handleGetException( resource, e, destination );
        }
        catch ( IOException e )
        {
            handleGetException( resource, e, destination );
        }
        finally
        {
            IoUtils.close( out );

            if ( channel != null )
            {
                channel.disconnect();
            }

            IoUtils.close( outputStream );
        }
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "getIfNewer is scp wagon must be still implemented" );
    }

    static int checkAck( InputStream in, boolean isGet )
        throws IOException, ResourceDoesNotExistException, TransferFailedException
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

                sb.append( (char) c );
            }
            while ( c != LF );

            String message = sb.toString();
            if ( b == 1 )
            {
                // error
                if ( message.endsWith( "No such file or directory\n" ) && isGet )
                {
                    // TODO: this might be too hokey?
                    throw new ResourceDoesNotExistException( message );
                }
                else
                {
                    throw new TransferFailedException( message );
                }
            }
            else
            {
                // fatal error
                throw new TransferFailedException( message );
            }
        }

        return b;
    }
}
