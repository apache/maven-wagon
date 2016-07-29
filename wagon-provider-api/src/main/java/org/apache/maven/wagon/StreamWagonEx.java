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

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by wittcnezh on 16-7-29.
 *
 * @author : <a href="wittcnezh@foxmail.com">wittcnezh</a>
 * @version : 1.0.0
 */
class StreamWagonEx extends StreamWagon
{

    StreamWagonEx( StreamWagon wagon )
    {
        repository = wagon.repository;
        authenticationInfo = wagon.authenticationInfo;
        setPermissionsOverride( wagon.getPermissionsOverride() );
        setInteractive( wagon.interactive );
        setReadTimeout( wagon.getReadTimeout() );
        setSessionEventSupport( wagon.sessionEventSupport );
        setTimeout( wagon.getTimeout() );
        setTransferEventSupport( wagon.transferEventSupport );
    }

    @Deprecated
    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    }

    @Deprecated
    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
    }

    @Deprecated
    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
    }

    @Deprecated
    public void closeConnection()
        throws ConnectionException
    {
    }

    // ----------------------------------------------------------------------
    // Update for external downloader or handler or extension.
    // ----------------------------------------------------------------------

    private static final String EXTERNAL_LOADER;
    private static final Pattern EXTERNAL_FILTER;
    private static final String EXTERNAL_STATE;

    static {
        final String keyExternalLoader = "external.loader";
        EXTERNAL_LOADER = ( null == System.getProperty ( keyExternalLoader )
            ? System.getenv ().get ( keyExternalLoader ) : System.getProperty ( keyExternalLoader ) );
        final String keyExternalFilter = "external.filter";
        EXTERNAL_FILTER = Pattern.compile(
            null == System.getProperty ( keyExternalFilter )
                ? ( null == System.getenv().get ( keyExternalFilter )
                    ? ".*\\.jar"
                    : System.getenv().get( keyExternalFilter ) )
                : System.getProperty ( keyExternalFilter )
        );
        final String keyExternalState = "external.state";
        EXTERNAL_STATE = (
            null == System.getProperty ( keyExternalState )
                ? ( null == System.getenv().get ( keyExternalState ) ? ".st" : System.getenv().get( keyExternalState ) )
                : System.getProperty ( keyExternalState )
        );
    }

    private String getFinalPath( File output, String url )
    {
        return output.getParent() + File.separatorChar + url.substring( url.lastIndexOf( "/" ) + 1 );
    }

    private File getStateFile( File output, String url )
    {
        return new File( getFinalPath( output, url ) + File.separatorChar + EXTERNAL_STATE );
    }

    private File getFinalFile( File output, String url )
    {
        return new File( getFinalPath( output, url ) );
    }

    /**
     * transfer data through external tool
     *
     * @param resource    resource to transfer
     * @param url         url to get from
     * @param output      output file destination
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     */
    protected void transfer( Resource resource, String url, File output, int requestType )
        throws IOException
    {
        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, requestType );
        transferEvent.setTimestamp( System.currentTimeMillis() );

        if ( output.exists() )
        {
            output.delete();
        }

        final File parent = output.getParentFile( );
        if ( ! parent.exists() )
        {
            parent.mkdirs();
        }

        final List<String> cmdLine = new ArrayList<String>( 3 );
        cmdLine.add( EXTERNAL_LOADER );
        cmdLine.add( output.getAbsolutePath() );
        cmdLine.add( url );
        ProcessBuilder pb = new ProcessBuilder( cmdLine ).directory( parent );
        Process process = pb.start();

        int exitCode;
        try
        {
            exitCode = process.waitFor();
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e.getMessage() );
        }

        if ( 0 != exitCode )
        {
            fireTransferError( resource, new Exception( "(" + exitCode + ")" ), TransferEvent.REQUEST_GET );
        }
        else
        {
            final File axel = getFinalFile( output, url );
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            final FileInputStream fis = new FileInputStream( axel );
            int length = fis.read( buffer );
            while ( length >= 0 )
            {
                if ( length > 0 )
                {
                    fireTransferProgress( transferEvent, buffer, length );
                }
                length = fis.read( buffer );
            }
            IOUtil.close( fis );
            if ( axel.exists() )
            {
                axel.renameTo( output );
            }
        }
    }

    private void getTransfer( Resource resource, String url, File destination )
        throws TransferFailedException
    {
        // ensure that the destination is created only when we are ready to transfer
        fireTransferDebug( "attempting to create parent directories for destination: " + destination.getName() );
        createParentDirectories( destination );

        fireGetStarted( resource, destination );

        try
        {
            transfer ( resource, url, destination, TransferEvent.REQUEST_GET );
        }
        catch ( final IOException e )
        {
            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( !deleted )
                {
                    destination.deleteOnExit();
                }
            }

            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            String msg = "GET request of: " + resource.getName() + " from " + repository.getName() + " failed";

            throw new TransferFailedException( msg, e );
        }

        fireGetCompleted( resource, destination );
    }

    public String getUrl( Resource resource )
    {
        String repositoryUrl = getRepository().getUrl();
        return repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + resource.getName();
    }

    public boolean handle ( long timestamp, Resource resource, File destination )
        throws TransferFailedException
    {
        if ( null == EXTERNAL_LOADER || EXTERNAL_LOADER.length() <= 0 )
        {
            return false;
        }

        final String url = getUrl( resource );

        if ( ! EXTERNAL_FILTER.matcher( url ).matches() )
        {
            return false;
        }

        final File st = getStateFile( destination, url );

        // Same as StreamWagon#getIfNewer
        if ( 0 == timestamp || st.exists() || timestamp < resource.getLastModified() )
        {
            getTransfer( resource, url, destination );
            return true;
        }
        else
        {
            return false;
        }
    }

}
