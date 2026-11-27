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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.message.HeaderGroup;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIAuthority;

/**
 * Removes the headers configured for a repository from requests that are not sent to that repository's origin.
 * <p>
 * The HTTP client copies the headers of the original request onto every redirected request, so headers configured
 * through {@code setHttpHeaders} or the method configuration (for example an {@code Authorization} header carrying a
 * token) would otherwise be sent to whatever host a repository redirects to. The wagon records the repository origin
 * and the configured header names in the request context; this interceptor drops those headers when the target host
 * of the request being sent differs from the origin in scheme, host or effective port. When the target host cannot be
 * determined the headers are dropped as well.
 */
public class OriginScopedHeadersInterceptor implements HttpRequestInterceptor {
    /**
     * Context attribute holding the {@link HttpHost} of the repository the request was made for.
     */
    public static final String ORIGIN = OriginScopedHeadersInterceptor.class.getName() + ".origin";

    /**
     * Context attribute holding the {@code Collection<String>} of header names that must stay on the origin.
     */
    public static final String HEADER_NAMES = OriginScopedHeadersInterceptor.class.getName() + ".headerNames";

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        if (context == null) {
            return;
        }
        Set<String> headerNames = headerNames(context.getAttribute(HEADER_NAMES));
        if (headerNames.isEmpty()) {
            return;
        }
        Object origin = context.getAttribute(ORIGIN);
        HttpHost target = targetHost(request, context);
        if (origin instanceof HttpHost && target != null && isSameOrigin((HttpHost) origin, target)) {
            return;
        }
        if (request instanceof HeaderGroup) {
            HeaderGroup headers = (HeaderGroup) request;
            for (String headerName : headerNames) {
                headers.removeHeaders(headerName);
            }
        }
    }

    /**
     * HttpClient 5 no longer publishes the target host as a context attribute. The route set by the client is
     * authoritative; an absolute request URI is used as a fallback for callers that invoke the interceptor directly.
     */
    private static HttpHost targetHost(HttpRequest request, HttpContext context) {
        // ProtocolExec sets the scheme and authority on the outgoing request before the
        // request interceptors run; that is the authoritative target in HttpClient 5.
        URIAuthority authority = request.getAuthority();
        if (authority != null && authority.getHostName() != null) {
            String scheme = request.getScheme() != null ? request.getScheme() : "http";
            return new HttpHost(scheme, authority.getHostName(), authority.getPort());
        }
        Object route = context.getAttribute(HttpClientContext.HTTP_ROUTE);
        if (route instanceof HttpRoute) {
            return ((HttpRoute) route).getTargetHost();
        }
        try {
            URI uri = request.getUri();
            if (uri != null && uri.isAbsolute() && uri.getHost() != null) {
                return new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
            }
        } catch (URISyntaxException e) {
            // an unknown target means the headers must be dropped
        }
        return null;
    }

    private static Set<String> headerNames(Object attribute) {
        if (!(attribute instanceof Collection)) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (Object name : (Collection<?>) attribute) {
            if (name != null) {
                names.add(String.valueOf(name));
            }
        }
        return names;
    }

    static boolean isSameOrigin(HttpHost origin, HttpHost target) {
        return origin.getSchemeName().equalsIgnoreCase(target.getSchemeName())
                && origin.getHostName().equalsIgnoreCase(target.getHostName())
                && effectivePort(origin) == effectivePort(target);
    }

    static int effectivePort(HttpHost host) {
        if (host.getPort() >= 0) {
            return host.getPort();
        }
        return "https".equalsIgnoreCase(host.getSchemeName()) ? 443 : 80;
    }
}
