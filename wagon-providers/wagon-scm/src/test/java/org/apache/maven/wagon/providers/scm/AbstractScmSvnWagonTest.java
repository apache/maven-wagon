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
package org.apache.maven.wagon.providers.scm;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test for ScmWagon using SVN as underlying SCM
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 *
 */
public abstract class AbstractScmSvnWagonTest extends AbstractScmWagonTest {
    private String repository;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // copy the repo for the test

        File origRepo = getTestFile("target/test-classes/test-repo-svn");

        File testRepo = getTestFile("target/test-classes/test-repo-svn-test");

        FileUtils.deleteDirectory(testRepo);

        FileUtils.copyDirectoryStructure(origRepo, testRepo);

        repository = "scm:svn:" + testRepo.getAbsoluteFile().toPath().toUri().toASCIIString();
    }

    protected String getScmId() {
        return "svn";
    }

    protected String getTestRepositoryUrl() {
        return repository;
    }

    protected boolean supportsGetIfNewer() {
        return false;
    }
}
