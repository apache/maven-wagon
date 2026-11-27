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

import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;

/**
 * A custom redirect strategy for Apache Maven Wagon HttpClient.
 *
 * @since 3.4.0
 */
public class WagonRedirectStrategy extends DefaultRedirectStrategy {

    private static final int SC_PERMANENT_REDIRECT = 308;

    private static final List<String> REDIRECTABLE_METHODS = Arrays.asList("GET", "HEAD", "PUT", "MKCOL");

    @Override
    public boolean isRedirected(final HttpRequest request, final HttpResponse response, final HttpContext context)
            throws ProtocolException {
        Args.notNull(request, "HTTP request");
        Args.notNull(response, "HTTP response");

        final int statusCode = response.getCode();
        switch (statusCode) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
            case SC_PERMANENT_REDIRECT:
                return REDIRECTABLE_METHODS.contains(request.getMethod());
            default:
                return false;
        }
    }

    // HttpClient 5's RedirectStrategy no longer has getRedirect(); the exec chain builds the
    // redirected request itself, so the 4.x hook that re-fired TRANSFER_STARTED is unavailable.
}
