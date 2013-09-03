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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
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

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public abstract class AbstractHttpClientWagon
    extends StreamWagon
{

    private BasicHttpContext localContext;

    private final class RequestEntityImplementation
        extends AbstractHttpEntity
    {

        private final static int BUFFER_SIZE = 2048;

        private final Resource resource;

        private final Wagon wagon;

        private ByteBuffer byteBuffer;

        private File source;

        private long length = -1;

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
                try
                {
                    byte[] bytes = IOUtil.toByteArray( stream );
                    byteBuffer = ByteBuffer.allocate( bytes.length );
                    byteBuffer.put( bytes );
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( e.getMessage(), e );
                }
            }
            this.resource = resource;
            this.length = resource == null ? -1 : resource.getContentLength();

            this.wagon = wagon;
        }

        public long getContentLength()
        {
            return length;
        }

        public InputStream getContent()
            throws IOException, IllegalStateException
        {
            if ( this.source != null )
            {
                return new FileInputStream( this.source );
            }
            return new ByteArrayInputStream( this.byteBuffer.array() );
        }

        public boolean isRepeatable()
        {
            return true;
        }

        public void writeTo( final OutputStream outstream )
            throws IOException
        {
            if ( outstream == null )
            {
                throw new IllegalArgumentException( "Output stream may not be null" );
            }
            TransferEvent transferEvent =
                new TransferEvent( wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT );
            transferEvent.setTimestamp( System.currentTimeMillis() );
            InputStream instream = ( this.source != null )
                ? new FileInputStream( this.source )
                : new ByteArrayInputStream( this.byteBuffer.array() );
            try
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                int l;
                if ( this.length < 0 )
                {
                    // until EOF
                    while ( ( l = instream.read( buffer ) ) != -1 )
                    {
                        fireTransferProgress( transferEvent, buffer, -1 );
                        outstream.write( buffer, 0, l );
                    }
                }
                else
                {
                    // no need to consume more than length
                    long remaining = this.length;
                    while ( remaining > 0 )
                    {
                        l = instream.read( buffer, 0, (int) Math.min( BUFFER_SIZE, remaining ) );
                        if ( l == -1 )
                        {
                            break;
                        }
                        fireTransferProgress( transferEvent, buffer, (int) Math.min( BUFFER_SIZE, remaining ) );
                        outstream.write( buffer, 0, l );
                        remaining -= l;
                    }
                }
            }
            finally
            {
                instream.close();
            }
        }

        public boolean isStreaming()
        {
            return true;
        }
    }

    protected static final int SC_NULL = -1;

    protected static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private DefaultHttpClient client;

    /**
     * @since 2.0
     */
    protected static ClientConnectionManager connectionManagerPooled;

    /**
     * @since 2.0
     */
    protected ClientConnectionManager clientConnectionManager =
        new BasicClientConnectionManager( createSchemeRegistry() );

    /**
     * use http(s) connection pool mechanism.
     * <b>enabled by default</b>
     *
     * @since 2.0
     */
    protected static boolean useClientManagerPooled =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.pool", "true" ) );

    /**
     * skip failure on certificate validity checks.
     * <b>disabled by default</b>
     *
     * @since 2.0
     */
    protected static boolean sslInsecure = Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.insecure", "false" ) );

    /**
     * if using sslInsecure, certificate date issues will be ignored
     * <b>disabled by default</b>
     *
     * @since 2.0
     */
    protected static boolean IGNORE_SSL_VALIDITY_DATES =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.ignore.validity.dates", "false" ) );

    /**
     * If enabled, ssl hostname verifier does not check hostname. Disable this will use a browser compat hostname verifier
     * <b>disabled by default</b>
     *
     * @since 2.0
     * @see BrowserCompatHostnameVerifier
     */
    protected static boolean sslAllowAll =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.allowall", "false" ) );

    private static SchemeRegistry createSchemeRegistry()
    {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register( new Scheme( "http", 80, PlainSocketFactory.getSocketFactory() ) );
        SSLSocketFactory sslSocketFactory;
        if ( sslInsecure )
        {
            try
            {
                sslSocketFactory = new SSLSocketFactory(
                    RelaxedX509TrustManager.createRelaxedSSLContext(),
                    sslAllowAll ? new RelaxedHostNameVerifier() : SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "failed to init SSLSocket Factory " + e.getMessage(), e );
            }
        }
        else
        {
            sslSocketFactory = new SSLSocketFactory(
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
        }

        Scheme httpsScheme = new Scheme( "https", 443, new ConfigurableSSLSocketFactoryDecorator( sslSocketFactory ) );
        schemeRegistry.register( httpsScheme );

        return schemeRegistry;
    }

    static
    {
        if ( !useClientManagerPooled )
        {
            System.out.println( "http connection pool disabled in wagon http" );
        }
        else
        {
            PoolingClientConnectionManager poolingClientConnectionManager =
                new PoolingClientConnectionManager( createSchemeRegistry() );
            int maxPerRoute =
                Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxPerRoute", "20" ) );
            poolingClientConnectionManager.setDefaultMaxPerRoute( maxPerRoute );
            int maxTotal = Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxTotal", "40" ) );
            poolingClientConnectionManager.setDefaultMaxPerRoute( maxPerRoute );
            poolingClientConnectionManager.setMaxTotal( maxTotal );

            connectionManagerPooled = poolingClientConnectionManager;
        }
    }

    /**
     * disable all host name verification
     *
     * @since 2.0
     */
    private static class RelaxedHostNameVerifier
        implements X509HostnameVerifier
    {
        public void verify( String s, SSLSocket sslSocket )
            throws IOException
        {
            //no op
        }

        public void verify( String s, X509Certificate x509Certificate )
            throws SSLException
        {
            //no op
        }

        public void verify( String s, String[] strings, String[] strings1 )
            throws SSLException
        {
            //no op
        }

        public boolean verify( String s, SSLSession sslSession )
        {
            return true;
        }
    }

    public ClientConnectionManager getConnectionManager()
    {
        if ( !useClientManagerPooled )
        {
            return clientConnectionManager;
        }
        return connectionManagerPooled;
    }

    public static void setConnectionManagerPooled( ClientConnectionManager clientConnectionManager )
    {
        connectionManagerPooled = clientConnectionManager;
    }

    public static void setUseClientManagerPooled( boolean pooledClientManager )
    {
        useClientManagerPooled = pooledClientManager;
    }

    /**
     * @plexus.configuration
     * @deprecated Use httpConfiguration instead.
     */
    private Properties httpHeaders;

    /**
     * @since 1.0-beta-6
     */
    private HttpConfiguration httpConfiguration;

    private HttpGet getMethod;

    public void openConnectionInternal()
    {
        repository.setUrl( getURL( repository ) );
        client = new DefaultHttpClient( getConnectionManager() );

        // WAGON-273: default the cookie-policy to browser compatible
        client.getParams().setParameter( ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY );

        if ( authenticationInfo != null )
        {

            String username = authenticationInfo.getUserName();
            String password = authenticationInfo.getPassword();

            if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
            {
                Credentials creds = new UsernamePasswordCredentials( username, password );

                String host = getRepository().getHost();
                int port = getRepository().getPort() > -1 ? getRepository().getPort() : AuthScope.ANY_PORT;

                client.getCredentialsProvider().setCredentials( new AuthScope( host, port ), creds );
                // preemptive off by default
                /*
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicAuth = new BasicScheme();
                HttpHost targetHost =
                    new HttpHost( repository.getHost(), repository.getPort(), repository.getProtocol() );
                authCache.put( targetHost, basicAuth );

                localContext = new BasicHttpContext();
                localContext.setAttribute( ClientContext.AUTH_CACHE, authCache );
                */
            }
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
                HttpHost proxy = new HttpHost( proxyHost, proxyPort );

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

                client.getParams().setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );
            }
        }
    }

    public void closeConnection()
    {
        if ( !useClientManagerPooled )
        {
            getConnectionManager().shutdown();
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
        put( resource, source, new RequestEntityImplementation( stream, resource, this, source ) );
    }

    private void put( Resource resource, File source, HttpEntity httpEntity )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {

        StringBuilder url = new StringBuilder( getURL( getRepository() ) );
        String[] parts = StringUtils.split( resource.getName(), "/" );
        for ( String part : parts )
        {
            // TODO: Fix encoding...
            // url += "/" + URLEncoder.encode( parts[i], System.getProperty("file.encoding") );
            if ( !url.toString().endsWith( "/" ) )
            {
                url.append( '/' );
            }
            url.append( URLEncoder.encode( part ) );
        }
        put( resource, source, httpEntity, url.toString() );
    }

    private void put( Resource resource, File source, HttpEntity httpEntity, String url )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {

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

        if ( authenticationInfo != null )
        {
            String username = authenticationInfo.getUserName();
            String password = authenticationInfo.getPassword();
            // preemptive for put
            if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
            {
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicAuth = new BasicScheme();
                HttpHost targetHost =
                    new HttpHost( repository.getHost(), repository.getPort(), repository.getProtocol() );
                authCache.put( targetHost, basicAuth );

                localContext = new BasicHttpContext();
                localContext.setAttribute( ClientContext.AUTH_CACHE, authCache );
            }
        }

        HttpPut putMethod = new HttpPut( url );

        firePutStarted( resource, source );

        try
        {
            putMethod.setEntity( httpEntity );

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
            catch ( HttpException e )
            {
                fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

                throw new TransferFailedException( e.getMessage(), e );
            }

            int statusCode = response.getStatusLine().getStatusCode();
            String reasonPhrase = ", ReasonPhrase: " + response.getStatusLine().getReasonPhrase() + ".";
            fireTransferDebug( url + " - Status code: " + statusCode + reasonPhrase );

            // Check that we didn't run out of retries.
            switch ( statusCode )
            {
                // Success Codes
                case HttpStatus.SC_OK: // 200
                case HttpStatus.SC_CREATED: // 201
                case HttpStatus.SC_ACCEPTED: // 202
                case HttpStatus.SC_NO_CONTENT:  // 204
                    break;
                // handle all redirect even if http specs says " the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user"
                case HttpStatus.SC_MOVED_PERMANENTLY: // 301
                case HttpStatus.SC_MOVED_TEMPORARILY: // 302
                case HttpStatus.SC_SEE_OTHER: // 303
                    put( resource, source, httpEntity, calculateRelocatedUrl( response ) );
                    return;
                case SC_NULL:
                {
                    TransferFailedException e =
                        new TransferFailedException( "Failed to transfer file: " + url + reasonPhrase );
                    fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                    throw e;
                }

                case HttpStatus.SC_FORBIDDEN:
                    fireSessionConnectionRefused();
                    throw new AuthorizationException( "Access denied to: " + url + reasonPhrase );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" + reasonPhrase );

                    //add more entries here
                default:
                {
                    TransferFailedException e = new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode + reasonPhrase );
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

    protected String calculateRelocatedUrl( HttpResponse response )
    {
        Header locationHeader = response.getFirstHeader( "Location" );
        String locationField = locationHeader.getValue();
        // is it a relative Location or a full ?
        return locationField.startsWith( "http" ) ? locationField : getURL( getRepository() ) + '/' + locationField;
    }

    protected void mkdirs( String dirname )
        throws HttpException, IOException
    {
        // nothing to do
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String repositoryUrl = getRepository().getUrl();
        String url = repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + resourceName;
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
        catch ( HttpException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }

        try
        {
            statusCode = response.getStatusLine().getStatusCode();
            String reasonPhrase = ", ReasonPhrase: " + response.getStatusLine().getReasonPhrase() + ".";
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    return true;

                case HttpStatus.SC_NOT_MODIFIED:
                    return true;

                case SC_NULL:
                    throw new TransferFailedException( "Failed to transfer file: " + url + reasonPhrase );

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url + reasonPhrase );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized " + reasonPhrase );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy " + reasonPhrase );

                case HttpStatus.SC_NOT_FOUND:
                    return false;

                //add more entries here
                default:
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode + reasonPhrase );
            }
        }
        finally
        {
            headMethod.abort();
        }
    }

    protected HttpResponse execute( HttpUriRequest httpMethod )
        throws HttpException, IOException
    {
        setParameters( httpMethod );
        setHeaders( httpMethod );
        client.getParams().setParameter( CoreProtocolPNames.USER_AGENT, getUserAgent( httpMethod ) );

        ProxyInfo proxyInfo = getProxyInfo( getRepository().getProtocol(), getRepository().getHost() );

        if ( proxyInfo != null )
        {
            if ( proxyInfo.getUserName() != null && proxyInfo.getPassword() != null )
            {
                Credentials creds;
                if ( proxyInfo.getNtlmHost() != null || proxyInfo.getNtlmDomain() != null )
                {
                    creds =
                        new NTCredentials( proxyInfo.getUserName(), proxyInfo.getPassword(), proxyInfo.getNtlmHost(),
                                           proxyInfo.getNtlmDomain() );
                }
                else
                {
                    creds = new UsernamePasswordCredentials( proxyInfo.getUserName(), proxyInfo.getPassword() );
                }

                Header bs = new BasicScheme().authenticate( creds, httpMethod, localContext );
                httpMethod.addHeader( "Proxy-Authorization", bs.getValue() );
            }

        }

        return client.execute( httpMethod, localContext );
    }

    protected void setParameters( HttpUriRequest method )
    {
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config != null )
        {
            HttpParams params = config.asMethodParams( method.getParams() );

            if ( config.isUsePreemptive() && authenticationInfo != null )
            {
                String username = authenticationInfo.getUserName();
                String password = authenticationInfo.getPassword();

                if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
                {

                    AuthCache authCache = new BasicAuthCache();
                    BasicScheme basicAuth = new BasicScheme();
                    HttpHost targetHost =
                        new HttpHost( repository.getHost(), repository.getPort(), repository.getProtocol() );
                    authCache.put( targetHost, basicAuth );

                    localContext = new BasicHttpContext();
                    localContext.setAttribute( ClientContext.AUTH_CACHE, authCache );
                }

            }

            if ( params != null )
            {
                method.setParams( params );
            }
        }

        if ( config == null )
        {
            int readTimeout = getReadTimeout();
            method.getParams().setParameter( CoreConnectionPNames.SO_TIMEOUT, readTimeout );
        }
    }

    protected void setHeaders( HttpUriRequest method )
    {
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config == null || config.isUseDefaultHeaders() )
        {
            // TODO: merge with the other headers and have some better defaults, unify with lightweight headers
            method.addHeader( "Cache-control", "no-cache" );
            method.addHeader( "Cache-store", "no-store" );
            method.addHeader( "Pragma", "no-cache" );
            method.addHeader( "Expires", "0" );
            method.addHeader( "Accept-Encoding", "gzip" );
        }

        if ( httpHeaders != null )
        {
            for ( Map.Entry<Object, Object> entry : httpHeaders.entrySet() )
            {
                method.addHeader( (String) entry.getKey(), (String) entry.getValue() );
            }
        }

        Header[] headers = config == null ? null : config.asRequestHeaders();
        if ( headers != null )
        {
            for ( int i = 0; i < headers.length; i++ )
            {
                method.addHeader( headers[i] );
            }
        }
    }

    protected String getUserAgent( HttpUriRequest method )
    {
        if ( httpHeaders != null )
        {
            String value = (String) httpHeaders.get( "User-Agent" );
            if ( value != null )
            {
                return value;
            }
        }
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );

        if ( config != null )
        {
            return (String) config.getHeaders().get( "User-Agent" );
        }
        return null;
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

        String repositoryUrl = getRepository().getUrl();
        String url = repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + resource.getName();
        getMethod = new HttpGet( url );
        long timestamp = resource.getLastModified();
        if ( timestamp > 0 )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
            fmt.setTimeZone( GMT_TIME_ZONE );
            Header hdr = new BasicHeader( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
            fireTransferDebug( "sending ==> " + hdr + "(" + timestamp + ")" );
            getMethod.addHeader( hdr );
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
        catch ( HttpException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( e.getMessage(), e );
        }

        statusCode = response.getStatusLine().getStatusCode();

        String reasonPhrase = ", ReasonPhrase:" + response.getStatusLine().getReasonPhrase() + ".";

        fireTransferDebug( url + " - Status code: " + statusCode + reasonPhrase );

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
                TransferFailedException e =
                    new TransferFailedException( "Failed to transfer file: " + url + " " + reasonPhrase );
                fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                throw e;
            }

            case HttpStatus.SC_FORBIDDEN:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Access denied to: " + url + " " + reasonPhrase );

            case HttpStatus.SC_UNAUTHORIZED:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Not authorized " + reasonPhrase );

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                fireSessionConnectionRefused();
                throw new AuthorizationException( "Not authorized by proxy " + reasonPhrase );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " " + reasonPhrase );

                // add more entries here
            default:
            {
                cleanupGetTransfer( resource );
                TransferFailedException e = new TransferFailedException(
                    "Failed to transfer file: " + url + ". Return code is: " + statusCode + " " + reasonPhrase );
                fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                throw e;
            }
        }

        InputStream is;

        Header contentLengthHeader = response.getFirstHeader( "Content-Length" );

        if ( contentLengthHeader != null )
        {
            try
            {
                long contentLength = Long.parseLong( contentLengthHeader.getValue() );

                resource.setContentLength( contentLength );
            }
            catch ( NumberFormatException e )
            {
                fireTransferDebug(
                    "error parsing content length header '" + contentLengthHeader.getValue() + "' " + e );
            }
        }

        Header lastModifiedHeader = response.getFirstHeader( "Last-Modified" );

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

        Header contentEncoding = response.getFirstHeader( "Content-Encoding" );
        boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase( contentEncoding.getValue() );

        try
        {
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
                "Error occurred while retrieving from remote repository " + getRepository() + ": " + e.getMessage();

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
        putFromStream( stream, resource.getName(), -1, -1 );
    }

    public Properties getHttpHeaders()
    {
        return httpHeaders;
    }

    public void setHttpHeaders( Properties httpHeaders )
    {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        // no needed in this implementation but throw an Exception if used
        throw new IllegalStateException( "this wagon http client must not use fillOutputData" );
    }
}
