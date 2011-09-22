package org.apache.maven.wagon.providers.ssh;

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
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ScpHelper
{
    public static final char PATH_SEPARATOR = '/';

    public static final int DEFAULT_SSH_PORT = 22;

    private final CommandExecutor executor;

    public ScpHelper( CommandExecutor executor )
    {
        this.executor = executor;
    }

    public static String getResourceDirectory( String resourceName )
    {
        String dir = PathUtils.dirname( resourceName );
        dir = StringUtils.replace( dir, "\\", "/" );
        return dir;
    }

    public static String getResourceFilename( String r )
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

    public static Resource getResource( String resourceName )
    {
        String r = StringUtils.replace( resourceName, "\\", "/" );
        return new Resource( r );
    }

    public static File getPrivateKey( AuthenticationInfo authenticationInfo )
        throws FileNotFoundException
    {
        // If user don't define a password, he want to use a private key
        File privateKey = null;
        if ( authenticationInfo.getPassword() == null )
        {

            if ( authenticationInfo.getPrivateKey() != null )
            {
                privateKey = new File( authenticationInfo.getPrivateKey() );
                if ( !privateKey.exists() )
                {
                    throw new FileNotFoundException( "Private key '" + privateKey + "' not found" );
                }
            }
            else
            {
                privateKey = findPrivateKey();
            }

            if ( privateKey != null && privateKey.exists() )
            {
                if ( authenticationInfo.getPassphrase() == null )
                {
                    authenticationInfo.setPassphrase( "" );
                }
            }
        }
        return privateKey;
    }

    private static File findPrivateKey()
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
            if ( !privateKey.exists() )
            {
                privateKey = null;
            }
        }

        return privateKey;
    }

    public static void createZip( List<String> files, File zipName, File basedir )
        throws IOException
    {
        ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipName ) );

        try
        {
            for ( String file : files )
            {
                file = file.replace( '\\', '/' );

                writeZipEntry( zos, new File( basedir, file ), file );
            }
        }
        finally
        {
            IOUtil.close( zos );
        }
    }

    private static void writeZipEntry( ZipOutputStream jar, File source, String entryName )
        throws IOException
    {
        byte[] buffer = new byte[1024];

        int bytesRead;

        FileInputStream is = new FileInputStream( source );

        try
        {
            ZipEntry entry = new ZipEntry( entryName );

            jar.putNextEntry( entry );

            while ( ( bytesRead = is.read( buffer ) ) != -1 )
            {
                jar.write( buffer, 0, bytesRead );
            }
        }
        finally
        {
            is.close();
        }
    }

    protected static String getPath( String basedir, String dir )
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

    public void putDirectory( Wagon wagon, File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Repository repository = wagon.getRepository();

        String basedir = repository.getBasedir();

        String destDir = StringUtils.replace( destinationDirectory, "\\", "/" );

        String path = getPath( basedir, destDir );
        try
        {
            if ( repository.getPermissions() != null )
            {
                String dirPerms = repository.getPermissions().getDirectoryMode();

                if ( dirPerms != null )
                {
                    String umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
                    executor.executeCommand( umaskCmd );
                }
            }

            //String mkdirCmd = "mkdir -p " + path;
            String mkdirCmd = "mkdir -p \"" + path + "\"";

            executor.executeCommand( mkdirCmd );
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

            List<String> files = FileUtils.getFileNames( sourceDirectory, "**/**", "", false );

            createZip( files, zipFile, sourceDirectory );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unable to create ZIP archive of directory", e );
        }

        wagon.put( zipFile, getPath( destDir, zipFile.getName() ) );

        try
        {
            //executor.executeCommand(
            //    "cd " + path + "; unzip -q -o " + zipFile.getName() + "; rm -f " + zipFile.getName() );
            executor.executeCommand( "cd \"" + path + "\"; unzip -q -o \"" + zipFile.getName() + "\"; rm -f \"" + zipFile.getName() + "\"" );

            zipFile.delete();

            RepositoryPermissions permissions = repository.getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                //executor.executeCommand( "chgrp -Rf " + permissions.getGroup() + " " + path );
                executor.executeCommand( "chgrp -Rf " + permissions.getGroup() + " \"" + path + "\"" );
            }

            if ( permissions != null && permissions.getFileMode() != null )
            {
                //executor.executeCommand( "chmod -Rf " + permissions.getFileMode() + " " + path );
                executor.executeCommand( "chmod -Rf " + permissions.getFileMode() + " \"" + path + "\"" );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }

    public List<String> getFileList( String destinationDirectory, Repository repository )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        try
        {
            String path = getPath( repository.getBasedir(), destinationDirectory );
            //Streams streams = executor.executeCommand( "ls -FlA " + path, false );
            Streams streams = executor.executeCommand( "ls -FlA \"" + path + "\"", false );

            List<String> ret = new LSParser().parseFiles( streams.getOut() );
            if ( ret == null || ret.isEmpty() )
            {
                throw new ResourceDoesNotExistException( "No such file or directory" );
            }
            return ret;
        }
        catch ( CommandExecutionException e )
        {
            if ( e.getMessage().trim().endsWith( "No such file or directory" ) )
            {
                throw new ResourceDoesNotExistException( e.getMessage().trim(), e );
            }
            else if ( e.getMessage().trim().endsWith( "Not a directory" ) )
            {
                throw new ResourceDoesNotExistException( e.getMessage().trim(), e );
            }
            else
            {
                throw new TransferFailedException( "Error performing file listing.", e );
            }
        }
    }

    public boolean resourceExists( String resourceName, Repository repository )
        throws TransferFailedException, AuthorizationException
    {
        try
        {
            String path = getPath( repository.getBasedir(), resourceName );
            //executor.executeCommand( "ls " + path, false );
            executor.executeCommand( "ls \"" + path + "\"" );

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

    public void createRemoteDirectories( String path, RepositoryPermissions permissions )
        throws CommandExecutionException
    {
        String umaskCmd = null;
        if ( permissions != null )
        {
            String dirPerms = permissions.getDirectoryMode();

            if ( dirPerms != null )
            {
                umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
            }
        }

        //String mkdirCmd = "mkdir -p " + path;
        String mkdirCmd = "mkdir -p \"" + path + "\"";

        if ( umaskCmd != null )
        {
            mkdirCmd = umaskCmd + "; " + mkdirCmd;
        }

        executor.executeCommand( mkdirCmd );
    }
}
