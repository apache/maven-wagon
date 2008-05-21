package org.apache.maven.wagon.providers.ssh.ganymed;

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

import ch.ethz.ssh2.SCPClient;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A base class for deployers and fetchers using protocols from SSH2 family and
 * JSch library for underlying implementation
 * <p/>
 * This is responsible for authentification stage of the process.
 * <p/>
 * We will first try to use public keys for authentication and if that doesn't
 * work then we fall back to using the login and password
 *
 * @version $Id$
 * @todo [BP] add compression flag
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="scp"
 *   instantiation-strategy="per-lookup"
 */
public class ScpWagon
    extends AbstractGanymedWagon
{
    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String basedir = getRepository().getBasedir();

        destination = StringUtils.replace( destination, "\\", "/" );
        String dir = PathUtils.dirname( destination );
        dir = StringUtils.replace( dir, "\\", "/" );

        Resource resource = new Resource( destination );

        firePutInitiated( resource, source );

        try
        {
            String umaskCmd = null;
            if ( getRepository().getPermissions() != null )
            {
                String dirPerms = getRepository().getPermissions().getDirectoryMode();

                if ( dirPerms != null )
                {
                    umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
                }
            }

            String mkdirCmd = "mkdir -p " + getPath( basedir, dir );

            if ( umaskCmd != null )
            {
                mkdirCmd = umaskCmd + "; " + mkdirCmd;
            }

            executeCommand( mkdirCmd );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }

        String path = getPath( basedir, destination );

        RepositoryPermissions permissions = getRepository().getPermissions();

        firePutStarted( resource, source );

        // TODO: should we just incorporate this code directly to be able to use the normal putTransfer and tracking?
        SCPClient client = new SCPClient( connection );
        try
        {
            int index = path.lastIndexOf( '/' );
            client.put( source.getAbsolutePath(), path.substring( index + 1 ), path.substring( 0, index ),
                        getOctalMode( permissions ) );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file. Reason: " + e.getMessage(), e );
        }

        postProcessListeners( resource, source, TransferEvent.REQUEST_PUT );

        firePutCompleted( resource, source );

        try
        {
            if ( permissions != null && permissions.getGroup() != null )
            {
                executeCommand( "chgrp -f " + permissions.getGroup() + " " + path );
            }
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        String basedir = getRepository().getBasedir();

        String path = getPath( basedir, resourceName );

        destination.getParentFile().mkdirs();

        OutputStream os;
        try
        {
            os = new FileOutputStream( destination );
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Error writing output file. Reason: " + e.getMessage(), e );
        }

        try
        {
            fireGetStarted( resource, destination );

            // TODO: should we just incorporate this code directly to be able to use the normal putTransfer and tracking?
            SCPClient client = new SCPClient( connection );
            client.get( path, os );

            postProcessListeners( resource, destination, TransferEvent.REQUEST_GET );

            fireGetCompleted( resource, destination );
        }
        catch ( IOException e )
        {
            if ( e.getCause().getMessage().trim().endsWith( "No such file or directory" ) )
            {
                throw new ResourceDoesNotExistException( e.getMessage().trim() );
            }
            else
            {
                throw new TransferFailedException( "Error transferring file. Reason: " + e.getMessage(), e );
            }
        }
        finally
        {
            IOUtil.close( os );
        }
    }
}
