package org.apache.maven.wagon;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import junit.framework.TestCase;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class PathUtilsTest extends TestCase
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
        assertEquals(
                "www.codehaus.org",
                PathUtils.host( "http://www.codehaus.org" ) );

        assertEquals( "localhost", PathUtils.host( null ) );

    }

    public void testProtocolResolving()
    {
        assertEquals( "http", PathUtils.protocol( "http://www.codehause.org" ) );
        assertEquals( "file", PathUtils.protocol( "file:///c:/temp" ) );

    }

    public void testPortResolving()
    {
        assertEquals( 80, PathUtils.port( "http://www.codehause.org:80/maven" ) );
        assertEquals(
                WagonConstants.UNKNOWN_PORT,
                PathUtils.port( "http://localhost/temp" ) );

        assertEquals( 10, PathUtils.port( "ftp://localhost:10" ) );

    }

    public void testPortBasedir()
    {
        assertEquals(
                "maven",
                PathUtils.basedir( "http://www.codehause.org:80/maven" ) );
        assertEquals( "temp", PathUtils.basedir( "http://localhost/temp" ) );

        assertEquals( "c:/temp", PathUtils.basedir( "file://c:/temp" ) );
        assertEquals( "", PathUtils.basedir( "http://localhost:80/" ) );
        assertEquals( "", PathUtils.basedir( "http://localhost/" ) );

    }

}
