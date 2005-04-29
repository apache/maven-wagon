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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateParser;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmException;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;

/**
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ScmWagon
    extends AbstractWagon
{
    private ScmManager scmManager;

    public ScmManager getScmManager()
    {
        return scmManager;
    }

    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    public void openConnection()
        throws ConnectionException
    {
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
            scmRepository = scmManager.makeScmRepository( getRepository().getUrl() );
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
        String url = getRepository().getUrl() + "/" + resourceName;

        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        firePutStarted( resource, source );

        // TODO: if not exists, checkout
        // TODO: put file in place
        // TODO: commit

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
            if ( destination.exists() )
            {
                scmManager.update( scmRepository, new ScmFileSet( destination.getParentFile(), destination ), null );
            }
            else
            {
                scmManager.checkOut( scmRepository, new ScmFileSet( destination.getParentFile() ), null );
            }
        }
        catch ( ScmException e )
        {
            throw new TransferFailedException( "Error getting file from SCM", e );
        }

        fireGetCompleted( resource, destination );
    }
}
