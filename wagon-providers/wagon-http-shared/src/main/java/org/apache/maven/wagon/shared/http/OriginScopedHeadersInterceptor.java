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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

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
    public void process(HttpRequest request, HttpContext context) {
        if (context == null) {
            return;
        }
        Set<String> headerNames = headerNames(context.getAttribute(HEADER_NAMES));
        if (headerNames.isEmpty()) {
            return;
        }
        Object origin = context.getAttribute(ORIGIN);
        Object target = context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
        if (origin instanceof HttpHost
                && target instanceof HttpHost
                && isSameOrigin((HttpHost) origin, (HttpHost) target)) {
            return;
        }
        for (String headerName : headerNames) {
            request.removeHeaders(headerName);
        }
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
