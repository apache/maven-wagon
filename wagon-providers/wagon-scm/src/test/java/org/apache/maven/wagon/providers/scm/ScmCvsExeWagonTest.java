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

import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.cvslib.cvsexe.CvsExeScmProvider;

/**
 * Test for ScmWagon using CVS Exe as underlying SCM
 *
 * @author <a href="carlos@apache.org">Carlos Sanchez</a>
 *
 */
public class ScmCvsExeWagonTest
    extends AbstractScmCvsWagonTest
{

    @Override
    protected void setUp()
        throws Exception
    {
        assumeHaveCvsBinary();
        if ( !testSkipped )
        {
            super.setUp();
        }
    }

    protected ScmProvider getScmProvider()
    {
        return new CvsExeScmProvider();
    }

    @Override
    public void testWagonGetFileList()
        throws Exception
    {
        // cvs rls is rare
    }

    @Override
    public void testWagonResourceExists()
        throws Exception
    {
        // cvs rls is rare
    }

    @Override
    public void testWagonResourceNotExists()
        throws Exception
    {
        // cvs rls is rare
    }

    @Override
    protected boolean supportsGetIfNewer()
    {
        return false;
    }

    /** Optionally set the testSkipped flag */
    protected void assumeHaveCvsBinary()
    {
        if ( !isSystemCmd( CVS_COMMAND_LINE ) )
        {
            testSkipped = true;
            System.err.println( "'" + CVS_COMMAND_LINE + "' is not a system command. Ignored " + getName() + "." );
        }
    }

    /** 'cvs' command line */
    public static final String CVS_COMMAND_LINE = "cvs";
}
