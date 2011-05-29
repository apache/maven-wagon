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

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
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
import java.util.Locale;
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
        implements HttpEntity
    {
        private final Resource resource;

        private final Wagon wagon;

        private final File source;


        private RequestEntityImplementation( final InputStream stream, final Resource resource, final Wagon wagon, final File source )
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

        public Header getContentType()
        {
            return null;
        }

        public Header getContentEncoding() {
            return null;  //X TODO
        }

        public InputStream getContent() throws IOException, IllegalStateException {
            FileInputStream fis = new FileInputStream( source );

            return fis;
        }

        public boolean isRepeatable()
        {
            return true;
        }

        public boolean isChunked() {
            return false;  //X TODO
        }

        public void writeTo( OutputStream output )
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

        public boolean isStreaming() {
            return false;  //X TODO
        }

        public void consumeContent() throws IOException {
            //X TODO
        }
    }

    protected static final int SC_NULL = -1;

    protected static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private DefaultHttpClient client;

    protected ClientConnectionManager connectionManager = new SingleClientConnManager();

    /**
     * @since 1.0-beta-6
     */
    private HttpConfiguration httpConfiguration;

    private HttpGet getMethod;

    public void openConnectionInternal()
    {
        repository.setUrl( getURL( repository ) );
        client = new DefaultHttpClient( connectionManager );
        String username = null;
        String password = null;

        if ( authenticationInfo != null )
        {
            username = authenticationInfo.getUserName();

            password = authenticationInfo.getPassword();
        }

        if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
        {
            Credentials creds = new UsernamePasswordCredentials( username, password );

            String host = getRepository().getHost();
            int port = getRepository().getPort() > -1 ? getRepository().getPort() : AuthScope.ANY_PORT;

            client.getCredentialsProvider().setCredentials( new AuthScope( host, port ), creds );
        }

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
                HttpHost proxy = new HttpHost(proxyHost, proxyPort);

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
                    
                    AuthScope authScope = new AuthScope( proxyHost, port );
                    client.getCredentialsProvider().setCredentials( authScope, creds );
                }

                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
        }

        /*X original
        hc.setHost( host );

        //start a session with the webserver
        client.setHostConfiguration( hc );
        */
    }

    public void closeConnection()
    {
        connectionManager.shutdown();

        /*X original
        if ( connectionManager instanceof MultiThreadedHttpConnectionManager )
        {
            ( (MultiThreadedHttpConnectionManager) connectionManager ).shutdown();
        }
        */
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
        String url = getRepository().getUrl();
        String[] parts = StringUtils.split( resource.getName(), "/" );
        for ( int i = 0; i < parts.length; i++ )
        {
            // TODO: Fix encoding...
            // url += "/" + URLEncoder.encode( parts[i], System.getProperty("file.encoding") );
            url += "/" + URLEncoder.encode( parts[i] );
        }

        //Parent directories need to be created before posting
        try
        {
            mkdirs( PathUtils.dirname( resource.getName() ) );
        }
        catch ( HttpException he )
        {
            fireTransferError( resource, he, TransferEvent.REQUEST_GET );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
        }

        HttpPut putMethod = new HttpPut( url );

        firePutStarted( resource, source );
                
        try
        {
            putMethod.setEntity(new RequestEntityImplementation(stream, resource, this, source));

            HttpResponse response;
            try
            {
                response = execute( putMethod );
            }
            catch ( IOException e )
            {
                fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

                throw new TransferFailedException( e.getMessage(), e );
            }
            catch (HttpException e) {
                fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

                throw new TransferFailedException( e.getMessage(), e );
            }

            int statusCode = response.getStatusLine().getStatusCode();

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
                default :
                {
                    TransferFailedException e =
                            new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                                + statusCode );
                    fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                    throw e;
                }
            }

            firePutCompleted( resource, source );
        }
        finally
        {
            putMethod.abort();
        }
    }
    
    protected void mkdirs( String dirname ) throws HttpException, IOException
    {
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String url = getRepository().getUrl() + "/" + resourceName;
        HttpHead headMethod = new HttpHead( url );
        HttpResponse response = null;
        int statusCode;
        try
        {
            response = execute( headMethod );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        catch (HttpException e) {
            throw new TransferFailedException( e.getMessage(), e );
        }

        try
        {
            statusCode = response.getStatusLine().getStatusCode();
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
            headMethod.abort();
        }
    }

    protected HttpResponse execute( HttpUriRequest httpMethod ) throws HttpException, IOException
    {
        int statusCode = SC_NULL;
        
        setParameters( httpMethod );
        setHeaders( httpMethod );
        
        return client.execute(httpMethod);
    }

    protected void setParameters( HttpUriRequest method )
    {
        HttpMethodConfiguration config = httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config != null )
        {
            HttpParams params = config.asMethodParams( method.getParams() );
            if ( params != null )
            {
                method.setParams( params );
            }
        }
        
        if ( config == null || config.getConnectionTimeout() == HttpMethodConfiguration.DEFAULT_CONNECTION_TIMEOUT )
        {
            method.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, getTimeout() );
        }
    }

    protected void setHeaders( HttpUriRequest method )
    {
        HttpMethodConfiguration config = httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config == null || config.isUseDefaultHeaders() )
        {
            // TODO: merge with the other headers and have some better defaults, unify with lightweight headers
            method.addHeader("Cache-control", "no-cache");
            method.addHeader("Cache-store", "no-store");
            method.addHeader("Pragma", "no-cache");
            method.addHeader("Expires", "0");
            method.addHeader("Accept-Encoding", "gzip");
        }
        
        Header[] headers = config == null ? null : config.asRequestHeaders();
        if ( headers != null )
        {
            for ( int i = 0; i < headers.length; i++ )
            {
                method.addHeader(headers[i]);
            }
        }
    }

    /**
     * getUrl
     * Implementors can override this to remove unwanted parts of the url such as role-hints
     * @param repository
     * @return
     */
    protected String getURL( Repository repository )
    {
        return repository.getUrl();
    }

