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
package org.apache.maven.wagon.providers.ssh;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * The order in which a key file is picked out of <code>~/.ssh</code>.
 */
public class ScpHelperTest {

    @Test
    public void ed25519IsPreferredWhenTheRuntimeCanUseIt() {
        assertArrayEquals(new String[] {"id_ed25519", "id_ecdsa", "id_rsa"}, ScpHelper.preferredPrivateKeyNames(true));
    }

    /**
     * The JDK grew EdDSA in Java 15. On anything older, and without a provider supplying it, an Ed25519 key
     * cannot be used at all -- so preferring it would fail the connection outright even though a usable
     * id_rsa is sitting next to it.
     */
    @Test
    public void ed25519IsDemotedWhenTheRuntimeCannotUseIt() {
        assertArrayEquals(new String[] {"id_ecdsa", "id_rsa", "id_ed25519"}, ScpHelper.preferredPrivateKeyNames(false));
    }

    /**
     * Demoted, not dropped: when it is the only key present it still has to be found. wagon-ssh-external
     * shells out to the host's scp, which does its own cryptography, so the JVM's capabilities say nothing
     * about whether the key is usable there.
     */
    @Test
    public void ed25519IsStillOfferedWhenTheRuntimeCannotUseIt() {
        assertTrue(Arrays.asList(ScpHelper.preferredPrivateKeyNames(false)).contains("id_ed25519"));
    }
}
