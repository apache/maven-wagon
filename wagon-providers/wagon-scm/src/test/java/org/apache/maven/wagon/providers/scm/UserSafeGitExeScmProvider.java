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

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.provider.git.GitScmTestUtils;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;

/**
 * @plexus.component role="org.apache.maven.scm.provider.ScmProvider" role-hint="git"
 */
public class UserSafeGitExeScmProvider extends GitExeScmProvider {

    @Override
    public CheckInScmResult checkIn(ScmRepository repository, ScmFileSet fileSet, ScmVersion scmVersion, String message)
            throws ScmException {
        GitScmTestUtils.setDefaultUser(fileSet.getBasedir());

        return super.checkIn(repository, fileSet, scmVersion, message);
    }
}
