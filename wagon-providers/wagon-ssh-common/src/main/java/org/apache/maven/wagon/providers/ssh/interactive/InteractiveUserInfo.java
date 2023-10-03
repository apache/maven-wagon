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
package org.apache.maven.wagon.providers.ssh.interactive;

/**
 * Interactive part for <code>UserInfo</code>
 *
 * @author Juan F. Codagnone
 * @see com.jcraft.jsch.UserInfo
 * @since Sep 12, 2005
 */
public interface InteractiveUserInfo {
    String ROLE = InteractiveUserInfo.class.getName();

    boolean promptYesNo(String message);

    void showMessage(String message);

    String promptPassword(String message);

    String promptPassphrase(String message);
}
