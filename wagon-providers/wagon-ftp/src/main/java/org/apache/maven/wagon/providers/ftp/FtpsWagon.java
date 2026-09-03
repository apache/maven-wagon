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

import javax.inject.Named;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FtpsWagon
 *
 *
 */
@Named("ftps")
public class FtpsWagon extends FtpWagon {
    private static final Logger LOG = LoggerFactory.getLogger(FtpsWagon.class);

    private String securityProtocol = "TLS";

    private boolean implicit = false;

    private boolean endpointChecking = true;

    /**
     * RFC 4217 data channel protection level: {@code P} for a protected data connection, {@code C} for a clear one.
     * Empty or {@code null} sends no {@code PROT} command at all, which leaves the server on its own default.
     */
    private String dataChannelProtection = "P";

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

    public String getDataChannelProtection() {
        return dataChannelProtection;
    }

    public void setDataChannelProtection(String dataChannelProtection) {
        this.dataChannelProtection = dataChannelProtection;
    }

    /**
     * Negotiates the data channel protection level. Without this the data connection stays clear even though the
     * control connection is encrypted, and a server that requires protection answers a transfer with
     * {@code 425 Server requires protected data connection}.
     */
    @Override
    protected void afterLogin(FTPClient client) throws IOException {
        if (dataChannelProtection == null || dataChannelProtection.isEmpty()) {
            return;
        }
        LOG.debug("Setting FTPS data channel protection level to [{}].", dataChannelProtection);
        FTPSClient secureClient = (FTPSClient) client;
        // PBSZ 0 is what RFC 4217 requires before PROT over TLS
        secureClient.execPBSZ(0);
        secureClient.execPROT(dataChannelProtection);
    }
}
