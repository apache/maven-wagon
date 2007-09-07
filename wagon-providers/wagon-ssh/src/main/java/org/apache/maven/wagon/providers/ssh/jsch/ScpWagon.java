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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SFTP protocol wagon.
 * 
 * A base class for deployers and fetchers using protocols from SSH2 family and
 * JSch library for underlying implementation.
 * 
 * This is responsible for authentification stage of the process.
 * 
 * We will first try to use public keys for authentication and if that doesn't
 * work then we fall back to using the login and password.
 *
 * @version $Id$
 * @todo [BP] add compression flag
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="scp"
 *   instantiation-strategy="per-lookup"
 */
public class ScpWagon
    extends AbstractJschWagon
{
    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        Resource resource = getResource( destination );

        String dir = getResourceDirectory( resource.getName() );

        firePutInitiated( resource, source );

        ScpHelper.createRemoteDirectories( getPath( basedir, dir ), getRepository().getPermissions(), this );

        RepositoryPermissions permissions = getRepository().getPermissions();

        put( source, basedir, resource, getOctalMode( permissions ) );

        setFileGroup( permissions, basedir, resource );
    }

    private void setFileGroup( RepositoryPermissions permissions, String basedir, Resource resource )
        throws TransferFailedException
    {
        try
        {
            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + getPath( basedir, resource.getName() ) );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        get( basedir, resource, destination );
    }

    private static final char COPY_START_CHAR = 'C';

    private static final char ACK_SEPARATOR = ' ';

    private static final String END_OF_FILES_MSG = "E\n";

    private static final int LINE_BUFFER_SIZE = 8192;

    private static final byte LF = '\n';

    public void put( File source, String basedir, Resource resource, String octalMode )
        throws TransferFailedException
    {
        String path = getPath( basedir, resource.getName() );

        String resourceName = resource.getName();

        ChannelExec channel = null;

        OutputStream out = null;
        try
        {
            // exec 'scp -t -d rfile' remotely
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

            command = "C" + octalMode + " " + filesize + " ";

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

            // This came from SCPClient in Ganymede SSH2. It is sent after all files.
            out.write( END_OF_FILES_MSG.getBytes() );
            out.flush();
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
    }

    private void checkAck( InputStream in )
        throws IOException, TransferFailedException
    {
        int code = in.read();
        if ( code == -1 )
        {
            throw new TransferFailedException( "Unexpected end of data" );
        }
        else if ( code == 1 )
        {
            String line = readLine( in );

            throw new TransferFailedException( "SCP terminated with error: '" + line + "'" );
        }
        else if ( code == 2 )
        {
            throw new TransferFailedException( "SCP terminated with error (code: " + code + ")" );
        }
        else if ( code != 0 )
        {
            throw new TransferFailedException( "SCP terminated with unknown error code" );
        }
    }

    public void get( String basedir, Resource resource, File destination )
        throws ResourceDoesNotExistException, TransferFailedException
    {
        String path = getPath( basedir, resource.getName() );

        //I/O streams for remote scp
        OutputStream out = null;

        ChannelExec channel = null;

        try
        {
            String cmd = "scp -f " + path;

            fireTransferDebug( "Executing command: " + cmd );

            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( cmd );

            // get I/O streams for remote scp
            out = channel.getOutputStream();

            InputStream in = channel.getInputStream();

            channel.connect();

            sendEom( out );

            int exitCode = in.read();

            if ( exitCode == 'P' )
            {
                // ignore modification times

                exitCode = in.read();
            }

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

            if ( line.charAt( 4 ) != ACK_SEPARATOR && line.charAt( 5 ) != ACK_SEPARATOR )
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

    protected String readLine( InputStream in )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();

        while ( true )
        {
            if ( sb.length() > LINE_BUFFER_SIZE )
            {
                throw new IOException( "Remote server sent a too long line" );
            }

            int c = in.read();

            if ( c < 0 )
            {
                throw new IOException( "Remote connection terminated unexpectedly." );
            }

            if ( c == LF )
            {
                break;
            }

            sb.append( (char) c );
        }
        return sb.toString();
    }

    protected static void sendEom( OutputStream out )
        throws IOException
    {
        out.write( 0 );

        out.flush();
    }
}
