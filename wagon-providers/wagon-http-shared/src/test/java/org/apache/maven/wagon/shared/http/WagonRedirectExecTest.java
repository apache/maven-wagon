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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WagonRedirectExecTest {

    private HttpServer server;

    private final AtomicReference<String> targetMethod = new AtomicReference<>();

    private final AtomicReference<String> targetBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/source", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(303, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            targetMethod.set(exchange.getRequestMethod());
            try (InputStream in = exchange.getRequestBody()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[256];
                for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                    out.write(buf, 0, n);
                }
                targetBody.set(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void putIsReplayedWithBodyAfter303() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .replaceExecInterceptor(
                        "REDIRECT",
                        new WagonRedirectExec(
                                new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE),
                                new WagonRedirectStrategy()))
                .build()) {
            HttpPut put = new HttpPut("http://127.0.0.1:" + server.getAddress().getPort() + "/source");
            put.setEntity(new ByteArrayEntity("payload".getBytes(StandardCharsets.UTF_8), ContentType.TEXT_PLAIN));

            int status = client.execute(put, response -> response.getCode());

            assertEquals(201, status);
            assertEquals("PUT", targetMethod.get());
            assertEquals("payload", targetBody.get());
        }
    }
}
