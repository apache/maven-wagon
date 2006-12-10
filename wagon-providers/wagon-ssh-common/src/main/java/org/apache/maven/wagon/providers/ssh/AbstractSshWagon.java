package org.apache.maven.wagon.providers.ssh;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo;
import org.apache.maven.wagon.providers.ssh.interactive.NullInteractiveUserInfo;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Common SSH operations.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo cache pass[words|phases]
 * @todo move permissions tools to repositorypermissionsutils
 */
public abstract class AbstractSshWagon
    extends AbstractWagon
    implements CommandExecutor, SshWagon
{
    protected KnownHostsProvider knownHostsProvider;

    protected InteractiveUserInfo interactiveUserInfo;

    protected static final char PATH_SEPARATOR = '/';

    protected static final int DEFAULT_SSH_PORT = 22;

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        fireSessionDebug( "getIfNewer in SCP wagon is not supported - performing an unconditional get" );
        get( resourceName, destination );
        return true;
    }

    protected String getOctalMode( RepositoryPermissions permissions )
    {
        String mode = "0644";
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
                fireSessionDebug( "Not using non-octal permissions: " + mode );
            }
        }
        return mode;
    }

    /**
     * @param permissions repository's permissions
     * @return the directory mode for the repository or <code>-1</code> if it
     *         wasn't set
     */
    protected int getDirectoryMode( RepositoryPermissions permissions )
    {
        int ret = -1;

        if ( permissions != null )
        {
            ret = getOctalMode( permissions.getDirectoryMode() );
        }

        return ret;
    }

    protected int getOctalMode( String mode )
    {
        int ret;
        try
        {
            ret = Integer.valueOf( mode, 8 ).intValue();
        }
        catch ( NumberFormatException e )
        {
            // TODO: warning level
            fireTransferDebug( "the file mode must be a numerical mode for SFTP" );
            ret = -1;
        }
        return ret;
    }

    protected static String getResourceDirectory( String resourceName )
    {
        String dir = PathUtils.dirname( resourceName );
        dir = StringUtils.replace( dir, "\\", "/" );
        return dir;
    }

    protected static String getResourceFilename( String r )
    {
        String filename;
        if ( r.lastIndexOf( PATH_SEPARATOR ) > 0 )
        {
            filename = r.substring( r.lastIndexOf( PATH_SEPARATOR ) + 1 );
        }
        else
        {
            filename = r;
        }
        return filename;
    }

    protected static Resource getResource( String resourceName )
    {
        String r = StringUtils.replace( resourceName, "\\", "/" );
        return new Resource( r );
    }

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

        if ( !interactive )
        {
            interactiveUserInfo = new NullInteractiveUserInfo();
        }
    }

    protected File getPrivateKey()
    {
        // If user don't define a password, he want to use a private key
        File privateKey = null;
        if ( authenticationInfo.getPassword() == null )
        {

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
            }
        }
        return privateKey;
    }

    protected int getPort()
    {
        int port = getRepository().getPort();

        if ( port == WagonConstants.UNKNOWN_PORT )
        {
            port = DEFAULT_SSH_PORT;
        }
        return port;
    }

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

    public void executeCommand( String command )
        throws CommandExecutionException
    {
        fireTransferDebug( "Executing command: " + command );

        executeCommand( command, false );
    }

    public final KnownHostsProvider getKnownHostsProvider()
    {
        return knownHostsProvider;
    }

    public final void setKnownHostsProvider( KnownHostsProvider knownHostsProvider )
    {
        if ( knownHostsProvider == null )
        {
            throw new IllegalArgumentException( "knownHostsProvider can't be null" );
        }
        this.knownHostsProvider = knownHostsProvider;
    }

    public InteractiveUserInfo getInteractiveUserInfo()
    {
        return interactiveUserInfo;
    }

    public void setInteractiveUserInfo( InteractiveUserInfo interactiveUserInfo )
    {
        if ( interactiveUserInfo == null )
        {
            throw new IllegalArgumentException( "interactiveUserInfo can't be null" );
        }
        this.interactiveUserInfo = interactiveUserInfo;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        String destDir = StringUtils.replace( destinationDirectory, "\\", "/" );

        String path = getPath( basedir, destDir );
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

        put( zipFile, getPath( destDir, zipFile.getName() ) );

        try
        {
            executeCommand( "cd " + path + "; unzip -q -o " + zipFile.getName() + "; rm -f " + zipFile.getName() );

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

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        try
        {
            String path = getPath( getRepository().getBasedir(), destinationDirectory );
            Streams streams = executeCommand( "ls -la " + path, false );

            BufferedReader br = new BufferedReader( new StringReader( streams.getOut() ) );

            List ret = new ArrayList();
            String line = br.readLine();

            while ( line != null )
            {
                String[] parts = StringUtils.split( line, " " );
                /* This should split out the 'ls' command output.
                 * Example: "-rw-r--r-- 1 joakim wheel    18 2006-12-10 10:00 test-resource.pom"
                 * 
                 * 0 : The permissions mask : "-rw-r--r--"
                 * 1 : Directory Complexity : "1"
                 * 2 : Owner                : "joakim"
                 * 3 : Group                : "wheel"
                 * 4 : Size                 : "18"
                 * 5 : Date                 : "2006-12-10"
                 * 6 : Time                 : "10:00"
                 * 7 : File Name            : "test-resource.pom"
                 */
                if ( parts.length >= 7 )
                {
                    // This is the filename portion of the 'ls' command output.
                    ret.add( parts[7] );
                }

                line = br.readLine();
            }

            return ret;
        }
        catch ( CommandExecutionException e )
        {
            if ( e.getMessage().trim().endsWith( "No such file or directory" ) )
            {
                throw new ResourceDoesNotExistException( e.getMessage().trim() );
            }
            else
            {
                throw new TransferFailedException( "Error performing file listing.", e );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error parsing file listing.", e );
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        try
        {
            String path = getPath( getRepository().getBasedir(), resourceName );
            executeCommand( "ls " + path );

            // Parsing of output not really needed.  As a failed ls results in a
            // CommandExectionException on the 'ls' command.

            return true;
        }
        catch ( CommandExecutionException e )
        {
            // Error?  Then the 'ls' command failed.  No such file found.
            return false;
        }
    }

    protected void handleGetException( Resource resource, Exception e, File destination )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        fireTransferError( resource, e, TransferEvent.REQUEST_GET );

        if ( destination.exists() )
        {
            boolean deleted = destination.delete();

            if ( !deleted )
            {
                destination.deleteOnExit();
            }
        }

        String msg = "Error occured while downloading '" + resource + "' from the remote repository:" + getRepository();

        throw new TransferFailedException( msg, e );
    }
}
