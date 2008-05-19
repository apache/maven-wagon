package org.apache.maven.wagon.providers.scm;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.util.FileUtils;

import com.sun.corba.se.spi.activation.Repository;

import java.io.File;
import java.io.IOException;

/**
 * Test for ScmWagon using SVN as underlying SCM
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class AbstractScmSvnWagonTest
    extends AbstractScmWagonTest
{
    private String repository;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // copy the repo for the test

        File origRepo = getTestFile( "target/test-classes/test-repo-svn" );

        File testRepo = getTestFile( "target/test-classes/test-repo-svn-test" );

        FileUtils.deleteDirectory( testRepo );

        FileUtils.copyDirectoryStructure( origRepo, testRepo );

        repository = testRepo.getAbsolutePath();

        // TODO: this is a hack for windows
        // Note: why not use File.toURL() ?
        if ( repository.indexOf( ":" ) >= 0 )
        {
            repository = "/" + repository;
        }
        repository = repository.replace( '\\', '/' );

        repository = "scm:svn:file://" + repository;
    }

    protected String getScmId()
    {
        return "svn";
    }

    protected String getTestRepositoryUrl()
    {
        return repository;
    }
}
