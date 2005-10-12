package org.apache.maven.wagon.providers.sshext;

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

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
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
 * @version $Id$
 * @todo [BP] add compression flag
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
    private String scpArgs = null;


    /**
     * Arguments to pass to the SSH command.
     *
     * @component.configuration
     */
    private String sshArgs = null;

    private int port;

    private File privateKey;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void openConnection()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            authenticationInfo = new AuthenticationInfo();
        }

        if ( authenticationInfo.getUserName() == null )
        {
            authenticationInfo.setUserName( System.getProperty( "user.name" ) );
        }

        port = getRepository().getPort();

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

                this.privateKey = privateKey;
            }
        }
        // nothing to connect to
    }

    // ----------------------------------------------------------------------
    // TODO: share with scp wagon
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
        // nothing to disconnect
    }

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        Commandline cl = new Commandline();

        cl.setExecutable( sshExecutable );

        if ( privateKey != null )
        {
            cl.createArgument().setValue( "-i" );
            cl.createArgument().setFile( privateKey );
        }

        if ( port != WagonConstants.UNKNOWN_PORT )
        {
            if ( sshExecutable.indexOf( "plink" ) >= 0 )
            {
                cl.createArgument().setLine( "-P " + port );
            }
            else
            {
                cl.createArgument().setLine( "-p " + port );
            }
        }

        // should check interactive flag, but scpexe never works in interactive mode right now due to i/o streams
        if ( sshExecutable.indexOf( "plink" ) >= 0 )
        {
            cl.createArgument().setValue( "-batch" );
        }
        else
        {
            cl.createArgument().setValue( "-o" );
            cl.createArgument().setValue( "BatchMode yes" );
        }

        if ( sshArgs != null )
        {
            cl.createArgument().setLine( sshArgs );
        }
        String remoteHost = authenticationInfo.getUserName() + "@" + getRepository().getHost();

        cl.createArgument().setValue( remoteHost );

        cl.createArgument().setValue( command );

        try
        {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( cl, null, err );
            if ( exitCode != 0 )
            {
                throw new CommandExecutionException( "Exit code " + exitCode + " - " + err.getOutput() );
            }
        }
        catch ( CommandLineException e )
        {
            throw new CommandExecutionException( "Error executing command line", e );
        }
    }


    private void executeScpCommand( File localFile, String remoteFile, boolean put )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        Commandline cl = new Commandline();

        cl.setWorkingDirectory( localFile.getParentFile().getAbsolutePath() );

        cl.setExecutable( scpExecutable );

        if ( privateKey != null )
        {
            cl.createArgument().setValue( "-i" );
            cl.createArgument().setFile( privateKey );
        }

        if ( port != WagonConstants.UNKNOWN_PORT )
        {
            cl.createArgument().setLine( "-P " + port );
        }

        // should check interactive flag, but scpexe never works in interactive mode right now due to i/o streams
        if ( sshExecutable.indexOf( "pscp" ) >= 0 )
        {
            cl.createArgument().setValue( "-batch" );
        }
        else
        {
            cl.createArgument().setValue( "-o" );
            cl.createArgument().setValue( "BatchMode yes" );
        }

        if ( scpArgs != null )
        {
            cl.createArgument().setLine( scpArgs );
        }
        String qualifiedRemoteFile = authenticationInfo.getUserName() + "@" + getRepository().getHost() + ":" +
            remoteFile;
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
        try
        {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine( cl, null, err );
            if ( exitCode != 0 )
            {
                if ( !put && err.getOutput().trim().endsWith( "No such file or directory" ) )
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

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        if ( !source.exists() )
        {
            throw new TransferFailedException( "Specified source file does not exist: " + source );
        }

        String basedir = getRepository().getBasedir();

        resourceName = StringUtils.replace( resourceName, "\\", "/" );

        String dir = PathUtils.dirname( resourceName );

        dir = StringUtils.replace( dir, "\\", "/" );

        String mkdirCmd = "mkdir -p " + basedir + "/" + dir + "\n";

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
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n" );
            }

            if ( permissions != null && permissions.getFileMode() != null )
            {
                executeCommand( "chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n" );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error executing command for transfer", e );
        }
        firePutCompleted( resource, source );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        resourceName = StringUtils.replace( resourceName, "\\", "/" );

        createParentDirectories( destination );

        Resource resource = new Resource( resourceName );

        fireGetStarted( resource, destination );

        executeScpCommand( destination, basedir + "/" + resourceName, false );

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
    {
        throw new UnsupportedOperationException( "getIfNewer is scp wagon must be still implemented" );
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

        destinationDirectory = StringUtils.replace( destinationDirectory, "\\", "/" );

        String path = getPath( basedir, destinationDirectory );
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

        put( zipFile, getPath( destinationDirectory, zipFile.getName() ) );

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

    public boolean supportsDirectoryCopy()
    {
        return true;
    }
}