/*X original
    protected HttpClient getClient()
    {
        return client;
    }

    public void setConnectionManager( HttpConnectionManager connectionManager )
    {
        this.connectionManager = connectionManager;
    }
*/

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
        
        String url = getRepository().getUrl() + "/" + resource.getName();
        getMethod = new HttpGet( url );
        long timestamp = resource.getLastModified();
        if ( timestamp > 0 )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
            fmt.setTimeZone( GMT_TIME_ZONE );
            Header hdr = new BasicHeader( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
            fireTransferDebug( "sending ==> " + hdr + "(" + timestamp + ")" );
            getMethod.addHeader(hdr);
        }

        HttpResponse response;
        int statusCode;
        try
        {
            response = execute( getMethod );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( e.getMessage(), e );
        }
        catch (HttpException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_GET);

            throw new TransferFailedException( e.getMessage(), e );
        }

        statusCode = response.getStatusLine().getStatusCode();

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
                TransferFailedException e =
                    new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                        + statusCode );
                fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                throw e;
            }
        }

        InputStream is = null;

        Header contentLengthHeader = response.getFirstHeader("Content-Length");

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

        Header lastModifiedHeader = response.getFirstHeader("Last-Modified");

        long lastModified = 0;

        if ( lastModifiedHeader != null )
        {
            try
            {
                lastModified = DateUtils.parseDate( lastModifiedHeader.getValue() ).getTime();

                resource.setLastModified( lastModified );
            }
            catch ( DateParseException e )
            {
                fireTransferDebug( "Unable to parse last modified header" );
            }

            fireTransferDebug( "last-modified = " + lastModifiedHeader.getValue() + " (" + lastModified + ")" );
        }

        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        boolean isGZipped =
            contentEncoding == null ? false : "gzip".equalsIgnoreCase( contentEncoding.getValue() );

        try
        {
            //X original is = getMethod.getResponseBodyAsStream();
            is = response.getEntity().getContent();

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
            getMethod.abort();
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        throw new IllegalStateException( "Should not be using the streaming wagon for HTTP PUT" );        
    }
}
