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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

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
    private String previousProxyExclusions;

    private String previousHttpProxyHost;

    private String previousHttpProxyPort;

    private HttpURLConnection putConnection;

    /**
     * Whether to use any proxy cache or not.
     * 
     * @plexus.configuration default="false"
     */
    private boolean useCache;

    /** @plexus.configuration */
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
        try
        {
            URL url = new URL( buildUrl( resource.getName() ) );
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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

            InputStream is = urlConnection.getInputStream();
            String contentEncoding = urlConnection.getHeaderField( "Content-Encoding" );
            boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase( contentEncoding );
            if ( isGZipped )
            {
                is = new GZIPInputStream( is );
            }
            inputData.setInputStream( is );
            resource.setLastModified( urlConnection.getLastModified() );
            resource.setContentLength( urlConnection.getContentLength() );
        }
        catch ( MalformedURLException e )
        {
            throw new ResourceDoesNotExistException( "Invalid repository URL", e );
        }
        catch ( FileNotFoundException e )
        {
            throw new ResourceDoesNotExistException( "Unable to locate resource in repository", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file: " + e.getMessage(), e );
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
            putConnection = (HttpURLConnection) url.openConnection();

            addHeaders( putConnection );

            putConnection.setRequestMethod( "PUT" );
            putConnection.setDoOutput( true );
            outputData.setOutputStream( putConnection.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file", e );
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
                    throw new ResourceDoesNotExistException( "File: " + buildUrl( resource.getName() )
                        + " does not exist" );

                    // add more entries here
                default:
                    throw new TransferFailedException( "Failed to transfer file: " + buildUrl( resource.getName() )
                        + ". Return code is: " + statusCode );
            }
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Error transferring file", e );
        }
    }

    protected void openConnectionInternal()
        throws ConnectionException, AuthenticationException
    {
        previousHttpProxyHost = System.getProperty( "http.proxyHost" );
        previousHttpProxyPort = System.getProperty( "http.proxyPort" );
        previousProxyExclusions = System.getProperty( "http.nonProxyHosts" );

        final ProxyInfo proxyInfo = getProxyInfo( "http", getRepository().getHost() );
        if ( proxyInfo != null )
        {
            System.setProperty( "http.proxyHost", proxyInfo.getHost() );
            System.setProperty( "http.proxyPort", String.valueOf( proxyInfo.getPort() ) );
            if ( proxyInfo.getNonProxyHosts() != null )
            {
                System.setProperty( "http.nonProxyHosts", proxyInfo.getNonProxyHosts() );
            }
            else
            {
                System.getProperties().remove( "http.nonProxyHosts" );
            }
        }
        else
        {
            System.getProperties().remove( "http.proxyHost" );
            System.getProperties().remove( "http.proxyPort" );
        }

        final boolean hasProxy = ( proxyInfo != null && proxyInfo.getUserName() != null );
        final boolean hasAuthentication = ( authenticationInfo != null && authenticationInfo.getUserName() != null );
        if ( hasProxy || hasAuthentication )
        {
            Authenticator.setDefault( new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    // TODO: ideally use getRequestorType() from JDK1.5 here...
                    if ( hasProxy && getRequestingHost().equals( proxyInfo.getHost() )
                        && getRequestingPort() == proxyInfo.getPort() )
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

    public void closeConnection()
        throws ConnectionException
    {
        if ( putConnection != null )
        {
            putConnection.disconnect();
        }
        if ( previousHttpProxyHost != null )
        {
            System.setProperty( "http.proxyHost", previousHttpProxyHost );
        }
        else
        {
            System.getProperties().remove( "http.proxyHost" );
        }
        if ( previousHttpProxyPort != null )
        {
            System.setProperty( "http.proxyPort", previousHttpProxyPort );
        }
        else
        {
            System.getProperties().remove( "http.proxyPort" );
        }
        if ( previousProxyExclusions != null )
        {
            System.setProperty( "http.nonProxyHosts", previousProxyExclusions );
        }
        else
        {
            System.getProperties().remove( "http.nonProxyHosts" );
        }
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        InputData inputData = new InputData();

        if ( !destinationDirectory.endsWith( "/" ) )
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
            throw new TransferFailedException( url + " - Could not open input stream for resource: '" + resource
                                               + "'" );
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
            headConnection = (HttpURLConnection) url.openConnection();

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
                    throw new TransferFailedException( "Failed to look for file: " + buildUrl( resourceName )
                        + ". Return code is: " + statusCode );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file", e );
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
}
