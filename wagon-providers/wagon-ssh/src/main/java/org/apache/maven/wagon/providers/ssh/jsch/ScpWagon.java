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
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SCP protocol wagon.
 * <p/>
 * Note that this implementation is <i>not</i> thread-safe, and multiple channels can not be used on the session at
 * the same time.
 * <p/>
 * See <a href="http://blogs.sun.com/janp/entry/how_the_scp_protocol_works">
 * http://blogs.sun.com/janp/entry/how_the_scp_protocol_works</a>
 * for information on how the SCP protocol works.
 *
 *
 * @todo [BP] add compression flag
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="scp"
 * instantiation-strategy="per-lookup"
 */
public class ScpWagon
    extends AbstractJschWagon
{
    private static final char COPY_START_CHAR = 'C';

    private static final char ACK_SEPARATOR = ' ';

    private static final String END_OF_FILES_MSG = "E\n";

    private static final int LINE_BUFFER_SIZE = 8192;

    private static final byte LF = '\n';

    private ChannelExec channel;

    private InputStream channelInputStream;

    private OutputStream channelOutputStream;

    private void setFileGroup( RepositoryPermissions permissions, String basedir, Resource resource )
        throws CommandExecutionException
    {
        if ( permissions != null && permissions.getGroup() != null )
        {
            //executeCommand( "chgrp -f " + permissions.getGroup() + " " + getPath( basedir, resource.getName() ) );
            executeCommand( "chgrp -f " + permissions.getGroup() + " \"" + getPath( basedir, resource.getName() ) + "\"" );
        }
    }

    protected void cleanupPutTransfer( Resource resource )
    {
        if ( channel != null )
        {
            channel.disconnect();
            channel = null;
        }
    }

    protected void finishPutTransfer( Resource resource, InputStream input, OutputStream output )
        throws TransferFailedException
    {
        try
        {
            sendEom( output );

            checkAck( channelInputStream );

            // This came from SCPClient in Ganymede SSH2. It is sent after all files.
            output.write( END_OF_FILES_MSG.getBytes() );
            output.flush();
        }
        catch ( IOException e )
        {
            handleIOException( resource, e );
        }

        String basedir = getRepository().getBasedir();
        try
        {
            setFileGroup( getRepository().getPermissions(), basedir, resource );
        }
        catch ( CommandExecutionException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( e.getMessage(), e );
        }
    }

    private void checkAck( InputStream in )
        throws IOException
    {
        int code = in.read();
        if ( code == -1 )
        {
            throw new IOException( "Unexpected end of data" );
        }
        else if ( code == 1 )
        {
            String line = readLine( in );

            throw new IOException( "SCP terminated with error: '" + line + "'" );
        }
        else if ( code == 2 )
        {
            throw new IOException( "SCP terminated with error (code: " + code + ")" );
        }
        else if ( code != 0 )
        {
            throw new IOException( "SCP terminated with unknown error code" );
        }
    }

    protected void finishGetTransfer( Resource resource, InputStream input, OutputStream output )
        throws TransferFailedException
    {
        try
        {
            checkAck( input );

            sendEom( channelOutputStream );
        }
        catch ( IOException e )
        {
            handleGetException( resource, e );
        }
    }

    protected void cleanupGetTransfer( Resource resource )
    {
        if ( channel != null )
        {
            channel.disconnect();
        }
    }

    protected void getTransfer( Resource resource, OutputStream output, InputStream input, boolean closeInput,
                                int maxSize )
        throws TransferFailedException
    {
        super.getTransfer( resource, output, input, closeInput, (int) resource.getContentLength() );
    }

    protected String readLine( InputStream in )
        throws IOException
    {
        StringBuilder sb = new StringBuilder();

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

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        Resource resource = inputData.getResource();

        String path = getPath( getRepository().getBasedir(), resource.getName() );
        //String cmd = "scp -p -f " + path;
        String cmd = "scp -p -f \"" + path + "\"";

        fireTransferDebug( "Executing command: " + cmd );

        try
        {
            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( cmd );

            // get I/O streams for remote scp
            channelOutputStream = channel.getOutputStream();

            InputStream in = channel.getInputStream();
            inputData.setInputStream( in );

            channel.connect();

            sendEom( channelOutputStream );

            int exitCode = in.read();

            if ( exitCode == 'T' )
            {
                String line = readLine( in );

                String[] times = line.split( " " );

                resource.setLastModified( Long.valueOf( times[0] ).longValue() * 1000 );

                sendEom( channelOutputStream );

                exitCode = in.read();
            }

            String line = readLine( in );

            if ( exitCode != COPY_START_CHAR )
            {
                if ( exitCode == 1 && ( line.indexOf( "No such file or directory" ) != -1
                    || line.indexOf( "no such file or directory" ) != 1 ) )
                {
                    throw new ResourceDoesNotExistException( line );
                }
                else
                {
                    throw new IOException( "Exit code: " + exitCode + " - " + line );
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
                throw new IOException( "Invalid transfer header: " + line );
            }

            int index = line.indexOf( ACK_SEPARATOR, 5 );
            if ( index < 0 )
            {
                throw new IOException( "Invalid transfer header: " + line );
            }

            int filesize = Integer.valueOf( line.substring( 5, index ) ).intValue();
            fireTransferDebug( "Remote file size: " + filesize );

            resource.setContentLength( filesize );

            String filename = line.substring( index + 1 );
            fireTransferDebug( "Remote filename: " + filename );

            sendEom( channelOutputStream );
        }
        catch ( JSchException e )
        {
            handleGetException( resource, e );
        }
        catch ( IOException e )
        {
            handleGetException( resource, e );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        Resource resource = outputData.getResource();

        String basedir = getRepository().getBasedir();

        String path = getPath( basedir, resource.getName() );

        String dir = ScpHelper.getResourceDirectory( resource.getName() );

        try
        {
            sshTool.createRemoteDirectories( getPath( basedir, dir ), getRepository().getPermissions() );
        }
        catch ( CommandExecutionException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( e.getMessage(), e );
        }

        String octalMode = getOctalMode( getRepository().getPermissions() );

        // exec 'scp -p -t rfile' remotely
        String command = "scp";
        if ( octalMode != null )
        {
            command += " -p";
        }
        command += " -t \"" + path + "\"";

        fireTransferDebug( "Executing command: " + command );

        String resourceName = resource.getName();

        OutputStream out = null;
        try
        {
            channel = (ChannelExec) session.openChannel( EXEC_CHANNEL );

            channel.setCommand( command );

            // get I/O streams for remote scp
            out = channel.getOutputStream();
            outputData.setOutputStream( out );

            channelInputStream = channel.getInputStream();

            channel.connect();

            checkAck( channelInputStream );

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = resource.getContentLength();

            String mode = octalMode == null ? "0644" : octalMode;
            command = "C" + mode + " " + filesize + " ";

            if ( resourceName.lastIndexOf( ScpHelper.PATH_SEPARATOR ) > 0 )
            {
                command += resourceName.substring( resourceName.lastIndexOf( ScpHelper.PATH_SEPARATOR ) + 1 );
            }
            else
            {
                command += resourceName;
            }

            command += "\n";

            out.write( command.getBytes() );

            out.flush();

            checkAck( channelInputStream );
        }
        catch ( JSchException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            String msg = "Error occurred while deploying '" + resourceName + "' to remote repository: "
                + getRepository().getUrl() + ": " + e.getMessage();

            throw new TransferFailedException( msg, e );
        }
        catch ( IOException e )
        {
            handleIOException( resource, e );
        }
    }

    private void handleIOException( Resource resource, IOException e )
        throws TransferFailedException
    {
        if ( e.getMessage().indexOf( "set mode: Operation not permitted" ) >= 0 )
        {
            fireTransferDebug( e.getMessage() );
        }
        else
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            String msg = "Error occurred while deploying '" + resource.getName() + "' to remote repository: "
                + getRepository().getUrl() + ": " + e.getMessage();

            throw new TransferFailedException( msg, e );
        }
    }

    public String getOctalMode( RepositoryPermissions permissions )
    {
        String mode = null;
        if ( permissions != null && permissions.getFileMode() != null )
        {
            if ( permissions.getFileMode().matches( "[0-9]{3,4}" ) )
            {
                mode = permissions.getFileMode();

                if ( mode.length() == 3 )
                {
                    mode = "0" + mode;
                }
            }
            else
            {
                // TODO: calculate?
                // TODO: as warning
                fireSessionDebug( "Not using non-octal permissions: " + permissions.getFileMode() );
            }
        }
        return mode;
    }
}
