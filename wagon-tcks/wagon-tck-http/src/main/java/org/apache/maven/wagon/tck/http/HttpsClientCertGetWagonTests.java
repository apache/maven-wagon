package org.apache.maven.wagon.tck.http;

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


import org.apache.maven.wagon.authentication.AuthenticationInfo;
import static org.apache.maven.wagon.tck.http.fixture.ServerFixture.SERVER_SSL_KEYSTORE_PASSWORD;
import static org.apache.maven.wagon.tck.http.fixture.ServerFixture.SERVER_SSL_KEYSTORE_RESOURCE_PATH;
import static org.apache.maven.wagon.tck.http.util.TestUtil.getResource;

/**
 * HTTP wagon tests that require HTTP Client Certificate authentication.
 */
public class HttpsClientCertGetWagonTests
    extends GetWagonTests
{
    @Override
    protected boolean isSsl()
    {
        return true;
    }

    @Override
    protected boolean isClientCertAuth()
    {
        return true;
    }

    @Override
    protected  AuthenticationInfo getAuthIfNeeded ( AuthenticationInfo info ) throws Exception
    {
        if ( info == null )
        {
            info = new AuthenticationInfo();
        }

        String keystore = getResource( SERVER_SSL_KEYSTORE_RESOURCE_PATH ).getAbsolutePath();

        //info.setKeyAlias(keyAlias);
        info.setKeyStore( keystore );
        info.setTrustStore( keystore );
        info.setKeyPassword( SERVER_SSL_KEYSTORE_PASSWORD );
        info.setKeyStorePassword( SERVER_SSL_KEYSTORE_PASSWORD );
        info.setTrustStorePassword( SERVER_SSL_KEYSTORE_PASSWORD );
        info.setKeyStoreType( null );
        info.setTrustStoreType( null );
        return info;
    }

}
