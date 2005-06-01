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
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
    implements SshCommandExecutor
{
    public static int DEFAULT_SSH_PORT = 22;

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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void openConnection()
        throws AuthenticationException
    {
        if ( authenticationInfo == null )
        {
            throw new IllegalArgumentException( "Authentication Credentials cannot be null for SSH protocol" );
        }

        int port = getRepository().getPort();

        if ( port == WagonConstants.UNKNOWN_PORT )
        {
            port = DEFAULT_SSH_PORT;
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

                // TODO: do something with it
            }
            else
            {
                String msg = "Private key was not found. You must define a private key or a password for repo: " +
                    getRepository().getName();

                throw new AuthenticationException( msg );
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
        throws TransferFailedException
    {
        Commandline cl = new Commandline();

        cl.setExecutable( sshExecutable );

        if ( sshArgs != null )
        {
            cl.createArgument().setLine( sshArgs );
        }
        String remoteHost = authenticationInfo.getUserName() + "@" + getRepository().getHost();

        cl.createArgument().setValue( remoteHost );

        cl.createArgument().setValue( command );

        try
        {
            CommandLineUtils.executeCommandLine( cl, null, null );
        }
        catch ( CommandLineException e )
        {
            throw new TransferFailedException( "Error executing command line", e );
        }
    }


    private void executeScpCommand( File localFile, String remoteFile, boolean put )
        throws TransferFailedException
    {
        Commandline cl = new Commandline();

        cl.setWorkingDirectory( localFile.getParentFile().getAbsolutePath() );

        cl.setExecutable( scpExecutable );

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
            CommandLineUtils.executeCommandLine( cl, null, null );
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

        executeCommand( mkdirCmd );

        firePutStarted( resource, source );

        executeScpCommand( source, basedir + "/" + resourceName, true );

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        RepositoryPermissions permissions = getRepository().getPermissions();

        if ( permissions != null && permissions.getGroup() != null )
        {
            executeCommand( "chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n" );
        }

        if ( permissions != null && permissions.getFileMode() != null )
        {
            executeCommand( "chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n" );
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

}
