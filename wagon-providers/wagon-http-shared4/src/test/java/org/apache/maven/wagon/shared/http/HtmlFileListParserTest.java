package org.apache.maven.wagon.shared.http;

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

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.maven.wagon.TransferFailedException;

/**
 * Unit Tests for the HtmlFileListParser
 */
public class HtmlFileListParserTest
    extends TestCase
{
    private static Logger logger = Logger.getLogger( HtmlFileListParserTest.class );

    private void assertContainsExpected( List/*<String>*/links, String[] expected )
    {
        if ( expected.length != links.size() )
        {
            Collections.sort( links );
            for ( Iterator iterator = links.iterator(); iterator.hasNext(); )
            {
                String link = (String) iterator.next();
                logger.info( "   \"" + link + "\", " );
            }
            assertEquals( "Links to Expected size", expected.length, links.size() );
        }

        for ( int i = 0; i < expected.length; i++ )
        {
            assertTrue( "Should find [" + expected[i] + "] in link list", links.contains( expected[i] ) );
        }
    }

    private void assertNotContainingAvoided( List/*<String>*/links, String[] avoided )
    {
        for ( int i = 0; i < avoided.length; i++ )
        {
            assertFalse( "Should not find [" + avoided[i] + "] in link list", links.contains( avoided[i] ) );
        }
    }

    private List/*<String>*/parseLinks( String url, String filename )
        throws TransferFailedException
    {
        InputStream is = this.getClass().getResourceAsStream( "/filelistings/" + filename );
        List files = HtmlFileListParser.parseFileList( url, is );

        assertNotNull( "file list should not be null.", files );
        assertFalse( "file list should not be empty.", files.isEmpty() );

        /* Debug */
        if ( false )
        {
            Iterator it = files.iterator();
            while ( it.hasNext() )
            {
                logger.info( "File: '" + it.next() + "'" );
            }
        }

        return files;
    }

    /**
     * Example showing jetty directory browsing of commons-lang 
     * 
     * @throws TransferFailedException
     */
    public void testParseCommonsLang()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks( "http://localhost/repository/commons-lang/commons-lang/2.3",
                                             "commons-lang.html" );

        String[] expected = new String[] {
            "commons-lang-2.3-javadoc.jar",
            "commons-lang-2.3-javadoc.jar.asc",
            "commons-lang-2.3-javadoc.jar.md5",
            "commons-lang-2.3-javadoc.jar.sha1",
            "commons-lang-2.3-sources.jar",
            "commons-lang-2.3-sources.jar.asc",
            "commons-lang-2.3-sources.jar.md5",
            "commons-lang-2.3-sources.jar.sha1",
            "commons-lang-2.3.jar",
            "commons-lang-2.3.jar.asc",
            "commons-lang-2.3.jar.md5",
            "commons-lang-2.3.jar.sha1",
            "commons-lang-2.3.pom",
            "commons-lang-2.3.pom.md5",
            "commons-lang-2.3.pom.sha1",
            "maven-metadata.xml",
            "maven-metadata.xml.md5",
            "maven-metadata.xml.sha1" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "../" };

        assertNotContainingAvoided( links, avoided );
    }

    public void testParseIbiblio()
        throws Exception
    {
        List/*<String>*/links = parseLinks( "http://www.ibiblio.org/maven2/org/apache/maven/wagon/",
                                             "ibiblio-wagon.html" );

        String[] expected = new String[] {
            "wagon-1.0-alpha-2.pom",
            "wagon-1.0-alpha-2.pom.asc",
            "wagon-1.0-alpha-2.pom.md5",
            "wagon-1.0-alpha-2.pom.sha1",
            "wagon-1.0-alpha-3-20050413.021234-4.pom",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.md5",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.sha1",
            "wagon-1.0-alpha-3-20050419.043745-5.pom",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.md5",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1",
            "wagon-1.0-alpha-3-20050419.044035-6.pom",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.md5",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.sha1",
            "wagon-1.0-alpha-3-20050421.162738-7.pom",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.md5",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.sha1",
            "wagon-1.0-alpha-3-20050422.075233-8.pom",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.md5",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.sha1",
            "wagon-1.0-alpha-3-20050429.051847-9.pom",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.md5",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.sha1",
            "wagon-file/",
            "wagon-ftp/",
            "wagon-http-lightweight/",
            "wagon-http/",
            "wagon-lightweight-http/",
            "wagon-provider-api/",
            "wagon-provider-test/",
            "wagon-provider/",
            "wagon-providers-1.0-alpha-2.pom",
            "wagon-providers-1.0-alpha-2.pom.asc",
            "wagon-providers-1.0-alpha-2.pom.md5",
            "wagon-providers-1.0-alpha-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.md5",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.md5",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.md5",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.md5",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.md5",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.sha1",
            "wagon-providers/",
            "wagon-scm/",
            "wagon-ssh-external/",
            "wagon-ssh/",
            "wagon-webdav/",
            "wagon/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/org/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A", "?D=A", "?M=A", "?N=D", "?S=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Test of an html which is improperly formatted, and contains full host-specific paths to the resources.
     * @throws Exception
     */
    public void testParseJetty()
        throws Exception
    {
        List/*<String>*/links = parseLinks( "http://www.ibiblio.org/maven2/org/apache/maven/wagon/",
                                             "jetty-wagon.html" );

        String[] expected = new String[] {
            "wagon-1.0-alpha-2.pom",
            "wagon-1.0-alpha-2.pom.asc",
            "wagon-1.0-alpha-2.pom.md5",
            "wagon-1.0-alpha-2.pom.sha1",
            "wagon-1.0-alpha-3-20050413.021234-4.pom",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.md5",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.sha1",
            "wagon-1.0-alpha-3-20050419.043745-5.pom",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.md5",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1",
            "wagon-1.0-alpha-3-20050419.044035-6.pom",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.md5",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.sha1",
            "wagon-1.0-alpha-3-20050421.162738-7.pom",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.md5",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.sha1",
            "wagon-1.0-alpha-3-20050422.075233-8.pom",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.md5",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.sha1",
            "wagon-1.0-alpha-3-20050429.051847-9.pom",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.md5",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.sha1",
            "wagon-file/",
            "wagon-ftp/",
            "wagon-http-lightweight/",
            "wagon-http/",
            "wagon-lightweight-http/",
            "wagon-provider-api/",
            "wagon-provider-test/",
            "wagon-provider/",
            "wagon-providers-1.0-alpha-2.pom",
            "wagon-providers-1.0-alpha-2.pom.asc",
            "wagon-providers-1.0-alpha-2.pom.md5",
            "wagon-providers-1.0-alpha-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.md5",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.md5",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.md5",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.md5",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.md5",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.sha1",
            "wagon-providers/",
            "wagon-scm/",
            "wagon-ssh-external/",
            "wagon-ssh/",
            "wagon-webdav/",
            "wagon/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/org/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Test of an html which is improperly formatted, contains full host-specific paths to the resources and uses non-normalized base URI
     * @throws Exception
     */
    public void testParseJettyWithNonNormalizedBaseURI()
        throws Exception
    {
        List/*<String>*/links = parseLinks( "http://www.ibiblio.org/maven2/org/apache/maven/wagon//",
                                             "jetty-wagon.html" );

        String[] expected = new String[] {
            "wagon-1.0-alpha-2.pom",
            "wagon-1.0-alpha-2.pom.asc",
            "wagon-1.0-alpha-2.pom.md5",
            "wagon-1.0-alpha-2.pom.sha1",
            "wagon-1.0-alpha-3-20050413.021234-4.pom",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.md5",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.sha1",
            "wagon-1.0-alpha-3-20050419.043745-5.pom",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.md5",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1",
            "wagon-1.0-alpha-3-20050419.044035-6.pom",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.md5",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.sha1",
            "wagon-1.0-alpha-3-20050421.162738-7.pom",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.md5",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.sha1",
            "wagon-1.0-alpha-3-20050422.075233-8.pom",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.md5",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.sha1",
            "wagon-1.0-alpha-3-20050429.051847-9.pom",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.md5",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.sha1",
            "wagon-file/",
            "wagon-ftp/",
            "wagon-http-lightweight/",
            "wagon-http/",
            "wagon-lightweight-http/",
            "wagon-provider-api/",
            "wagon-provider-test/",
            "wagon-provider/",
            "wagon-providers-1.0-alpha-2.pom",
            "wagon-providers-1.0-alpha-2.pom.asc",
            "wagon-providers-1.0-alpha-2.pom.md5",
            "wagon-providers-1.0-alpha-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.md5",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.md5",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.md5",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.md5",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.md5",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.sha1",
            "wagon-providers/",
            "wagon-scm/",
            "wagon-ssh-external/",
            "wagon-ssh/",
            "wagon-webdav/",
            "wagon/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/org/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Using repository.codehaus.org output as an example.
     * This is an example of an older RHEL installation of apache httpd with old fancy indexing output
     * This example tests how to detect directories properly.
     * 
     * @throws TransferFailedException
     */
    public void testParseMevenIde()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks( "http://repository.codehaus.org/org/codehaus/mevenide/", "mevenide.html" );

        String[] expected = new String[] {
            "apisupport/",
            "autoupdate/",
            "continuum-rpc/",
            "continuum/",
            "debugger-bridge/",
            "deployment-bridge/",
            "feature/",
            "grammar/",
            "ide-mojos/",
            "indexer/",
            "j2ee/",
            "junit/",
            "maven-metadata.xml",
            "maven-metadata.xml.md5",
            "maven-metadata.xml.sha1",
            "mevenide2-parent/",
            "nb-mvn-embedder/",
            "nb-project/",
            "nb-repo-browser/",
            "netbeans-debugger-plugin/",
            "netbeans-deploy-plugin/",
            "netbeans-libs/",
            "netbeans-nbmreload-plugin/",
            "netbeans-repository/",
            "netbeans-run-plugin/",
            "netbeans/",
            "persistence/",
            "plugin-bridges/",
            "plugins/",
            "reload-nbm-bridge/",
            "run-jar-bridge/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/org/codehaus/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    public void testParseMirror()
        throws Exception
    {
        List/*<String>*/links = parseLinks( "http://www.ibiblio.org/maven2/org/apache/maven/wagon/",
                                             "mirror-wagon.html" );

        String[] expected = new String[] {
            "wagon-1.0-alpha-2.pom",
            "wagon-1.0-alpha-2.pom.asc",
            "wagon-1.0-alpha-2.pom.md5",
            "wagon-1.0-alpha-2.pom.sha1",
            "wagon-1.0-alpha-3-20050413.021234-4.pom",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.md5",
            "wagon-1.0-alpha-3-20050413.021234-4.pom.sha1",
            "wagon-1.0-alpha-3-20050419.043745-5.pom",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.md5",
            "wagon-1.0-alpha-3-20050419.043745-5.pom.sha1",
            "wagon-1.0-alpha-3-20050419.044035-6.pom",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.md5",
            "wagon-1.0-alpha-3-20050419.044035-6.pom.sha1",
            "wagon-1.0-alpha-3-20050421.162738-7.pom",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.md5",
            "wagon-1.0-alpha-3-20050421.162738-7.pom.sha1",
            "wagon-1.0-alpha-3-20050422.075233-8.pom",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.md5",
            "wagon-1.0-alpha-3-20050422.075233-8.pom.sha1",
            "wagon-1.0-alpha-3-20050429.051847-9.pom",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.md5",
            "wagon-1.0-alpha-3-20050429.051847-9.pom.sha1",
            "wagon-file/",
            "wagon-ftp/",
            "wagon-http-lightweight/",
            "wagon-http/",
            "wagon-lightweight-http/",
            "wagon-provider-api/",
            "wagon-provider-test/",
            "wagon-provider/",
            "wagon-providers-1.0-alpha-2.pom",
            "wagon-providers-1.0-alpha-2.pom.asc",
            "wagon-providers-1.0-alpha-2.pom.md5",
            "wagon-providers-1.0-alpha-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.md5",
            "wagon-providers-1.0-alpha-3-20050407.202848-1.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.md5",
            "wagon-providers-1.0-alpha-3-20050419.044035-2.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.md5",
            "wagon-providers-1.0-alpha-3-20050421.162738-3.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.md5",
            "wagon-providers-1.0-alpha-3-20050422.075233-4.pom.sha1",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.md5",
            "wagon-providers-1.0-alpha-3-20050429.051847-5.pom.sha1",
            "wagon-providers/",
            "wagon-scm/",
            "wagon-ssh-external/",
            "wagon-ssh/",
            "wagon-webdav/",
            "wagon/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] {
            "/org/codehaus/",
            "?C=S;O=A",
            "?C=D;O=A",
            "?C=M;O=A",
            "mailto:mirror.admin@mirror.com" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Example of output from repo1.maven.org
     * This example is of nekohtml specifically.
     * 
     * @throws TransferFailedException
     */
    public void testParseNekoHtml()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks( "http://repo1.maven.org//maven2/nekohtml/nekohtml/1.9.6/", "nekohtml.html" );

        String[] expected = new String[] {
            "nekohtml-1.9.6-javadoc.jar",
            "nekohtml-1.9.6-javadoc.jar.md5",
            "nekohtml-1.9.6-javadoc.jar.sha1",
            "nekohtml-1.9.6-sources.jar",
            "nekohtml-1.9.6-sources.jar.md5",
            "nekohtml-1.9.6-sources.jar.sha1",
            "nekohtml-1.9.6.jar",
            "nekohtml-1.9.6.jar.md5",
            "nekohtml-1.9.6.jar.sha1",
            "nekohtml-1.9.6.pom",
            "nekohtml-1.9.6.pom.md5",
            "nekohtml-1.9.6.pom.sha1" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/maven2/nekohtml/nekohtml/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Example of detecting directories on repo1.maven.org
     * 
     * @throws TransferFailedException
     */
    public void testParseNetSourceforge()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks( "http://repo1.maven.org/maven2/net/sf/", "net_sf.html" );

        String[] expected = new String[] {
            "a2j/",
            "aislib/",
            "alchim/",
            "antenna/",
            "apt-jelly/",
            "beanlib/",
            "bluecove/",
            "buildbox/",
            "click/",
            "clirr/",
            "datavision/",
            "dozer/",
            "dtddoc/",
            "dynpageplus/",
            "ehcache/",
            "ezmorph/",
            "falcon/",
            "grester/",
            "gwt-widget/",
            "hermesftp/",
            "hibernate/",
            "jcharts/",
            "jdatabaseimport/",
            "jeceira/",
            "jfcunit/",
            "jfig/",
            "jguard/",
            "jipcam/",
            "jlynx/",
            "jour/",
            "jpf/",
            "json-lib/",
            "jsptest/",
            "jsr107cache/",
            "jt400/",
            "jxls/",
            "kxml/",
            "ldaptemplate/",
            "locale4j/",
            "mapasuta/",
            "maven-har/",
            "maven-sar/",
            "opencsv/",
            "oval/",
            "proguard/",
            "qdwizard/",
            "resultsetmapper/",
            "retrotranslator/",
            "saxon/",
            "shadesdb/",
            "smc/",
            "speculoos/",
            "springlayout/",
            "stat-scm/",
            "statsvn/",
            "stax/",
            "struts/",
            "tacos/",
            "testextensions/",
            "webdav-servlet/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/maven2/net/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Another larger example of the directory link detection on repository.codehaus.org
     * 
     * @throws TransferFailedException
     */
    public void testParseOrgCodehaus()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks( "http://repository.codehaus.org/org/codehaus", "org.codehaus.html" );

        String[] expected = new String[] {
            "agilifier/",
            "benji/",
            "bruce/",
            "btm/",
            "cargo/",
            "castor-spring/",
            "castor/",
            "cozmos/",
            "dataforge/",
            "dimple/",
            "droolsdotnet/",
            "enunciate/",
            "fabric3/",
            "gant/",
            "grails-plugins/",
            "groovy/",
            "gsoc/",
            "guessencoding/",
            "gumtree/",
            "gwt-openlayers/",
            "haus/",
            "izpack/",
            "javasim/",
            "jedi/",
            "jequel/",
            "jet/",
            "jettison/",
            "jfdi/",
            "jikesrvm/",
            "jra/",
            "jremoting/",
            "jtestme/",
            "jtestr/",
            "labs-ng/",
            "logicabyss/",
            "marionette/",
            "mevenide/",
            "modello/",
            "mojo/",
            "mvel/",
            "mvflex/",
            "native-mojo/",
            "openim/",
            "plexus/",
            "polymap/",
            "prometheus/",
            "prophit/",
            "quaere/",
            "redback/",
            "rulessandpit/",
            "rvm/",
            "savana/",
            "scala-ide/",
            "senro/",
            "sonar/",
            "staxmate/",
            "stomp/",
            "svn4j/",
            "swiby/",
            "swizzle/",
            "sxc/",
            "testdox/",
            "wadi/",
            "waffle/",
            "woodstox/",
            "xdoclet/",
            "xfire/",
            "xharness/",
            "xsite/",
            "xstream/",
            "xwire/" };

        assertContainsExpected( links, expected );

        String[] avoided = new String[] { "/org/", "?C=S;O=A", "?C=D;O=A", "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }

    /**
     * Test the output found from apache httpd with fancy indexing and dav module.
     * Using people.apache.org output as source material.
     * 
     * @throws TransferFailedException
     */
    public void testParsePeopleApacheOrg()
        throws TransferFailedException
    {
        List/*<String>*/links = parseLinks(
                                             "http://people.apache.org/repo/m2-ibiblio-rsync-repository/org/apache/maven/archiva/archiva-plexus-runtime/1.0.1/",
                                             "org.apache.maven.html" );

        String[] expected = new String[] {
            "archiva-plexus-runtime-1.0.1-bin.tar.gz",
            "archiva-plexus-runtime-1.0.1-bin.tar.gz.asc",
            "archiva-plexus-runtime-1.0.1-bin.tar.gz.asc.md5",
            "archiva-plexus-runtime-1.0.1-bin.tar.gz.asc.sha1",
            "archiva-plexus-runtime-1.0.1-bin.tar.gz.md5",
            "archiva-plexus-runtime-1.0.1-bin.tar.gz.sha1",
            "archiva-plexus-runtime-1.0.1-bin.zip",
            "archiva-plexus-runtime-1.0.1-bin.zip.asc",
            "archiva-plexus-runtime-1.0.1-bin.zip.asc.md5",
            "archiva-plexus-runtime-1.0.1-bin.zip.asc.sha1",
            "archiva-plexus-runtime-1.0.1-bin.zip.md5",
            "archiva-plexus-runtime-1.0.1-bin.zip.sha1",
            "archiva-plexus-runtime-1.0.1-sources.jar",
            "archiva-plexus-runtime-1.0.1-sources.jar.asc",
            "archiva-plexus-runtime-1.0.1-sources.jar.asc.md5",
            "archiva-plexus-runtime-1.0.1-sources.jar.asc.sha1",
            "archiva-plexus-runtime-1.0.1-sources.jar.md5",
            "archiva-plexus-runtime-1.0.1-sources.jar.sha1",
            "archiva-plexus-runtime-1.0.1.jar",
            "archiva-plexus-runtime-1.0.1.jar.asc",
            "archiva-plexus-runtime-1.0.1.jar.asc.md5",
            "archiva-plexus-runtime-1.0.1.jar.asc.sha1",
            "archiva-plexus-runtime-1.0.1.jar.md5",
            "archiva-plexus-runtime-1.0.1.jar.sha1",
            "archiva-plexus-runtime-1.0.1.pom",
            "archiva-plexus-runtime-1.0.1.pom.asc",
            "archiva-plexus-runtime-1.0.1.pom.asc.md5",
            "archiva-plexus-runtime-1.0.1.pom.asc.sha1",
            "archiva-plexus-runtime-1.0.1.pom.md5",
            "archiva-plexus-runtime-1.0.1.pom.sha1",
            "readme artifacts.txt"};

        assertContainsExpected( links, expected );

        String[] avoided = new String[] {
            "/repo/m2-ibiblio-rsync-repository/org/apache/maven/archiva/archiva-plexus-runtime/",
            "?C=S;O=A",
            "?C=D;O=A",
            "?C=M;O=A" };

        assertNotContainingAvoided( links, avoided );
    }
}
