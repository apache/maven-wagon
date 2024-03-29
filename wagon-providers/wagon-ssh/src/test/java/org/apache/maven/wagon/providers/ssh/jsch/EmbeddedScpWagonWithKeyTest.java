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
import java.io.IOException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.AbstractEmbeddedScpWagonWithKeyTest;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostEntry;
import org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class EmbeddedScpWagonWithKeyTest extends AbstractEmbeddedScpWagonWithKeyTest {

    @Override
    protected Wagon getWagon() throws Exception {
        ScpWagon scpWagon = (ScpWagon) super.getWagon();
        scpWagon.setInteractive(false);
        scpWagon.setKnownHostsProvider(new KnownHostsProvider() {
            public void storeKnownHosts(String contents) throws IOException {}

            public void addKnownHost(KnownHostEntry knownHost) throws IOException {}

            public void setHostKeyChecking(String hostKeyChecking) {}

            public String getHostKeyChecking() {
                return "no";
            }

            public String getContents() {
                return null;
            }
        });
        return scpWagon;
    }

    protected String getProtocol() {
        return "scp";
    }

    protected AuthenticationInfo getAuthInfo() {
        AuthenticationInfo authInfo = super.getAuthInfo();
        // user : guest/guest123 -  passphrase : toto01
        authInfo.setUserName("guest");
        // authInfo.setPassword( TestData.getUserPassword() );
        authInfo.setPrivateKey(new File("src/test/ssh-keys/id_rsa").getPath());

        return authInfo;
    }
}
