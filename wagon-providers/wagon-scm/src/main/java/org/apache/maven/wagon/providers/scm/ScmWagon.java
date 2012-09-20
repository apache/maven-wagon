package org.apache.maven.wagon.providers.scm;

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

import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmRevision;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.list.ListScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Wagon provider to get and put files from and to SCM systems, using Maven-SCM as underlying transport.
 * <p/>
 * TODO it probably creates problems if the same wagon is used in two different SCM protocols, as instance variables can
 * keep incorrect state.
 * TODO: For doing releases, we either have to be able to add files with checking out the repository structure which may not be
 * possible, or the checkout directory needs to be a constant. Doing releases won't scale if you have to checkout the
 * whole repository structure in order to add 3 files.
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="carlos@apache.org">Carlos Sanchez</a>
 * @author Jason van Zyl
 *
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="scm" instantiation-strategy="per-lookup"
 */
public class ScmWagon
    extends AbstractWagon
{
    /**
     * @plexus.requirement
     */
    private ScmManager scmManager;

    /**
     * The SCM version, if any.
     *
     * @parameter
     */
    private String scmVersion;

    /**
     * The SCM version type, if any. Defaults to "branch".
     *
     * @parameter
     */
    private String scmVersionType;

    private File checkoutDirectory;

    /**
     * Get the {@link ScmManager} used in this Wagon
     *
     * @return the {@link ScmManager}
     */
    public ScmManager getScmManager()
    {
        return scmManager;
    }

    /**
     * Set the {@link ScmManager} used in this Wagon
     *
     * @param scmManager
     */
    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    /**
     * Get the scmVersion used in this Wagon
     *
     * @return the scmVersion
     */
    public String getScmVersion()
    {
        return scmVersion;
    }

    /**
     * Set the scmVersion
     *
     * @param scmVersion the scmVersion to set
     */
    public void setScmVersion( String scmVersion )
    {
        this.scmVersion = scmVersion;
    }

    /**
     * Get the scmVersionType used in this Wagon
     *
     * @return the scmVersionType
     */
    public String getScmVersionType()
    {
        return scmVersionType;
    }

    /**
     * Set the scmVersionType
     *
     * @param scmVersionType the scmVersionType to set
     */
    public void setScmVersionType( String scmVersionType )
    {
        this.scmVersionType = scmVersionType;
    }

    /**
     * Get the directory where Wagon will checkout files from SCM. This directory will be deleted!
     *
     * @return directory
     */
    public File getCheckoutDirectory()
    {
        return checkoutDirectory;
    }

    /**
     * Set the directory where Wagon will checkout files from SCM. This directory will be deleted!
     *
     * @param checkoutDirectory
     */
    public void setCheckoutDirectory( File checkoutDirectory )
    {
        this.checkoutDirectory = checkoutDirectory;
    }

    /**
     * Convenience method to get the {@link ScmProvider} implementation to handle the provided SCM type
     *
     * @param scmType type of SCM, eg. <code>svn</code>, <code>cvs</code>
     * @return the {@link ScmProvider} that will handle provided SCM type
     * @throws NoSuchScmProviderException if there is no {@link ScmProvider} able to handle that SCM type
     */
    public ScmProvider getScmProvider( String scmType )
        throws NoSuchScmProviderException
    {
        return getScmManager().getProviderByType( scmType );
    }

    /**
     * This will cleanup the checkout directory
     */
    public void openConnectionInternal()
        throws ConnectionException
    {
        if ( checkoutDirectory == null )
        {
            checkoutDirectory = createCheckoutDirectory();
        }

        if ( checkoutDirectory.exists() )
        {
            removeCheckoutDirectory();
        }

        checkoutDirectory.mkdirs();
    }

    private File createCheckoutDirectory()
    {
        File checkoutDirectory;

        DecimalFormat fmt = new DecimalFormat( "#####" );

        Random rand = new Random( System.currentTimeMillis() + Runtime.getRuntime().freeMemory() );

        synchronized ( rand )
        {
            do
            {
                checkoutDirectory = new File( System.getProperty( "java.io.tmpdir" ),
                                              "wagon-scm" + fmt.format( Math.abs( rand.nextInt() ) ) + ".checkout" );
            }
            while ( checkoutDirectory.exists() );
        }

        return checkoutDirectory;
    }


    private void removeCheckoutDirectory()
        throws ConnectionException
    {
        if ( checkoutDirectory == null )
        {
            return; // Silently return.
        }

        try
        {
            FileUtils.deleteDirectory( checkoutDirectory );
        }
        catch ( IOException e )
        {
            throw new ConnectionException( "Unable to cleanup checkout directory", e );
        }
    }

    /**
     * Construct the ScmVersion to use for operations.
     * <p/>
     * <p>If scmVersion is supplied, scmVersionType must also be supplied to
     * take effect.</p>
     */
    private ScmVersion makeScmVersion()
    {
        if ( StringUtils.isBlank( scmVersion ) )
        {
            return null;
        }
        if ( scmVersion.length() > 0 )
        {
            if ( "revision".equals( scmVersionType ) )
            {
                return new ScmRevision( scmVersion );
            }
            else if ( "tag".equals( scmVersionType ) )
            {
                return new ScmTag( scmVersion );
            }
            else if ( "branch".equals( scmVersionType ) )
            {
                return new ScmBranch( scmVersion );
            }
        }

        return null;
    }

    private ScmRepository getScmRepository( String url )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        String username = null;

        String password = null;

        String privateKey = null;

        String passphrase = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();

            privateKey = authenticationInfo.getPrivateKey();

            passphrase = authenticationInfo.getPassphrase();
        }

        ScmRepository scmRepository = getScmManager().makeScmRepository( url );

        ScmProviderRepository providerRepository = scmRepository.getProviderRepository();

        if ( StringUtils.isNotEmpty( username ) )
        {
            providerRepository.setUser( username );
        }

        if ( StringUtils.isNotEmpty( password ) )
        {
            providerRepository.setPassword( password );
        }

        if ( providerRepository instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost providerRepo = (ScmProviderRepositoryWithHost) providerRepository;

            if ( StringUtils.isNotEmpty( privateKey ) )
            {
                providerRepo.setPrivateKey( privateKey );
            }

            if ( StringUtils.isNotEmpty( passphrase ) )
            {
                providerRepo.setPassphrase( passphrase );
            }
        }

        return scmRepository;
    }

    public void put( File source, String targetName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( source.isDirectory() )
        {
            throw new IllegalArgumentException( "Source is a directory: " + source );
        }
        putInternal( source, targetName );
    }

    /**
     * Puts both files and directories
     *
     * @param source
     * @param targetName
     * @throws TransferFailedException
     */
    private void putInternal( File source, String targetName )
        throws TransferFailedException
    {
        Resource target = new Resource( targetName );

        firePutInitiated( target, source );

        try
        {
            ScmRepository scmRepository = getScmRepository( getRepository().getUrl() );

            target.setContentLength( source.length() );
            target.setLastModified( source.lastModified() );

            firePutStarted( target, source );

            String msg = "Wagon: Adding " + source.getName() + " to repository";

            ScmProvider scmProvider = getScmProvider( scmRepository.getProvider() );

            String checkoutTargetName = source.isDirectory() ? targetName : getDirname( targetName );
            String relPath = checkOut( scmProvider, scmRepository, checkoutTargetName, target );

            File newCheckoutDirectory = new File( checkoutDirectory, relPath );

            File scmFile = new File( newCheckoutDirectory, source.isDirectory() ? "" : getFilename( targetName ) );

            boolean fileAlreadyInScm = scmFile.exists();

            if ( !scmFile.equals( source ) )
            {
                if ( source.isDirectory() )
                {
                    FileUtils.copyDirectoryStructure( source, scmFile );
                }
                else
                {
                    FileUtils.copyFile( source, scmFile );
                }
            }

            if ( !fileAlreadyInScm || scmFile.isDirectory() )
            {
                int addedFiles = addFiles( scmProvider, scmRepository, newCheckoutDirectory,
                                           source.isDirectory() ? "" : scmFile.getName() );

                if ( !fileAlreadyInScm && addedFiles == 0 )
                {
                    throw new ScmException(
                        "Unable to add file to SCM: " + scmFile + "; see error messages above for more information" );
                }
            }

            ScmResult result =
                scmProvider.checkIn( scmRepository, new ScmFileSet( checkoutDirectory ), makeScmVersion(), msg );

            checkScmResult( result );
        }
        catch ( ScmException e )
        {
            fireTransferError( target, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( "Error interacting with SCM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            fireTransferError( target, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( "Error interacting with SCM: " + e.getMessage(), e );
        }

        if ( source.isFile() )
        {
            postProcessListeners( target, source, TransferEvent.REQUEST_PUT );
        }

        firePutCompleted( target, source );
    }

    /**
     * Returns the relative path to targetName in the checkout dir. If the targetName already exists in the scm, this
     * will be the empty string.
     *
     * @param scmProvider
     * @param scmRepository
     * @param targetName
     * @return
     * @throws TransferFailedException
     */
    private String checkOut( ScmProvider scmProvider, ScmRepository scmRepository, String targetName,
                             Resource resource )
        throws TransferFailedException
    {
        checkoutDirectory = createCheckoutDirectory();

        Stack<String> stack = new Stack<String>();

        String target = targetName;

        // totally ignore scmRepository parent stuff since that is not supported by all scms.
        // Instead, assume that that url exists. If not, then that's an error.
        // Check whether targetName, which is a relative path into the scm, exists.
        // If it doesn't, check the parent, etc.

        try
        {
            while ( target.length() > 0 && !scmProvider.list( scmRepository,
                                                              new ScmFileSet( new File( "." ), new File( target ) ),
                                                              false, makeScmVersion() ).isSuccess() )
            {
                stack.push( getFilename( target ) );
                target = getDirname( target );
            }
        }
        catch ( ScmException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error listing repository: " + e.getMessage(), e );
        }

        // ok, we've established that target exists, or is empty.
        // Check the resource out; if it doesn't exist, that means we're in the svn repo url root,
        // and the configuration is incorrect. We will not try repo.getParent since most scm's don't
        // implement that.

        try
        {
            String repoUrl = getRepository().getUrl();
            if ( "svn".equals( scmProvider.getScmType() ) )
            {
                // Subversion is the only SCM that adds path structure to represent tags and branches.
                // The rest use scmVersion and scmVersionType.
                repoUrl += "/" + target.replace( '\\', '/' );
            }
            scmRepository = getScmRepository( repoUrl );
            CheckOutScmResult ret =
                scmProvider.checkOut( scmRepository, new ScmFileSet( new File( checkoutDirectory, "" ) ),
                                      makeScmVersion(), false );

            checkScmResult( ret );
        }
        catch ( ScmException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error checking out: " + e.getMessage(), e );
        }

        // now create the subdirs in target, if it's a parent of targetName

        String relPath = "";

        while ( !stack.isEmpty() )
        {
            String p = (String) stack.pop();
            relPath += p + "/";

            File newDir = new File( checkoutDirectory, relPath );
            if ( !newDir.mkdirs() )
            {
                throw new TransferFailedException(
                    "Failed to create directory " + newDir.getAbsolutePath() + "; parent should exist: "
                        + checkoutDirectory );
            }

            try
            {
                addFiles( scmProvider, scmRepository, checkoutDirectory, relPath );
            }
            catch ( ScmException e )
            {
                fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

                throw new TransferFailedException( "Failed to add directory " + newDir + " to working copy", e );
            }
        }

        return relPath;
    }

    /**
     * Add a file or directory to a SCM repository. If it's a directory all its contents are added recursively.
     * <p/>
     * TODO this is less than optimal, SCM API should provide a way to add a directory recursively
     *
     * @param scmProvider   SCM provider
     * @param scmRepository SCM repository
     * @param basedir       local directory corresponding to scmRepository
     * @param scmFilePath   path of the file or directory to add, relative to basedir
     * @return the number of files added.
     * @throws ScmException
     */
    private int addFiles( ScmProvider scmProvider, ScmRepository scmRepository, File basedir, String scmFilePath )
        throws ScmException
    {
        int addedFiles = 0;

        File scmFile = new File( basedir, scmFilePath );

        if ( scmFilePath.length() != 0 )
        {
            AddScmResult result = scmProvider.add( scmRepository, new ScmFileSet( basedir, new File( scmFilePath ) ) );

            /*
             * TODO dirty fix to work around files with property svn:eol-style=native if a file has that property, first
             * time file is added it fails, second time it succeeds the solution is check if the scm provider is svn and
             * unset that property when the SCM API allows it
             */
            if ( !result.isSuccess() )
            {
                result = scmProvider.add( scmRepository, new ScmFileSet( basedir, new File( scmFilePath ) ) );
            }

            addedFiles = result.getAddedFiles().size();
        }

        String reservedScmFile = scmProvider.getScmSpecificFilename();

        if ( scmFile.isDirectory() )
        {
            File[] files = scmFile.listFiles();

            for ( int i = 0; i < files.length; i++ )
            {
                if ( reservedScmFile != null && !reservedScmFile.equals( files[i].getName() ) )
                {
                    addedFiles += addFiles( scmProvider, scmRepository, basedir,
                                            ( scmFilePath.length() == 0 ? "" : scmFilePath + "/" )
                                                + files[i].getName() );
                }
            }
        }

        return addedFiles;
    }

    /**
     * @return true
     */
    public boolean supportsDirectoryCopy()
    {
        return true;
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !sourceDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Source is not a directory: " + sourceDirectory );
        }

        putInternal( sourceDirectory, destinationDirectory );
    }

    /**
     * Check that the ScmResult was a successful operation
     *
     * @param result
     * @throws TransferFailedException if result was not a successful operation
     * @throws ScmException
     */
    private void checkScmResult( ScmResult result )
        throws ScmException
    {
        if ( !result.isSuccess() )
        {
            throw new ScmException(
                "Unable to commit file. " + result.getProviderMessage() + " " + ( result.getCommandOutput() == null
                    ? ""
                    : result.getCommandOutput() ) );
        }
    }

    public void closeConnection()
        throws ConnectionException
    {
        removeCheckoutDirectory();
    }

    /**
     * Not implemented
     *
     * @throws UnsupportedOperationException always
     */
    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "Not currently supported: getIfNewer" );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        String url = getRepository().getUrl() + "/" + resourceName;

        // remove the file
        url = url.substring( 0, url.lastIndexOf( '/' ) );

        try
        {
            ScmRepository scmRepository = getScmRepository( url );

            fireGetStarted( resource, destination );

            // TODO: limitations:
            // - destination filename must match that in the repository - should allow the "-d" CVS equiv to be passed
            //   in
            // - we don't get granular exceptions from SCM (ie, auth, not found)
            // - need to make it non-recursive to save time
            // - exists() check doesn't test if it is in SCM already

            File scmFile = new File( checkoutDirectory, resourceName );

            File basedir = scmFile.getParentFile();

            ScmProvider scmProvider = getScmProvider( scmRepository.getProvider() );

            String reservedScmFile = scmProvider.getScmSpecificFilename();

            if ( reservedScmFile != null && new File( basedir, reservedScmFile ).exists() )
            {
                scmProvider.update( scmRepository, new ScmFileSet( basedir ), makeScmVersion() );
            }
            else
            {
                // TODO: this should be checking out a full hierarchy (requires the -d equiv)
                basedir.mkdirs();

                scmProvider.checkOut( scmRepository, new ScmFileSet( basedir ), makeScmVersion() );
            }

            if ( !scmFile.exists() )
            {
                throw new ResourceDoesNotExistException( "Unable to find resource " + destination + " after checkout" );
            }

            if ( !scmFile.equals( destination ) )
            {
                FileUtils.copyFile( scmFile, destination );
            }
        }
        catch ( ScmException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( "Error getting file from SCM", e );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( "Error getting file from SCM", e );
        }

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }

    /**
     * @return a List&lt;String&gt; with filenames/directories at the resourcepath.
     * @see org.apache.maven.wagon.AbstractWagon#getFileList(java.lang.String)
     */
    public List<String> getFileList( String resourcePath )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        try
        {
            ScmRepository repository = getScmRepository( getRepository().getUrl() );

            ScmProvider provider = getScmProvider( repository.getProvider() );

            ListScmResult result =
                provider.list( repository, new ScmFileSet( new File( "." ), new File( resourcePath ) ), false,
                               makeScmVersion() );

            if ( !result.isSuccess() )
            {
                throw new ResourceDoesNotExistException( result.getProviderMessage() );
            }

            List<String> files = new ArrayList<String>();

            for ( ScmFile f : result.getFiles() )
            {
                files.add( f.getPath() );
            }

            return files;
        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error getting filelist from SCM", e );
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        try
        {
            getFileList( resourceName );

            return true;
        }
        catch ( ResourceDoesNotExistException e )
        {
            return false;
        }
    }

    private String getFilename( String filename )
    {
        String fname = StringUtils.replace( filename, "/", File.separator );
        return FileUtils.filename( fname );
    }

    private String getDirname( String filename )
    {
        String fname = StringUtils.replace( filename, "/", File.separator );
        return FileUtils.dirname( fname );
    }
}
