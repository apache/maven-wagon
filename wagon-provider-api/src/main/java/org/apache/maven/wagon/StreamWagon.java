package org.apache.maven.wagon;

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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

/**
 * Base class for wagon which provide stream based API.
 *
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public abstract class StreamWagon
    extends AbstractWagon
    implements StreamingWagon
{
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public abstract void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException;

    public abstract void fillOutputData( OutputData outputData )
        throws TransferFailedException;

    public abstract void closeConnection()
        throws ConnectionException;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getIfNewer( resourceName, destination, 0 );
    }

    protected void checkInputStream( InputStream is, Resource resource )
        throws TransferFailedException
    {
        if ( is == null )
        {
            TransferFailedException e =
                new TransferFailedException( getRepository().getUrl()
                    + " - Could not open input stream for resource: '" + resource + "'" );
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
            throw e;
        }
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        boolean retValue = false;

        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        InputStream is = getInputStream( resource );

        // always get if timestamp is 0 (ie, target doesn't exist), otherwise only if older than the remote file
        if ( timestamp == 0 || timestamp < resource.getLastModified() )
        {
            retValue = true;

            checkInputStream( is, resource );

            createParentDirectories( destination );

            getTransfer( resource, destination, is );
        }
        else
        {
            IOUtil.close( is );
        }

        return retValue;
    }

    protected InputStream getInputStream( Resource resource )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        InputData inputData = new InputData();

        inputData.setResource( resource );

        try
        {
            fillInputData( inputData );
        }
        catch ( TransferFailedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
            throw e;
        }
        catch ( ResourceDoesNotExistException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
            throw e;
        }

        return inputData.getInputStream();
    }

    // source doesn't exist exception
    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        OutputStream os = getOutputStream( resource );

        checkOutputStream( resource, os );

        putTransfer( resource, source, os, true );
    }

    protected void checkOutputStream( Resource resource, OutputStream os )
        throws TransferFailedException
    {
        if ( os == null )
        {
            TransferFailedException e =
                new TransferFailedException( getRepository().getUrl()
                    + " - Could not open output stream for resource: '" + resource + "'" );
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
            throw e;
        }
    }

    protected OutputStream getOutputStream( Resource resource )
        throws TransferFailedException
    {
        OutputData outputData = new OutputData();

        outputData.setResource( resource );

        try
        {
            fillOutputData( outputData );
        }
        catch ( TransferFailedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw e;
        }

        OutputStream os = outputData.getOutputStream();
        return os;
    }

    public boolean getIfNewerToStream( String resourceName, OutputStream stream, long timestamp )
        throws ResourceDoesNotExistException, TransferFailedException
    {
        boolean retValue = false;

        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, null );

        InputStream is = getInputStream( resource );

        // always get if timestamp is 0 (ie, target doesn't exist), otherwise only if older than the remote file
        if ( timestamp == 0 || timestamp < resource.getLastModified() )
        {
            retValue = true;

            checkInputStream( is, resource );

            fireGetStarted( resource, null );

            getTransfer( resource, stream, is, true, Integer.MAX_VALUE );

            fireGetCompleted( resource, null );
        }
        else
        {
            IOUtil.close( is );
        }
        
        return retValue;
    }

    public void getToStream( String resourceName, OutputStream stream )
        throws ResourceDoesNotExistException, TransferFailedException
    {
        getIfNewerToStream( resourceName, stream, 0 );
    }

    public void putFromStream( InputStream stream, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );

        firePutInitiated( resource, null );

        putFromStream( stream, resource );
    }

    public void putFromStream( InputStream stream, String destination, long contentLength, long lastModified )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );

        firePutInitiated( resource, null );

        resource.setContentLength( contentLength );

        resource.setLastModified( lastModified );

        putFromStream( stream, resource );
    }

    private void putFromStream( InputStream stream, Resource resource )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        OutputStream os = getOutputStream( resource );

        checkOutputStream( resource, os );

        firePutStarted( resource, null );

        putTransfer( resource, stream, os, true );

        firePutCompleted( resource, null );
    }
}
