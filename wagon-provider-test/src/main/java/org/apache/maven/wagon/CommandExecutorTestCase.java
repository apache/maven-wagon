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
package org.apache.maven.wagon;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for command executor tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 */
public abstract class CommandExecutorTestCase extends PlexusTestCase {
    @Test
    public void testErrorInCommandExecuted() throws Exception {
        CommandExecutor exec = (CommandExecutor) lookup(CommandExecutor.ROLE);

        Repository repository = getTestRepository();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName(System.getProperty("user.name"));

        exec.connect(repository, authenticationInfo);

        try {
            exec.executeCommand("fail");
            fail("Command should have failed");
        } catch (CommandExecutionException e) {
            assertTrue(e.getMessage().trim().endsWith("fail: command not found"));
        } finally {
            exec.disconnect();
        }
    }

    @Test
    public void testIgnoreFailuresInCommandExecuted() throws Exception {
        CommandExecutor exec = (CommandExecutor) lookup(CommandExecutor.ROLE);

        Repository repository = getTestRepository();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName(System.getProperty("user.name"));

        exec.connect(repository, authenticationInfo);

        try {
            Streams streams = exec.executeCommand("fail", true);
            // expect no exception, and stderr has something.
            assertTrue(streams.getErr().length() > 0);
        } finally {
            exec.disconnect();
        }
    }

    @Test
    public void testExecuteSuccessfulCommand() throws Exception {
        CommandExecutor exec = (CommandExecutor) lookup(CommandExecutor.ROLE);

        Repository repository = getTestRepository();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName(System.getProperty("user.name"));

        exec.connect(repository, authenticationInfo);

        try {
            exec.executeCommand("ls");
        } finally {
            exec.disconnect();
        }
    }

    protected abstract Repository getTestRepository();
}
