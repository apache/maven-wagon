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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * LightweightHttpWagon
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="http" instantiation-strategy="per-lookup"
 */
public class LightweightHttpWagon
    extends StreamWagon
{
    private HttpURLConnection putConnection;

    private Proxy proxy = Proxy.NO_PROXY;

    public static final int MAX_REDIRECTS = 10;

    /**
     * Whether to use any proxy cache or not.
     *
     * @plexus.configuration default="false"
     */
    private boolean useCache;

    /**
     * @plexus.configuration
     */
    private Properties httpHeaders;

    /**
     * Builds a complete URL string from the repository URL and the relative path passed.
     *
     * @param path the relative path
     * @return the complete URL
     */
    private String buildUrl( String path )
    {
        final String repoUrl = getRepository().getUrl();

        path = path.replace( ' ', '+' );

        if ( repoUrl.charAt( repoUrl.length() - 1 ) != '/' )
        {
            return repoUrl + '/' + path;
        }

        return repoUrl + path;
    }

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = inputData.getResource();

        String visitingUrl = buildUrl( resource.getName() );
        try
        {
            List<String> visitedUrls = new ArrayList<String>();

            for ( int redirectCount = 0; redirectCount < MAX_REDIRECTS; redirectCount++ )
            {
                if ( visitedUrls.contains( visitingUrl ) )
                {
                    throw new TransferFailedException( "Cyclic http redirect detected. Aborting! " + visitingUrl );
                }
                visitedUrls.add( visitingUrl );

                URL url = new URL( visitingUrl );
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection( this.proxy );
                urlConnection.setRequestProperty( "Accept-Encoding", "gzip" );
                if ( !useCache )
                {
                    urlConnection.setRequestProperty( "Pragma", "no-cache" );
                }

                addHeaders( urlConnection );

                // TODO: handle all response codes
                int responseCode = urlConnection.getResponseCode();
                if ( responseCode == HttpURLConnection.HTTP_FORBIDDEN
                    || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED )
                {
                    throw new AuthorizationException( "Access denied to: " + buildUrl( resource.getName() ) );
                }
                if ( responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP )
                {
                    visitingUrl = urlConnection.getHeaderField( "Location" );
                    continue;
                }

                InputStream is = urlConnection.getInputStream();
                String contentEncoding = urlConnection.getHeaderField( "Content-Encoding" );
                boolean isGZipped = contentEncoding != null && "gzip".equalsIgnoreCase( contentEncoding );
                if ( isGZipped )
                {
                    is = new GZIPInputStream( is );
                }
                inputData.setInputStream( is );
                resource.setLastModified( urlConnection.getLastModified() );
                resource.setContentLength( urlConnection.getContentLength() );
                break;
            }
        }
        catch ( MalformedURLException e )
        {
            throw new ResourceDoesNotExistException( "Invalid repository URL: " + e.getMessage(), e );
        }
        catch ( FileNotFoundException e )
        {
            throw new ResourceDoesNotExistException( "Unable to locate resource in repository", e );
        }
        catch ( IOException e )
        {
            StringBuilder message = new StringBuilder( "Error transferring file: " );
            message.append( e.getMessage() );
            message.append( " from " + visitingUrl );
            if ( getProxyInfo() != null && getProxyInfo().getHost() != null )
            {
                message.append( " with proxyInfo " ).append( getProxyInfo().toString() );
            }
            throw new TransferFailedException( message.toString(), e );
        }
    }

    private void addHeaders( URLConnection urlConnection )
    {
        if ( httpHeaders != null )
        {
            for ( Iterator i = httpHeaders.keySet().iterator(); i.hasNext(); )
            {
                String header = (String) i.next();
                urlConnection.setRequestProperty( header, httpHeaders.getProperty( header ) );
            }
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        Resource resource = outputData.getResource();
        try
        {
            URL url = new URL( buildUrl( resource.getName() ) );
            putConnection = (HttpURLConnection) url.openConnection( this.proxy );

            addHeaders( putConnection );

            putConnection.setRequestMethod( "PUT" );
            putConnection.setDoOutput( true );
            outputData.setOutputStream( putConnection.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file: " + e.getMessage(), e );
        }
    }

    protected void finishPutTransfer( Resource resource, InputStream input, OutputStream output )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        try
        {
            int statusCode = putConnection.getResponseCode();

            switch ( statusCode )
            {
                // Success Codes
                case HttpURLConnection.HTTP_OK: // 200
                case HttpURLConnection.HTTP_CREATED: // 201
                case HttpURLConnection.HTTP_ACCEPTED: // 202
                case HttpURLConnection.HTTP_NO_CONTENT: // 204
                    break;

                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + buildUrl( resource.getName() ) );

                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new ResourceDoesNotExistException(
                        "File: " + buildUrl( resource.getName() ) + " does not exist" );

                    // add more entries here
                default:
                    throw new TransferFailedException(
                        "Failed to transfer file: " + buildUrl( resource.getName() ) + ". Return code is: "
                            + statusCode );
            }
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error transferring file: " + e.getMessage(), e );
        }
    }

    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
        final ProxyInfo proxyInfo = getProxyInfo( "http", getRepository().getHost() );
        if ( proxyInfo != null )
        {
            this.proxy = getProxy( proxyInfo );
        }

        final boolean hasProxy = ( proxyInfo != null && proxyInfo.getUserName() != null );
        final boolean hasAuthentication = ( authenticationInfo != null && authenticationInfo.getUserName() != null );
        if ( hasProxy || hasAuthentication )
        {
            Authenticator.setDefault( new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    if ( getRequestorType() == RequestorType.PROXY )
                    {
                        String password = "";
                        if ( proxyInfo.getPassword() != null )
                        {
                            password = proxyInfo.getPassword();
                        }
                        return new PasswordAuthentication( proxyInfo.getUserName(), password.toCharArray() );
                    }

                    if ( hasAuthentication )
                    {
                        String password = "";
                        if ( authenticationInfo.getPassword() != null )
                        {
                            password = authenticationInfo.getPassword();
                        }
                        return new PasswordAuthentication( authenticationInfo.getUserName(), password.toCharArray() );
                    }

                    return super.getPasswordAuthentication();
                }
            } );
        }
        else
        {
            Authenticator.setDefault( null );
        }
    }

    private Proxy getProxy( ProxyInfo proxyInfo )
    {
        return new Proxy( getProxyType( proxyInfo ), getSocketAddress( proxyInfo ) );
    }

    private Type getProxyType( ProxyInfo proxyInfo ) {
        if ( ProxyInfo.PROXY_SOCKS4.equals( proxyInfo.getType() ) || ProxyInfo.PROXY_SOCKS5.equals( proxyInfo.getType() ) )
        {
            return Type.SOCKS;
        }
        else return Type.HTTP;
    }

    public SocketAddress getSocketAddress( ProxyInfo proxyInfo )
    {
        return InetSocketAddress.createUnresolved(proxyInfo.getHost(), proxyInfo.getPort());
    }

    public void closeConnection()
        throws ConnectionException
    {
        if ( putConnection != null )
        {
            putConnection.disconnect();
        }
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        InputData inputData = new InputData();

        if ( destinationDirectory.length() > 0 && !destinationDirectory.endsWith( "/" ) )
        {
            destinationDirectory += "/";
        }

        String url = buildUrl( destinationDirectory );

        Resource resource = new Resource( destinationDirectory );

        inputData.setResource( resource );

        fillInputData( inputData );

        InputStream is = inputData.getInputStream();

        if ( is == null )
        {
            throw new TransferFailedException(
                url + " - Could not open input stream for resource: '" + resource + "'" );
        }

        return HtmlFileListParser.parseFileList( url, is );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        HttpURLConnection headConnection;

        try
        {
            URL url = new URL( buildUrl( new Resource( resourceName ).getName() ) );
            headConnection = (HttpURLConnection) url.openConnection( this.proxy );

            addHeaders( headConnection );

            headConnection.setRequestMethod( "HEAD" );
            headConnection.setDoOutput( true );

            int statusCode = headConnection.getResponseCode();

            switch ( statusCode )
            {
                case HttpURLConnection.HTTP_OK:
                    return true;

                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpURLConnection.HTTP_NOT_FOUND:
                    return false;

                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new AuthorizationException( "Access denied to: " + url );

                default:
                    throw new TransferFailedException(
                        "Failed to look for file: " + buildUrl( resourceName ) + ". Return code is: " + statusCode );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file: " + e.getMessage(), e );
        }
    }

    public boolean isUseCache()
    {
        return useCache;
    }

    public void setUseCache( boolean useCache )
    {
        this.useCache = useCache;
    }

    public Properties getHttpHeaders()
    {
        return httpHeaders;
    }

    public void setHttpHeaders( Properties httpHeaders )
    {
        this.httpHeaders = httpHeaders;
    }

    void setSystemProperty( String key, String value )
    {
        if ( value != null )
        {
            System.setProperty( key, value );
        }
        else
        {
            System.getProperties().remove( key );
        }
    }

}
