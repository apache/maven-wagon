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

import java.net.URI;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

/**
 * A custom redirect strategy for Apache Maven Wagon HttpClient.
 *
 * @since 3.4.0
 *
 */
public class WagonRedirectStrategy extends DefaultRedirectStrategy
{

    private static final int SC_PERMANENT_REDIRECT = 308;

    public WagonRedirectStrategy()
    {
        super( new String[] {
                HttpGet.METHOD_NAME,
                HttpHead.METHOD_NAME,
                HttpPut.METHOD_NAME,
                "MKCOL" } );
    }

    @Override
    public boolean isRedirected( final HttpRequest request, final HttpResponse response,
            final HttpContext context ) throws ProtocolException
    {
        Args.notNull( request, "HTTP request" );
        Args.notNull( response, "HTTP response" );

        final int statusCode = response.getStatusLine().getStatusCode();
        final String method = request.getRequestLine().getMethod();
        switch ( statusCode )
        {
        case HttpStatus.SC_MOVED_TEMPORARILY:
        case HttpStatus.SC_MOVED_PERMANENTLY:
        case HttpStatus.SC_SEE_OTHER:
        case HttpStatus.SC_TEMPORARY_REDIRECT:
        case SC_PERMANENT_REDIRECT:
            return isRedirectable( method );
        default:
            return false;
        }
    }

    @Override
    public HttpUriRequest getRedirect( final HttpRequest request, final HttpResponse response,
            final HttpContext context ) throws ProtocolException
    {
        final URI uri = getLocationURI( request, response, context );
        return RequestBuilder.copy( request ).setUri( uri ).build();
    }

}
