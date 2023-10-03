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

import com.jcraft.jsch.UserInfo;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo;

/**
 * WagonUserInfo
 *
 *
 */
class WagonUserInfo implements UserInfo {
    private final InteractiveUserInfo userInfo;

    private String password;

    private String passphrase;

    WagonUserInfo(AuthenticationInfo authInfo, InteractiveUserInfo userInfo) {
        this.userInfo = userInfo;

        this.password = authInfo.getPassword();

        this.passphrase = authInfo.getPassphrase();
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getPassword() {
        return password;
    }

    public boolean promptPassphrase(String message) {
        if (passphrase == null && userInfo != null) {
            passphrase = userInfo.promptPassphrase(message);
        }
        return passphrase != null;
    }

    public boolean promptPassword(String message) {
        if (password == null && userInfo != null) {
            password = userInfo.promptPassword(message);
        }
        return password != null;
    }

    public boolean promptYesNo(String message) {
        return userInfo != null && userInfo.promptYesNo(message);
    }

    public void showMessage(String message) {
        if (userInfo != null) {
            userInfo.showMessage(message);
        }
    }
}
