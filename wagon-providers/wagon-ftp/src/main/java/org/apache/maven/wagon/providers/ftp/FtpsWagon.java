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
package org.apache.maven.wagon.providers.ftp;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FtpsWagon
 *
 *
 */
@Singleton
@Named("ftps")
public class FtpsWagon extends FtpWagon {
    private static final Logger LOG = LoggerFactory.getLogger(FtpsWagon.class);

    private String securityProtocol = "TLS";

    private boolean implicit = false;
    private boolean endpointChecking = true;

    @Inject
    public FtpsWagon(
            @Named("${passiveMode:-true}") boolean passiveMode,
            @Named("${controlEncoding:-ISO-8859-1}") String controlEncoding,
            @Named("${securityProtocol:-TLS}") String securityProtocol,
            @Named("${implicit:-false}") boolean implicit,
            @Named("${endpointChecking:-true}") boolean endpointChecking) {
        super(passiveMode, controlEncoding);
        this.securityProtocol = securityProtocol;
        this.implicit = implicit;
        this.endpointChecking = endpointChecking;
    }

    @Override
    protected FTPClient createClient() {
        LOG.debug(
                "Creating secure FTP client. Protocol: [{}], implicit mode: [{}], endpoint checking: [{}].",
                securityProtocol,
                implicit,
                endpointChecking);
        FTPSClient client = new FTPSClient(securityProtocol, implicit);
        client.setEndpointCheckingEnabled(endpointChecking);
        return client;
    }
}
