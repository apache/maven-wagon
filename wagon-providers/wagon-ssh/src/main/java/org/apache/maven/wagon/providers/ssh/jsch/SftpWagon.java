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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * SFTP protocol wagon.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo [BP] add compression flag
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="sftp"
 *   instantiation-strategy="per-lookup"
 */
public class SftpWagon
    extends AbstractJschWagon
{
    private static final String SFTP_CHANNEL = "sftp";

    private static final int S_IFDIR = 0x4000;

    private static final long MILLIS_PER_SEC = 1000L;

    public void put( String basedir, Resource resource, File source )
        throws TransferFailedException
    {
        String resourceName = resource.getName();
        String dir = getResourceDirectory( resourceName );

        ChannelSftp channel = null;

        try
        {
            channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );

            channel.connect();

            RepositoryPermissions permissions = getRepository().getPermissions();

            int directoryMode = getDirectoryMode( permissions );

            channel.cd( "/" );
            
            mkdirs( channel, basedir + "/", directoryMode );
            
            mkdirs( channel, resourceName, directoryMode );

            putFile( channel, source, resource, permissions );

            String[] dirs = PathUtils.dirnames( dir );
            for ( int i = 0; i < dirs.length; i++ )
            {
                channel.cd( ".." );
            }
        }
        catch ( SftpException e )
        {
            String msg = "Error occured while deploying '" + resourceName + "' " + "to remote repository: " +
                getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        catch ( JSchException e )
        {
            String msg = "Error occured while deploying '" + resourceName + "' " + "to remote repository: " +
                getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    private void putFile( ChannelSftp channel, File source, Resource resource, RepositoryPermissions permissions )
        throws SftpException, TransferFailedException
    {
        resource.setContentLength( source.length() );
        
        resource.setLastModified( source.lastModified() );
        
        String filename = getResourceFilename( resource.getName() );

        firePutStarted( resource, source );

        channel.put( source.getAbsolutePath(), filename );

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        if ( permissions != null && permissions.getGroup() != null )
        {
            setGroup( channel, filename, permissions );
        }

        if ( permissions != null && permissions.getFileMode() != null )
        {
            setFileMode( channel, filename, permissions );
        }

        firePutCompleted( resource, source );
    }

    private void setGroup( ChannelSftp channel, String filename, RepositoryPermissions permissions )
        throws SftpException
    {
        try
        {
            int group = Integer.valueOf( permissions.getGroup() ).intValue();
            channel.chgrp( group, filename );
        }
        catch ( NumberFormatException e )
        {
            // TODO: warning level
            fireTransferDebug( "Not setting group: must be a numerical GID for SFTP" );
        }
    }

    private void setFileMode( ChannelSftp channel, String filename, RepositoryPermissions permissions )
        throws SftpException
    {
        try
        {
            int mode = getOctalMode( permissions.getFileMode() );
            channel.chmod( mode, filename );
        }
        catch ( NumberFormatException e )
        {
            // TODO: warning level
            fireTransferDebug( "Not setting mode: must be a numerical mode for SFTP" );
        }
    }

    private void mkdirs( ChannelSftp channel, String resourceName, int mode )
        throws TransferFailedException, SftpException
    {
        String[] dirs = PathUtils.dirnames( resourceName );
        for ( int i = 0; i < dirs.length; i++ )
        {
            mkdir( channel, dirs[i], mode );

            channel.cd( dirs[i] );
        }
    }

    private void mkdir( ChannelSftp channel, String dir, int mode )
        throws TransferFailedException, SftpException
    {
        try
        {
            SftpATTRS attrs = channel.stat( dir );
            if ( ( attrs.getPermissions() & S_IFDIR ) == 0 )
            {
                throw new TransferFailedException( "Remote path is not a directory:" + dir );
            }
        }
        catch ( SftpException e )
        {
            // doesn't exist, make it and try again
            channel.mkdir( dir );
            if ( mode != -1 )
            {
                try
                {
                    channel.chmod( mode, dir );
                }
                catch ( SftpException e1 )
                {
                    // for some extrange reason we recive this exception,
                    // even when chmod success
                }
            }
        }
    }

    public boolean getIfNewer( Resource resource, File destination, long timestamp )
        throws ResourceDoesNotExistException, TransferFailedException
    {
        String filename = getResourceFilename( resource.getName() );

        String dir = getResourceDirectory( resource.getName() );

        // we already setuped the root directory. Ignore beginning /
        if ( dir.length() > 0 && dir.charAt( 0 ) == PATH_SEPARATOR )
        {
            dir = dir.substring( 1 );
        }

        ChannelSftp channel = null;
        
        boolean bDownloaded = true;
        try
        {
            channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );

            channel.connect();

            SftpATTRS attrs = changeToRepositoryDirectory( channel, dir, filename );

            long lastModified = attrs.getMTime() * MILLIS_PER_SEC;
            if ( timestamp <= 0 || lastModified > timestamp )
            {
                resource.setContentLength( attrs.getSize() );
                
                resource.setLastModified( lastModified );
                
                fireGetStarted( resource, destination );

                channel.get( filename, destination.getAbsolutePath() );

                postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

                fireGetCompleted( resource, destination );

                String[] dirs = PathUtils.dirnames( dir );

                for ( int i = 0; i < dirs.length; i++ )
                {
                    channel.cd( ".." );
                }

                bDownloaded = true;
            }
            else
            {
                bDownloaded = false;
            }
        }
        catch ( SftpException e )
        {
            handleGetException( resource, e, destination );
        }
        catch ( JSchException e )
        {
            handleGetException( resource, e, destination );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }

        return bDownloaded;
    }

    private SftpATTRS changeToRepositoryDirectory( ChannelSftp channel, String dir, String filename )
        throws ResourceDoesNotExistException, SftpException
    {
        // This must be called first to ensure that if the file doesn't exist it throws an exception
        SftpATTRS attrs;
        try
        {
            channel.cd( repository.getBasedir() );

            if ( dir.length() > 0 )
            {
                channel.cd( dir );
            }

            attrs = channel.stat( filename );
        }
        catch ( SftpException e )
        {
            if ( e.toString().trim().endsWith( "No such file" ) )
            {
                throw new ResourceDoesNotExistException( e.toString(), e );
            }
            else
            {
                throw e;
            }
        }
        return attrs;
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        Resource resource = getResource( destination );

        firePutInitiated( resource, source );

        put( basedir, resource, source );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getIfNewer( resourceName, destination, 0 );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        createParentDirectories( destination );

        Resource resource = getResource( resourceName );

        fireGetInitiated( resource, destination );

        return getIfNewer( resource, destination, timestamp );
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        final RepositoryPermissions permissions = repository.getPermissions();

        ChannelSftp channel = null;

        try
        {
            channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );

            channel.connect();

            channel.cd( "/" );
            
            String basedir = getRepository().getBasedir();
            int directoryMode = getDirectoryMode( permissions );
            
            mkdirs( channel, basedir + "/", directoryMode );
            
            fireTransferDebug( "Recursively uploading directory " + sourceDirectory.getAbsolutePath() + " as "
                + destinationDirectory );
            
            mkdirs( channel, destinationDirectory, directoryMode );
            ftpRecursivePut( channel, sourceDirectory, null, getResourceFilename( destinationDirectory ), directoryMode );
        }
        catch ( SftpException e )
        {
            String msg =
                "Error occured while deploying '" + sourceDirectory.getAbsolutePath() + "' " + "to remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        catch ( JSchException e )
        {
            String msg =
                "Error occured while deploying '" + sourceDirectory.getAbsolutePath() + "' " + "to remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }

    private void ftpRecursivePut( ChannelSftp channel, File sourceFile, String prefix, String fileName, int directoryMode )
        throws TransferFailedException, SftpException
    {
        final RepositoryPermissions permissions = repository.getPermissions();

        if ( sourceFile.isDirectory() )
        {
            if ( !fileName.equals( "." ) )
            {
                prefix = getFileName( prefix, fileName );
                mkdir( channel, fileName, directoryMode );
                channel.cd( fileName );
            }

            File[] files = sourceFile.listFiles();
            if ( files != null && files.length > 0 )
            {
                // Directories first, then files. Let's go deep early.
                for ( int i = 0; i < files.length; i++ )
                {
                    if ( files[i].isDirectory() )
                    {
                        ftpRecursivePut( channel, files[i], prefix, files[i].getName(), directoryMode );
                    }
                }
                for ( int i = 0; i < files.length; i++ )
                {
                    if ( !files[i].isDirectory() )
                    {
                        ftpRecursivePut( channel, files[i], prefix, files[i].getName(), directoryMode );
                    }
                }
            }
            
            channel.cd( ".." );
        }
        else
        {
            Resource resource = getResource( getFileName( prefix, fileName ) );

            firePutInitiated( resource, sourceFile );

            putFile( channel, sourceFile, resource, permissions );
        }
    }

    private String getFileName( String prefix, String fileName )
    {
        if ( prefix != null )
        {
            prefix = prefix + "/" + fileName;
        }
        else
        {
            prefix = fileName;
        }
        return prefix;
    }
    
    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String filename = getResourceFilename( destinationDirectory );

        String dir = getResourceDirectory( destinationDirectory );

        // we already setuped the root directory. Ignore beginning /
        if ( dir.length() > 0 && dir.charAt( 0 ) == PATH_SEPARATOR )
        {
            dir = dir.substring( 1 );
        }

        ChannelSftp channel = null;
        try
        {
            channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );

            channel.connect();

            SftpATTRS attrs = changeToRepositoryDirectory( channel, dir, filename );
            if ( ( attrs.getPermissions() & S_IFDIR ) == 0 )
            {
                throw new TransferFailedException( "Remote path is not a directory:" + dir );
            }

            Vector fileList = channel.ls( filename );
            List files = new ArrayList( fileList.size() );
            for ( Iterator i = fileList.iterator(); i.hasNext(); )
            {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) i.next();
                
                files.add( entry.getFilename() );
            }
            return files;
        }
        catch ( SftpException e )
        {
            String msg =
                "Error occured while listing '" + destinationDirectory + "' " + "on remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        catch ( JSchException e )
        {
            String msg =
                "Error occured while listing '" + destinationDirectory + "' " + "on remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }
    
    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String filename = getResourceFilename( resourceName );

        String dir = getResourceDirectory( resourceName );

        // we already setuped the root directory. Ignore beginning /
        if ( dir.length() > 0 && dir.charAt( 0 ) == PATH_SEPARATOR )
        {
            dir = dir.substring( 1 );
        }

        ChannelSftp channel = null;
        try
        {
            channel = (ChannelSftp) session.openChannel( SFTP_CHANNEL );

            channel.connect();

            changeToRepositoryDirectory( channel, dir, filename );
            
            return true;
        }
        catch ( ResourceDoesNotExistException e )
        {
            return false;
        }
        catch ( SftpException e )
        {
            String msg =
                "Error occured while looking for '" + resourceName + "' " + "on remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        catch ( JSchException e )
        {
            String msg =
                "Error occured while looking for '" + resourceName + "' " + "on remote repository: "
                    + getRepository().getUrl();

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( channel != null )
            {
                channel.disconnect();
            }
        }
    }
}
