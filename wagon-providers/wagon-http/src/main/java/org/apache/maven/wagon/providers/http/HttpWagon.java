package org.apache.maven.wagon.providers.http;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class HttpWagon
    extends AbstractWagon
{
    private final static int DEFAULT_NUMBER_OF_ATTEMPTS = 3;

    private final static int SC_NULL = -1;

    private HttpClient client = null;

    private int numberOfAttempts = DEFAULT_NUMBER_OF_ATTEMPTS;

    public void openConnection()
    {
        client = new HttpClient( new MultiThreadedHttpConnectionManager() );

        final AuthenticationInfo authInfo = getRepository().getAuthenticationInfo();

        String username = null;

        String password = null;

        if ( authInfo != null )
        {
            username = authInfo.getUserName();

            password = authInfo.getPassword();
        }

        String host = getRepository().getHost();

        if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
        {
            Credentials creds = new UsernamePasswordCredentials( username, password );

            client.getState().setCredentials( null, host, creds );
        }

        HostConfiguration hc = new HostConfiguration();

        ProxyInfo proxyInfo = getRepository().getProxyInfo();

        if ( proxyInfo != null )
        {
            String proxyUsername = proxyInfo.getUserName();

            String proxyPassword = proxyInfo.getPassword();

            String proxyHost = proxyInfo.getHost();

            if ( StringUtils.isNotEmpty( proxyUsername )
                && StringUtils.isNotEmpty( proxyPassword )
                && StringUtils.isNotEmpty( proxyHost ) )
            {
                Credentials creds = new UsernamePasswordCredentials( username, password );

                client.getState().setProxyCredentials( null, proxyHost, creds );
            }
        }

        hc.setHost( host );

        //start a session with the webserver
        client.setHostConfiguration( hc );
    }

    // put
    public void put( File source, String resource )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resource;

        PutMethod putMethod = new PutMethod( url );

        try
        {

            InputStream is = new PutInputStream( source, resource, this, getTransferEventSupport()  );

            putMethod.setRequestBody( is );
        }
        catch ( FileNotFoundException e )
        {
            fireTransferError( resource, e );

            throw new ResourceDoesNotExistException( "Source file does not exist: " + source, e );
        }

        int statusCode = SC_NULL;

        int attempt = 0;

        fireTransferDebug( "about to execute client for put" );

        // We will retry up to NumberOfAttempts times.
        while ( ( statusCode == SC_NULL ) && ( attempt < getNumberOfAttempts() ) )
        {
            try
            {
                firePutStarted( resource, source );

                statusCode = client.executeMethod( putMethod );

                firePutCompleted( resource, source );

            }
            catch ( HttpRecoverableException e )
            {
                attempt++;

                continue;
            }
            catch ( IOException e )
            {
                throw new TransferFailedException( e.getMessage(), e );
            }
        }

        fireTransferDebug( url + " - Status code: " + statusCode );

        // Check that we didn't run out of retries.
        switch ( statusCode )
        {
            case HttpStatus.SC_OK:
                break;

             case HttpStatus.SC_CREATED:
                break;

            case SC_NULL:
                throw new ResourceDoesNotExistException( "File: " + url + " does not extist" );

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denided to: " + url );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                //add more entries here
            default :
                throw new TransferFailedException( "Failed to trasfer file: " + url + ". Return code is: " + statusCode );
        }

        putMethod.releaseConnection();

        firePutCompleted( resource, source );
    }

    public void closeConnection()
    {
    }

    public void get( String resource, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resource;

        GetMethod getMethod = new GetMethod( url );

        getMethod.addRequestHeader( "Cache-control", "no-cache" );

        getMethod.addRequestHeader( "Cache-store", "no-store" );

        getMethod.addRequestHeader( "Pragma", "no-cache" );

        getMethod.addRequestHeader( "Expires", "0" );


        int statusCode = SC_NULL;

        int attempt = 0;

        // We will retry up to NumberOfAttempts times.
        while ( ( statusCode == SC_NULL ) && ( attempt < getNumberOfAttempts() ) )
        {
            try
            {
                // execute the getMethod.
                statusCode = client.executeMethod( getMethod );
            }
            catch ( HttpRecoverableException e )
            {
                attempt++;

                continue;
            }
            catch ( IOException e )
            {
                throw new TransferFailedException( e.getMessage(), e );
            }
        }

        fireTransferDebug( url + " - Status code: " + statusCode );

        // Check that we didn't run out of retries.
        switch ( statusCode )
        {
            case HttpStatus.SC_OK:
                break;

            case SC_NULL:
                throw new ResourceDoesNotExistException( "File: " + url + " does not extist" );

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denided to: " + url );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                //add more entries here
            default :
                throw new TransferFailedException( "Failed to trasfer file: "
                                                   + url
                                                   + ". Return code is: "
                                                   + statusCode );
        }

        OutputStream os = null;

        InputStream is = null;

        File dir = destination.getParentFile();

        if ( dir != null   && !dir.exists() )
        {

            if ( ! dir.mkdirs() )
            {
                String msg = "Some of the required local directories do not exist and could not be created. " +
                		"Requested local path:  "  + destination.getAbsolutePath();

                throw new TransferFailedException( msg );
            }

        }


        try
        {
            os = new LazyFileOutputStream( destination );

            is = getMethod.getResponseBodyAsStream();

            getTransfer( resource, destination, is, os);
        }
        catch ( Exception e )
        {
            fireTransferError( source.getName(), e );

            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( ! deleted )
                {
                    destination.deleteOnExit();
                }
            }

            String msg = "Error occured while deploying to remote repository:" + getRepository();


            throw new TransferFailedException( msg, e );
        }
        finally
        {
            shutdownStream( is );

            shutdownStream( os );
        }

        getMethod.releaseConnection();
    }

    public int getNumberOfAttempts()
    {
        return numberOfAttempts;
    }

    public void setNumberOfAttempts( int numberOfAttempts )
    {
        this.numberOfAttempts = numberOfAttempts;
    }
}
