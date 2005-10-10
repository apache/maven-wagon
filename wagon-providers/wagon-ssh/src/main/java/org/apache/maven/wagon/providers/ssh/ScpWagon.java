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
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.EOFException;
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
    private static final char PATH_SEPARATOR = '/';

    private static final char COPY_START_CHAR = 'C';

    private static final char ACK_SEPARATOR = ' ';

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        resourceName = StringUtils.replace( resourceName, "\\", "/" );
        String dir = PathUtils.dirname( resourceName );
        dir = StringUtils.replace( dir, "\\", "/" );

        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        try
        {
            if ( getRepository().getPermissions() != null )
            {
                String dirPerms = getRepository().getPermissions().getDirectoryMode();

                if ( dirPerms != null )
                {
                    String umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
                    executeCommand( umaskCmd );
                }
            }

            String mkdirCmd = "mkdir -p " + getPath( basedir, dir );

            executeCommand( mkdirCmd );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }

        ChannelExec channel = null;

        OutputStream out = null;

        String path = getPath( basedir, resourceName );

        try
        {
            // exec 'scp -t rfile' remotely
            String command = "scp -t " + path;

            fireTransferDebug( "Executing command: " + command );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            InputStream in = channel.getInputStream();

            channel.connect();

            checkAck( in );

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

            checkAck( in );

            putTransfer( resource, source, out, false );

            sendEom( out );

            checkAck( in );
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
                IOUtil.close( out );

                channel.disconnect();
            }
        }

        try
        {
            RepositoryPermissions permissions = getRepository().getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + path );
            }

            // TODO: could avoid this by replacing 0644 above
            if ( permissions != null && permissions.getFileMode() != null )
            {
                executeCommand( "chmod -f " + permissions.getFileMode() + " " + path );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }

    private static void checkAck( InputStream in )
        throws IOException, TransferFailedException
    {
        int code = in.read();
        if ( code == -1 )
        {
            throw new TransferFailedException( "Unexpected end of data" );
        }
        else if ( code != 0 )
        {
            throw new TransferFailedException( "Did receive proper ACK: '" + code + "'" );
        }
    }

    private static String getPath( String basedir, String dir )
    {
        String path;
        path = basedir;
        if ( !basedir.endsWith( "/" ) && !dir.startsWith( "/" ) )
        {
            path += "/";
        }
        path += dir;
        return path;
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

        String basedir = getRepository().getBasedir();

        try
        {
            String path = getPath( basedir, resourceName );
            String cmd = "scp -f " + path;

            fireTransferDebug( "Executing command: " + cmd );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( cmd );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            in = channel.getInputStream();

            channel.connect();

            sendEom( out );

            int exitCode = in.read();

            String line = readLine( in );

            if ( exitCode != COPY_START_CHAR )
            {
                if ( exitCode == 1 && line.endsWith( "No such file or directory" ) )
                {
                    throw new ResourceDoesNotExistException( line );
                }
                else
                {
                    throw new TransferFailedException( "Exit code: " + exitCode + " - " + line );
                }
            }

            if ( line == null )
            {
                throw new EOFException( "Unexpected end of data" );
            }

            String perms = line.substring( 0, 4 );
            fireTransferDebug( "Remote file permissions: " + perms );

            if ( line.charAt( 4 ) != ACK_SEPARATOR )
            {
                throw new TransferFailedException( "Invalid transfer header: " + line );
            }

            int index = line.indexOf( ACK_SEPARATOR, 5 );
            if ( index < 0 )
            {
                throw new TransferFailedException( "Invalid transfer header: " + line );
            }

            int filesize = Integer.valueOf( line.substring( 5, index ) ).intValue();
            fireTransferDebug( "Remote file size: " + filesize );

            resource.setContentLength( filesize );

            String filename = line.substring( index + 1 );
            fireTransferDebug( "Remote filename: " + filename );

            sendEom( out );

            getTransfer( resource, destination, in, false, filesize );

            if ( destination.length() != filesize )
            {
                throw new TransferFailedException(
                    "Expected file length: " + filesize + "; received = " + destination.length() );
            }

            // TODO: we could possibly have received additional files here

            checkAck( in );

            sendEom( out );

            if ( in.read() != -1 )
            {
                throw new TransferFailedException(
                    "End of stream not encountered - server possibly attempted to send multiple files" );
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
            IOUtil.close( out );

            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "getIfNewer is scp wagon must be still implemented" );
    }

}
