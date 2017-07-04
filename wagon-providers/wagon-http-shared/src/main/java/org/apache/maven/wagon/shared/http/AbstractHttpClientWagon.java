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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.ChallengeState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public abstract class AbstractHttpClientWagon
    extends StreamWagon
{
    private final class RequestEntityImplementation
        extends AbstractHttpEntity
    {

        private static final int BUFFER_SIZE = 2048;

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
                    stream.close();
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( e.getMessage(), e );
                }
                finally
                {
                    IOUtil.close( stream );
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

        public void writeTo( final OutputStream outputStream )
            throws IOException
        {
            if ( outputStream == null )
            {
                throw new NullPointerException( "outputStream cannot be null" );
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
                        outputStream.write( buffer, 0, l );
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
                        outputStream.write( buffer, 0, l );
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

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    /**
     * use http(s) connection pool mechanism.
     * <b>enabled by default</b>
     */
    private static boolean persistentPool =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.pool", "true" ) );

    /**
     * skip failure on certificate validity checks.
     * <b>disabled by default</b>
     */
    private static final boolean SSL_INSECURE =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.insecure", "false" ) );

    /**
     * if using sslInsecure, certificate date issues will be ignored
     * <b>disabled by default</b>
     */
    private static final boolean IGNORE_SSL_VALIDITY_DATES =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.ignore.validity.dates", "false" ) );

    /**
     * If enabled, ssl hostname verifier does not check hostname. Disable this will use a browser compat hostname
     * verifier <b>disabled by default</b>
     */
    private static final boolean SSL_ALLOW_ALL =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.allowall", "false" ) );


    /**
     * Maximum concurrent connections per distinct route.
     * <b>20 by default</b>
     */
    private static final int MAX_CONN_PER_ROUTE =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxPerRoute", "20" ) );

    /**
     * Maximum concurrent connections in total.
     * <b>40 by default</b>
     */
    private static final int MAX_CONN_TOTAL =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxTotal", "40" ) );

    /**
     * Internal connection manager
     */
    private static HttpClientConnectionManager httpClientConnectionManager = createConnManager();


    /**
     * See RFC6585
     */
    protected static final int SC_TOO_MANY_REQUESTS = 429;

    /**
     * For exponential backoff.
     */

    /**
     * Initial seconds to back off when a HTTP 429 received.
     * Subsequent 429 responses result in exponental backoff.
     * <b>5 by default</b>
     *
     * @since 2.7
     */
    private int initialBackoffSeconds =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.backoffSeconds", "5" ) );

    /**
     * The maximum amount of time we want to back off in the case of
     * repeated HTTP 429 response codes.
     *
     * @since 2.7
     */
    private static final int MAX_BACKOFF_WAIT_SECONDS =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxBackoffSeconds", "180" ) );


    protected int backoff( int wait, String url )
        throws InterruptedException, TransferFailedException
    {
        TimeUnit.SECONDS.sleep( wait );
        int nextWait = wait * 2;
        if ( nextWait >= getMaxBackoffWaitSeconds() )
        {
            throw new TransferFailedException(
                "Waited too long to access: " + url + ". Return code is: " + SC_TOO_MANY_REQUESTS );
        }
        return nextWait;
    }

    @SuppressWarnings( "checkstyle:linelength" )
    private static PoolingHttpClientConnectionManager createConnManager()
    {

        String sslProtocolsStr = System.getProperty( "https.protocols" );
        String cipherSuitesStr = System.getProperty( "https.cipherSuites" );
        String[] sslProtocols = sslProtocolsStr != null ? sslProtocolsStr.split( " *, *" ) : null;
        String[] cipherSuites = cipherSuitesStr != null ? cipherSuitesStr.split( " *, *" ) : null;

        SSLConnectionSocketFactory sslConnectionSocketFactory;
        if ( SSL_INSECURE )
        {
            try
            {
                SSLContext sslContext = new SSLContextBuilder().useSSL().loadTrustMaterial( null,
                                                                                            new RelaxedTrustStrategy(
                                                                                                IGNORE_SSL_VALIDITY_DATES ) ).build();
                sslConnectionSocketFactory = new SSLConnectionSocketFactory( sslContext, sslProtocols, cipherSuites,
                                                                             SSL_ALLOW_ALL
                                                                                 ? SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
                                                                                 : SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
            }
            catch ( Exception ex )
            {
                throw new SSLInitializationException( ex.getMessage(), ex );
            }
        }
        else
        {
            sslConnectionSocketFactory =
                new SSLConnectionSocketFactory( HttpsURLConnection.getDefaultSSLSocketFactory(), sslProtocols,
                                                cipherSuites,
                                                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
        }

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register( "http",
                                                                                                                 PlainConnectionSocketFactory.INSTANCE ).register(
            "https", sslConnectionSocketFactory ).build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager( registry );
        if ( persistentPool )
        {
            connManager.setDefaultMaxPerRoute( MAX_CONN_PER_ROUTE );
            connManager.setMaxTotal( MAX_CONN_TOTAL );
        }
        else
        {
            connManager.setMaxTotal( 1 );
        }
        return connManager;
    }

    private static CloseableHttpClient httpClient = createClient();

    private static CloseableHttpClient createClient()
    {
        return HttpClientBuilder.create() //
            .useSystemProperties() //
            .disableConnectionState() //
            .setConnectionManager( httpClientConnectionManager ) //
            .build();
    }

    private CredentialsProvider credentialsProvider;

    private AuthCache authCache;

    private Closeable closeable;

    /**
     * @plexus.configuration
     * @deprecated Use httpConfiguration instead.
     */
    private Properties httpHeaders;

    /**
     * @since 1.0-beta-6
     */
    private HttpConfiguration httpConfiguration;

    /**
     * Basic auth scope overrides
     * @since 2.8
     */
    private BasicAuthScope basicAuth;

    /**
     * Proxy basic auth scope overrides
     * @since 2.8
     */
    private BasicAuthScope proxyAuth;

    public void openConnectionInternal()
    {
        repository.setUrl( getURL( repository ) );

        credentialsProvider = new BasicCredentialsProvider();
        authCache = new BasicAuthCache();

        if ( authenticationInfo != null )
        {

            String username = authenticationInfo.getUserName();
            String password = authenticationInfo.getPassword();

            if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
            {
                Credentials creds = new UsernamePasswordCredentials( username, password );

                String host = getRepository().getHost();
                int port = getRepository().getPort();

                credentialsProvider.setCredentials( getBasicAuthScope().getScope( host, port ), creds );
            }
        }

        ProxyInfo proxyInfo = getProxyInfo( getRepository().getProtocol(), getRepository().getHost() );
        if ( proxyInfo != null )
        {
            String proxyUsername = proxyInfo.getUserName();
            String proxyPassword = proxyInfo.getPassword();
            String proxyHost = proxyInfo.getHost();
            String proxyNtlmHost = proxyInfo.getNtlmHost();
            String proxyNtlmDomain = proxyInfo.getNtlmDomain();
            if ( proxyHost != null )
            {
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

                    int proxyPort = proxyInfo.getPort();

                    AuthScope authScope = getProxyBasicAuthScope().getScope( proxyHost, proxyPort );
                    credentialsProvider.setCredentials( authScope, creds );
                }
            }
        }
    }

    public void closeConnection()
    {
        if ( !persistentPool )
        {
            httpClientConnectionManager.closeIdleConnections( 0, TimeUnit.MILLISECONDS );
        }

        if ( authCache != null )
        {
            authCache.clear();
            authCache = null;
        }

        if ( credentialsProvider != null )
        {
            credentialsProvider.clear();
            credentialsProvider = null;
        }
    }

    public static CloseableHttpClient getHttpClient()
    {
        return httpClient;
    }

    public static void setPersistentPool( boolean persistentPool )
    {
        persistentPool = persistentPool;
    }

    public static void setPoolingHttpClientConnectionManager(
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager )
    {
        httpClientConnectionManager = poolingHttpClientConnectionManager;
        httpClient = createClient();
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
        put( resource, source, httpEntity, buildUrl( resource ) );
    }

    /**
     * Builds a complete URL string from the repository URL and the relative path of the resource passed.
     *
     * @param resource the resource to extract the relative path from.
     * @return the complete URL
     */
    private String buildUrl( Resource resource )
    {
        return EncodingUtil.encodeURLToString( getRepository().getUrl(), resource.getName() );
    }


    private void put( Resource resource, File source, HttpEntity httpEntity, String url )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        put( getInitialBackoffSeconds(), resource, source, httpEntity, url );
    }


    private void put( int wait, Resource resource, File source, HttpEntity httpEntity, String url )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {

        //Parent directories need to be created before posting
        try
        {
            mkdirs( PathUtils.dirname( resource.getName() ) );
        }
        catch ( HttpException he )
        {
            fireTransferError( resource, he, TransferEvent.REQUEST_PUT );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
        }

        // preemptive for put
        // TODO: is it a good idea, though? 'Expect-continue' handshake would serve much better

        Repository repo = getRepository();
        HttpHost targetHost = new HttpHost( repo.getHost(), repo.getPort(), repo.getProtocol() );
        AuthScope targetScope = getBasicAuthScope().getScope( targetHost );

        if ( credentialsProvider.getCredentials( targetScope ) != null )
        {
            BasicScheme targetAuth = new BasicScheme();
            authCache.put( targetHost, targetAuth );
        }

        HttpPut putMethod = new HttpPut( url );

        firePutStarted( resource, source );

        try
        {
            putMethod.setEntity( httpEntity );

            CloseableHttpResponse response = execute( putMethod );
            try
            {
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
                    // handle all redirect even if http specs says " the user agent MUST NOT automatically redirect
                    // the request unless it can be confirmed by the user"
                    case HttpStatus.SC_MOVED_PERMANENTLY: // 301
                    case HttpStatus.SC_MOVED_TEMPORARILY: // 302
                    case HttpStatus.SC_SEE_OTHER: // 303
                        put( resource, source, httpEntity, calculateRelocatedUrl( response ) );
                        return;
                    case HttpStatus.SC_FORBIDDEN:
                        fireSessionConnectionRefused();
                        throw new AuthorizationException( "Access denied to: " + url + reasonPhrase );

                    case HttpStatus.SC_NOT_FOUND:
                        throw new ResourceDoesNotExistException( "File: " + url + " does not exist" + reasonPhrase );

                    case SC_TOO_MANY_REQUESTS:
                        put( backoff( wait, url ), resource, source, httpEntity, url );
                        break;
                    //add more entries here
                    default:
                        TransferFailedException e = new TransferFailedException(
                            "Failed to transfer file: " + url + ". Return code is: " + statusCode + reasonPhrase );
                        fireTransferError( resource, e, TransferEvent.REQUEST_PUT );
                        throw e;
                }

                firePutCompleted( resource, source );

                EntityUtils.consume( response.getEntity() );
            }
            finally
            {
                response.close();
            }
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
        catch ( InterruptedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( e.getMessage(), e );
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
        return resourceExists( getInitialBackoffSeconds(), resourceName );
    }


    private boolean resourceExists( int wait, String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        String repositoryUrl = getRepository().getUrl();
        String url = repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + resourceName;
        HttpHead headMethod = new HttpHead( url );
        try
        {
            CloseableHttpResponse response = execute( headMethod );
            try
            {
                int statusCode = response.getStatusLine().getStatusCode();
                String reasonPhrase = ", ReasonPhrase: " + response.getStatusLine().getReasonPhrase() + ".";
                boolean result;
                switch ( statusCode )
                {
                    case HttpStatus.SC_OK:
                        result = true;
                        break;
                    case HttpStatus.SC_NOT_MODIFIED:
                        result = true;
                        break;
                    case HttpStatus.SC_FORBIDDEN:
                        throw new AuthorizationException( "Access denied to: " + url + reasonPhrase );

                    case HttpStatus.SC_UNAUTHORIZED:
                        throw new AuthorizationException( "Not authorized " + reasonPhrase );

                    case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                        throw new AuthorizationException( "Not authorized by proxy " + reasonPhrase );

                    case HttpStatus.SC_NOT_FOUND:
                        result = false;
                        break;

                    case SC_TOO_MANY_REQUESTS:
                        return resourceExists( backoff( wait, resourceName ), resourceName );

                    //add more entries here
                    default:
                        throw new TransferFailedException(
                            "Failed to transfer file: " + url + ". Return code is: " + statusCode + reasonPhrase );
                }

                EntityUtils.consume( response.getEntity() );
                return result;
            }
            finally
            {
                response.close();
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        catch ( HttpException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        catch ( InterruptedException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }

    }

    protected CloseableHttpResponse execute( HttpUriRequest httpMethod )
        throws HttpException, IOException
    {
        setHeaders( httpMethod );
        String userAgent = getUserAgent( httpMethod );
        if ( userAgent != null )
        {
            httpMethod.setHeader( HTTP.USER_AGENT, userAgent );
        }

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        // WAGON-273: default the cookie-policy to browser compatible
        requestConfigBuilder.setCookieSpec( CookieSpecs.BROWSER_COMPATIBILITY );

        Repository repo = getRepository();
        ProxyInfo proxyInfo = getProxyInfo( repo.getProtocol(), repo.getHost() );
        if ( proxyInfo != null )
        {
            HttpHost proxy = new HttpHost( proxyInfo.getHost(), proxyInfo.getPort() );
            requestConfigBuilder.setProxy( proxy );
        }

        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( httpMethod );

        if ( config != null )
        {
            ConfigurationUtils.copyConfig( config, requestConfigBuilder );
        }
        else
        {
            requestConfigBuilder.setSocketTimeout( getReadTimeout() );
            if ( httpMethod instanceof HttpPut )
            {
                requestConfigBuilder.setExpectContinueEnabled( true );
            }
        }

        if ( httpMethod instanceof HttpPut )
        {
            requestConfigBuilder.setRedirectsEnabled( false );
        }

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCredentialsProvider( credentialsProvider );
        localContext.setAuthCache( authCache );
        localContext.setRequestConfig( requestConfigBuilder.build() );

        if ( config != null && config.isUsePreemptive() )
        {
            HttpHost targetHost = new HttpHost( repo.getHost(), repo.getPort(), repo.getProtocol() );
            AuthScope targetScope = getBasicAuthScope().getScope( targetHost );

            if ( credentialsProvider.getCredentials( targetScope ) != null )
            {
                BasicScheme targetAuth = new BasicScheme();
                authCache.put( targetHost, targetAuth );
            }
        }

        if ( proxyInfo != null )
        {
            if ( proxyInfo.getHost() != null )
            {
                HttpHost proxyHost = new HttpHost( proxyInfo.getHost(), proxyInfo.getPort() );
                AuthScope proxyScope = getProxyBasicAuthScope().getScope( proxyHost );

                if ( credentialsProvider.getCredentials( proxyScope ) != null )
                {
                    /* This is extremely ugly because we need to set challengeState to PROXY, but
                     * the constructor is deprecated. Alternatively, we could subclass BasicScheme
                     * to ProxyBasicScheme and set the state internally in the constructor.
                     */
                    BasicScheme proxyAuth = new BasicScheme( ChallengeState.PROXY );
                    authCache.put( proxyHost, proxyAuth );
                }
            }
        }

        return httpClient.execute( httpMethod, localContext );
    }

    public void setHeaders( HttpUriRequest method )
    {
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );
        if ( config == null || config.isUseDefaultHeaders() )
        {
            // TODO: merge with the other headers and have some better defaults, unify with lightweight headers
            method.addHeader(  "Cache-control", "no-cache" );
            method.addHeader( "Cache-store", "no-store" );
            method.addHeader( "Pragma", "no-cache" );
            method.addHeader( "Expires", "0" );
            method.addHeader( "Accept-Encoding", "gzip" );
        }

        if ( httpHeaders != null )
        {
            for ( Map.Entry<Object, Object> entry : httpHeaders.entrySet() )
            {
                method.setHeader( (String) entry.getKey(), (String) entry.getValue() );
            }
        }

        Header[] headers = config == null ? null : config.asRequestHeaders();
        if ( headers != null )
        {
            for ( Header header : headers )
            {
                method.setHeader( header );
            }
        }

        Header userAgentHeader = method.getFirstHeader( HTTP.USER_AGENT );
        if ( userAgentHeader == null )
        {
            String userAgent = getUserAgent( method );
            if ( userAgent != null )
            {
                method.setHeader( HTTP.USER_AGENT, userAgent );
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

    /**
     * Get the override values for standard HttpClient AuthScope
     *
     * @return the basicAuth
     */
    public BasicAuthScope getBasicAuthScope()
    {
        if ( basicAuth == null )
        {
            basicAuth = new BasicAuthScope();
        }
        return basicAuth;
    }

    /**
     * Set the override values for standard HttpClient AuthScope
     *
     * @param basicAuth the AuthScope to set
     */
    public void setBasicAuthScope( BasicAuthScope basicAuth )
    {
        this.basicAuth = basicAuth;
    }

    /**
     * Get the override values for proxy HttpClient AuthScope
     *
     * @return the proxyAuth
     */
    public BasicAuthScope getProxyBasicAuthScope()
    {
        if ( proxyAuth == null )
        {
            proxyAuth = new BasicAuthScope();
        }
        return proxyAuth;
    }

    /**
     * Set the override values for proxy HttpClient AuthScope
     *
     * @param proxyAuth the AuthScope to set
     */
    public void setProxyBasicAuthScope( BasicAuthScope proxyAuth )
    {
        this.proxyAuth = proxyAuth;
    }

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        fillInputData( getInitialBackoffSeconds(), inputData );
    }

    private void fillInputData( int wait, InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = inputData.getResource();

        String repositoryUrl = getRepository().getUrl();
        String url = repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + resource.getName();
        HttpGet getMethod = new HttpGet( url );
        long timestamp = resource.getLastModified();
        if ( timestamp > 0 )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
            fmt.setTimeZone( GMT_TIME_ZONE );
            Header hdr = new BasicHeader( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
            fireTransferDebug( "sending ==> " + hdr + "(" + timestamp + ")" );
            getMethod.addHeader( hdr );
        }

        try
        {
            CloseableHttpResponse response = execute( getMethod );
            closeable = response;
            int statusCode = response.getStatusLine().getStatusCode();

            String reasonPhrase = ", ReasonPhrase:" + response.getStatusLine().getReasonPhrase() + ".";

            fireTransferDebug( url + " - Status code: " + statusCode + reasonPhrase );

            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_NOT_MODIFIED:
                    // return, leaving last modified set to original value so getIfNewer should return unmodified
                    return;
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

                case SC_TOO_MANY_REQUESTS:
                    fillInputData( backoff( wait, url ), inputData );
                    break;

                // add more entries here
                default:
                    cleanupGetTransfer( resource );
                    TransferFailedException e = new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode + " " + reasonPhrase );
                    fireTransferError( resource, e, TransferEvent.REQUEST_GET );
                    throw e;
            }

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
            if ( lastModifiedHeader != null )
            {
                Date lastModified = DateUtils.parseDate( lastModifiedHeader.getValue() );
                if ( lastModified != null )
                {
                    resource.setLastModified( lastModified.getTime() );
                    fireTransferDebug( "last-modified = " + lastModifiedHeader.getValue() + " ("
                        + lastModified.getTime() + ")" );
                }
            }

            HttpEntity entity = response.getEntity();
            if ( entity != null )
            {
                inputData.setInputStream( entity.getContent() );
            }
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
        catch ( InterruptedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( e.getMessage(), e );
        }

    }

    protected void cleanupGetTransfer( Resource resource )
    {
        if ( closeable != null )
        {
            try
            {
                closeable.close();
            }
            catch ( IOException ignore )
            {
                // ignore
            }

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

    public int getInitialBackoffSeconds()
    {
        return initialBackoffSeconds;
    }

    public void setInitialBackoffSeconds( int initialBackoffSeconds )
    {
        this.initialBackoffSeconds = initialBackoffSeconds;
    }

    public static int getMaxBackoffWaitSeconds()
    {
        return MAX_BACKOFF_WAIT_SECONDS;
    }
}
