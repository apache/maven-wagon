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
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class ProxyConnectionVerifierFilter
    implements Filter
{
    private static Logger logger = LoggerFactory.getLogger( ProxyConnectionVerifierFilter.class );

    public void destroy()
    {
    }

    @SuppressWarnings( "unchecked" )
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        // HttpServletResponse response = (HttpServletResponse) resp;

        Enumeration<String> kEn = request.getHeaderNames();
        for ( String key : Collections.list( kEn ) )
        {
            if ( key == null )
            {
                continue;
            }

            Enumeration<String> vEn = request.getHeaders( key );
            if ( vEn != null )
            {
                for ( String val : Collections.list( vEn ) )
                {
                    logger.info( key + ": " + val );
                }
            }
        }

        chain.doFilter( req, resp );
    }

    public void init( final FilterConfig filterConfig )
        throws ServletException
    {
    }

}
