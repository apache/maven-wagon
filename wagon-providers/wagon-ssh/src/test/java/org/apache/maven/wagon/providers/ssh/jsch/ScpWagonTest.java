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
package org.apache.maven.wagon.providers.ssh.jsch;

import java.io.File;

import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.TestData;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class ScpWagonTest extends StreamingWagonTestCase {

    protected String getProtocol() {
        return "scp";
    }

    public String getTestRepositoryUrl() {
        return TestData.getTestRepositoryUrl();
    }

    protected AuthenticationInfo getAuthInfo() {
        AuthenticationInfo authInfo = super.getAuthInfo();

        authInfo.setUserName(TestData.getUserName());

        File privateKey = TestData.getPrivateKey();

        if (privateKey.exists()) {
            authInfo.setPrivateKey(privateKey.getAbsolutePath());

            authInfo.setPassphrase("");
        }

        return authInfo;
    }

    protected long getExpectedLastModifiedOnGet(Repository repository, Resource resource) {
        return new File(repository.getBasedir(), resource.getName()).lastModified();
    }
}
