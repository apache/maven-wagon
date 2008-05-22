package org.apache.maven.wagon;

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

import junit.framework.TestCase;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class PathUtilsTest
    extends TestCase
{
    public void testFilenameResolving()
    {
        assertEquals( "filename", PathUtils.filename( "dir/filename" ) );

        assertEquals( "filename", PathUtils.filename( "filename" ) );

        assertEquals( "filename", PathUtils.filename( "dir1/dir2/filename" ) );
    }

    public void testDirResolving()
    {
        assertEquals( "dir", PathUtils.dirname( "dir/filename" ) );

        assertEquals( "", PathUtils.dirname( "filename" ) );

        assertEquals( "dir1/dir2", PathUtils.dirname( "dir1/dir2/filename" ) );
    }

    public void testDirSpliting()
    {
        final String path = "a/b/c";

        final String[] dirs = PathUtils.dirnames( path );

        assertNotNull( dirs );

        assertEquals( 2, dirs.length );

        assertEquals( "a", dirs[0] );

        assertEquals( "b", dirs[1] );

    }

    public void testHostResolving()
    {
        assertEquals( "www.codehaus.org", PathUtils.host( "http://www.codehaus.org" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "HTTP://www.codehaus.org" ) );

        assertEquals( "localhost", PathUtils.host( null ) );
        assertEquals( "localhost", PathUtils.host( "file:///c:/temp" ) );
        assertEquals( "localhost", PathUtils.host( "FILE:///c:/temp" ) );

    }

    public void testScmHostResolving()
    {
        assertEquals( "www.codehaus.org", PathUtils.host( "scm:svn:http://www.codehaus.org" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "SCM:SVN:HTTP://www.codehaus.org" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "scm:svn:http://www.codehaus.org/repos/module" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "SCM:SVN:HTTP://www.codehaus.org/repos/module" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "scm:cvs:pserver:anoncvs@www.codehaus.org:/root" ) );
        assertEquals( "www.codehaus.org", PathUtils.host( "SCM:CVS:pserver:anoncvs@www.codehaus.org:/root" ) );
    }

    public void testProtocolResolving()
    {
        assertEquals( "http", PathUtils.protocol( "http://www.codehause.org" ) );
        assertEquals( "HTTP", PathUtils.protocol( "HTTP://www.codehause.org" ) );
        assertEquals( "file", PathUtils.protocol( "file:///c:/temp" ) );
        assertEquals( "scm", PathUtils.protocol( "scm:svn:http://localhost/repos/module" ) );
        assertEquals( "scm", PathUtils.protocol( "scm:cvs:pserver:anoncvs@cvs.apache.org:/home/cvspublic" ) );
    }

    public void testUserInfo()
    {
        String urlWithUsername = "http://brett@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsername ) );
        assertNull( PathUtils.password( urlWithUsername ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsername ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsername ) );

        String urlWithUsernamePassword = "http://brett:porter@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsernamePassword ) );
        assertEquals( "porter", PathUtils.password( urlWithUsernamePassword ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsernamePassword ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsernamePassword ) );
    }

    public void testSubversionUserInfo()
    {
        String urlWithUsername = "scm:svn:http://brett@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsername ) );
        assertNull( PathUtils.password( urlWithUsername ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsername ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsername ) );

        String urlWithUsernamePassword = "scm:svn:http://brett:porter@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsernamePassword ) );
        assertEquals( "porter", PathUtils.password( urlWithUsernamePassword ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsernamePassword ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsernamePassword ) );

        String urlWithUpperCaseProtocol = "SCM:SVN:HTTP://brett@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUpperCaseProtocol ) );
        assertNull( PathUtils.password( urlWithUpperCaseProtocol ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUpperCaseProtocol ) );
        assertEquals( "/", PathUtils.basedir( urlWithUpperCaseProtocol ) );
    }

    public void testCvsUserInfo()
    {
        String urlWithUsername = "scm:cvs:pserver:brett@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsername ) );
        assertNull( PathUtils.password( urlWithUsername ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsername ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsername ) );

        String urlWithUsernamePassword = "scm:cvs:pserver:brett:porter@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUsernamePassword ) );
        assertEquals( "porter", PathUtils.password( urlWithUsernamePassword ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUsernamePassword ) );
        assertEquals( "/", PathUtils.basedir( urlWithUsernamePassword ) );

        String urlWithUpperCaseProtocol = "SCM:CVS:pserver:brett@www.codehaus.org";
        assertEquals( "brett", PathUtils.user( urlWithUpperCaseProtocol ) );
        assertNull( PathUtils.password( urlWithUpperCaseProtocol ) );
        assertEquals( "www.codehaus.org", PathUtils.host( urlWithUpperCaseProtocol ) );
        assertEquals( "/", PathUtils.basedir( urlWithUpperCaseProtocol ) );
    }

    public void testFileBasedir()
    {
        // see http://www.mozilla.org/quality/networking/testing/filetests.html
        
        // strict forms
        assertEquals( "c:/temp", PathUtils.basedir( "file:///c|/temp" ) );
        assertEquals( "localhost", PathUtils.host( "file:///c|/temp" ) );
        assertEquals( "c:/temp", PathUtils.basedir( "file://localhost/c|/temp" ) );
        assertEquals( "localhost", PathUtils.host( "file://localhost/c|/temp" ) );
        assertEquals( "/temp", PathUtils.basedir( "file:///temp" ) );
        assertEquals( "localhost", PathUtils.host( "file:///temp" ) );
        assertEquals( "/temp", PathUtils.basedir( "file://localhost/temp" ) );
        assertEquals( "localhost", PathUtils.host( "file://localhost/temp" ) );

        // strict form, with : for drive separator
        assertEquals( "c:/temp", PathUtils.basedir( "file:///c:/temp" ) );
        assertEquals( "localhost", PathUtils.host( "file:///c:/temp" ) );
        assertEquals( "c:/temp", PathUtils.basedir( "file://localhost/c:/temp" ) );
        assertEquals( "localhost", PathUtils.host( "file://localhost/c:/temp" ) );

        // convenience forms
        assertEquals( "c:/temp", PathUtils.basedir( "file://c:/temp" ) );
        assertEquals( "c:/temp", PathUtils.basedir( "file://c|/temp" ) );
        assertEquals( "c:/temp", PathUtils.basedir( "file:c:/temp" ) );
        assertEquals( "c:/temp", PathUtils.basedir( "file:c|/temp" ) );
        assertEquals( "/temp", PathUtils.basedir( "file:/temp" ) );

        assertEquals( "c:/temp", PathUtils.basedir( "FILE:///c:/temp" ) );
        assertEquals( "localhost", PathUtils.host( "FILE:///c:/temp" ) );
    }

    public void testEmptyBasedir()
    {
        assertEquals( "/", PathUtils.basedir( "http://www.codehaus.org:80" ) );
        assertEquals( "/", PathUtils.basedir( "http://www.codehaus.org" ) );
        assertEquals( "/", PathUtils.basedir( "http://www.codehaus.org:80/" ) );
        assertEquals( "/", PathUtils.basedir( "http://www.codehaus.org/" ) );
        assertEquals( "/", PathUtils.basedir( "HTTP://www.codehaus.org/" ) );
    }

    public void testPortResolving()
    {
        assertEquals( 80, PathUtils.port( "http://www.codehause.org:80/maven" ) );
        assertEquals( 80, PathUtils.port( "HTTP://www.codehause.org:80/maven" ) );
        assertEquals( WagonConstants.UNKNOWN_PORT, PathUtils.port( "http://localhost/temp" ) );

        assertEquals( 10, PathUtils.port( "ftp://localhost:10" ) );
        assertEquals( 10, PathUtils.port( "FTP://localhost:10" ) );
    }

    public void testScmPortResolving()
    {
        assertEquals( 80, PathUtils.port( "scm:svn:http://www.codehaus.org:80/maven" ) );
        assertEquals( 80, PathUtils.port( "SCM:SVN:HTTP://www.codehaus.org:80/maven" ) );
        assertEquals( WagonConstants.UNKNOWN_PORT, PathUtils.port( "scm:cvs:pserver:anoncvs@localhost:/temp:module" ) );

        assertEquals( 2402, PathUtils.port( "scm:cvs:pserver:anoncvs@localhost:2402/temp:module" ) );
        assertEquals( 2402, PathUtils.port( "SCM:CVS:pserver:anoncvs@localhost:2402/temp:module" ) );
    }

    public void testScmBasedir()
    {
        assertEquals( "/maven", PathUtils.basedir( "scm:svn:http://www.codehause.org/maven" ) );
        assertEquals( "/maven", PathUtils.basedir( "SCM:SVN:HTTP://www.codehause.org/maven" ) );
        assertEquals( "/maven", PathUtils.basedir( "scm:svn:http://www.codehause.org:80/maven" ) );
        assertEquals( "/maven", PathUtils.basedir( "scm:cvs:pserver:anoncvs@www.codehause.org:80/maven" ) );
        assertEquals( "/maven", PathUtils.basedir( "scm:cvs:pserver:anoncvs@www.codehause.org:/maven" ) );
        assertEquals( "/maven/module", PathUtils.basedir( "scm:cvs:pserver:anoncvs@www.codehause.org:80/maven:module" ) );
        assertEquals( "/maven/module", PathUtils.basedir( "scm:cvs:pserver:anoncvs@www.codehause.org:/maven:module" ) );
        assertEquals( "/maven/module", PathUtils.basedir( "SCM:CVS:pserver:anoncvs@www.codehause.org:/maven:module" ) );
    }

    public void testPortBasedir()
    {
        assertEquals( "/maven", PathUtils.basedir( "http://www.codehause.org:80/maven" ) );
        assertEquals( "/temp", PathUtils.basedir( "http://localhost/temp" ) );

        assertEquals( "c:/temp", PathUtils.basedir( "file://c:/temp" ) );
        assertEquals( "/", PathUtils.basedir( "http://localhost:80/" ) );
        assertEquals( "/", PathUtils.basedir( "http://localhost/" ) );
    }

}
