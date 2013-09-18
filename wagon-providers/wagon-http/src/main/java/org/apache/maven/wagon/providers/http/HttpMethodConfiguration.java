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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.maven.wagon.Wagon;

public class HttpMethodConfiguration
{

    private Boolean useDefaultHeaders;

    private Properties headers = new Properties();

    private Properties params = new Properties();

    private int connectionTimeout = Wagon.DEFAULT_CONNECTION_TIMEOUT;

    private int readTimeout =
        Integer.parseInt( System.getProperty( "maven.wagon.rto", Integer.toString( Wagon.DEFAULT_READ_TIMEOUT ) ) );

    private boolean usePreemptive = false;

    public boolean isUseDefaultHeaders()
    {
        return useDefaultHeaders == null || useDefaultHeaders.booleanValue();
    }

    public HttpMethodConfiguration setUseDefaultHeaders( boolean useDefaultHeaders )
    {
        this.useDefaultHeaders = Boolean.valueOf( useDefaultHeaders );
        return this;
    }

    public Boolean getUseDefaultHeaders()
    {
        return useDefaultHeaders;
    }

    public HttpMethodConfiguration addHeader( String header, String value )
    {
        headers.setProperty( header, value );
        return this;
    }

    public Properties getHeaders()
    {
        return headers;
    }

    public HttpMethodConfiguration setHeaders( Properties headers )
    {
        this.headers = headers;
        return this;
    }

    public HttpMethodConfiguration addParam( String param, String value )
    {
        params.setProperty( param, value );
        return this;
    }

    public Properties getParams()
    {
        return params;
    }

    public HttpMethodConfiguration setParams( Properties params )
    {
        this.params = params;
        return this;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public HttpMethodConfiguration setConnectionTimeout( int connectionTimeout )
    {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public int getReadTimeout()
    {
        return readTimeout;
    }

    public HttpMethodConfiguration setReadTimeout( int readTimeout )
    {
        this.readTimeout = readTimeout;
        return this;
    }

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

    private static final String COERCE_PATTERN = "%(\\w+),(.+)";

    public RequestConfig asRequestConfig()
    {
        if ( !hasParams() )
        {
            return null;
        }
        RequestConfig.Builder builder = RequestConfig.custom();
        // WAGON-273: default the cookie-policy to browser compatible
        builder.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY);
        if ( connectionTimeout > 0 )
        {
            builder.setConnectTimeout(connectionTimeout);
        }
        if ( readTimeout > 0 )
        {
            builder.setSocketTimeout(readTimeout);
        }
        if ( params != null )
        {

            Pattern coercePattern = Pattern.compile( COERCE_PATTERN );
            for ( Iterator<?> it = params.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry<String, String> entry = (Map.Entry) it.next();
                String key = entry.getKey();
                String value = entry.getValue();
                Matcher matcher = coercePattern.matcher(value);
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
                    builder.setProxy( new HttpHost( value ));
                }
                else if ( key.equals( LOCAL_ADDRESS ) )
                {
                    try {
                        builder.setLocalAddress( InetAddress.getByName( value ) );
                    }
                    catch (UnknownHostException ignore) {
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
            }
        }
        return builder.build();
    }

    private boolean hasParams()
    {
        if ( connectionTimeout < 1 && params == null && readTimeout < 1 )
        {
            return false;
        }

        return true;
    }

    public boolean isUsePreemptive()
    {
        return usePreemptive;
    }

    public HttpMethodConfiguration setUsePreemptive( boolean usePreemptive )
    {
        this.usePreemptive = usePreemptive;
        return this;
    }

    private void fillParams( HttpParams p )
    {
        if ( !hasParams() )
        {
            return;
        }

        if ( connectionTimeout > 0 )
        {
            p.setParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout );
        }

        if ( readTimeout > 0 )
        {
            p.setParameter( CoreConnectionPNames.SO_TIMEOUT, readTimeout );
        }

        if ( params != null )
        {
            Pattern coercePattern = Pattern.compile( COERCE_PATTERN );

            for ( Iterator<?> it = params.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry<String, String> entry = (Map.Entry) it.next();

                String key = entry.getKey();
                String value = entry.getValue();

                Matcher matcher = coercePattern.matcher( value );
                if ( matcher.matches() )
                {
                    char type = matcher.group( 1 ).charAt( 0 );
                    value = matcher.group( 2 );

                    switch ( type )
                    {
                        case 'i':
                        {
                            p.setIntParameter( key, Integer.parseInt( value ) );
                            break;
                        }
                        case 'd':
                        {
                            p.setDoubleParameter( key, Double.parseDouble( value ) );
                            break;
                        }
                        case 'l':
                        {
                            p.setLongParameter( key, Long.parseLong( value ) );
                            break;
                        }
                        case 'b':
                        {
                            p.setBooleanParameter( key, Boolean.valueOf( value ).booleanValue() );
                            break;
                        }
                        case 'c':
                        {
                            String[] entries = value.split( "," );
                            List<String> collection = new ArrayList<String>();
                            for ( int i = 0; i < entries.length; i++ )
                            {
                                collection.add( entries[i].trim() );
                            }

                            p.setParameter( key, collection );
                            break;
                        }
                        case 'm':
                        {
                            String[] entries = value.split( "," );

                            Map<String, String> map = new LinkedHashMap<String, String>();
                            for ( int i = 0; i < entries.length; i++ )
                            {
                                int idx = entries[i].indexOf( "=>" );
                                if ( idx < 1 )
                                {
                                    break;
                                }

                                String mapKey = entries[i].substring( 0, idx );
                                String mapVal = entries[i].substring( idx + 1, entries[i].length() );
                                map.put( mapKey.trim(), mapVal.trim() );
                            }

                            p.setParameter( key, map );
                            break;
                        }
                    }
                }
                else
                {
                    p.setParameter( key, value );
                }
            }
        }
    }

    public Header[] asRequestHeaders()
    {
        if ( headers == null )
        {
            return new Header[0];
        }

        Header[] result = new Header[headers.size()];

        int index = 0;
        for ( Iterator<?> it = headers.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, String> entry = (Map.Entry) it.next();

            String key = entry.getKey();
            String value = entry.getValue();

            Header header = new BasicHeader( key, value );
            result[index++] = header;
        }

        return result;
    }

    private HttpMethodConfiguration copy()
    {
        HttpMethodConfiguration copy = new HttpMethodConfiguration();

        copy.setConnectionTimeout( getConnectionTimeout() );
        copy.setReadTimeout( getReadTimeout() );
        if ( getHeaders() != null )
        {
            copy.setHeaders( getHeaders() );
        }

        if ( getParams() != null )
        {
            copy.setParams( getParams() );
        }

        copy.setUseDefaultHeaders( isUseDefaultHeaders() );

        return copy;
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
