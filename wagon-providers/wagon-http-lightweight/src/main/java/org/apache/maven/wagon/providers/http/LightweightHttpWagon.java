package org.apache.maven.wagon.providers.http;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

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

    public void fillInputData( InputData inputData )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        Repository repository = getRepository();
        String repositoryUrl = repository.getUrl();
        Resource resource = inputData.getResource();
        try
              {
            URL url;
            if ( repositoryUrl.endsWith( "/" ) )
            {
                url = new URL( repositoryUrl + resource.getName() );
            }
            else
            {
                url = new URL( repositoryUrl + "/" + resource.getName() );
            }
            URLConnection urlConnection = url.openConnection();
            inputData.setInputStream( urlConnection.getInputStream() );
            resource.setLastModified( urlConnection.getLastModified() );
            resource.setContentLength( urlConnection.getContentLength() );
        }
        catch ( MalformedURLException e )
        {
            throw new ResourceDoesNotExistException( e.getMessage() );
        }
        catch ( FileNotFoundException e )
        {
            throw new ResourceDoesNotExistException( e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage() );
        }
    }

    public void fillOutputData( OutputData outputData )
        throws TransferFailedException
    {
        throw new UnsupportedOperationException( "PUT operation is not supported by Light Weight  HTTP wagon" );
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        previousProxyHost = System.getProperty( "http.proxyHost" );
        previousProxyPort = System.getProperty( "http.proxyPort" );
        previousProxyExclusions = System.getProperty( "http.nonProxyHosts" );

        if ( proxyInfo != null )
        {
            System.setProperty( "http.proxyHost", proxyInfo.getHost() );
            System.setProperty( "http.proxyPort", String.valueOf( proxyInfo.getPort() ) );
            System.setProperty( "http.nonProxyHosts", proxyInfo.getNonProxyHosts() );
            if ( proxyInfo.getUserName() != null )
            {
                Authenticator.setDefault( new Authenticator()
                {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        String password = "";
                        if ( proxyInfo.getPassword() != null )
                        {
                            password = proxyInfo.getPassword();
                        }
                        return new PasswordAuthentication( proxyInfo.getUserName(), password.toCharArray() );
                    }
                } );
            }
        }
    }

    public void closeConnection()
        throws ConnectionException
    {
        System.setProperty( "http.proxyHost", previousProxyHost );
        System.setProperty( "http.proxyPort", previousProxyPort );
        System.setProperty( "http.nonProxyHosts", previousProxyExclusions );
    }
}

