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

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.impl.classic.RedirectExec;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;

/**
 * HttpClient 5 follows RFC 9110 and converts a PUT into a GET on a 303 response. The HttpClient 4 based
 * wagon replayed the PUT (and its body) at the redirect location, which is what the TCK and existing
 * users expect. Wrap the stock redirect handler and present such responses as 307 so the method
 * and the repeatable entity are preserved.
 */
final class WagonRedirectExec implements ExecChainHandler {

    private final RedirectExec delegate;

    WagonRedirectExec(HttpRoutePlanner routePlanner, RedirectStrategy redirectStrategy) {
        this.delegate = new RedirectExec(routePlanner, redirectStrategy);
    }

    @Override
    public ClassicHttpResponse execute(
            final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        ExecChain preservingStatus = (req, scp) -> {
            ClassicHttpResponse response = chain.proceed(req, scp);
            if (response.getCode() == HttpStatus.SC_SEE_OTHER && Method.PUT.isSame(req.getMethod())) {
                response.setCode(HttpStatus.SC_TEMPORARY_REDIRECT);
            }
            return response;
        };
        return delegate.execute(request, scope, preservingStatus);
    }
}
