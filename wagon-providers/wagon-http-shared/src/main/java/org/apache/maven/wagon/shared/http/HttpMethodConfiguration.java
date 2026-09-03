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
package org.apache.maven.wagon.shared.http;

import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.maven.wagon.Wagon;

/**
 *
 */
public class HttpMethodConfiguration {

    private Boolean useDefaultHeaders;

    private Properties headers = new Properties();

    private Properties params = new Properties();

    private Integer connectionTimeout;

    private Integer readTimeout;

    private Boolean usePreemptive;

    public boolean isUseDefaultHeaders() {
        return useDefaultHeaders == null || useDefaultHeaders.booleanValue();
    }

    public HttpMethodConfiguration setUseDefaultHeaders(boolean useDefaultHeaders) {
        this.useDefaultHeaders = Boolean.valueOf(useDefaultHeaders);
        return this;
    }

    public Boolean getUseDefaultHeaders() {
        return useDefaultHeaders;
    }

    public HttpMethodConfiguration addHeader(String header, String value) {
        headers.setProperty(header, value);
        return this;
    }

    public Properties getHeaders() {
        return headers;
    }

    public HttpMethodConfiguration setHeaders(Properties headers) {
        this.headers = headers;
        return this;
    }

    public HttpMethodConfiguration addParam(String param, String value) {
        params.setProperty(param, value);
        return this;
    }

    public Properties getParams() {
        return params;
    }

    public HttpMethodConfiguration setParams(Properties params) {
        this.params = params;
        return this;
    }

    public int getConnectionTimeout() {
        return connectionTimeout != null ? connectionTimeout : Wagon.DEFAULT_CONNECTION_TIMEOUT;
    }

    public HttpMethodConfiguration setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public boolean isConnectionTimeoutSet() {
        return connectionTimeout != null;
    }

    public int getReadTimeout() {
        return readTimeout != null
                ? readTimeout
                : Integer.parseInt(System.getProperty("maven.wagon.rto", Integer.toString(Wagon.DEFAULT_READ_TIMEOUT)));
    }

    public HttpMethodConfiguration setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public boolean isReadTimeoutSet() {
        return readTimeout != null;
    }

    public boolean isUsePreemptive() {
        return Boolean.TRUE.equals(usePreemptive);
    }

    public HttpMethodConfiguration setUsePreemptive(boolean usePreemptive) {
        this.usePreemptive = Boolean.valueOf(usePreemptive);
        return this;
    }

    public Boolean getUsePreemptive() {
        return usePreemptive;
    }

    public boolean isUsePreemptiveSet() {
        return usePreemptive != null;
    }

    public Header[] asRequestHeaders() {
        if (headers == null) {
            return new Header[0];
        }

        Header[] result = new Header[headers.size()];

        int index = 0;
        for (Map.Entry entry : headers.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Header header = new BasicHeader(key, value);
            result[index++] = header;
        }

        return result;
    }

    HttpMethodConfiguration copy() {
        HttpMethodConfiguration copy = new HttpMethodConfiguration();

        if (connectionTimeout != null) {
            copy.setConnectionTimeout(connectionTimeout);
        }
        if (readTimeout != null) {
            copy.setReadTimeout(readTimeout);
        }
        if (getHeaders() != null) {
            copy.getHeaders().putAll(getHeaders());
        }

        if (getParams() != null) {
            copy.getParams().putAll(getParams());
        }

        if (useDefaultHeaders != null) {
            copy.setUseDefaultHeaders(useDefaultHeaders);
        }

        if (usePreemptive != null) {
            copy.setUsePreemptive(usePreemptive);
        }

        return copy;
    }
}
