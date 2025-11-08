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

import java.net.URI;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon.WagonHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom redirect strategy for Apache Maven Wagon HttpClient.
 *
 * @since 3.4.0
 *
 */
public class WagonRedirectStrategy extends DefaultRedirectStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(WagonRedirectStrategy.class);

    private static final int SC_PERMANENT_REDIRECT = 308;

    public WagonRedirectStrategy() {
        super(new String[] {
            HttpGet.METHOD_NAME,
            HttpHead.METHOD_NAME,
            HttpPut.METHOD_NAME,
            /**
             * This covers the most basic case where the redirection relocates to another
             * collection which has an existing parent collection.
             */
            "MKCOL"
        });
    }

    @Override
    public boolean isRedirected(final ClassicHttpRequest request, final ClassicHttpResponse response, final HttpContext context)
            throws ProtocolException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");

        final int statusCode = response.getCode();
        final String method = request.getRequestLine().getMethod();
        switch (statusCode) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
            case SC_PERMANENT_REDIRECT:
                return isRedirectable(method);
            default:
                return false;
        }
    }

    @Override
    public HttpUriRequest getRedirect(final ClassicHttpRequest request, final ClassicHttpResponse response, final HttpContext context)
            throws ProtocolException {
        final URI uri = getLocationURI(request, response, context);
        if (request instanceof HttpEntityContainer) {
            HttpEntityContainer encRequest = (HttpEntityContainer) request;
            if (encRequest.getEntity() instanceof AbstractHttpClientWagon.WagonHttpEntity) {
                AbstractHttpClientWagon.WagonHttpEntity whe = (WagonHttpEntity) encRequest.getEntity();
                if (whe.getWagon() instanceof AbstractHttpClientWagon) {
                    // Re-execute AbstractWagon#firePutStarted(Resource, File)
                    AbstractHttpClientWagon httpWagon = (AbstractHttpClientWagon) whe.getWagon();
                    TransferEvent transferEvent = new TransferEvent(
                            httpWagon, whe.getResource(), TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT);
                    transferEvent.setTimestamp(System.currentTimeMillis());
                    transferEvent.setLocalFile(whe.getSource());
                    httpWagon
                            .getTransferEventSupport()
                            .fireDebug(String.format(
                                    "Following redirect from '%s' to '%s'",
                                    request.getRequestLine().getUri(), uri.toASCIIString()));
                    httpWagon.getTransferEventSupport().fireTransferStarted(transferEvent);
                } else {
                    LOGGER.warn(
                            "Cannot properly handle redirect transfer event, wagon has unexpected class: {}",
                            whe.getWagon().getClass());
                }
            }
        }

        return ClassicRequestBuilder.copy(request).setUri(uri).build();
    }
}
