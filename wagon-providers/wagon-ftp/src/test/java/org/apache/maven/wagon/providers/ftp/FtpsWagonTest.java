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
package org.apache.maven.wagon.providers.ftp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the data channel protection negotiated after login, without opening a connection: the recording client
 * intercepts the two commands rather than sending them.
 */
class FtpsWagonTest {

    @Test
    void protectsTheDataChannelByDefault() throws IOException {
        RecordingFtpsClient client = new RecordingFtpsClient();

        new FtpsWagon().afterLogin(client);

        // PBSZ 0 has to precede PROT over TLS, per RFC 4217
        assertEquals(Arrays.asList("PBSZ 0", "PROT P"), client.commands);
    }

    @Test
    void honoursAConfiguredProtectionLevel() throws IOException {
        RecordingFtpsClient client = new RecordingFtpsClient();
        FtpsWagon wagon = new FtpsWagon();
        wagon.setDataChannelProtection("C");

        wagon.afterLogin(client);

        assertEquals(Arrays.asList("PBSZ 0", "PROT C"), client.commands);
    }

    @Test
    void sendsNothingWhenProtectionIsUnset() throws IOException {
        RecordingFtpsClient client = new RecordingFtpsClient();
        FtpsWagon wagon = new FtpsWagon();
        wagon.setDataChannelProtection(null);

        wagon.afterLogin(client);

        assertEquals(Collections.emptyList(), client.commands);
    }

    private static final class RecordingFtpsClient extends FTPSClient {

        private final List<String> commands = new ArrayList<>();

        @Override
        public void execPBSZ(long pbsz) {
            commands.add("PBSZ " + pbsz);
        }

        @Override
        public void execPROT(String prot) {
            commands.add("PROT " + prot);
        }
    }
}
