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

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;
import org.apache.maven.wagon.shared.http.HttpMessageUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatResourceDoesNotExistMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatTransferDebugMessage;
import static org.apache.maven.wagon.shared.http.HttpMessageUtils.formatTransferFailedMessage;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class HttpWagon
    extends AbstractHttpClientWagon
{

    public List<String> getFileList( String destinationDirectory )
        throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException
    {
        return getFileList( getInitialBackoffSeconds(), destinationDirectory );
    }

    private List<String> getFileList( int wait, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( destinationDirectory.length() > 0 && !destinationDirectory.endsWith( "/" ) )
        {
            destinationDirectory += "/";
        }

        String url = getRepository().getUrl() + "/" + destinationDirectory;

        HttpGet getMethod = new HttpGet( url );

        try
        {
            CloseableHttpResponse response = execute( getMethod );
            try
            {
                String reasonPhrase = response.getStatusLine().getReasonPhrase();
                int statusCode = response.getStatusLine().getStatusCode();

                fireTransferDebug( formatTransferDebugMessage( url, statusCode, reasonPhrase, getProxyInfo() ) );

                switch ( statusCode )
                {
                    case HttpStatus.SC_OK:
                        break;

                    // TODO Move 401/407 to AuthenticationException after WAGON-587
                    case HttpStatus.SC_FORBIDDEN:
                    case HttpStatus.SC_UNAUTHORIZED:
                    case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        throw new AuthorizationException( HttpMessageUtils.formatAuthorizationMessage( url, statusCode,
                                reasonPhrase, getProxyInfo() ) );

                    case HttpStatus.SC_NOT_FOUND:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        throw new ResourceDoesNotExistException( formatResourceDoesNotExistMessage( url, statusCode,
                                reasonPhrase, getProxyInfo() ) );

                    case SC_TOO_MANY_REQUESTS:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        return getFileList( backoff( wait, url ), destinationDirectory );

                    //add more entries here
                    default:
                        EntityUtils.consumeQuietly( response.getEntity() );
                        throw new TransferFailedException( formatTransferFailedMessage( url, statusCode, reasonPhrase,
                                getProxyInfo() ) );
                }
                HttpEntity entity = response.getEntity();
                if ( entity != null )
                {
                    return HtmlFileListParser.parseFileList( url, entity.getContent() );
                }
                else
                {
                    return Collections.emptyList();
                }

            }
            finally
            {
                response.close();
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Could not read response body.", e );
        }
        catch ( HttpException e )
        {
            throw new TransferFailedException( "Could not read response body.", e );
        }
        catch ( InterruptedException e )
        {
            throw new TransferFailedException( "Unable to wait for resource.", e );
        }
    }

}
