package org.apache.maven.wagon.tck.http.fixture;

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
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.plexus.util.Base64;

/**
 *
 */
public class ProxyAuthenticationFilter
    implements Filter
{

    private final String username;

    private final String password;

    public ProxyAuthenticationFilter( final String username, final String password )
    {
        this.username = username;
        this.password = password;
    }

    public void destroy()
    {
    }

    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String header = request.getHeader( "Proxy-Authorization" );
        if ( header == null )
        {
            response.setStatus( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
            response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );
            return;
        }
        else
        {
            String data = header.substring( "BASIC ".length() );
            data = new String( Base64.decodeBase64( data.getBytes( StandardCharsets.US_ASCII ) ) );
            String[] creds = data.split( ":" );

            if ( !creds[0].equals( username ) || !creds[1].equals( password ) )
            {
                response.sendError( HttpServletResponse.SC_UNAUTHORIZED );
            }
        }

        chain.doFilter( req, resp );
    }

    public void init( final FilterConfig filterConfig )
        throws ServletException
    {
    }

}
