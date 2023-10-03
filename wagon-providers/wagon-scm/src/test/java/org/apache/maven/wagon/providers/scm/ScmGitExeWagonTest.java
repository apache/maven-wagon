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

import org.apache.maven.scm.provider.ScmProvider;

/**
 * Test for ScmWagon using Git Exe as underlying SCM
 */
public class ScmGitExeWagonTest extends AbstractScmGitWagonTest {

    protected ScmProvider getScmProvider() {
        return new UserSafeGitExeScmProvider();
    }

    @Override
    public void testWagonGetFileList() throws Exception {
        // remote list unsupported
        // When a command is unsupported, SCM throws NoSuchCommandScmException.
        // However, there's no equivalent exception in the Wagon API.
        // ScmWagon wraps NoSuchCommandScmException with TransferFailedException, which gives no specific info.
        // TODO: WagonTestCase should somehow determine whether a command was unsupported
        // and skip the test using org.junit.Assume
    }

    @Override
    public void testWagonGetFileListWhenDirectoryDoesNotExist() throws Exception {
        // remote list unsupported
    }

    @Override
    public void testWagonResourceExists() throws Exception {
        // remote list unsupported
    }

    @Override
    public void testWagonResourceNotExists() throws Exception {
        // remote list unsupported
    }

    @Override
    protected boolean supportsGetIfNewer() {
        return false;
    }
}
