package org.apache.maven.wagon.providers.ftp;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;


/**
 * FtpWagon 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="ftp"
 *   instantiation-strategy="per-lookup"
 */
public class FtpWagon
    extends StreamWagon
{
    private FTPClient ftp;
    
    /** @plexus.configuration default-value="true" */
    private boolean passiveMode = true;

    public boolean isPassiveMode()
    {
        return passiveMode;
    }

    public void setPassiveMode( boolean passiveMode )
    {
        this.passiveMode = passiveMode;
    }

    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
        AuthenticationInfo authInfo = getAuthenticationInfo();

        if ( authInfo == null )
        {
            throw new IllegalArgumentException( "Authentication Credentials cannot be null for FTP protocol" );
        }

        if ( authInfo.getUserName() == null )
        {
            authInfo.setUserName( System.getProperty( "user.name" ) );
        }

        String username = authInfo.getUserName();

        String password = authInfo.getPassword();

        if ( username == null )
        {
            throw new AuthenticationException( "Username not specified for repository " + getRepository().getId() );
        }
        if ( password == null )
        {
            throw new AuthenticationException( "Password not specified for repository " + getRepository().getId() );
        }

        String host = getRepository().getHost();

        ftp = new FTPClient();
        ftp.setDefaultTimeout( getTimeout() );
        ftp.setDataTimeout( getTimeout() );
        
        ftp.addProtocolCommandListener( new PrintCommandListener( this ) );

        try
        {
            if ( getRepository().getPort() != WagonConstants.UNKNOWN_PORT )
            {
                ftp.connect( host, getRepository().getPort() );
            }
            else
            {
                ftp.connect( host );
            }

            // After connection attempt, you should check the reply code to
            // verify
            // success.
            int reply = ftp.getReplyCode();

            if ( !FTPReply.isPositiveCompletion( reply ) )
            {
                ftp.disconnect();

                throw new AuthenticationException( "FTP server refused connection." );
            }
        }
        catch ( IOException e )
        {
            if ( ftp.isConnected() )
            {
                try
                {
                    fireSessionError( e );

                    ftp.disconnect();
                }
                catch ( IOException f )
                {
                    // do nothing
                }
            }

            throw new AuthenticationException( "Could not connect to server.", e );
        }

        try
        {
            if ( !ftp.login( username, password ) )
            {
                throw new AuthenticationException( "Cannot login to remote system" );
            }

            fireSessionDebug( "Remote system is " + ftp.getSystemName() );

            // Set to binary mode.
            ftp.setFileType( FTP.BINARY_FILE_TYPE );
            ftp.setListHiddenFiles( true );

            // Use passive mode as default because most of us are
            // behind firewalls these days.
            if ( isPassiveMode() )
            {                
                ftp.enterLocalPassiveMode();
            }
        }
        catch ( IOException e )
        {
            throw new ConnectionException( "Cannot login to remote system", e );
        }
    }

    protected void firePutCompleted( Resource resource, File file )
    {
        try
        {
            // TODO [BP]: verify the order is correct
            ftp.completePendingCommand();

            RepositoryPermissions permissions = repository.getPermissions();

            if ( permissions != null && permissions.getGroup() != null )
            {
                // ignore failures
                ftp.sendSiteCommand( "CHGRP " + permissions.getGroup() + " " + resource.getName() );
            }

            if ( permissions != null && permissions.getFileMode() != null )
            {
                // ignore failures
                ftp.sendSiteCommand( "CHMOD " + permissions.getFileMode() + " " + resource.getName() );
            }
        }
        catch ( IOException e )
        {
            // TODO: handle
            // michal I am not sure  what error means in that context
            // I think that we will be able to recover or simply we will fail later on
        }

        super.firePutCompleted( resource, file );
    }

    protected void fireGetCompleted( Resource resource, File localFile )
    {
        try
        {
            ftp.completePendingCommand();
        }
        catch ( IOException e )
        {
            // TODO: handle
            // michal I am not sure  what error means in that context
            // actually I am not even sure why we have to invoke that command
            // I think that we will be able to recover or simply we will fail later on
        }
        super.fireGetCompleted( resource, localFile );
    }

    public void closeConnection()
        throws ConnectionException
    {
        if ( ftp != null && ftp.isConnected() )
        {
            try
            {
                // This is a NPE rethink shutting down the streams
                ftp.disconnect();
            }
            catch ( IOException e )
            {
                throw new ConnectionException( "Failed to close connection to FTP repository", e );
            }
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        OutputStream os;

        Resource resource = outputData.getResource();

        RepositoryPermissions permissions = repository.getPermissions();

        try
        {
            if ( !ftp.changeWorkingDirectory( getRepository().getBasedir() ) )
            {
                throw new TransferFailedException(
                    "Required directory: '" + getRepository().getBasedir() + "' " + "is missing" );
            }

            String[] dirs = PathUtils.dirnames( resource.getName() );

            for ( int i = 0; i < dirs.length; i++ )
            {
                boolean dirChanged = ftp.changeWorkingDirectory( dirs[i] );

                if ( !dirChanged )
                {
                    // first, try to create it
                    boolean success = ftp.makeDirectory( dirs[i] );

                    if ( success )
                    {
                        if ( permissions != null && permissions.getGroup() != null )
                        {
                            // ignore failures
                            ftp.sendSiteCommand( "CHGRP " + permissions.getGroup() + " " + dirs[i] );
                        }

                        if ( permissions != null && permissions.getDirectoryMode() != null )
                        {
                            // ignore failures
                            ftp.sendSiteCommand( "CHMOD " + permissions.getDirectoryMode() + " " + dirs[i] );
                        }

                        dirChanged = ftp.changeWorkingDirectory( dirs[i] );
                    }
                }

                if ( !dirChanged )
                {
                    throw new TransferFailedException( "Unable to create directory " + dirs[i] );
                }
            }

            // we come back to original basedir so
            // FTP wagon is ready for next requests
            if ( !ftp.changeWorkingDirectory( getRepository().getBasedir() ) )
            {
                throw new TransferFailedException( "Unable to return to the base directory" );
            }

            os = ftp.storeFileStream( resource.getName() );

            if ( os == null )
            {
                String msg = "Cannot transfer resource:  '" + resource
                    + "'. Output stream is null. FTP Server response: " + ftp.getReplyString();

                throw new TransferFailedException( msg );

            }

            fireTransferDebug( "resource = " + resource );

        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring over FTP", e );
        }

        outputData.setOutputStream( os );

    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        InputStream is;

        Resource resource = inputData.getResource();

        try
        {
            ftpChangeDirectory( resource );

            String filename = PathUtils.filename( resource.getName() );
            FTPFile[] ftpFiles = ftp.listFiles( filename );

            if ( ftpFiles == null || ftpFiles.length <= 0 )
            {
                throw new ResourceDoesNotExistException( "Could not find file: '" + resource + "'" );
            }

            long contentLength = ftpFiles[0].getSize();

            //@todo check how it works! javadoc of common login says:
            // Returns the file timestamp. This usually the last modification time.
            //
            Calendar timestamp = ftpFiles[0].getTimestamp();
            long lastModified = timestamp != null ? timestamp.getTimeInMillis() : 0;

            resource.setContentLength( contentLength );

            resource.setLastModified( lastModified );

            is = ftp.retrieveFileStream( filename );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file via FTP", e );
        }

        inputData.setInputStream( is );
    }

    private void ftpChangeDirectory( Resource resource )
        throws IOException, TransferFailedException, ResourceDoesNotExistException
    {
        if ( !ftp.changeWorkingDirectory( getRepository().getBasedir() ) )
        {
            throw new ResourceDoesNotExistException(
                "Required directory: '" + getRepository().getBasedir() + "' " + "is missing" );
        }

        String[] dirs = PathUtils.dirnames( resource.getName() );

        for ( int i = 0; i < dirs.length; i++ )
        {
            boolean dirChanged = ftp.changeWorkingDirectory( dirs[i] );

            if ( !dirChanged )
            {
                String msg = "Resource " + resource + " not found. Directory " + dirs[i] + " does not exist";

                throw new ResourceDoesNotExistException( msg );
            }
        }
    }

    public class PrintCommandListener
        implements ProtocolCommandListener
    {
        private FtpWagon wagon;

        public PrintCommandListener( FtpWagon wagon )
        {
            this.wagon = wagon;
        }

        public void protocolCommandSent( ProtocolCommandEvent event )
        {
            wagon.fireSessionDebug( "Command sent: " + event.getMessage() );

        }

        public void protocolReplyReceived( ProtocolCommandEvent event )
        {
            wagon.fireSessionDebug( "Reply received: " + event.getMessage() );
        }
    }

    protected void fireSessionDebug( String msg )
    {
        super.fireSessionDebug( msg );
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destinationDirectory );
        
        try 
        {
            ftpChangeDirectory( resource );
    
            String filename = PathUtils.filename( resource.getName() );
            FTPFile[] ftpFiles = ftp.listFiles( filename );
    
            if ( ftpFiles == null || ftpFiles.length <= 0 )
            {
                throw new ResourceDoesNotExistException( "Could not find file: '" + resource + "'" );
            }
            
            List ret = new ArrayList();
            for( int i=0; i < ftpFiles.length; i++ )
            {
                String name = ftpFiles[i].getName();
                
                if ( ftpFiles[i].isDirectory() && !name.endsWith( "/" ) )
                {
                    name += "/";
                }
                
                ret.add( name );
            }
            
            return ret;
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file via FTP", e );
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        try
        {
            ftpChangeDirectory( resource );

            String filename = PathUtils.filename( resource.getName() );
            int status = ftp.stat( filename );

            return ( ( status == FTPReply.FILE_STATUS ) || ( status == FTPReply.FILE_STATUS_OK )
                     || ( status == FTPReply.COMMAND_OK ) || ( status == FTPReply.SYSTEM_STATUS ) );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file via FTP", e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            return false;
        }
    }
    
    public boolean supportsDirectoryCopy()
    {
        return true;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {

        // Change to root.
        try
        {
            if ( !ftp.changeWorkingDirectory( getRepository().getBasedir() ) )
            {
                throw new TransferFailedException( "Required directory: '" + getRepository().getBasedir() + "' "
                                + "is missing" );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Cannot change to root path " + getRepository().getBasedir() );
        }

        fireTransferDebug( "Recursively uploading directory " + sourceDirectory.getAbsolutePath() + " as "
                        + destinationDirectory );
        ftpRecursivePut( sourceDirectory, destinationDirectory );
    }

    private void ftpRecursivePut( File sourceFile, String fileName ) throws TransferFailedException
    {
        final RepositoryPermissions permissions = repository.getPermissions();

        fireTransferDebug( "processing = " + sourceFile.getAbsolutePath() + " as " + fileName );

        if ( sourceFile.isDirectory() )
        {
            if ( !fileName.equals( "." ) )
            {
                try
                {
                    // change directory if it already exists.
                    if ( !ftp.changeWorkingDirectory( fileName ) )
                    {
                        // first, try to create it
                        if ( makeFtpDirectoryRecursive( fileName, permissions ) )
                        {
                            if ( !ftp.changeWorkingDirectory( fileName ) )
                            {
                                throw new TransferFailedException( "Unable to change cwd on ftp server to " + fileName
                                                + " when processing " + sourceFile.getAbsolutePath() );
                            }
                        }
                        else
                        {
                            throw new TransferFailedException( "Unable to create directory " + fileName
                                            + " when processing " + sourceFile.getAbsolutePath() );
                        }
                    }
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "IOException caught while processing path at "
                                    + sourceFile.getAbsolutePath(), e );
                }
            }

            File[] files = sourceFile.listFiles();
            if ( files != null && files.length > 0 )
            {
                fireTransferDebug( "listing children of = " + sourceFile.getAbsolutePath() + " found " + files.length );

                // Directories first, then files. Let's go deep early.
                for ( int i = 0; i < files.length; i++ )
                {
                    if ( files[i].isDirectory() )
                    {
                        ftpRecursivePut( files[i], files[i].getName() );
                    }
                }
                for ( int i = 0; i < files.length; i++ )
                {
                    if ( !files[i].isDirectory() )
                    {
                        ftpRecursivePut( files[i], files[i].getName() );
                    }
                }
            }

            // Step back up a directory once we're done with the contents of this one.
            try
            {
                ftp.changeToParentDirectory();
            }
            catch ( IOException e )
            {
                throw new TransferFailedException( "IOException caught while attempting to step up to parent directory"
                                                   + " after successfully processing " + sourceFile.getAbsolutePath(),
                                                   e );
            }
        }
        else
        {
            // Oh how I hope and pray, in denial, but today I am still just a file.
            
            FileInputStream sourceFileStream = null;
            try
            {
                sourceFileStream = new FileInputStream( sourceFile );
                
                // It's a file. Upload it in the current directory.
                if ( ftp.storeFile( fileName, sourceFileStream ) )
                {
                    if ( permissions != null )
                    {
                        // Process permissions; note that if we get errors or exceptions here, they are ignored.
                        // This appears to be a conscious decision, based on other parts of this code.
                        String group = permissions.getGroup();
                        if ( group != null )
                            try
                            {
                                ftp.sendSiteCommand( "CHGRP " + permissions.getGroup() );
                            }
                            catch ( IOException e )
                            {
                            }
                        String mode = permissions.getFileMode();
                        if ( mode != null )
                            try
                            {
                                ftp.sendSiteCommand( "CHMOD " + permissions.getDirectoryMode() );
                            }
                            catch ( IOException e )
                            {
                            }
                    }
                }
                else
                {
                    String msg =
                        "Cannot transfer resource:  '" + sourceFile.getAbsolutePath() + "' FTP Server response: "
                                        + ftp.getReplyString();
                    throw new TransferFailedException( msg );
                }
            }
            catch ( IOException e )
            {
                throw new TransferFailedException( "IOException caught while attempting to upload "
                                + sourceFile.getAbsolutePath(), e );
            }
            finally
            {
                IOUtil.close( sourceFileStream );
            }

        }

        fireTransferDebug( "completed = " + sourceFile.getAbsolutePath() );
    }

    /**
     * Set the permissions (if given) for the underlying folder.
     * Note: Since the FTP SITE command might be server dependent, we cannot
     * rely on the functionality available on each FTP server!
     * So we can only try and hope it works (and catch away all Exceptions).
     *
     * @param permissions group and directory permissions
     */
    private void setPermissions(RepositoryPermissions permissions) {
        if ( permissions != null )
        {
            // Process permissions; note that if we get errors or exceptions here, they are ignored.
            // This appears to be a conscious decision, based on other parts of this code.
            String group = permissions.getGroup();
            if ( group != null )
            {
                try
                {
                    ftp.sendSiteCommand( "CHGRP " + permissions.getGroup() );
                }
                catch ( IOException e )
                {
                }
            }
            String mode = permissions.getDirectoryMode();
            if ( mode != null )
            {
                try
                {
                    ftp.sendSiteCommand( "CHMOD " + permissions.getDirectoryMode() );
                }
                catch ( IOException e )
                {
                }
            }
        }
    }

    /**
     * Recursively create directories.
     *
     * @param fileName the path to create (might be nested)
     * @param permissions
     * @return ok
     * @throws IOException
     */
    private boolean makeFtpDirectoryRecursive(String fileName, RepositoryPermissions permissions) throws IOException
    {
        if ( fileName == null || fileName.length() == 0 )
        {
            return false;
        }

        int slashPos = fileName.indexOf( "/" );
        String oldPwd = null;
        boolean ok = false;

        if ( slashPos == 0 )
        {
            // this is an absolute directory
            oldPwd = ftp.printWorkingDirectory();

            // start with the root
            ftp.changeWorkingDirectory( "/" );
            fileName = fileName.substring( 1 );
        }

        if (  slashPos > 0 && slashPos < fileName.length() - 1 )
        {
            if ( oldPwd == null)
            {
                oldPwd = ftp.printWorkingDirectory();
            }

            String nextDir = fileName.substring( 0, slashPos );
            ok |= ftp.makeDirectory( nextDir );

            if ( ok )
            {
                // set the permissions for the freshly created directory
                setPermissions( permissions );


                ftp.changeWorkingDirectory( nextDir );

                // now create the deeper directories
                String remainingDirs = fileName.substring( slashPos + 1 );
                ok |= makeFtpDirectoryRecursive( remainingDirs, permissions);
            }
        }
        else
        {
            ok = ftp.makeDirectory( fileName );
        }

        if ( oldPwd != null )
        {
            // change back to the old working directory
            ftp.changeWorkingDirectory( oldPwd );
        }

        return ok;
    }
}
