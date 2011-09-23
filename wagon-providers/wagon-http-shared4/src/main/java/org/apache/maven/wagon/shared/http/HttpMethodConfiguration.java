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
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpMethodConfiguration
{

    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    public static final int DEFAULT_READ_TIMEOUT = 60000;

    private static final String COERCE_PATTERN = "%(\\w+),(.+)";

    private Boolean useDefaultHeaders;

    private Properties headers = new Properties();

    private Properties params = new Properties();

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private int readTimeout = DEFAULT_READ_TIMEOUT;

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

    public HttpParams asMethodParams( HttpParams defaults )
    {
        if ( !hasParams() )
        {
            return null;
        }

        DefaultedHttpParams p = new DefaultedHttpParams( new BasicHttpParams(), defaults );

        fillParams( p );

        return p;
    }

    private boolean hasParams()
    {
        if ( connectionTimeout < 1 && params == null && readTimeout < 1 )
        {
            return false;
        }

        return true;
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

            if ( local.getConnectionTimeout() != DEFAULT_CONNECTION_TIMEOUT )
            {
                result.setConnectionTimeout( local.getConnectionTimeout() );
            }

            if ( local.getReadTimeout() != DEFAULT_READ_TIMEOUT )
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
