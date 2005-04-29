package org.apache.maven.wagon.providers.scm;

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

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ScmWagon
    extends AbstractWagon
{
    private ScmManager scmManager;

    private File checkoutDirectory;

    public ScmManager getScmManager()
    {
        return scmManager;
    }

    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    public File getCheckoutDirectory()
    {
        return checkoutDirectory;
    }

    public void setCheckoutDirectory( File checkoutDirectory )
    {
        this.checkoutDirectory = checkoutDirectory;
    }

    public void openConnection()
        throws ConnectionException
    {
        if ( !checkoutDirectory.exists() )
        {
            checkoutDirectory.mkdirs();
        }
    }

    private ScmRepository getScmRepository( String url )
        throws TransferFailedException
    {
        String username = null;

        String password = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();
        }

        ScmRepository scmRepository;
        try
        {
            scmRepository = scmManager.makeScmRepository( url );
        }
        catch ( ScmRepositoryException e )
        {
            throw new TransferFailedException( "Error initialising SCM repository", e );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new TransferFailedException( "Unknown SCM type", e );
        }

        // TODO: this should be generic...
        if ( scmRepository.getProvider().equals( "svn" ) )
        {
            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) scmRepository.getProviderRepository();

            if ( username != null && username.length() > 0 )
            {
                svnRepo.setUser( username );
            }
            if ( password != null && password.length() > 0 )
            {
                svnRepo.setPassword( password );
            }
        }
        return scmRepository;
    }

    // put
    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        String url = getRepository().getUrl() + "/" + resourceName;

        ScmRepository scmRepository = getScmRepository( url );

        firePutStarted( resource, source );

        try
        {
            File scmFile = new File( checkoutDirectory, resourceName );
            scmFile.getParentFile().mkdirs();

            boolean alreadyExists = scmFile.exists();

            if ( !scmFile.equals( source ) )
            {
                FileUtils.copyFile( source, scmFile );
            }

            if ( alreadyExists )
            {
                scmManager.update( scmRepository, new ScmFileSet( checkoutDirectory, resourceName, null ), null );
            }
            else
            {
                scmManager.checkOut( scmRepository, new ScmFileSet( scmFile.getParentFile() ), null );
                scmManager.add( scmRepository, new ScmFileSet( scmFile.getParentFile(), scmFile ) );
            }

            String msg = "Adding " + resourceName + " to repository";
            scmManager.checkIn( scmRepository, new ScmFileSet( scmFile.getParentFile(), scmFile ), null, msg );
        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error interacting with SCM", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error interacting with SCM", e );
        }

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        firePutCompleted( resource, source );
    }

    public void closeConnection()
    {
    }

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

        ScmRepository scmRepository = getScmRepository( url );

        fireGetStarted( resource, destination );

        // TODO: limitations:
        //  - destination filename must match that in the repository - should allow the "-d" CVS equiv to be passed in
        //  - we don't get granular exceptions from SCM (ie, auth, not found)
        //  - need to make it non-recursive to save time
        //  - exists() check doesn't test if it is in SCM already

        try
        {
            File scmFile = new File( checkoutDirectory, resourceName );
            scmFile.getParentFile().mkdirs();

            if ( scmFile.exists() )
            {
                scmManager.update( scmRepository, new ScmFileSet( checkoutDirectory, resourceName, null ), null );
            }
            else
            {
                scmManager.checkOut( scmRepository, new ScmFileSet( scmFile.getParentFile() ), null );
            }

            if ( !scmFile.equals( destination ) )
            {
                FileUtils.copyFile( scmFile, destination );
            }
        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error getting file from SCM", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error getting file from SCM", e );
        }

        postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

        fireGetCompleted( resource, destination );
    }
}
