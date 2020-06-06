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
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.ChallengeState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.AuthSchemes;
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
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
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
import org.codehaus.plexus.util.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatAuthorizationMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatResourceDoesNotExistMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatTransferDebugMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatTransferFailedMessage;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public abstract class AbstractHttpClientWagon
    extends StreamWagon
{
    final class WagonHttpEntity
        extends AbstractHttpEntity
    {
        private final Resource resource;

        private final Wagon wagon;

        private InputStream stream;

        private File source;

        private long length = -1;

        private boolean repeatable;

        private WagonHttpEntity( final InputStream stream, final Resource resource, final Wagon wagon,
                                             final File source )
            throws TransferFailedException
        {
            if ( source != null )
            {
                this.source = source;
                this.repeatable = true;
            }
            else
            {
                this.stream = stream;
                this.repeatable = false;
            }
            this.resource = resource;
            this.length = resource == null ? -1 : resource.getContentLength();

            this.wagon = wagon;
        }

        public Resource getResource()
        {
            return resource;
        }

        public Wagon getWagon()
        {
            return wagon;
        }

        public InputStream getContent()
            throws IOException, IllegalStateException
        {
            if ( this.source != null )
            {
                return new FileInputStream( this.source );
            }
            return stream;
        }

        public File getSource()
        {
            return source;
        }

        public long getContentLength()
        {
            return length;
        }

        public boolean isRepeatable()
        {
            return repeatable;
        }

        public void writeTo( final OutputStream output )
            throws IOException
        {
            if ( output == null )
            {
                throw new NullPointerException( "output cannot be null" );
            }
            TransferEvent transferEvent =
                new TransferEvent( wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT );
            transferEvent.setTimestamp( System.currentTimeMillis() );

            try ( ReadableByteChannel input = ( this.source != null )
                    ? new RandomAccessFile( this.source, "r" ).getChannel()
                    : Channels.newChannel( stream ) )
            {
                ByteBuffer buffer = ByteBuffer.allocate( getBufferCapacityForTransfer( this.length ) );
                int halfBufferCapacity = buffer.capacity() / 2;

                long remaining = this.length < 0L ? Long.MAX_VALUE : this.length;
                while ( remaining > 0L )
                {
                    int read = input.read( buffer );
                    if ( read == -1 )
                    {
                        // EOF, but some data has not been written yet.
                        if ( ( (Buffer) buffer ).position() != 0 )
                        {
                            ( (Buffer) buffer ).flip();
                            fireTransferProgress( transferEvent, buffer.array(), ( (Buffer) buffer ).limit() );
                            output.write( buffer.array(), 0, ( (Buffer) buffer ).limit() );
                            ( (Buffer) buffer ).clear();
                        }

                        break;
                    }

                    // Prevent minichunking/fragmentation: when less than half the buffer is utilized,
                    // read some more bytes before writing and firing progress.
                    if ( ( (Buffer) buffer ).position() < halfBufferCapacity )
                    {
                        continue;
                    }

                    ( (Buffer) buffer ).flip();
                    fireTransferProgress( transferEvent, buffer.array(), ( (Buffer) buffer ).limit() );
                    output.write( buffer.array(), 0, ( (Buffer) buffer ).limit() );
                    remaining -= ( (Buffer) buffer ).limit();
                    ( (Buffer) buffer ).clear();

                }
                output.flush();
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
     * Time to live in seconds for an HTTP connection. After that time, the connection will be dropped.
     * Intermediates tend to drop connections after some idle period. Set to -1 to maintain connections
     * indefinitely. This value defaults to 300 seconds.
     *
     * @since 3.2
     */
    private static final long CONN_TTL =
        Long.getLong( "maven.wagon.httpconnectionManager.ttlSeconds", 300L );

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
            throw new TransferFailedException( formatTransferFailedMessage( url, SC_TOO_MANY_REQUESTS,
                    null, getProxyInfo() ) );
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

        PoolingHttpClientConnectionManager connManager =
            new PoolingHttpClientConnectionManager( registry, null, null, null, CONN_TTL, TimeUnit.SECONDS );
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

    /**
     * The type of the retry handler, defaults to {@code standard}.
     * Values can be {@link default DefaultHttpRequestRetryHandler},
     * or {@link standard StandardHttpRequestRetryHandler},
     * or a fully qualified name class with a no-arg.
     *
     * @since 3.2
     */
    private static final String RETRY_HANDLER_CLASS =
            System.getProperty( "maven.wagon.http.retryHandler.class", "standard" );

    /**
     * Whether or not methods that have successfully sent their request will be retried,
     * defaults to {@code false}.
     * Note: only used for default and standard retry handlers.
     *
     * @since 3.2
     */
    private static final boolean RETRY_HANDLER_REQUEST_SENT_ENABLED =
            Boolean.getBoolean( "maven.wagon.http.retryHandler.requestSentEnabled" );

    /**
     * Number of retries for the retry handler, defaults to 3.
     * Note: only used for default and standard retry handlers.
     *
     * @since 3.2
     */
    private static final int RETRY_HANDLER_COUNT =
            Integer.getInteger( "maven.wagon.http.retryHandler.count", 3 );

    /**
     * Comma-separated list of non-retryable exception classes.
     * Note: only used for default retry handler.
     *
     * @since 3.2
     */
    private static final String RETRY_HANDLER_EXCEPTIONS =
            System.getProperty( "maven.wagon.http.retryHandler.nonRetryableClasses" );

    private static HttpRequestRetryHandler createRetryHandler()
    {
        switch ( RETRY_HANDLER_CLASS )
        {
            case "default":
                if ( StringUtils.isEmpty( RETRY_HANDLER_EXCEPTIONS ) )
                {
                    return new DefaultHttpRequestRetryHandler(
                            RETRY_HANDLER_COUNT, RETRY_HANDLER_REQUEST_SENT_ENABLED );
                }
                return new DefaultHttpRequestRetryHandler(
                        RETRY_HANDLER_COUNT, RETRY_HANDLER_REQUEST_SENT_ENABLED, getNonRetryableExceptions() )
                {
                };
            case "standard":
                return new StandardHttpRequestRetryHandler( RETRY_HANDLER_COUNT, RETRY_HANDLER_REQUEST_SENT_ENABLED );
            default:
                try
                {
                    final ClassLoader classLoader = AbstractHttpClientWagon.class.getClassLoader();
                    return HttpRequestRetryHandler.class.cast( classLoader.loadClass( RETRY_HANDLER_CLASS )
                                                                          .getConstructor().newInstance() );
                }
                catch ( final Exception e )
                {
                    throw new IllegalArgumentException( e );
                }
        }
    }

    /**
     * The type of the serviceUnavailableRetryStrategy, defaults to {@code none}.
     * Values can be {@link default DefaultServiceUnavailableRetryStrategy},
     * or {@link standard StandardServiceUnavailableRetryStrategy}, or
     * a fully qualified name class with a no-arg or none to not use a ServiceUnavailableRetryStrategy.
     */
    private static final String SERVICE_UNAVAILABLE_RETRY_STRATEGY_CLASS =
            System.getProperty( "maven.wagon.http.serviceUnavailableRetryStrategy.class", "none" );

    /**
     * Interval in milliseconds between retries when using a serviceUnavailableRetryStrategy.
     * <b>1000 by default</b>
     */
    private static final int SERVICE_UNAVAILABLE_RETRY_STRATEGY_RETRY_INTERVAL =
        Integer.getInteger( "maven.wagon.http.serviceUnavailableRetryStrategy.retryInterval", 1000 );

    /**
     * Maximum number of retries when using a serviceUnavailableRetryStrategy.
     * <b>5 by default</b>
     */
    private static final int SERVICE_UNAVAILABLE_RETRY_STRATEGY_MAX_RETRIES =
        Integer.getInteger( "maven.wagon.http.serviceUnavailableRetryStrategy.maxRetries", 5 );

    private static ServiceUnavailableRetryStrategy createServiceUnavailableRetryStrategy()
    {
        switch ( SERVICE_UNAVAILABLE_RETRY_STRATEGY_CLASS )
        {
            case "none": return null;
            case "default":
                return new DefaultServiceUnavailableRetryStrategy(
                    SERVICE_UNAVAILABLE_RETRY_STRATEGY_MAX_RETRIES, SERVICE_UNAVAILABLE_RETRY_STRATEGY_RETRY_INTERVAL );
            case "standard":
                return new StandardServiceUnavailableRetryStrategy(
                    SERVICE_UNAVAILABLE_RETRY_STRATEGY_MAX_RETRIES, SERVICE_UNAVAILABLE_RETRY_STRATEGY_RETRY_INTERVAL );
            default:
                try
                {
                    final ClassLoader classLoader = AbstractHttpClientWagon.class.getClassLoader();
                    return ServiceUnavailableRetryStrategy.class.cast(
                            classLoader.loadClass( SERVICE_UNAVAILABLE_RETRY_STRATEGY_CLASS )
                                                                          .getConstructor().newInstance() );
                }
                catch ( final Exception e )
                {
                    throw new IllegalArgumentException( e );
                }
        }
    }

    private static Registry<AuthSchemeProvider> createAuthSchemeRegistry()
    {
        return RegistryBuilder.<AuthSchemeProvider>create()
            .register( AuthSchemes.BASIC, new BasicSchemeFactory( StandardCharsets.UTF_8 ) )
            .register( AuthSchemes.DIGEST, new DigestSchemeFactory( StandardCharsets.UTF_8 ) )
            .register( AuthSchemes.NTLM, new NTLMSchemeFactory() )
            .build();
    }

    private static Collection<Class<? extends IOException>> getNonRetryableExceptions()
    {
        final List<Class<? extends IOException>> exceptions = new ArrayList<>();
        final ClassLoader loader = AbstractHttpClientWagon.class.getClassLoader();
        for ( final String ex : RETRY_HANDLER_EXCEPTIONS.split( "," ) )
        {
            try
            {
                exceptions.add( ( Class<? extends IOException> ) loader.loadClass( ex ) );
            }
            catch ( final ClassNotFoundException e )
            {
                throw new IllegalArgumentException( e );
            }
        }
        return exceptions;
    }

    private static CloseableHttpClient httpClient = createClient();

    private static CloseableHttpClient createClient()
    {
        return HttpClientBuilder.create() //
            .useSystemProperties() //
            .disableConnectionState() //
            .setConnectionManager( httpClientConnectionManager ) //
            .setRetryHandler( createRetryHandler() )
            .setServiceUnavailableRetryStrategy( createServiceUnavailableRetryStrategy() )
            .setDefaultAuthSchemeRegistry( createAuthSchemeRegistry() )
            .setRedirectStrategy( new WagonRedirectStrategy() )
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

                AuthScope targetScope = getBasicAuthScope().getScope( getRepository().getHost(),
                                                                    getRepository().getPort() );
                credentialsProvider.setCredentials( targetScope, creds );
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

                    AuthScope proxyScope = getProxyBasicAuthScope().getScope( proxyHost, proxyInfo.getPort() );
                    credentialsProvider.setCredentials( proxyScope, creds );
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

    public static void setPersistentPool( boolean persistent )
    {
        persistentPool = persistent;
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
        put( resource, source, new WagonHttpEntity( stream, resource, this, source ) );
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
        return buildUrl( resource.getName() );
    }

    /**
     * Builds a complete URL string from the repository URL and the relative path of the resource passed.
     *
     * @param resourceName the resourcerelative path
     * @return the complete URL
     */
    private String buildUrl( String resourceName )
    {
        return EncodingUtil.encodeURLToString( getRepository().getUrl(), resourceName );
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

        // FIXME Perform only when preemptive has been configured
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
                fireTransferDebug( formatTransferDebugMessage( url, response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );
                int statusCode = response.getStatusLine().getStatusCode();

                // Check that we didn't run out of retries.
                switch ( statusCode )
                {
                    // Success Codes
                    case HttpStatus.SC_OK: // 200
                    case HttpStatus.SC_CREATED: // 201
                    case HttpStatus.SC_ACCEPTED: // 202
                    case HttpStatus.SC_NO_CONTENT:  // 204
                        break;

                    // TODO Move 401/407 to AuthenticationException after WAGON-587
                    case HttpStatus.SC_FORBIDDEN:
                    case HttpStatus.SC_UNAUTHORIZED:
                    case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        fireSessionConnectionRefused();
                        throw new AuthorizationException( formatAuthorizationMessage( url,
                                response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );

                    case HttpStatus.SC_NOT_FOUND:
                    case HttpStatus.SC_GONE:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        throw new ResourceDoesNotExistException( formatResourceDoesNotExistMessage( url,
                                response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );

                    case SC_TOO_MANY_REQUESTS:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        put( backoff( wait, url ), resource, source, httpEntity, url );
                        break;
                    //add more entries here
                    default:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        TransferFailedException e = new TransferFailedException( formatTransferFailedMessage( url,
                                response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );
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
        catch ( IOException | HttpException | InterruptedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( formatTransferFailedMessage( url, getProxyInfo() ), e );
        }

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
        String url = buildUrl( resourceName );
        HttpHead headMethod = new HttpHead( url );
        try
        {
            CloseableHttpResponse response = execute( headMethod );
            try
            {
                int statusCode = response.getStatusLine().getStatusCode();
                boolean result;
                switch ( statusCode )
                {
                    case HttpStatus.SC_OK:
                        result = true;
                        break;
                    case HttpStatus.SC_NOT_MODIFIED:
                        result = true;
                        break;

                    // TODO Move 401/407 to AuthenticationException after WAGON-587
                    case HttpStatus.SC_FORBIDDEN:
                    case HttpStatus.SC_UNAUTHORIZED:
                    case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                        throw new AuthorizationException( formatAuthorizationMessage( url,
                                response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );

                    case HttpStatus.SC_NOT_FOUND:
                    case HttpStatus.SC_GONE:
                        result = false;
                        break;

                    case SC_TOO_MANY_REQUESTS:
                        return resourceExists( backoff( wait, resourceName ), resourceName );

                    //add more entries here
                    default:
                        throw new TransferFailedException( formatTransferFailedMessage( url,
                                response.getStatusLine().getStatusCode(),
                                response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );
                }

                return result;
            }
            finally
            {
                response.close();
            }
        }
        catch ( IOException | HttpException | InterruptedException e )
        {
            throw new TransferFailedException( formatTransferFailedMessage( url, getProxyInfo() ), e );
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
            method.addHeader( "Pragma", "no-cache" );
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
            String value = (String) httpHeaders.get( HTTP.USER_AGENT );
            if ( value != null )
            {
                return value;
            }
        }
        HttpMethodConfiguration config =
            httpConfiguration == null ? null : httpConfiguration.getMethodConfiguration( method );

        if ( config != null )
        {
            return (String) config.getHeaders().get( HTTP.USER_AGENT );
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

        String url = buildUrl( resource );
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

            fireTransferDebug( formatTransferDebugMessage( url, response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );
            int statusCode = response.getStatusLine().getStatusCode();

            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_NOT_MODIFIED:
                    // return, leaving last modified set to original value so getIfNewer should return unmodified
                    return;

                // TODO Move 401/407 to AuthenticationException after WAGON-587
                case HttpStatus.SC_FORBIDDEN:
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    EntityUtils.consumeQuietly( response.getEntity() );
                    fireSessionConnectionRefused();
                    throw new AuthorizationException( formatAuthorizationMessage( url,
                            response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
                            getProxyInfo() ) );

                case HttpStatus.SC_NOT_FOUND:
                case HttpStatus.SC_GONE:
                    EntityUtils.consumeQuietly( response.getEntity() );
                    throw new ResourceDoesNotExistException( formatResourceDoesNotExistMessage( url,
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase(), getProxyInfo() ) );

                case SC_TOO_MANY_REQUESTS:
                    EntityUtils.consumeQuietly( response.getEntity() );
                    fillInputData( backoff( wait, url ), inputData );
                    break;

                // add more entries here
                default:
                    EntityUtils.consumeQuietly( response.getEntity() );
                    cleanupGetTransfer( resource );
                    TransferFailedException e = new TransferFailedException( formatTransferFailedMessage( url,
                            response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
                            getProxyInfo() ) );
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
        catch ( IOException | HttpException | InterruptedException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            throw new TransferFailedException( formatTransferFailedMessage( url, getProxyInfo() ), e );
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

    protected CredentialsProvider getCredentialsProvider()
    {
        return credentialsProvider;
    }

    protected AuthCache getAuthCache()
    {
        return authCache;
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
