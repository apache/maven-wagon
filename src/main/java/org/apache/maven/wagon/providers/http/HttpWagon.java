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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateParser;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.http.dav.DavResource;
import org.apache.maven.wagon.providers.http.dav.MkColMethod;
import org.apache.maven.wagon.providers.http.dav.MultiStatus;
import org.apache.maven.wagon.providers.http.dav.PropFindMethod;
import org.apache.maven.wagon.providers.http.links.LinkParser;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.xml.sax.SAXException;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class HttpWagon
    extends AbstractWagon
{
    private static final int SC_NULL = -1;

    private HttpClient client;

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private HttpConnectionManager connectionManager;

    private LinkParser linkParser = new LinkParser();

    protected boolean isDav;

    public void openConnection()
    {
        client = new HttpClient( connectionManager );

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

        // Try simple put first (works 90% of the time) 
        int statusCode = putSource( url, source, resource );
        if ( isSuccessfulPUT( statusCode ) )
        {
            // expected path.
            return;
        }

        // Problem. Check that the collections exist.
        if ( ( isDav ) && ( statusCode == HttpStatus.SC_CONFLICT ) )
        {
            URI absoluteURI = toURI( url );
            URI parentURI = absoluteURI.resolve( "./" ).normalize();
            Stack/*<URI>*/missingPaths = davGetMissingPaths( parentURI );

            if ( missingPaths.empty() )
            {
                throw new TransferFailedException( "Unable to put (Conflict, collections exist) file "
                    + source.getAbsolutePath() + " to " + absoluteURI.toASCIIString() );
            }

            while ( !missingPaths.empty() )
            {
                URI missingPath = (URI) missingPaths.pop();
                davCreateCollection( missingPath );
            }

            statusCode = putSource( url, source, resource );
            if ( isSuccessfulPUT( statusCode ) )
            {
                // Expected Good result.
                return;
            }
        }

        throw new TransferFailedException( "Unable to upload (" + statusCode + "/"
            + HttpStatus.getStatusText( statusCode ) + ") file " + source.getAbsolutePath() + " to " + url );
    }

    /**
     * HTTP RFC 2616 section 9.6 "PUT": "If an existing resource is modified,
     * either the 200 (OK) or 204 (No Content) response codes SHOULD be sent
     * to indicate successful completion of the request." 
     */
    private boolean isSuccessfulPUT( int status )
    {
        return ( ( status == HttpStatus.SC_CREATED ) || ( status == HttpStatus.SC_OK ) || ( status == HttpStatus.SC_NO_CONTENT ) );
    }

    private int putSource( String url, File source, Resource resource )
        throws ResourceDoesNotExistException, TransferFailedException, AuthorizationException
    {
        firePutInitiated( resource, source );

        PutMethod putMethod = new PutMethod( url );
        putMethod.getParams().setSoTimeout( getTimeout() );

        // TODO: worry about setting the Mime-Type on the request header.

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

        int statusCode = execute( putMethod );

        fireTransferDebug( url + " - Status code: " + statusCode );

        // Check that we didn't run out of retries.
        switch ( statusCode )
        {
            // Success Codes
            /**
             * HTTP RFC 2616 section 9.6 "PUT": "If an existing resource is modified,
             * either the 200 (OK) or 204 (No Content) response codes SHOULD be sent
             * to indicate successful completion of the request." 
             */
            case HttpStatus.SC_OK: // 200
            case HttpStatus.SC_CREATED: // 201
            case HttpStatus.SC_ACCEPTED: // 202
            case HttpStatus.SC_NO_CONTENT: // 204
                break;

            /* 409/Conflict is usually seen when attempting to PUT on a 
             * WebDAV server without the parent Collections existing first.
             * 
             * We want to exit out of this and allow the put() routine to 
             * manage the creation of the collections.
             */
            case HttpStatus.SC_CONFLICT: // 409 
                break;

            case SC_NULL:
                throw new TransferFailedException( "Failed to transfer file: " + url );

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denied to: " + url );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                //add more entries here
            default:
                throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                    + statusCode );
        }

        putMethod.releaseConnection();

        firePutCompleted( resource, source );

        return statusCode;
    }

    public void davCreateCollection( URI uri )
        throws TransferFailedException
    {
        MkColMethod method = new MkColMethod( uri );
        try
        {
            int status = client.executeMethod( method );
            if ( status == HttpStatus.SC_CREATED )
            {
                return;
            }

            throw new TransferFailedException( "Unable to create collection (" + status + "/"
                + HttpStatus.getStatusText( status ) + "): " + uri.toASCIIString() );
        }
        catch ( HttpException e )
        {
            throw new TransferFailedException( "Unable to create collection: " + uri.toASCIIString(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unable to create collection: " + uri.toASCIIString(), e );
        }
        finally
        {
            method.releaseConnection();
        }
    }

    /**
     * Collect the stack of missing paths.
     * 
     * @param absoluteURI the abosoluteURI to start from.
     * @return
     * @throws TransferFailedException 
     */
    public Stack/*<URI>*/davGetMissingPaths( URI targetURI )
        throws TransferFailedException
    {
        URI baseuri = toURI( getRepository().getUrl() );

        Stack/*<URI>*/missingPaths = new Stack/*<URI>*/();
        URI currentURI = targetURI;
        if ( !currentURI.getPath().endsWith( "/" ) )
        {
            try
            {
                currentURI = new URI( targetURI.toASCIIString() + "/" );
            }
            catch ( URISyntaxException e )
            {
                fireTransferDebug( "Should never happen: " + e.getMessage() );
            }
        }

        boolean done = false;
        while ( !done )
        {
            if ( targetURI.equals( baseuri ) )
            {
                done = true;
                break;
            }

            if ( davCollectionExists( currentURI ) )
            {
                done = true;
                break;
            }

            missingPaths.push( currentURI );

            currentURI = currentURI.resolve( "../" ).normalize();
        }

        return missingPaths;
    }

    public boolean davCollectionExists( URI uri )
        throws TransferFailedException
    {
        PropFindMethod method = new PropFindMethod( uri );
        try
        {
            method.setDepth( 1 );
            int status = client.executeMethod( method );
            if ( ( status == HttpStatus.SC_MULTI_STATUS ) || ( status == HttpStatus.SC_OK ) )
            {
                MultiStatus multistatus = method.getMultiStatus();
                if ( multistatus == null )
                {
                    return false;
                }

                DavResource resource = multistatus.getResource( uri.getPath() );
                if ( resource == null )
                {
                    return false;
                }

                return resource.isCollection();
            }

            if ( status == HttpStatus.SC_BAD_REQUEST )
            {
                throw new TransferFailedException( "Bad HTTP Request (400) during PROPFIND on \"" + uri.toASCIIString()
                    + "\"" );
            }
        }
        catch ( HttpException e )
        {
            fireTransferDebug( "Can't determine if collection exists: " + uri + " : " + e.getMessage() );
        }
        catch ( IOException e )
        {
            fireTransferDebug( "Can't determine if collection exists: " + uri + " : " + e.getMessage() );
        }
        finally
        {
            method.releaseConnection();
        }
        return false;
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
        getMethod.getParams().setSoTimeout( getTimeout() );

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

            int statusCode = execute( getMethod );

            fireTransferDebug( url + " - Status code: " + statusCode );

            // TODO [BP]: according to httpclient docs, really should swallow the output on error. verify if that is required
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_NOT_MODIFIED:
                    return false;

                case SC_NULL:
                    throw new TransferFailedException( "Failed to transfer file: " + url );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                    //add more entries here
                default:
                    throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                        + statusCode );
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
                    fireTransferDebug( "error parsing content length header '" + contentLengthHeader.getValue() + "' "
                        + e );
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
                boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase( contentEncoding
                    .getValue() );

                try
                {
                    is = getMethod.getResponseBodyAsStream();
                    if ( isGZipped )
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

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !destinationDirectory.endsWith( "/" ) )
        {
            destinationDirectory += "/";
        }

        String url = getRepository().getUrl() + "/" + destinationDirectory;

        if ( isDav )
        {
            return getDavListing( url );
        }
        else
        {
            return getHttpListing( url );
        }
    }

    private List getDavListing( String url )
        throws TransferFailedException
    {
        URI absoluteURI = toURI( url );

        PropFindMethod method = new PropFindMethod( absoluteURI );
        try
        {
            int status = client.executeMethod( method );
            if ( ( status == HttpStatus.SC_MULTI_STATUS ) || ( status == HttpStatus.SC_OK ) )
            {
                MultiStatus multistatus = method.getMultiStatus();
                Set/*<String>*/listing = new HashSet/*<String>*/();

                Iterator itresources = multistatus.getResources().iterator();
                while ( itresources.hasNext() )
                {
                    DavResource resource = (DavResource) itresources.next();
                    if ( resource.isCollection() )
                    {
                        continue;
                    }
                    String href = resource.getHref();

                    int idx = href.lastIndexOf( '/' );
                    if ( idx < 0 )
                    {
                        listing.add( href );
                    }
                    else
                    {
                        listing.add( href.substring( idx + 1 ) );
                    }
                }

                return new ArrayList( listing );
            }

            return Collections.emptyList();
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Could not read response body.", e );
        }
        finally
        {
            method.releaseConnection();
        }

    }

    private List getHttpListing( String url )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        URI absoluteURI = toURI( url );

        GetMethod getMethod = new GetMethod( url );
        getMethod.getParams().setSoTimeout( getTimeout() );

        try
        {
            // TODO: make these configurable

            getMethod.addRequestHeader( "Cache-control", "no-cache" );
            getMethod.addRequestHeader( "Cache-store", "no-store" );
            getMethod.addRequestHeader( "Pragma", "no-cache" );
            getMethod.addRequestHeader( "Expires", "0" );

            int statusCode = execute( getMethod );

            fireTransferDebug( url + " - Status code: " + statusCode );

            // TODO [BP]: according to httpclient docs, really should swallow the output on error. verify if that is required
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case SC_NULL:
                    throw new TransferFailedException( "Failed to transfer file: " );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                    //add more entries here
                default:
                    throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                        + statusCode );
            }

            InputStream is = null;

            is = getMethod.getResponseBodyAsStream();

            Set/*<String>*/links = linkParser.collectLinks( absoluteURI, is );

            return new ArrayList( links );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Could not read response body.", e );
        }
        catch ( SAXException e )
        {
            throw new TransferFailedException( "Could not parse response body.", e );
        }
        finally
        {
            getMethod.releaseConnection();
        }
    }

    private URI toURI( String url )
        throws TransferFailedException
    {
        try
        {
            return new URI( url );
        }
        catch ( URISyntaxException e )
        {
            throw new TransferFailedException( "Invalid uri: " + url );
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resourceName;

        HeadMethod headMethod = new HeadMethod( url );
        headMethod.getParams().setSoTimeout( getTimeout() );

        int statusCode = execute( headMethod );

        try
        {
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    return true;

                case HttpStatus.SC_NOT_MODIFIED:
                    return true;

                case SC_NULL:
                    throw new TransferFailedException( "Failed to transfer file: " + url );

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

    private int execute( HttpMethod httpMethod )
        throws TransferFailedException
    {
        int statusCode = SC_NULL;
        try
        {
            // execute the method.
            statusCode = client.executeMethod( httpMethod );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        return statusCode;
    }

    public void setConnectionManager( HttpConnectionManager connectionManager )
    {
        this.connectionManager = connectionManager;
    }

    public boolean isWebDavCapableServer()
    {
        String baseurl = getRepository().getUrl() + "/";

        OptionsMethod method = new OptionsMethod( baseurl );
        try
        {
            int status = client.executeMethod( method );
            if ( status == HttpStatus.SC_OK )
            {
                // Test DAV Header Options
                boolean supportsDav1 = false;

                Header davOptionHeader = method.getResponseHeader( "dav" );
                if ( davOptionHeader != null )
                {
                    String davSupport[] = StringUtils.split( davOptionHeader.getValue(), "," );
                    for ( int i = 0; i < davSupport.length; i++ )
                    {
                        String support = davSupport[i].trim();
                        support = support.trim();
                        if ( "1".equals( support ) )
                        {
                            supportsDav1 = true;
                        }
                    }
                }

                if ( !supportsDav1 )
                {
                    fireTransferDebug( "No DAV Support: " + baseurl );
                    return false;
                }

                // Not validate.
                String requiredMethods[] = new String[] { "HEAD", "GET", "MKCOL", "PROPFIND", "PUT", "OPTIONS" };

                boolean supportsRequired = true;
                for ( int i = 0; i < requiredMethods.length; i++ )
                {
                    String required = requiredMethods[i];
                    if ( !method.isAllowed( required ) )
                    {
                        fireTransferDebug( "No " + required + " Support: " + baseurl );
                        supportsRequired = false;
                    }
                }

                return supportsRequired;
            }
        }
        catch ( HttpException e )
        {
            fireTransferDebug( "Unable to get OPTIONS from " + baseurl + " : " + e.getMessage() );
        }
        catch ( IOException e )
        {
            fireTransferDebug( "Unable to get OPTIONS from " + baseurl + " : " + e.getMessage() );
        }
        finally
        {
            method.releaseConnection();
        }

        return false;
    }

}
