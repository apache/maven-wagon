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

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

/**
 * SCP deployer using "external" scp program.  To allow for
 * ssh-agent type behavior, until we can construct a Java SSH Agent and interface for JSch.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo [BP] add compression flag
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="scpexe"
 * instantiation-strategy="per-lookup"
 */
public class ScpExternalWagon
    extends AbstractWagon
    implements CommandExecutor
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

    private ScpHelper sshTool = new ScpHelper( this );

    private static final int SSH_FATAL_EXIT_CODE = 255;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void openConnectionInternal()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            authenticationInfo = new AuthenticationInfo();
        }
    }

    public void closeConnection()
    {
        // nothing to disconnect
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        fireSessionDebug( "getIfNewer in SCP wagon is not supported - performing an unconditional get" );
        get( resourceName, destination );
        return true;
    }

    /**
     * @return The hostname of the remote server prefixed with the username, which comes either from the repository URL
     *         or from the authenticationInfo.
     */
    private String buildRemoteHost()
    {
        String username = this.getRepository().getUsername();
        if ( username == null )
        {
            username = authenticationInfo.getUserName();
        }

        if ( username == null )
        {
            return getRepository().getHost();
        }
        else
        {
            return username + "@" + getRepository().getHost();
        }
    }

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        fireTransferDebug( "Executing command: " + command );

        executeCommand( command, false );
    }

    public Streams executeCommand( String command, boolean ignoreFailures )
        throws CommandExecutionException
    {
        boolean putty = isPuTTY();

        File privateKey;
        try
        {
            privateKey = ScpHelper.getPrivateKey( authenticationInfo );
        }
        catch ( FileNotFoundException e )
        {
            throw new CommandExecutionException( e.getMessage(), e );
        }
        Commandline cl = createBaseCommandLine( putty, sshExecutable, privateKey );

        int port =
            repository.getPort() == WagonConstants.UNKNOWN_PORT ? ScpHelper.DEFAULT_SSH_PORT : repository.getPort();
        if ( port != ScpHelper.DEFAULT_SSH_PORT )
        {
            if ( putty )
            {
                cl.createArg().setLine( "-P " + port );
            }
            else
            {
                cl.createArg().setLine( "-p " + port );
            }
        }

        if ( sshArgs != null )
        {
            cl.createArg().setLine( sshArgs );
        }

        String remoteHost = this.buildRemoteHost();

        cl.createArg().setValue( remoteHost );

        cl.createArg().setValue( command );

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

    protected boolean isPuTTY()
    {
        return sshExecutable.toLowerCase( Locale.ENGLISH ).indexOf( "plink" ) >= 0;
    }

    private Commandline createBaseCommandLine( boolean putty, String executable, File privateKey )
    {
        Commandline cl = new Commandline();

        cl.setExecutable( executable );

        if ( privateKey != null )
        {
            cl.createArg().setValue( "-i" );
            cl.createArg().setFile( privateKey );
        }

        String password = authenticationInfo.getPassword();
        if ( putty && password != null )
        {
            cl.createArg().setValue( "-pw" );
            cl.createArg().setValue( password );
        }

        // should check interactive flag, but scpexe never works in interactive mode right now due to i/o streams
        if ( putty )
        {
            cl.createArg().setValue( "-batch" );
        }
        else
        {
            cl.createArg().setValue( "-o" );
            cl.createArg().setValue( "BatchMode yes" );
        }
        return cl;
    }


    private void executeScpCommand( Resource resource, File localFile, boolean put )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        boolean putty = isPuTTYSCP();

        File privateKey;
        try
        {
            privateKey = ScpHelper.getPrivateKey( authenticationInfo );
        }
        catch ( FileNotFoundException e )
        {
            fireSessionConnectionRefused();

            throw new AuthorizationException( e.getMessage() );
        }
        Commandline cl = createBaseCommandLine( putty, scpExecutable, privateKey );

        cl.setWorkingDirectory( localFile.getParentFile().getAbsolutePath() );

        int port =
            repository.getPort() == WagonConstants.UNKNOWN_PORT ? ScpHelper.DEFAULT_SSH_PORT : repository.getPort();
        if ( port != ScpHelper.DEFAULT_SSH_PORT )
        {
            cl.createArg().setLine( "-P " + port );
        }

        if ( scpArgs != null )
        {
            cl.createArg().setLine( scpArgs );
        }

        String resourceName = normalizeResource( resource );
        String remoteFile = getRepository().getBasedir() + "/" + resourceName;

        remoteFile = StringUtils.replace( remoteFile, " ", "\\ " );

        String qualifiedRemoteFile = this.buildRemoteHost() + ":" + remoteFile;
        if ( put )
        {
            cl.createArg().setValue( localFile.getName() );
            cl.createArg().setValue( qualifiedRemoteFile );
        }
        else
        {
            cl.createArg().setValue( qualifiedRemoteFile );
            cl.createArg().setValue( localFile.getName() );
        }

        fireSessionDebug( "Executing command: " + cl.toString() );

        try
        {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( cl, null, err );
            if ( exitCode != 0 )
            {
                if ( !put && err.getOutput().trim().toLowerCase( Locale.ENGLISH ).indexOf( "no such file or directory" )
                    != -1 )
                {
                    throw new ResourceDoesNotExistException( err.getOutput() );
                }
                else
                {
                    TransferFailedException e =
                        new TransferFailedException( "Exit code: " + exitCode + " - " + err.getOutput() );

                    fireTransferError( resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET );

                    throw e;
                }
            }
        }
        catch ( CommandLineException e )
        {
            fireTransferError( resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET );

            throw new TransferFailedException( "Error executing command line", e );
        }
    }

    boolean isPuTTYSCP()
    {
        return scpExecutable.toLowerCase( Locale.ENGLISH ).indexOf( "pscp" ) >= 0;
    }

    private String normalizeResource( Resource resource )
    {
        return StringUtils.replace( resource.getName(), "\\", "/" );
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );

        firePutInitiated( resource, source );

        if ( !source.exists() )
        {
            throw new ResourceDoesNotExistException( "Specified source file does not exist: " + source );
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
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error executing command for transfer", e );
        }

        resource.setContentLength( source.length() );

        resource.setLastModified( source.lastModified() );

        firePutStarted( resource, source );

        executeScpCommand( resource, source, true );

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        try
        {
            RepositoryPermissions permissions = getRepository().getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n",
                                true );
            }

            if ( permissions != null && permissions.getFileMode() != null )
            {
                executeCommand( "chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n",
                                true );
            }
        }
        catch ( CommandExecutionException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error executing command for transfer", e );
        }
        firePutCompleted( resource, source );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String path = StringUtils.replace( resourceName, "\\", "/" );

        Resource resource = new Resource( path );

        fireGetInitiated( resource, destination );

        createParentDirectories( destination );

        fireGetStarted( resource, destination );

        executeScpCommand( resource, destination, false );

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }

    //
    // these parameters are user specific, so should not be read from the repository itself.
    // They can be configured by plexus, or directly on the instantiated object.
    // Alternatively, we may later accept a generic parameters argument to connect, or some other configure(Properties)
    // method on a Wagon.
    //

    public List<String> getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return sshTool.getFileList( destinationDirectory, repository );
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        sshTool.putDirectory( this, sourceDirectory, destinationDirectory );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        return sshTool.resourceExists( resourceName, repository );
    }

    public boolean supportsDirectoryCopy()
    {
        return true;
    }

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
}
