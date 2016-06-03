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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FtpClientFactory
{

    private static final Logger LOG = LoggerFactory.getLogger( FtpClientFactory.class );

    static FTPClient get( boolean secure, String securityProtocol, boolean isImplicit )
    {
        FTPClient client;
        if ( secure )
        {
            client = new FTPSClient( securityProtocol, isImplicit );
            LOG.debug( "created FTP client for '{}', implicit: '{}'", securityProtocol, isImplicit );
        }
        else
        {
            client = new FTPClient();
            LOG.debug( "created insecure FTP client" );
        }
        return client;
    }
}
