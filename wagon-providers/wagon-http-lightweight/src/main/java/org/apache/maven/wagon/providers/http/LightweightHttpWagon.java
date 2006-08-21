package org.apache.maven.wagon.providers.http;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class LightweightHttpWagon
    extends StreamWagon
{
    private String previousProxyExclusions;

    private String previousProxyHost;

    private String previousProxyPort;

    private HttpURLConnection putConnection;

    /**
     * Whether to use any proxy cache or not.
     *
     * @component.configuration default="false"
     */
    private boolean useCache;

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        Resource resource = inputData.getResource();
        try
        {
            URL url = resolveResourceURL( resource );
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty( "Accept-Encoding", "gzip" );
            if ( !useCache )
            {
                urlConnection.setRequestProperty( "Pragma", "no-cache" );
            }
            InputStream is = urlConnection.getInputStream();
            String contentEncoding = urlConnection.getHeaderField( "Content-Encoding" );
            boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding);
            if (isGZipped)
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
            throw new TransferFailedException( "Error transferring file", e );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        Resource resource = outputData.getResource();
        try
        {
            URL url = resolveResourceURL( resource );
            putConnection = (HttpURLConnection) url.openConnection();

            putConnection.setRequestMethod( "PUT" );
            putConnection.setDoOutput( true );
            outputData.setOutputStream( putConnection.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file", e );
        }
    }

    private URL resolveResourceURL( Resource resource )
        throws MalformedURLException
    {
        String repositoryUrl = getRepository().getUrl();
        
        URL url;
        if ( repositoryUrl.endsWith( "/" ) )
        {
            url = new URL( repositoryUrl + resource.getName() );
        }
        else
        {
            url = new URL( repositoryUrl + "/" + resource.getName() );
        }
        return url;
    }


    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        super.put( source, resourceName );

        try
        {
            String url = getRepository().getUrl() + "/" + resourceName;
            int statusCode = putConnection.getResponseCode();

            switch ( statusCode )
            {
                case HttpURLConnection.HTTP_OK:
                    break;

                case HttpURLConnection.HTTP_CREATED:
                    break;

                case HttpURLConnection.HTTP_NO_CONTENT:
                    break;

                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                //add more entries here
                default :
                    throw new TransferFailedException(
                        "Failed to transfer file: " + url + ". Return code is: " + statusCode );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file", e );
        }
    }


    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        previousProxyHost = System.getProperty( "http.proxyHost" );
        previousProxyPort = System.getProperty( "http.proxyPort" );
        previousProxyExclusions = System.getProperty( "http.nonProxyHosts" );

        final ProxyInfo proxyInfo = this.proxyInfo;
        if ( proxyInfo != null )
        {
            System.setProperty( "http.proxyHost", proxyInfo.getHost() );
            System.setProperty( "http.proxyPort", String.valueOf( proxyInfo.getPort() ) );
            if ( proxyInfo.getNonProxyHosts() != null )
            {
                System.setProperty( "http.nonProxyHosts", proxyInfo.getNonProxyHosts() );
            }
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
                    if ( hasProxy && getRequestingHost().equals( proxyInfo.getHost() ) &&
                        getRequestingPort() == proxyInfo.getPort() )
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
    }

    public void closeConnection()
        throws ConnectionException
    {
        if ( putConnection != null )
        {
            putConnection.disconnect();
        }
        if ( previousProxyHost != null )
        {
            System.setProperty( "http.proxyHost", previousProxyHost );
        }
        if ( previousProxyPort != null )
        {
            System.setProperty( "http.proxyPort", previousProxyPort );
        }
        if ( previousProxyExclusions != null )
        {
            System.setProperty( "http.nonProxyHosts", previousProxyExclusions );
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

        String url = getRepository().getUrl() + "/" + destinationDirectory;

        Resource resource = new Resource( destinationDirectory );

        inputData.setResource( resource );

        fillInputData( inputData );

        InputStream is = inputData.getInputStream();

        if ( is == null )
        {
            throw new TransferFailedException( url + " - Could not open input stream for resource: '" + resource + "'" );
        }

        return HtmlFileListParser.parseFileList( url, is );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        HttpURLConnection headConnection;
        
        try
        {
            URL url = resolveResourceURL( new Resource(resourceName) );
            headConnection = (HttpURLConnection) url.openConnection();
    
            headConnection.setRequestMethod( "HEAD" );
            headConnection.setDoOutput( true );
            
            int statusCode = headConnection.getResponseCode();

            switch ( statusCode )
            {
                case HttpURLConnection.HTTP_OK:
                    return true;

                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AuthorizationException( "Access denided to: " + url );

                case HttpURLConnection.HTTP_NOT_FOUND:
                    return false;
            }
        } catch ( IOException e )
        {
            throw new TransferFailedException( "Error transferring file", e );
        }
        
        return false;
    }
}

