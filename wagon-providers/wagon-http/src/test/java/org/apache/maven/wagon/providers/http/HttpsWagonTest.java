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

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HttpsWagonTest extends HttpWagonTest {
    protected String getProtocol() {
        return "https";
    }

    protected boolean assertOnTransferProgress() {
        return false;
    }

    protected ServerConnector addConnector(Server server) {
        System.setProperty("javax.net.ssl.trustStore", new File("src/test/resources/ssl/keystore").getAbsolutePath());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath("src/test/resources/ssl/keystore");
        sslContextFactory.setKeyStorePassword("wagonhttp");
        sslContextFactory.setKeyManagerPassword("wagonhttp");
        sslContextFactory.setTrustStorePath("src/test/resources/ssl/keystore");
        sslContextFactory.setTrustStorePassword("wagonhttp");
        ServerConnector serverConnector = new ServerConnector(server, sslContextFactory);
        server.addConnector(serverConnector);
        return serverConnector;
    }
}
