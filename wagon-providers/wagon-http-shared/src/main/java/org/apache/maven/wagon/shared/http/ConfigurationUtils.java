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

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.message.BasicHeader;
import org.apache.maven.wagon.Wagon;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration helper class
 */
public class ConfigurationUtils
{

    private static final String SO_TIMEOUT                  = "http.socket.timeout";
    private static final String STALE_CONNECTION_CHECK      = "http.connection.stalecheck";
    private static final String CONNECTION_TIMEOUT          = "http.connection.timeout";
    private static final String USE_EXPECT_CONTINUE         = "http.protocol.expect-continue";
    private static final String DEFAULT_PROXY               = "http.route.default-proxy";
    private static final String LOCAL_ADDRESS               = "http.route.local-address";
    private static final String PROXY_AUTH_PREF             = "http.auth.proxy-scheme-pref";
    private static final String TARGET_AUTH_PREF            = "http.auth.target-scheme-pref";
    private static final String HANDLE_AUTHENTICATION       = "http.protocol.handle-authentication";
    private static final String ALLOW_CIRCULAR_REDIRECTS    = "http.protocol.allow-circular-redirects";
    private static final String CONN_MANAGER_TIMEOUT        = "http.conn-manager.timeout";
    private static final String COOKIE_POLICY               = "http.protocol.cookie-policy";
    private static final String MAX_REDIRECTS               = "http.protocol.max-redirects";
    private static final String HANDLE_REDIRECTS            = "http.protocol.handle-redirects";
    private static final String REJECT_RELATIVE_REDIRECT    = "http.protocol.reject-relative-redirect";
    private static final String HANDLE_CONTENT_COMPRESSION  = "http.protocol.handle-content-compression";
    // TODO Add contentCompressionEnabled and normalizeUri

    private static final String COERCE_PATTERN = "%(\\w+),(.+)";

    public static void copyConfig( HttpMethodConfiguration config, RequestConfig.Builder builder )
    {
        if ( config.getConnectionTimeout() > 0 )
        {
            builder.setConnectTimeout( config.getConnectionTimeout() );
        }
        if ( config.getReadTimeout() > 0 )
        {
            builder.setSocketTimeout( config.getReadTimeout() );
        }
        Properties params = config.getParams();
        if ( params != null )
        {

            Pattern coercePattern = Pattern.compile( COERCE_PATTERN );
            for ( Map.Entry entry : params.entrySet() )
            {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                Matcher matcher = coercePattern.matcher( value );
                if ( matcher.matches() )
                {
                    value = matcher.group( 2 );
                }

                if ( key.equals( SO_TIMEOUT ) )
                {
                    builder.setSocketTimeout( Integer.parseInt( value ) );
                }
                else if ( key.equals( STALE_CONNECTION_CHECK ) )
                {
                    builder.setStaleConnectionCheckEnabled( Boolean.valueOf( value ) );
                }
                else if ( key.equals( CONNECTION_TIMEOUT ) )
                {
                    builder.setConnectTimeout( Integer.parseInt( value ) );
                }
                else if ( key.equals( USE_EXPECT_CONTINUE ) )
                {
                    builder.setExpectContinueEnabled( Boolean.valueOf( value ) );
                }
                else if ( key.equals( DEFAULT_PROXY ) )
                {
                    builder.setProxy( HttpHost.create( value ) );
                }
                else if ( key.equals( LOCAL_ADDRESS ) )
                {
                    try
                    {
                        builder.setLocalAddress( InetAddress.getByName( value ) );
                    }
                    catch ( UnknownHostException ignore )
                    {
                        // ignore
                    }
                }
                else if ( key.equals( PROXY_AUTH_PREF ) )
                {
                    builder.setProxyPreferredAuthSchemes( Arrays.asList( value.split( "," ) ) );
                }
                else if ( key.equals( TARGET_AUTH_PREF ) )
                {
                    builder.setTargetPreferredAuthSchemes( Arrays.asList( value.split( "," ) ) );
                }
                else if ( key.equals( HANDLE_AUTHENTICATION ) )
                {
                    builder.setAuthenticationEnabled( Boolean.valueOf( value ) );
                }
                else if ( key.equals( ALLOW_CIRCULAR_REDIRECTS ) )
                {
                    builder.setCircularRedirectsAllowed( Boolean.valueOf( value ) );
                }
                else if ( key.equals( CONN_MANAGER_TIMEOUT ) )
                {
                    builder.setConnectionRequestTimeout( Integer.parseInt( value ) );
                }
                else if ( key.equals( COOKIE_POLICY ) )
                {
                    builder.setCookieSpec( value );
                }
                else if ( key.equals( MAX_REDIRECTS ) )
                {
                    builder.setMaxRedirects( Integer.parseInt( value ) );
                }
                else if ( key.equals( HANDLE_REDIRECTS ) )
                {
                    builder.setRedirectsEnabled( Boolean.valueOf( value ) );
                }
                else if ( key.equals( REJECT_RELATIVE_REDIRECT ) )
                {
                    builder.setRelativeRedirectsAllowed( !Boolean.valueOf( value ) );
                }
                else if ( key.equals ( HANDLE_CONTENT_COMPRESSION ) )
                {
                    builder.setContentCompressionEnabled( Boolean.valueOf( value ) );
                }
            }
        }
    }

    public static Header[] asRequestHeaders( HttpMethodConfiguration config )
    {
        Properties headers = config.getHeaders();
        if ( headers == null )
        {
            return new Header[0];
        }

        Header[] result = new Header[headers.size()];

        int index = 0;
        for ( Map.Entry entry : headers.entrySet() )
        {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Header header = new BasicHeader( key, value );
            result[index++] = header;
        }

        return result;
    }

    public static HttpMethodConfiguration merge( HttpMethodConfiguration defaults, HttpMethodConfiguration base,
                                            HttpMethodConfiguration local )
    {
        HttpMethodConfiguration result = merge( defaults, base );
        return merge( result, local );
    }

    public static HttpMethodConfiguration merge( HttpMethodConfiguration base, HttpMethodConfiguration local )
    {
        if ( base == null && local == null )
        {
            return null;
        }
        else if ( base == null )
        {
            return local;
        }
        else if ( local == null )
        {
            return base;
        }
        else
        {
            HttpMethodConfiguration result = base.copy();

            if ( local.getConnectionTimeout() != Wagon.DEFAULT_CONNECTION_TIMEOUT )
            {
                result.setConnectionTimeout( local.getConnectionTimeout() );
            }

            if ( local.getReadTimeout() != Wagon.DEFAULT_READ_TIMEOUT )
            {
                result.setReadTimeout( local.getReadTimeout() );
            }

            if ( local.getHeaders() != null )
            {
                result.getHeaders().putAll( local.getHeaders() );
            }

            if ( local.getParams() != null )
            {
                result.getParams().putAll( local.getParams() );
            }

            if ( local.getUseDefaultHeaders() != null )
            {
                result.setUseDefaultHeaders( local.isUseDefaultHeaders() );
            }

            return result;
        }
    }

}
