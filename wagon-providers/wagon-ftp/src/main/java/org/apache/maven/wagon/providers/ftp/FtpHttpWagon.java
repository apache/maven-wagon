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
import javax.inject.Singleton;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FtpHttpWagon
 *
 */
@Singleton
@Named("ftph")
public class FtpHttpWagon extends FtpWagon {

    private static final Logger LOG = LoggerFactory.getLogger(FtpHttpWagon.class);

    public FtpHttpWagon(
            @Named("${passiveMode}") boolean passiveMode, @Named("${controlEncoding}") String controlEncoding) {
        super(passiveMode, controlEncoding);
    }

    @Override
    protected FTPClient createClient() {
        ProxyInfo proxyInfo = getProxyInfo();

        LOG.debug("Creating FTP over HTTP proxy client. Proxy Host: [{}].", proxyInfo.getHost());

        return new FTPHTTPClient(
                proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUserName(), proxyInfo.getPassword());
    }
}
