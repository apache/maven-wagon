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

            int port = getRepository().getPort();

            if ( port == WagonConstants.UNKNOWN_PORT )
            {
                port = DEFAULT_SSH_PORT;
            }

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
        catch ( Exception e )
        {
            fireSessionError( e );

            throw new AuthenticationException( "Cannot connect. Reason: " + e.getMessage(), e );
        }
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
        AuthenticationInfo authenticationInfo = getRepository().getAuthenticationInfo();

        String args = /* TODO: getRepository().getSshArgs(); */ null;
        String executable = /* TODO: getRepository().getSshExe(); */ "ssh";

        Commandline cl = new Commandline();
        cl.setExecutable( executable );
        if ( args != null )
        {
            cl.createArgument().setLine( args );
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
        AuthenticationInfo authenticationInfo = getRepository().getAuthenticationInfo();

        String args = /* TODO: getRepository().getScpArgs(); */ null;
        String executable = /* TODO: getRepository().getScpExe(); */ "scp";

        Commandline cl = new Commandline();
        cl.setWorkingDirectory( localFile.getParentFile().getAbsolutePath() );
        cl.setExecutable( executable );
        if ( args != null )
        {
            cl.createArgument().setLine( args );
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

        firePutStarted( resource, source );

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

        executeScpCommand( source, basedir + "/" + resourceName, true );

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        RepositoryPermissions permissions = getRepository().getPermissions();

        if ( permissions != null && permissions.getGroup() != null )
        {
            executeCommand( "chgrp " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n" );
        }

        if ( permissions != null && permissions.getFileMode() != null )
        {
            executeCommand( "chmod " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n" );
        }
        firePutCompleted( resource, source );
    }

    private void postProcessListeners( Resource resource, File source, int requestType )
        throws TransferFailedException
    {
        byte[] buffer = new byte[ DEFAULT_BUFFER_SIZE ];

        TransferEvent transferEvent = new TransferEvent( this, resource , TransferEvent.TRANSFER_PROGRESS, requestType );

        try
        {
            InputStream input = new FileInputStream( source );

            while ( true )
            {
                int n = input.read( buffer ) ;

                if ( n == -1 )
                {
                   break;
                }

                fireTransferProgress( transferEvent, buffer, n );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Failed to post-process the source file", e );
        }
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
}
