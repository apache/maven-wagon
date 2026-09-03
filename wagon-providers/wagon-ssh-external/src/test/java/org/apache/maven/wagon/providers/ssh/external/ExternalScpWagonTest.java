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
package org.apache.maven.wagon.providers.ssh.external;

import java.io.File;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ScpExternalWagon} that do not require an active SSH daemon.
 */
public class ExternalScpWagonTest {

    @Test
    public void testIsPuTTY() {
        ScpExternalWagon wagon = new ScpExternalWagon();

        wagon.setSshExecutable("c:\\program files\\PuTTY\\plink.exe");
        assertTrue(wagon.isPuTTY());
        wagon.setSshExecutable("plink");
        assertTrue(wagon.isPuTTY());
        wagon.setSshExecutable("PLINK");
        assertTrue(wagon.isPuTTY());
        wagon.setSshExecutable("PlInK");
        assertTrue(wagon.isPuTTY());
        wagon.setSshExecutable("ssh");
        assertFalse(wagon.isPuTTY());

        wagon.setScpExecutable("c:\\program files\\PuTTY\\pscp.exe");
        assertTrue(wagon.isPuTTYSCP());
        wagon.setScpExecutable("pscp");
        assertTrue(wagon.isPuTTYSCP());
        wagon.setScpExecutable("PSCP");
        assertTrue(wagon.isPuTTYSCP());
        wagon.setScpExecutable("PsCp");
        assertTrue(wagon.isPuTTYSCP());
        wagon.setScpExecutable("scp");
        assertFalse(wagon.isPuTTYSCP());
    }

    @Test
    public void testBuildCommandLineWithRelativeFileWithoutParent() throws Exception {
        ScpExternalWagon wagon = new ScpExternalWagon();
        wagon.setScpExecutable("scp");
        wagon.connect(new Repository("test", "scpexe://localhost/tmp"), new AuthenticationInfo());
        File bareFile = new File("test-relative-file.jar");

        Commandline cl = wagon.buildCommandLine(new Resource("test-relative-file.jar"), bareFile, true);

        assertNotNull(cl.getWorkingDirectory());
        assertEquals(
                bareFile.getAbsoluteFile().getParentFile().getAbsolutePath(),
                cl.getWorkingDirectory().getAbsolutePath());
    }

    @Test
    public void testBuildCommandLineWithParentDirectory() throws Exception {
        ScpExternalWagon wagon = new ScpExternalWagon();
        wagon.setScpExecutable("scp");
        wagon.connect(new Repository("test", "scpexe://localhost/tmp"), new AuthenticationInfo());
        File fileWithParent = new File("target", "test-file.jar");

        Commandline cl = wagon.buildCommandLine(new Resource("test-file.jar"), fileWithParent, true);

        assertNotNull(cl.getWorkingDirectory());
        assertEquals(
                fileWithParent.getParentFile().getAbsolutePath(),
                cl.getWorkingDirectory().getAbsolutePath());
    }
}
