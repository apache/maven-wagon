package org.apache.maven.wagon.providers.http;

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
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateParser;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="http"
 *   instantiation-strategy="per-lookup"
 */
public class HttpWagon
    extends AbstractWagon
{
    private static final int DEFAULT_NUMBER_OF_ATTEMPTS = 3;

    private static final int SC_NULL = -1;

    private HttpClient client;

    private int numberOfAttempts = DEFAULT_NUMBER_OF_ATTEMPTS;

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    public void openConnection()
    {
        client = new HttpClient( new MultiThreadedHttpConnectionManager() );

        String username = null;

        String password = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();
        }

        String host = getRepository().getHost();

        if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
        {
            Credentials creds = new UsernamePasswordCredentials( username, password );

            client.getState().setCredentials( null, host, creds );
            client.getState().setAuthenticationPreemptive( true );
        }

        HostConfiguration hc = new HostConfiguration();

        if ( proxyInfo != null )
        {
            String proxyUsername = proxyInfo.getUserName();

            String proxyPassword = proxyInfo.getPassword();

            String proxyHost = proxyInfo.getHost();

            int proxyPort = proxyInfo.getPort();

            String proxyNtlmHost = proxyInfo.getNtlmHost();

            String proxyNtlmDomain = proxyInfo.getNtlmDomain();

            if ( proxyHost != null )
            {
                hc.setProxy( proxyHost, proxyPort );

                if ( proxyUsername != null && proxyPassword != null )
                {
                    Credentials creds;
                    if ( proxyNtlmHost != null || proxyNtlmDomain != null )
                    {
                        creds = new NTCredentials( proxyUsername, proxyPassword, proxyNtlmHost, proxyNtlmDomain );
                    }
                    else
                    {
                        creds = new UsernamePasswordCredentials( proxyUsername, proxyPassword );
                    }

                    client.getState().setProxyCredentials( null, proxyHost, creds );
                    client.getState().setAuthenticationPreemptive( true );
                }
            }
        }

        hc.setHost( host );

        //start a session with the webserver
        client.setHostConfiguration( hc );
    }

    // put
    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resourceName;

        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        PutMethod putMethod = new PutMethod( url );

        try
        {
            InputStream is = new PutInputStream( source, resource, this, getTransferEventSupport() );

            putMethod.setRequestBody( is );
        }
        catch ( FileNotFoundException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new ResourceDoesNotExistException( "Source file does not exist: " + source, e );
        }

        int statusCode = SC_NULL;

        int attempt = 0;

        fireTransferDebug( "about to execute client for put" );

        // We will retry up to NumberOfAttempts times.
        while ( statusCode == SC_NULL && attempt < getNumberOfAttempts() )
        {
            try
            {
                firePutStarted( resource, source );

                statusCode = client.executeMethod( putMethod );
            }
            catch ( HttpRecoverableException e )
            {
                attempt++;
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
            // Success Codes
            case HttpStatus.SC_OK: // 200
            case HttpStatus.SC_CREATED: // 201
            case HttpStatus.SC_ACCEPTED: // 202
            case HttpStatus.SC_NO_CONTENT:  // 204
                break;

            case SC_NULL:
                throw new TransferFailedException(
                    "Failed to transfer file: " + url + " after " + attempt + " attempts" );

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denied to: " + url );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                //add more entries here
            default :
                throw new TransferFailedException(
                    "Failed to transfer file: " + url + ". Return code is: " + statusCode );
        }

        putMethod.releaseConnection();

        firePutCompleted( resource, source );
    }

    public void closeConnection()
    {
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        get( resourceName, destination, 0 );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return get( resourceName, destination, timestamp );
    }

    /**
     * @param resourceName
     * @param destination
     * @param timestamp the timestamp to check against, only downloading if newer. If <code>0</code>, always download
     * @return <code>true</code> if newer version was downloaded, <code>false</code> otherwise.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    public boolean get( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        boolean retValue = false;

        String url = getRepository().getUrl() + "/" + resourceName;

        GetMethod getMethod = new GetMethod( url );

        try
        {
            // TODO: make these configurable

            getMethod.addRequestHeader( "Cache-control", "no-cache" );
            getMethod.addRequestHeader( "Cache-store", "no-store" );
            getMethod.addRequestHeader( "Pragma", "no-cache" );
            getMethod.addRequestHeader( "Expires", "0" );
            getMethod.addRequestHeader( "Accept-Encoding", "gzip" );

            if ( timestamp > 0 )
            {
                SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
                fmt.setTimeZone( GMT_TIME_ZONE );
                Header hdr = new Header( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
                fireTransferDebug( "sending ==> " + hdr + "(" + timestamp + ")" );
                getMethod.addRequestHeader( hdr );
            }

            int statusCode = SC_NULL;

            int attempt = 0;

            // We will retry up to NumberOfAttempts times.
            while ( statusCode == SC_NULL && attempt < getNumberOfAttempts() )
            {
                try
                {
                    // execute the getMethod.
                    statusCode = client.executeMethod( getMethod );
                }
                catch ( HttpRecoverableException e )
                {
                    attempt++;
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( e.getMessage(), e );
                }
            }

            fireTransferDebug( url + " - Status code: " + statusCode );

            // TODO [BP]: according to httpclient docs, really should swallow the output on error. verify if that is required
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_NOT_MODIFIED:
                    return false;

                case SC_NULL:
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + " after " + attempt + " attempts" );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                    //add more entries here
                default :
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode );
            }

            InputStream is = null;

            Header contentLengthHeader = getMethod.getResponseHeader( "Content-Length" );

            if ( contentLengthHeader != null )
            {
                try
                {
                    long contentLength = Integer.valueOf( contentLengthHeader.getValue() ).intValue();

                    resource.setContentLength( contentLength );
                }
                catch ( NumberFormatException e )
                {
                    fireTransferDebug(
                        "error parsing content length header '" + contentLengthHeader.getValue() + "' " + e );
                }
            }

            Header lastModifiedHeader = getMethod.getResponseHeader( "Last-Modified" );

            long lastModified = 0;

            if ( lastModifiedHeader != null )
            {
                try
                {
                    lastModified = DateParser.parseDate( lastModifiedHeader.getValue() ).getTime();
                }
                catch ( DateParseException e )
                {
                    fireTransferDebug( "Unable to parse last modified header" );
                }

                fireTransferDebug( "last-modified = " + lastModifiedHeader.getValue() + " (" + lastModified + ")" );
            }

            // always get if timestamp is 0 (ie, target doesn't exist), otherwise only if older than the remote file
            if ( timestamp == 0 || timestamp < lastModified )
            {
                retValue = true;
                
                Header contentEncoding = getMethod.getResponseHeader( "Content-Encoding" );
                boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding.getValue());

                try
                {
                    is = getMethod.getResponseBodyAsStream();
                    if (isGZipped)
                    {
                        is = new GZIPInputStream( is );
                    }

                    getTransfer( resource, destination, is );
                }
                catch ( IOException e )
                {
                    fireTransferError( resource, e, TransferEvent.REQUEST_GET );

                    if ( destination.exists() )
                    {
                        boolean deleted = destination.delete();

                        if ( !deleted )
                        {
                            destination.deleteOnExit();
                        }
                    }

                    String msg = "Error occurred while deploying to remote repository:" + getRepository();

                    throw new TransferFailedException( msg, e );
                }
                finally
                {
                    IOUtil.close( is );
                }

                if ( lastModified > 0 )
                {
                    resource.setLastModified( lastModified );
                }
            }
            else
            {
                fireTransferDebug( "Local file is newer: not downloaded" );
            }

            return retValue;
        }
        finally
        {
            getMethod.releaseConnection();
        }
    }

    public int getNumberOfAttempts()
    {
        return numberOfAttempts;
    }

    public void setNumberOfAttempts( int numberOfAttempts )
    {
        this.numberOfAttempts = numberOfAttempts;
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException 
    {
        if ( !destinationDirectory.endsWith( "/" ) )
        {
            destinationDirectory += "/";
        }

        String url = getRepository().getUrl() + "/" + destinationDirectory;

        GetMethod getMethod = new GetMethod( url );

        try
        {
            // TODO: make these configurable

            getMethod.addRequestHeader( "Cache-control", "no-cache" );
            getMethod.addRequestHeader( "Cache-store", "no-store" );
            getMethod.addRequestHeader( "Pragma", "no-cache" );
            getMethod.addRequestHeader( "Expires", "0" );

            int statusCode = SC_NULL;

            int attempt = 0;

            // We will retry up to NumberOfAttempts times.
            while ( statusCode == SC_NULL && attempt < getNumberOfAttempts() )
            {
                try
                {
                    // execute the getMethod.
                    statusCode = client.executeMethod( getMethod );
                }
                catch ( HttpRecoverableException e )
                {
                    attempt++;
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( e.getMessage(), e );
                }
            }

            fireTransferDebug( url + " - Status code: " + statusCode );

            // TODO [BP]: according to httpclient docs, really should swallow the output on error. verify if that is required
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case SC_NULL:
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + " after " + attempt + " attempts" );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                    //add more entries here
                default :
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode );
            }

            InputStream is = null;
            
            is = getMethod.getResponseBodyAsStream();

            return HtmlFileListParser.parseFileList( url, is );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Could not read response body.", e );
        }
        finally
        {
            getMethod.releaseConnection();
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resourceName;
        
        HeadMethod headMethod = new HeadMethod( url );
        
        int statusCode = SC_NULL;
        int attempt = 0;
        
        try
        {
            while ( statusCode == SC_NULL && attempt < getNumberOfAttempts() )
            {
                try
                {
                    // execute the getMethod.
                    statusCode = client.executeMethod( headMethod );
                }
                catch ( HttpRecoverableException e )
                {
                    attempt++;
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( e.getMessage(), e );
                }
            }

            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    return true;

                case HttpStatus.SC_NOT_MODIFIED:
                    return true;

                case SC_NULL:
                    throw new TransferFailedException( "Failed to transfer file: " + url + " after " + attempt
                        + " attempts" );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    return false;

                    //add more entries here
                default:
                    throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                        + statusCode );
            }
        }
        finally
        {
            headMethod.releaseConnection();
        }
    }
}
