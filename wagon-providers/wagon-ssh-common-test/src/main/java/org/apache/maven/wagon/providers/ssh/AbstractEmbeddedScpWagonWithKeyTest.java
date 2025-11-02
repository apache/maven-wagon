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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public abstract class AbstractEmbeddedScpWagonWithKeyTest extends StreamingWagonTestCase {

    SshServerEmbedded sshServerEmbedded;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        String sshKeyResource = "ssh-keys/id_rsa.pub";

        sshServerEmbedded = new SshServerEmbedded(getProtocol(), Arrays.asList(sshKeyResource), true);

        sshServerEmbedded.start();
        System.out.println("sshd on port " + sshServerEmbedded.getPort());
    }

    @Override
    protected void tearDownWagonTestingFixtures() throws Exception {

        sshServerEmbedded.stop();
    }

    protected abstract String getProtocol();

    protected int getTestRepositoryPort() {
        return sshServerEmbedded.getPort();
    }

    public String getTestRepositoryUrl() {
        return TestData.getTestRepositoryUrl(sshServerEmbedded.getPort());
    }

    protected AuthenticationInfo getAuthInfo() {
        AuthenticationInfo authInfo = super.getAuthInfo();
        // user : guest/guest123 -  passphrase : toto01
        authInfo.setUserName("guest");
        // authInfo.setPassword( TestData.getUserPassword() );
        authInfo.setPrivateKey(new File("src/test/ssh-keys/id_rsa").getPath());

        return authInfo;
    }

    protected long getExpectedLastModifiedOnGet(Repository repository, Resource resource) {
        return new File(repository.getBasedir(), resource.getName()).lastModified();
    }

    @Test
    public void testConnect() throws Exception {
        getWagon().connect(new Repository("foo", getTestRepositoryUrl()), getAuthInfo());
        assertTrue(true);
    }

    @Override
    protected boolean supportsGetIfNewer() {
        return false;
    }

    @Test
    public void testWithSpaces() throws Exception {
        String dir = "foo   test";
        File spaceDirectory = new File(TestData.getRepoPath(), dir);
        if (spaceDirectory.exists()) {
            FileUtils.deleteDirectory(spaceDirectory);
        }
        spaceDirectory.mkdirs();

        String subDir = "foo bar";
        File sub = new File(spaceDirectory, subDir);
        if (sub.exists()) {
            FileUtils.deleteDirectory(sub);
        }
        sub.mkdirs();

        File dummy = new File("src/test/resources/dummy.txt");
        FileUtils.copyFileToDirectory(dummy, sub);

        String url = getTestRepositoryUrl() + "/" + dir;
        Repository repo = new Repository("foo", url);
        Wagon wagon = getWagon();
        wagon.connect(repo, getAuthInfo());
        List<String> files = wagon.getFileList(subDir);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertTrue(files.contains("dummy.txt"));

        wagon.put(new File("src/test/resources/dummy.txt"), subDir + "/newdummy.txt");

        files = wagon.getFileList(subDir);
        assertNotNull(files);
        assertEquals(2, files.size());
        assertTrue(files.contains("dummy.txt"));
        assertTrue(files.contains("newdummy.txt"));

        File sourceWithSpace = new File("target/directory with spaces");
        if (sourceWithSpace.exists()) {
            FileUtils.deleteDirectory(sourceWithSpace);
        }
        File resources = new File("src/test/resources");

        FileUtils.copyDirectory(resources, sourceWithSpace);

        wagon.putDirectory(sourceWithSpace, "target with spaces");

        files = wagon.getFileList("target with spaces");

        assertNotNull(files);
        assertTrue(files.contains("dummy.txt"));
        assertFalse(files.contains("newdummy.txt"));
        assertTrue(files.contains("log4j.xml"));
    }
}
