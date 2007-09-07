package org.apache.maven.wagon.providers.ssh.external;

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

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.AbstractSshWagon;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * SCP deployer using "external" scp program.  To allow for
 * ssh-agent type behavior, until we can construct a Java SSH Agent and interface for JSch.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:ScpExternalWagon.java 477260 2006-11-20 17:11:39Z brett $
 * @todo [BP] add compression flag
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="scpexe"
 *   instantiation-strategy="per-lookup"
 */
public class ScpExternalWagon
    extends AbstractSshWagon
{
    /**
     * The external SCP command to use - default is <code>scp</code>.
     *
     * @component.configuration default="scp"
     */
    private String scpExecutable = "scp";

    /**
     * The external SSH command to use - default is <code>ssh</code>.
     *
     * @component.configuration default="ssh"
     */
    private String sshExecutable = "ssh";

    /**
     * Arguments to pass to the SCP command.
     *
     * @component.configuration
     */
    private String scpArgs;

    /**
     * Arguments to pass to the SSH command.
     *
     * @component.configuration
     */
    private String sshArgs;

    private static final int SSH_FATAL_EXIT_CODE = 255;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------
    
    public String getProtocol()
    {
        return "scpexe";
    }

    public void openConnection()
        throws AuthenticationException
    {
        super.openConnection();

        // nothing to connect to
    }

    public void closeConnection()
    {
        // nothing to disconnect
    }

    public Streams executeCommand( String command, boolean ignoreFailures )
        throws CommandExecutionException
    {
        boolean putty = sshExecutable.indexOf( "plink" ) >= 0;

        Commandline cl = createBaseCommandLine( putty, sshExecutable );

        int port = getPort();
        if ( port != DEFAULT_SSH_PORT )
        {
            if ( putty )
            {
                cl.createArgument().setLine( "-P " + port );
            }
            else
            {
                cl.createArgument().setLine( "-p " + port );
            }
        }

        if ( sshArgs != null )
        {
            cl.createArgument().setLine( sshArgs );
        }
        String remoteHost = authenticationInfo.getUserName() + "@" + getRepository().getHost();

        cl.createArgument().setValue( remoteHost );

        cl.createArgument().setValue( command );

        fireSessionDebug( "Executing command: " + cl.toString() );

        try
        {
            CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( cl, out, err );
            Streams streams = new Streams();
            streams.setOut( out.getOutput() );
            streams.setErr( err.getOutput() );
            fireSessionDebug( streams.getOut() );
            fireSessionDebug( streams.getErr() );
            if ( exitCode != 0 )
            {
                if ( !ignoreFailures || exitCode == SSH_FATAL_EXIT_CODE )
                {
                    throw new CommandExecutionException( "Exit code " + exitCode + " - " + err.getOutput() );
                }
            }
            return streams;
        }
        catch ( CommandLineException e )
        {
            throw new CommandExecutionException( "Error executing command line", e );
        }
    }

    private Commandline createBaseCommandLine( boolean putty, String executable )
    {
        Commandline cl = new Commandline();

        cl.setExecutable( executable );

        File privateKey = getPrivateKey();
        if ( privateKey != null )
        {
            cl.createArgument().setValue( "-i" );
            cl.createArgument().setFile( privateKey );
        }

        String password = authenticationInfo.getPassword();
        if ( putty && password != null )
        {
            cl.createArgument().setValue( "-pw" );
            cl.createArgument().setValue( password );
        }

        // should check interactive flag, but scpexe never works in interactive mode right now due to i/o streams
        if ( putty )
        {
            cl.createArgument().setValue( "-batch" );
        }
        else
        {
            cl.createArgument().setValue( "-o" );
            cl.createArgument().setValue( "BatchMode yes" );
        }
        return cl;
    }


    private void executeScpCommand( File localFile, String remoteFile, boolean put )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        boolean putty = scpExecutable.indexOf( "pscp" ) >= 0;

        Commandline cl = createBaseCommandLine( putty, scpExecutable );

        cl.setWorkingDirectory( localFile.getParentFile().getAbsolutePath() );

        int port = getPort();
        if ( port != DEFAULT_SSH_PORT )
        {
            cl.createArgument().setLine( "-P " + port );
        }

        if ( scpArgs != null )
        {
            cl.createArgument().setLine( scpArgs );
        }
        String qualifiedRemoteFile =
            authenticationInfo.getUserName() + "@" + getRepository().getHost() + ":" + remoteFile;
        if ( put )
        {
            cl.createArgument().setValue( localFile.getName() );
            cl.createArgument().setValue( qualifiedRemoteFile );
        }
        else
        {
            cl.createArgument().setValue( qualifiedRemoteFile );
            cl.createArgument().setValue( localFile.getName() );
        }

        fireSessionDebug( "Executing command: " + cl.toString() );

        try
        {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( cl, null, err );
            if ( exitCode != 0 )
            {
                if ( !put && err.getOutput().trim().toLowerCase().endsWith( "no such file or directory" ) )
                {
                    throw new ResourceDoesNotExistException( err.getOutput() );
                }
                else
                {
                    throw new TransferFailedException( "Exit code: " + exitCode + " - " + err.getOutput() );
                }
            }
        }
        catch ( CommandLineException e )
        {
            throw new TransferFailedException( "Error executing command line", e );
        }
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );

        firePutInitiated( resource, source );

        if ( !source.exists() )
        {
            throw new TransferFailedException( "Specified source file does not exist: " + source );
        }

        String basedir = getRepository().getBasedir();

        String resourceName = StringUtils.replace( destination, "\\", "/" );

        String dir = PathUtils.dirname( resourceName );

        dir = StringUtils.replace( dir, "\\", "/" );

        String umaskCmd = null;
        if ( getRepository().getPermissions() != null )
        {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if ( dirPerms != null )
            {
                umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
            }
        }

        String mkdirCmd = "mkdir -p " + basedir + "/" + dir + "\n";

        if ( umaskCmd != null )
        {
            mkdirCmd = umaskCmd + "; " + mkdirCmd;
        }

        try
        {
            executeCommand( mkdirCmd );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error executing command for transfer", e );
        }

        firePutStarted( resource, source );

        executeScpCommand( source, basedir + "/" + resourceName, true );

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        try
        {
            RepositoryPermissions permissions = getRepository().getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n",
                                true );
            }

            String fileMode = "644";
            if ( permissions != null && permissions.getFileMode() != null )
            {
                fileMode = permissions.getFileMode();
            }
            executeCommand( "chmod -f " + fileMode + " " + basedir + "/" + resourceName + "\n", true );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error executing command for transfer", e );
        }
        firePutCompleted( resource, source );
    }

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        executeCommand( command, false );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        String path = StringUtils.replace( resourceName, "\\", "/" );

        createParentDirectories( destination );

        Resource resource = new Resource( path );

        fireGetStarted( resource, destination );

        executeScpCommand( destination, basedir + "/" + path, false );

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }

    //
    // these parameters are user specific, so should not be read from the repository itself.
    // They can be configured by plexus, or directly on the instantiated object.
    // Alternatively, we may later accept a generic parameters argument to connect, or some other configure(Properties)
    // method on a Wagon.
    //

    public String getScpExecutable()
    {
        return scpExecutable;
    }

    public void setScpExecutable( String scpExecutable )
    {
        this.scpExecutable = scpExecutable;
    }

    public String getSshExecutable()
    {
        return sshExecutable;
    }

    public void setSshExecutable( String sshExecutable )
    {
        this.sshExecutable = sshExecutable;
    }

    public String getScpArgs()
    {
        return scpArgs;
    }

    public void setScpArgs( String scpArgs )
    {
        this.scpArgs = scpArgs;
    }

    public String getSshArgs()
    {
        return sshArgs;
    }

    public void setSshArgs( String sshArgs )
    {
        this.sshArgs = sshArgs;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        String dir = StringUtils.replace( destinationDirectory, "\\", "/" );

        String path = getPath( basedir, dir );
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

            String mkdirCmd = "mkdir -p " + path;

            executeCommand( mkdirCmd );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }

        File zipFile;
        try
        {
            zipFile = File.createTempFile( "wagon", ".zip" );
            zipFile.deleteOnExit();

            List files = FileUtils.getFileNames( sourceDirectory, "**/**", "", false );

            createZip( files, zipFile, sourceDirectory );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unable to create ZIP archive of directory", e );
        }

        put( zipFile, getPath( dir, zipFile.getName() ) );

        try
        {
            executeCommand( "cd " + path + "; unzip -o " + zipFile.getName() + "; rm -f " + zipFile.getName() );

            zipFile.delete();

            RepositoryPermissions permissions = getRepository().getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -Rf " + permissions.getGroup() + " " + path );
            }

            if ( permissions != null && permissions.getFileMode() != null )
            {
                executeCommand( "chmod -Rf " + permissions.getFileMode() + " " + path );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }
}
