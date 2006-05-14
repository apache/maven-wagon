package org.apache.maven.wagon.providers.webdav;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
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
    protected Map headers = new HashMap();

    public CorrectedWebdavResource( HttpURL url )
        throws HttpException, IOException
    {
        super( url );
    }

    /**
     * Add all additionals headers that have been previously registered
     * with addRequestHeader to the method
     */
    protected void generateAdditionalHeaders( HttpMethod method )
    {
        for ( Iterator iterator = headers.keySet().iterator(); iterator.hasNext(); )
        {
            String header = (String) iterator.next();
            method.setRequestHeader( header, (String) headers.get( header ) );
        }
    }

    /**
     * Get InputStream for the GET method for the given path.
     *
     * @param path the server relative path of the resource to get
     * @return InputStream
     * @exception HttpException
     * @exception IOException
     */
    public InputStream getMethodData( String path )
        throws HttpException, IOException
    {

        setClient();

        GetMethod method = new GetMethod( URIUtil.encodePathQuery( path ) );

        generateTransactionHeader( method );
        generateAdditionalHeaders( method );
        client.executeMethod( method );

        int statusCode = method.getStatusLine().getStatusCode();
        setStatusCode( statusCode );

        if ( statusCode >= 200 && statusCode < 300 )
            return method.getResponseBodyAsStream();
        else
            throw new IOException( "Couldn't get file" );
    }

    /**
     * Add a header in the request sent to the webdav server
     *
     * @param header Header name
     * @param value Value
     */
    public void addRequestHeader( String header, String value )
    {
        headers.put( header, value );
    }

    /**
     * Execute the PUT method for the given path.
     *
     * @param path the server relative path to put the data
     * @param is The input stream.
     * @return true if the method is succeeded.
     * @exception HttpException
     * @exception IOException
     */
    public boolean putMethod( String path, InputStream is, int contentLength )
        throws HttpException, IOException
    {

        setClient();
        PutMethod method = new PutMethod( URIUtil.encodePathQuery( path ) );
        generateIfHeader( method );
        if ( getGetContentType() != null && !getGetContentType().equals( "" ) )
            method.setRequestHeader( "Content-Type", getGetContentType() );
        method.setRequestContentLength( contentLength );
        method.setRequestBody( is );
        generateTransactionHeader( method );
        generateAdditionalHeaders( method );
        int statusCode = client.executeMethod( method );

        setStatusCode( statusCode );
        return ( statusCode >= 200 && statusCode < 300 ) ? true : false;
    }

}
