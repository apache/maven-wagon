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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
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
import java.net.URLEncoder;
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

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    /**
     * use http(s) connection pool mechanism.
     * <b>enabled by default</b>
     */
    private final static boolean PERSISTENT_POOL =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.pool", "true" ) );

    /**
     * skip failure on certificate validity checks.
     * <b>disabled by default</b>
     */
    private final static boolean SSL_INSECURE =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.insecure", "false" ) );

    /**
     * if using sslInsecure, certificate date issues will be ignored
     * <b>disabled by default</b>
     */
    private final static boolean IGNORE_SSL_VALIDITY_DATES =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.ignore.validity.dates", "false" ) );

    /**
     * If enabled, ssl hostname verifier does not check hostname. Disable this will use a browser compat hostname verifier
     * <b>disabled by default</b>
     */
    private final static boolean SSL_ALLOW_ALL =
        Boolean.valueOf( System.getProperty( "maven.wagon.http.ssl.allowall", "false" ) );


    /**
     * Maximum conncurrent connections per distinct route.
     * <b>20 by default</b>
     */
    private final static int MAX_CONN_PER_ROUTE =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxPerRoute", "20" ) );

    /**
     * Maximum conncurrent connections in total.
     * <b>40 by default</b>
     */
    private final static int MAX_CONN_TOTAL =
        Integer.parseInt( System.getProperty( "maven.wagon.httpconnectionManager.maxTotal", "40" ) );

    /**
     * Internal connection manager
     */
    private static final PoolingHttpClientConnectionManager CONN_MAN = createConnManager();

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
        if ( PERSISTENT_POOL )
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

    private static CloseableHttpClient CLIENT = createClient();

    private static CloseableHttpClient createClient()
    {
        return HttpClientBuilder.create().useSystemProperties().disableConnectionState().setConnectionManager(
            CONN_MAN ).build();
    }

    private static String DEFAULT_USER_AGENT = getDefaultUserAgent();

    private static String getDefaultUserAgent()
    {
        Properties props = new Properties();

        InputStream is = AbstractHttpClientWagon.class.getResourceAsStream(
            "/META-INF/maven/org.apache.maven.wagon/wagon-http/pom.properties" );
        if ( is != null )
        {
            try
            {
                props.load( is );
            }
            catch ( IOException ignore )
            {
            }
            finally
            {
                IOUtil.close( is );
            }
        }

        String ver = props.getProperty( "version", "unknown-version" );
        return "Apache-Maven-Wagon/" + ver + " (Java " + System.getProperty( "java.version" ) + "; ";
    }


    private CredentialsProvider credentialsProvider;

    private AuthCache authCache;

    private HttpClientContext localContext;

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

    public void openConnectionInternal()
    {
        repository.setUrl( getURL( repository ) );

        localContext = HttpClientContext.create();
        credentialsProvider = new BasicCredentialsProvider();
        authCache = new BasicAuthCache();
        localContext.setCredentialsProvider( credentialsProvider );
        localContext.setAuthCache( authCache );

        if ( authenticationInfo != null )
        {

            String username = authenticationInfo.getUserName();
            String password = authenticationInfo.getPassword();

            if ( StringUtils.isNotEmpty( username ) && StringUtils.isNotEmpty( password ) )
            {
                Credentials creds = new UsernamePasswordCredentials( username, password );

                String host = getRepository().getHost();
                int port = getRepository().getPort() > -1 ? getRepository().getPort() : AuthScope.ANY_PORT;

                credentialsProvider.setCredentials( new AuthScope( host, port ), creds );

                // This is needed In order to use the preemptive basic authentication
                enablePreemptiveConfiguration();
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

                    int port = proxyInfo.getPort() > -1 ? proxyInfo.getPort() : AuthScope.ANY_PORT;

                    AuthScope authScope = new AuthScope( proxyHost, port );
                    credentialsProvider.setCredentials( authScope, creds );
                }
            }
        }
    }

    private void enablePreemptiveConfiguration() {
        HttpMethodConfiguration httpMethodConfiguration = new HttpMethodConfiguration();
        httpMethodConfiguration.setUsePreemptive( true );
        HttpConfiguration config = new HttpConfiguration();
        config.setAll( httpMethodConfiguration );
        setHttpConfiguration( config );
    }

    public void closeConnection()
    {
        if ( !PERSISTENT_POOL )
        {
            CONN_MAN.closeIdleConnections( 0, TimeUnit.MILLISECONDS );
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

        // preemptive for put
        // TODO: is it a good idea, though? 'Expect-continue' handshake would serve much better

        Repository repo = getRepository();
        HttpHost targetHost = new HttpHost( repo.getHost(), repo.getPort(), repo.getProtocol() );
        AuthScope targetScope = new AuthScope( targetHost );

        if ( credentialsProvider.getCredentials( targetScope ) != null )
        {
            BasicScheme targetAuth = new BasicScheme();
            try {
                targetAuth.processChallenge(new BasicHeader(AUTH.WWW_AUTH, "BASIC preemptive"));
                authCache.put( targetHost, targetAuth  );
            }
            catch ( MalformedChallengeException ignore )
            {
            }
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
                    // handle all redirect even if http specs says " the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user"
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
        }

        localContext.setRequestConfig( requestConfigBuilder.build() );

        if ( config != null && config.isUsePreemptive() )
        {
            HttpHost targetHost = new HttpHost( repo.getHost(), repo.getPort(), repo.getProtocol() );
            AuthScope targetScope = new AuthScope( targetHost );

            if ( credentialsProvider.getCredentials( targetScope ) != null )
            {
                BasicScheme targetAuth = new BasicScheme();
                targetAuth.processChallenge( new BasicHeader(AUTH.WWW_AUTH, "BASIC preemptive" ) );
                authCache.put( targetHost, targetAuth  );
            }
        }

        if ( proxyInfo != null )
        {
            if ( proxyInfo.getHost() != null )
            {
                HttpHost proxyHost = new HttpHost( proxyInfo.getHost(), proxyInfo.getPort() );
                AuthScope proxyScope = new AuthScope( proxyHost );

                String proxyUsername = proxyInfo.getUserName();
                String proxyPassword = proxyInfo.getPassword();
                String proxyNtlmHost = proxyInfo.getNtlmHost();
                String proxyNtlmDomain = proxyInfo.getNtlmDomain();

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

                    credentialsProvider.setCredentials( proxyScope, creds );
                    BasicScheme proxyAuth = new BasicScheme();
                    proxyAuth.processChallenge( new BasicHeader(AUTH.PROXY_AUTH, "BASIC preemptive" ) );
                    authCache.put( proxyHost, proxyAuth );
                }
            }
        }

        return CLIENT.execute( httpMethod, localContext );
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
            method.addHeader( "User-Agent", DEFAULT_USER_AGENT );
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
            for ( Header header : headers )
            {
                method.addHeader( header );
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
                    fireTransferDebug( "last-modified = " + lastModifiedHeader.getValue() +
                                           " (" + lastModified.getTime() + ")" );
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
}