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
package org.apache.maven.wagon.tck.http.fixture;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.codehaus.plexus.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class AuthSnoopFilter implements Filter {
    private static Logger logger = LoggerFactory.getLogger(AuthSnoopFilter.class);

    public void destroy() {}

    public void doFilter(final ServletRequest req, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            logger.info("Authorization: " + authHeader);
            String data = authHeader.substring("BASIC ".length());
            String decoded = new String(Base64.decodeBase64(data.getBytes(StandardCharsets.US_ASCII)));
            logger.info(decoded);
            String[] creds = decoded.split(":");

            logger.info("User: " + creds[0] + "\nPassword: " + creds[1]);
        }
    }

    public void init(final FilterConfig filterConfig) throws ServletException {}
}
