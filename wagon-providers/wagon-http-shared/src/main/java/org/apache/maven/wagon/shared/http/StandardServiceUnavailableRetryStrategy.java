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

import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.ServiceUnavailableRetryStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;

/**
 * An implementation of the {@link ServiceUnavailableRetryStrategy} interface.
 * that retries {@code 408} (Request Timeout), {@code 429} (Too Many Requests),
 * and {@code 500} (Server side error) responses for a fixed number of times at a fixed interval.
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class StandardServiceUnavailableRetryStrategy implements ServiceUnavailableRetryStrategy {
    /**
     * Maximum number of allowed retries if the server responds with a HTTP code
     * in our retry code list.
     */
    private final int maxRetries;

    /**
     * Retry interval between subsequent requests, in milliseconds.
     */
    private final long retryInterval;

    public StandardServiceUnavailableRetryStrategy(final int maxRetries, final int retryInterval) {
        super();
        Args.positive(maxRetries, "Max retries");
        Args.positive(retryInterval, "Retry interval");
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    @Override
    public boolean retryRequest(final ClassicHttpResponse response, final int executionCount, final HttpContext context) {
        int statusCode = response.getCode();
        boolean retryableStatusCode = statusCode == HttpStatus.SC_REQUEST_TIMEOUT
                // Too Many Requests ("standard" rate-limiting)
                || statusCode == AbstractHttpClientWagon.SC_TOO_MANY_REQUESTS
                // Assume server errors are momentary hiccups
                || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
                || statusCode == HttpStatus.SC_BAD_GATEWAY
                || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
                || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
        return executionCount <= maxRetries && retryableStatusCode;
    }

    @Override
    public long getRetryInterval() {
        return retryInterval;
    }
}
