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
package org.apache.maven.wagon.proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:lafeuil@gmail.com">Thomas Champagne</a>
 */
public class ProxyInfoUtilsTest {
    public ProxyInfoUtilsTest(final String name) {
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testValidateNonProxyHostsWithNullProxy() {
        assertFalse(ProxyUtils.validateNonProxyHosts(null, "maven.apache.org"), "www.ibiblio.org");
    }

    @Test
    public void testValidateNonProxyHostsWithUniqueHost() {

        final ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setUserName("username");
        proxyInfo.setPassword("password");
        proxyInfo.setHost("http://www.ibiblio.org");
        proxyInfo.setPort(0);
        proxyInfo.setType("SOCKSv4");
        proxyInfo.setNonProxyHosts("*.apache.org");

        assertTrue(ProxyUtils.validateNonProxyHosts(proxyInfo, "maven.apache.org"), "maven.apache.org");

        assertFalse(ProxyUtils.validateNonProxyHosts(proxyInfo, "www.ibiblio.org"), "www.ibiblio.org");

        assertFalse(ProxyUtils.validateNonProxyHosts(proxyInfo, null), "null");

        proxyInfo.setNonProxyHosts(null);
        assertFalse(ProxyUtils.validateNonProxyHosts(proxyInfo, "maven.apache.org"), "NonProxyHosts = null");

        proxyInfo.setNonProxyHosts("");
        assertFalse(ProxyUtils.validateNonProxyHosts(proxyInfo, "maven.apache.org"), "NonProxyHosts = \"\"");
    }

    @Test
    public void testValidateNonProxyHostsWithMultipleHost() {

        final ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setUserName("username");
        proxyInfo.setPassword("password");
        proxyInfo.setHost("http://www.ibiblio.org");
        proxyInfo.setPort(0);
        proxyInfo.setType("SOCKSv4");
        proxyInfo.setNonProxyHosts("*.apache.org|*.codehaus.org");

        assertTrue(ProxyUtils.validateNonProxyHosts(proxyInfo, "maven.apache.org"), "maven.apache.org");
        assertTrue(ProxyUtils.validateNonProxyHosts(proxyInfo, "wiki.codehaus.org"), "wiki.codehaus.org");

        assertFalse(ProxyUtils.validateNonProxyHosts(proxyInfo, "www.ibiblio.org"), "www.ibiblio.org");
    }
}
