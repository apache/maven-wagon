package org.apache.maven.wagon;

/*
 * ==================================================================== The
 * Apache Software License, Version 1.1
 * 
 * Copyright (c) 2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The end-userName documentation
 * included with the redistribution, if any, must include the following
 * acknowledgment: "This product includes software developed by the Apache
 * Software Foundation (http://www.apache.org/)." Alternately, this
 * acknowledgment may appear in the software itself, if and wherever such
 * third-party acknowledgments normally appear. 4. The names "Apache" and
 * "Apache Software Foundation" and "Apache Maven" must not be used to endorse
 * or promote products derived from this software without prior written
 * permission. For written permission, please contact apache@apache.org. 5.
 * Products derived from this software may not be called "Apache", "Apache
 * Maven", nor may "Apache" appear in their name, without prior written
 * permission of the Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation. For more information on the
 * Apache Software Foundation, please see <http://www.apache.org/> .
 * 
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
