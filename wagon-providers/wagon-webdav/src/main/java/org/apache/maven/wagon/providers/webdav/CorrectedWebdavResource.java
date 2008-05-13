package org.apache.maven.wagon.providers.webdav;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.webdav.lib.WebdavResource;

/**
 * Corrected Webdav Resource.
 * 
 * This extension to the WebdavResource object corrects a Content-Length
 * bug in the WebdavLib.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class CorrectedWebdavResource
    extends WebdavResource
{
    /**
     * Map of additional headers
     */
    private Map headers = new HashMap();

    public CorrectedWebdavResource( HttpURL url )
        throws IOException
    {
        super( url );
        setFollowRedirects( true ); // TODO: Make this configurable.
    }
    
    public void setConnectionTimeout(int timeout)
    {
    	client.setConnectionTimeout(timeout);
    }

    public void setHeaders( Map headers )
    {
        this.headers = headers;
    }

    public Map getHeaders()
    {
        return headers;
    }

    /**
     * Add all additionals headers that have been previously registered
     * with addRequestHeader to the method
     */
    protected void generateAdditionalHeaders( HttpMethod method )
    {
        Iterator iterator = getHeaders().keySet().iterator();
        while ( iterator.hasNext() )
        {
            String header = (String) iterator.next();
            method.setRequestHeader( header, (String) getHeaders().get( header ) );
        }
    }

    /**
     * Get InputStream for the GET method for the given path.
     *
     * @param path the server relative path of the resource to get
     * @throws IOException
     * @return InputStream
     */
    public InputStream getMethodData( String path )
        throws IOException
    {
        setClient();

        GetMethod method = new GetMethod( URIUtil.encodePathQuery( path ) );
        method.setFollowRedirects( super.followRedirects );

        generateTransactionHeader( method );
        generateAdditionalHeaders( method );
        client.executeMethod( method );

        int statusCode = method.getStatusLine().getStatusCode();
        setStatusCode( statusCode );

        if ( isHttpSuccess( statusCode ) )
        {
            Header contentEncoding = method.getResponseHeader( "Content-Encoding" );
            boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding.getValue());
            
            if (isGZipped)
            {
                return new GZIPInputStream(method.getResponseBodyAsStream());
            }
            return method.getResponseBodyAsStream();
        }
        else
        {
            throw new IOException( "Couldn't get file" );
        }
    }

    /**
     * Add a header in the request sent to the webdav server
     *
     * @param header Header name
     * @param value Value
     */
    public void addRequestHeader( String header, String value )
    {
        getHeaders().put( header, value );
    }

    /**
     * Execute the PUT method for the given path.
     *
     * @param path the server relative path to put the data
     * @param inputStream The input stream.
     * @throws IOException
     * @return true if the method is succeeded.
     */
    public boolean putMethod( String path, InputStream inputStream, int contentLength )
        throws IOException
    {

        setClient();
        PutMethod method = new PutMethod( URIUtil.encodePathQuery( path ) );
        method.setFollowRedirects( super.followRedirects );
        generateIfHeader( method );
        if ( getGetContentType() != null && !getGetContentType().equals( "" ) )
        {
            method.setRequestHeader( "Content-Type", getGetContentType() );
        }
        method.setRequestContentLength( contentLength );
        method.setRequestBody( inputStream );
        generateTransactionHeader( method );
        generateAdditionalHeaders( method );
        int statusCode = client.executeMethod( method );

        setStatusCode( statusCode );
        return isHttpSuccess( statusCode );
    }

    /**
     * Check if the http status code passed as argument is a success 
     * @param statusCode
     * @return true if code represents a HTTP success
     */
    private boolean isHttpSuccess( int statusCode )
    {
        return ( statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES );
    }

}
