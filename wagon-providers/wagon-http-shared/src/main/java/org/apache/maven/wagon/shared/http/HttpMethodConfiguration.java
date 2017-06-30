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
import org.apache.maven.wagon.Wagon;

import java.util.Map;
import java.util.Properties;

/**
 * 
 */
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

    public boolean isUsePreemptive()
    {
        return usePreemptive;
    }

    public HttpMethodConfiguration setUsePreemptive( boolean usePreemptive )
    {
        this.usePreemptive = usePreemptive;
        return this;
    }

    public Header[] asRequestHeaders()
    {
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

    HttpMethodConfiguration copy()
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

}
