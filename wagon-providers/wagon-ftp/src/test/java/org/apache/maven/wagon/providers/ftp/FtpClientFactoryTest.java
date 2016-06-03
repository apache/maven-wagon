package org.apache.maven.wagon.providers.ftp;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.Test;

public class FtpClientFactoryTest
{

    @Test
    public void shouldCreateSimpleFTPClient() throws Exception
    {
        //given
        String irrelevantSecurityProtocol = "SSL";
        boolean irrelevantImplicitMode = false;
        // when
        FTPClient actual =
            FtpClientFactory.get( false, irrelevantSecurityProtocol, irrelevantImplicitMode );
        // then
        assertThat( actual.getClass().getName(), is( FTPClient.class.getName() ) );
    }

    @Test
    public void shouldCreateDefaultFTPSClient() throws Exception
    {
        // when
        FTPClient actual = FtpClientFactory.get( true, null, false );
        // then
        assertThat( actual.getClass().getName(), is( FTPSClient.class.getName() ) );
    }

    @Test
    public void shouldCreateImplicitSSLClient() throws Exception
    {
        // when
        FTPClient actual = FtpClientFactory.get( true, "SSL", true );
        // then
        assertThat( actual.getClass().getName(), is( FTPSClient.class.getName() ) );
    }

    @Test
    public void shouldCreateExplicitTLSClient() throws Exception
    {
        // when
        FTPClient actual = FtpClientFactory.get( true, "TLS", false );
        // then
        assertThat( actual.getClass().getName(), is( FTPSClient.class.getName() ) );
    }

}
