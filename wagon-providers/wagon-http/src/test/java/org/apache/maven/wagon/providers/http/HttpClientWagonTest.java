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
package org.apache.maven.wagon.providers.http;

import junit.framework.TestCase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.ConfigurationUtils;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;

public class HttpClientWagonTest extends TestCase {

    public void testSetMaxRedirectsParamViaConfig() {
        HttpMethodConfiguration methodConfig = new HttpMethodConfiguration();
        int maxRedirects = 2;
        methodConfig.addParam("http.protocol.max-redirects", "%i," + maxRedirects);

        HttpConfiguration config = new HttpConfiguration();
        config.setAll(methodConfig);

        HttpHead method = new HttpHead();
        RequestConfig.Builder builder = RequestConfig.custom();
        ConfigurationUtils.copyConfig(config.getMethodConfiguration(method), builder);
        RequestConfig requestConfig = builder.build();

        assertEquals(2, requestConfig.getMaxRedirects());
    }

    public void testDefaultHeadersUsedByDefault() {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll(new HttpMethodConfiguration());

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration(config);

        HttpHead method = new HttpHead();
        wagon.setHeaders(method);

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Pragma", "no-cache" );
        // "Accept-Encoding" is automatically set by HttpClient at runtime

        Header header = method.getFirstHeader("Cache-control");
        assertNotNull(header);
        assertEquals("no-cache", header.getValue());

        header = method.getFirstHeader("Pragma");
        assertNotNull(header);
        assertEquals("no-cache", header.getValue());
    }

    public void testTurnOffDefaultHeaders() {
        HttpConfiguration config = new HttpConfiguration();
        config.setAll(new HttpMethodConfiguration().setUseDefaultHeaders(false));

        TestWagon wagon = new TestWagon();
        wagon.setHttpConfiguration(config);

        HttpHead method = new HttpHead();
        wagon.setHeaders(method);

        // these are the default headers.
        // method.addRequestHeader( "Cache-control", "no-cache" );
        // method.addRequestHeader( "Pragma", "no-cache" );

        Header header = method.getFirstHeader("Cache-control");
        assertNull(header);

        header = method.getFirstHeader("Pragma");
        assertNull(header);
    }

    private static final class TestWagon extends AbstractHttpClientWagon {
        @Override
        public void fillOutputData(OutputData outputData) throws TransferFailedException {}
    }
}
