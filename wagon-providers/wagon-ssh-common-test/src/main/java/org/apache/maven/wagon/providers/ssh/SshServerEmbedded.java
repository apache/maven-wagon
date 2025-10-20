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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;

/**
 * @author Olivier Lamy
 */
public class SshServerEmbedded {
    private String wagonProtocol;

    private int port;

    private SshServer sshd;

    private List<String> sshKeysResources = new ArrayList<>();

    private TestPublickeyAuthenticator publickeyAuthenticator;

    TestPasswordAuthenticator passwordAuthenticator = new TestPasswordAuthenticator();

    private boolean keyAuthz;

    /**
     * @param wagonProtocol    scp scpexe
     * @param sshKeysResources paths in the classlaoder with ssh keys
     */
    public SshServerEmbedded(String wagonProtocol, List<String> sshKeysResources, boolean keyAuthz) {
        this.wagonProtocol = wagonProtocol;

        this.sshKeysResources = sshKeysResources;

        this.sshd = SshServer.setUpDefaultServer();

        // this.sshd.setKeyExchangeFactories(  );

        this.keyAuthz = keyAuthz;

        publickeyAuthenticator = new TestPublickeyAuthenticator(this.keyAuthz);
    }

    /**
     * @return random port used
     */
    public int start() throws IOException {
        sshd.setPort(0);

        sshd.setPublickeyAuthenticator(this.publickeyAuthenticator);

        sshd.setPasswordAuthenticator(this.passwordAuthenticator);

        File path = new File("target/keys");
        path.mkdirs();
        path = new File(path, "simple.key");
        path.delete();

        SimpleGeneratorHostKeyProvider provider = new SimpleGeneratorHostKeyProvider(path.toPath());
        provider.setAlgorithm("RSA");
        provider.setKeySize(1024);

        sshd.setKeyPairProvider(provider);

        // sshd.setFileSystemFactory(  );

        final ProcessShellFactory processShellFactory = new ProcessShellFactory("/bin/sh", "-i", "-l");
        sshd.setShellFactory(processShellFactory);

        ScpCommandFactory commandFactory = new ScpCommandFactory.Builder()
                .withDelegate((channel, command) -> new ShellCommand(command))
                .build();
        sshd.setCommandFactory(commandFactory);

        sshd.setFileSystemFactory(new NativeFileSystemFactory());
        sshd.start();
        this.port = sshd.getPort();
        return this.port;
    }

    public void stop() throws IOException {
        sshd.stop();
    }

    public int getPort() {
        return port;
    }

    public PasswordAuthenticator getPasswordAuthenticator() {
        return passwordAuthenticator;
    }
}
