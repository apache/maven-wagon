package org.apache.maven.wagon.providers.scm;

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

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

/**
 * Test for ScmWagon using CVS as underlying SCM
 * 
 * @author <a href="brett@apache.org">Brett Porter</a>
 *
 */
public abstract class AbstractScmCvsWagonTest
    extends AbstractScmWagonTest
{
    private String repository;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File origRepo = getTestFile( "target/test-classes/test-repo-cvs" );

        File testRepo = getTestFile( "target/test-classes/test-repo-cvs-test" );

        FileUtils.deleteDirectory( testRepo );

        FileUtils.copyDirectoryStructure( origRepo, testRepo );

        repository = "scm:cvs|local|" + testRepo.getAbsolutePath() + "|repository";

    }

    protected String getScmId()
    {
        return "cvs";
    }

    protected String getTestRepositoryUrl()
        throws IOException
    {
        return repository;
    }
}
