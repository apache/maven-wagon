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
package org.apache.maven.wagon.shared.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.maven.wagon.Wagon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationUtilsTest {

    @Test
    public void testCopyConfigDoesNotOverwriteBuilderTimeoutsWhenUnset() {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(12345);
        builder.setSocketTimeout(54321);

        HttpMethodConfiguration config = new HttpMethodConfiguration();
        assertFalse(config.isConnectionTimeoutSet());
        assertFalse(config.isReadTimeoutSet());

        ConfigurationUtils.copyConfig(config, builder);

        RequestConfig requestConfig = builder.build();
        assertEquals(12345, requestConfig.getConnectTimeout());
        assertEquals(54321, requestConfig.getSocketTimeout());
    }

    @Test
    public void testCopyConfigOverwritesBuilderTimeoutsWhenSet() {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(12345);
        builder.setSocketTimeout(54321);

        HttpMethodConfiguration config = new HttpMethodConfiguration();
        config.setConnectionTimeout(999);
        config.setReadTimeout(888);
        assertTrue(config.isConnectionTimeoutSet());
        assertTrue(config.isReadTimeoutSet());

        ConfigurationUtils.copyConfig(config, builder);

        RequestConfig requestConfig = builder.build();
        assertEquals(999, requestConfig.getConnectTimeout());
        assertEquals(888, requestConfig.getSocketTimeout());
    }

    @Test
    public void testMergePreservesUsePreemptiveFromBase() {
        HttpMethodConfiguration base = new HttpMethodConfiguration();
        base.setUsePreemptive(true);

        HttpMethodConfiguration local = new HttpMethodConfiguration();
        assertFalse(local.isUsePreemptiveSet());

        HttpMethodConfiguration merged = ConfigurationUtils.merge(base, local);
        assertTrue(merged.isUsePreemptive());
    }

    @Test
    public void testMergeOverridesUsePreemptiveFromLocal() {
        HttpMethodConfiguration base = new HttpMethodConfiguration();
        base.setUsePreemptive(true);

        HttpMethodConfiguration local = new HttpMethodConfiguration();
        local.setUsePreemptive(false);
        assertTrue(local.isUsePreemptiveSet());

        HttpMethodConfiguration merged = ConfigurationUtils.merge(base, local);
        assertFalse(merged.isUsePreemptive());
    }

    @Test
    public void testMergePreservesTimeoutsFromBase() {
        HttpMethodConfiguration base = new HttpMethodConfiguration();
        base.setConnectionTimeout(10000);
        base.setReadTimeout(20000);

        HttpMethodConfiguration local = new HttpMethodConfiguration();

        HttpMethodConfiguration merged = ConfigurationUtils.merge(base, local);
        assertEquals(10000, merged.getConnectionTimeout());
        assertEquals(20000, merged.getReadTimeout());
    }

    @Test
    public void testMergeOverridesTimeoutsEvenIfDefaultValue() {
        HttpMethodConfiguration base = new HttpMethodConfiguration();
        base.setConnectionTimeout(99999);
        base.setReadTimeout(88888);

        HttpMethodConfiguration local = new HttpMethodConfiguration();
        local.setConnectionTimeout(Wagon.DEFAULT_CONNECTION_TIMEOUT);
        local.setReadTimeout(Wagon.DEFAULT_READ_TIMEOUT);

        HttpMethodConfiguration merged = ConfigurationUtils.merge(base, local);
        assertEquals(Wagon.DEFAULT_CONNECTION_TIMEOUT, merged.getConnectionTimeout());
        assertEquals(Wagon.DEFAULT_READ_TIMEOUT, merged.getReadTimeout());
    }

    @Test
    public void testCopyClonesPreemptiveAndTimeouts() {
        HttpMethodConfiguration config = new HttpMethodConfiguration();
        config.setConnectionTimeout(111);
        config.setReadTimeout(222);
        config.setUsePreemptive(true);

        HttpMethodConfiguration copy = config.copy();
        assertTrue(copy.isConnectionTimeoutSet());
        assertEquals(111, copy.getConnectionTimeout());
        assertTrue(copy.isReadTimeoutSet());
        assertEquals(222, copy.getReadTimeout());
        assertTrue(copy.isUsePreemptiveSet());
        assertTrue(copy.isUsePreemptive());
    }
}
