package org.apache.maven.wagon.shared.http;

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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;

/**
 * Unit Tests for the HtmlFileListParser
 */
public class HtmlFileListParserTest
    extends TestCase
{
    private void assertContains( List files, String string )
    {
        if ( !files.contains( string ) )
        {
            fail( "File List does not contain expected '" + string + "'." );
        }
    }

    private List getFileList( String resourceName )
        throws TransferFailedException
    {
        InputStream is = this.getClass().getResourceAsStream( resourceName );
        List files = HtmlFileListParser.parseFileList( "http://www.ibiblio.org/maven2/org/apache/maven/wagon/", is );

        assertNotNull( "file list should not be null.", files );
        assertFalse( "file list should not be empty.", files.isEmpty() );

        /* Debug */
        if ( false )
        {
            Iterator it = files.iterator();
            while ( it.hasNext() )
            {
                System.out.println( "File: '" + it.next() + "'" );
            }
        }

        return files;
    }

    public void testParseIbiblio()
        throws Exception
    {
        List files = getFileList( "/filelistings/ibiblio-wagon.html" );

        assertTrue( "file list should contain at least 50 entries. (actually contains " + files.size() + " entries)",
                    files.size() > 50 );

        assertContains( files, "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1" );
        assertContains( files, "wagon-providers/" );
    }

    public void testParseMirror()
        throws Exception
    {
        List files = getFileList( "/filelistings/mirror-wagon.html" );

        assertTrue( "file list should contain at least 50 entries. (actually contains " + files.size() + " entries)",
                    files.size() > 50 );

        assertContains( files, "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1" );
        assertContains( files, "wagon-providers/" );
    }

    /**
     * Test of an html which is improperly formatted, and contains full host-specific paths to the resources.
     * @throws Exception
     */
    public void testParseJetty()
        throws Exception
    {
        List files = getFileList( "/filelistings/jetty-wagon.html" );

        assertTrue( "file list should contain at least 50 entries. (actually contains " + files.size() + " entries)",
                    files.size() > 50 );

        assertContains( files, "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1" );
        assertContains( files, "wagon-providers/" );
    }
}
