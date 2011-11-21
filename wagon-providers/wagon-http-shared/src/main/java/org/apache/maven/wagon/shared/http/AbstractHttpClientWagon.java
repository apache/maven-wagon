package org.apache.maven.wagon.shared.http;

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
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public abstract class AbstractHttpClientWagon
    extends StreamWagon
{
    private final class RequestEntityImplementation
        implements RequestEntity
    {
        private final Resource resource;

        private final Wagon wagon;

        private final File source;

        private RequestEntityImplementation( final InputStream stream, final Resource resource, final Wagon wagon,
                                             final File source )
            throws TransferFailedException
        {
            if ( source != null )
            {
                this.source = source;
            }
            else
            {
                FileOutputStream fos = null;
                try
                {
                    this.source = File.createTempFile( "http-wagon.", ".tmp" );
                    this.source.deleteOnExit();

                    fos = new FileOutputStream( this.source );
                    IOUtil.copy( stream, fos );
                }
                catch ( IOException e )
                {
                    fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                    throw new TransferFailedException( "Failed to buffer stream contents to temp file for upload.", e );
                }
                finally
                {
                    IOUtil.close( fos );
                }
            }

            this.resource = resource;
            this.wagon = wagon;
        }

        public long getContentLength()
        {
            return resource.getContentLength();
        }

        public String getContentType()
        {
            return null;
        }

        public boolean isRepeatable()
        {
            return true;
        }

        public void writeRequest( OutputStream output )
            throws IOException
        {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

            TransferEvent transferEvent =
                new TransferEvent( wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT );
            transferEvent.setTimestamp( System.currentTimeMillis() );

            FileInputStream fin = null;
            try
            {
                fin = new FileInputStream( source );
                int remaining = Integer.MAX_VALUE;
                while ( remaining > 0 )
                {
                    int n = fin.read( buffer, 0, Math.min( buffer.length, remaining ) );

                    if ( n == -1 )
                    {
                        break;
                    }

                    fireTransferProgress( transferEvent, buffer, n );

                    output.write( buffer, 0, n );

                    remaining -= n;
                }
            }
            finally
            {
                IOUtil.close( fin );
            }

            output.flush();
        }
    }

    protected static final int SC_NULL = -1;

    protected static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private HttpClient client;

    protected HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

    /**
     * @deprecated Use httpConfiguration instead.
     */
    private Properties httpHeaders;

    /**
     * @since 1.0-beta-6
     */
    private HttpConfiguration httpConfiguration;

    private HttpMethod getMethod;

    public void openConnectionInternal()
    {
        repository.setUrl( getURL( repository ) );
        client = new HttpClient( connectionManager );

        // WAGON-273: default the cookie-policy to browser compatible
        client.getParams().setCookiePolicy( CookiePolicy.BROWSER_COMPATIBILITY );

        String username = null;
        String password = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();

            client.getParams().setAuthenticationPreemptive( true );
        }

        String host = getRepository().getHost();

        if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
        {
            Credentials creds = new UsernamePasswordCredentials( username, password );

            int port = getRepository().getPort() > -1 ? getRepository().getPort() : AuthScope.ANY_PORT;

            AuthScope scope = new AuthScope( host, port );
            client.getState().setCredentials( scope, creds );
        }

        HostConfiguration hc = new HostConfiguration();

        ProxyInfo proxyInfo = getProxyInfo( getRepository().getProtocol(), getRepository().getHost() );
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

                    int port = proxyInfo.getPort() > -1 ? proxyInfo.getPort() : AuthScope.ANY_PORT;

                    AuthScope scope = new AuthScope( proxyHost, port );
                    client.getState().setProxyCredentials( scope, creds );
                }
            }
        }

        hc.setHost( host );

        //start a session with the webserver
        client.setHostConfiguration( hc );
    }

    public void closeConnection()
    {
        if ( connectionManager instanceof MultiThreadedHttpConnectionManager )
        {
            ( (MultiThreadedHttpConnectionManager) connectionManager ).shutdown();
        }
    }

    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        firePutInitiated( resource, source );

        resource.setContentLength( source.length() );

        resource.setLastModified( source.lastModified() );

        put( null, resource, source );
    }

    public void putFromStream( final InputStream stream, String destination, long contentLength, long lastModified )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( destination );

        firePutInitiated( resource, null );

        resource.setContentLength( contentLength );

        resource.setLastModified( lastModified );

        put( stream, resource, null );
    }

    private void put( final InputStream stream, Resource resource, File source )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        StringBuilder url = new StringBuilder( getRepository().getUrl() );
        String[] parts = StringUtils.split( resource.getName(), "/" );
        for ( String part : parts )// int i = 0; i < parts.length; i++ )
        {
            // TODO: Fix encoding...
            // url += "/" + URLEncoder.encode( parts[i], System.getProperty("file.encoding") );
            if ( !url.toString().endsWith( "/" ) )
            {
                url.append( '/' );
            }
            url.append( URLEncoder.encode( part ) );
        }

        //Parent directories need to be created before posting
        try
        {
            mkdirs( PathUtils.dirname( resource.getName() ) );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
        }

        PutMethod putMethod = new PutMethod( url.toString() );

        firePutStarted( resource, source );

        try
        {
            putMethod.setRequestEntity( new RequestEntityImplementation( stream, resource, this, source ) );

            int statusCode;
            try
            {
                statusCode = execute( putMethod );
            }
            catch ( IOException e )
            {
                fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

                throw new TransferFailedException( e.getMessage(), e );
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
                {
                    TransferFailedException e = new TransferFailedException( "Failed to transfer file: " + url );
                    fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                    throw e;
                }

                case HttpStatus.SC_FORBIDDEN:
                    fireSessionConnectionRefused();
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                    //add more entries here
                default:
                {
                    TransferFailedException e = new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode );
                    fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                    throw e;
                }
            }

            firePutCompleted( resource, source );
        }
        finally
        {
            putMethod.releaseConnection();
        }
    }

    protected void mkdirs( String dirname )
        throws IOException
    {
        // do nothing as default.
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        StringBuilder url = new StringBuilder( getRepository().getUrl() );
        if ( !url.toString().endsWith( "/" ) )
        {
            url.append( '/' );
        }
        url.append( resourceName );
        HeadMethod headMethod = new HeadMethod( url.toString() );
        int statusCode;
        try
        {
            statusCode = execute( headMethod );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
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
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode );
            }
        }
        finally
        {
            headMethod.releaseConnection();
        }
    }

    protected int execute( HttpMethod httpMethod )
        throws IOException
    {
        int statusCode;

        setParameters( httpMethod );
        setHeaders( httpMethod );

        statusCode = client.executeMethod( httpMethod );
        return statusCode;
    }

    protected void setParameters( HttpMethod method )
    {
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config != null )
        {
            HttpMethodParams params = config.asMethodParams( method.getParams() );
            if ( params != null )
            {
                method.setParams( params );
            }
        }

        if ( config == null || config.getConnectionTimeout() == HttpMethodConfiguration.DEFAULT_CONNECTION_TIMEOUT )
        {
            method.getParams().setSoTimeout( getTimeout() );
        }
    }

    protected void setHeaders( HttpMethod method )
    {
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config == null || config.isUseDefaultHeaders() )
        {
            // TODO: merge with the other headers and have some better defaults, unify with lightweight headers
            method.addRequestHeader( "Cache-control", "no-cache" );
            method.addRequestHeader( "Cache-store", "no-store" );
            method.addRequestHeader( "Pragma", "no-cache" );
            method.addRequestHeader( "Expires", "0" );
            method.addRequestHeader( "Accept-Encoding", "gzip" );
        }

        if ( httpHeaders != null )
        {
            for ( Iterator i = httpHeaders.keySet().iterator(); i.hasNext(); )
            {
                String header = (String) i.next();
                method.addRequestHeader( header, httpHeaders.getProperty( header ) );
            }
        }

        Header[] headers = config == null ? null : config.asRequestHeaders();
        if ( headers != null )
        {
            for ( int i = 0; i < headers.length; i++ )
            {
                method.addRequestHeader( headers[i] );
            }
        }
    }

    /**
     * getUrl
     * Implementors can override this to remove unwanted parts of the url such as role-hints
     *
     * @param repository
     * @return
     */
    protected String getURL( Repository repository )
    {
        return repository.getUrl();
    }

    protected HttpClient getClient()
    {
        return client;
    }

    public void setConnectionManager( HttpConnectionManager connectionManager )
    {
        this.connectionManager = connectionManager;
    }

    public Properties getHttpHeaders()
    {
        return httpHeaders;
    }

    public void setHttpHeaders( Properties httpHeaders )
    {
        this.httpHeaders = httpHeaders;
    }

    public HttpConfiguration getHttpConfiguration()
    {
        return httpConfiguration;
    }

    public void setHttpConfiguration( HttpConfiguration httpConfiguration )
    {
        this.httpConfiguration = httpConfiguration;
    }

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = inputData.getResource();

        StringBuilder url = new StringBuilder( getRepository().getUrl() );
        if ( !url.toString().endsWith( "/" ) )
        {
            url.append( '/' );
        }
        url.append( resource.getName() );

        getMethod = new GetMethod( url.toString() );
        long timestamp = resource.getLastModified();
        if ( timestamp > 0 )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
            fmt.setTimeZone( GMT_TIME_ZONE );
            Header hdr = new Header( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
            fireTransferDebug( "sending ==> " + hdr + "(" + timestamp + ")" );
            getMethod.addRequestHeader( hdr );
        }

        int statusCode;
        try
        {
            statusCode = execute( getMethod );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( e.getMessage(), e );
        }

        fireTransferDebug( url + " - Status code: " + statusCode );

        // TODO [BP]: according to httpclient docs, really should swallow the output on error. verify if that is
        // required
        switch ( statusCode )
        {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_MODIFIED:
                // return, leaving last modified set to original value so getIfNewer should return unmodified
                return;

            case SC_NULL:
            {
                TransferFailedException e = new TransferFailedException( "Failed to transfer file: " + url );
                fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                throw e;
            }

            case HttpStatus.SC_FORBIDDEN:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Access denied to: " + url );

            case HttpStatus.SC_UNAUTHORIZED:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Not authorized." );

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Not authorized by proxy." );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                // add more entries here
            default:
            {
                cleanupGetTransfer( resource );
                TransferFailedException e = new TransferFailedException(
                    "Failed to transfer file: " + url + ". Return code is: " + statusCode );
                fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                throw e;
            }
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
                lastModified = DateUtil.parseDate( lastModifiedHeader.getValue() ).getTime();

                resource.setLastModified( lastModified );
            }
            catch ( DateParseException e )
            {
                fireTransferDebug( "Unable to parse last modified header" );
            }

            fireTransferDebug( "last-modified = " + lastModifiedHeader.getValue() + " (" + lastModified + ")" );
        }

        Header contentEncoding = getMethod.getResponseHeader( "Content-Encoding" );
        boolean isGZipped = contentEncoding != null && "gzip".equalsIgnoreCase( contentEncoding.getValue() );

        try
        {
            is = getMethod.getResponseBodyAsStream();
            if ( isGZipped )
            {
                is = new GZIPInputStream( is );
            }
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            String msg =
                "Error occurred while retrieving from remote repository:" + getRepository() + ": " + e.getMessage();

            throw new TransferFailedException( msg, e );
        }

        inputData.setInputStream( is );
    }

    protected void cleanupGetTransfer( Resource resource )
    {
        if ( getMethod != null )
        {
            getMethod.releaseConnection();
        }
    }

    @Override
    public void putFromStream( InputStream stream, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        putFromStream( stream, destination, -1, -1 );
    }

    @Override
    protected void putFromStream( InputStream stream, Resource resource )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        putFromStream( stream, resource.getName( ), -1, -1 );
    }

    @Override
    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        // no needed in this implementation but throw an Exception if used
        throw new IllegalStateException( "this wagon http client must not use fillOutputData" );
    }
}
