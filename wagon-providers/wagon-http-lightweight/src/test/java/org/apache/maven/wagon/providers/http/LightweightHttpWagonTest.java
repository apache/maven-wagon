package org.apache.maven.wagon.providers.http;

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

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.WagonTestCase;
import org.codehaus.plexus.jetty.Httpd;

import java.io.File;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class LightweightHttpWagonTest
    extends WagonTestCase
{
    private Httpd httpd;

    protected String getProtocol()
    {
        return "http";
    }

    protected String getTestRepositoryUrl()
    {
        return "http://localhost:10007";
    }

    protected void setupWagonTestingFixtures()
        throws Exception
    {
        // File round trip testing
        
        File file = FileTestUtils.createUniqueFile( "local-repository", "test-resource" );

        file.delete();

        file.getParentFile().mkdirs();

        File f = new File( FileTestUtils.createDir( "http-repository" ), "test-resource" );

        f.delete();

        f.getParentFile().mkdirs();

        httpd = (Httpd) lookup( Httpd.ROLE );
    }
    
    protected void tearDownWagonTestingFixtures()
        throws Exception
    {
        release( httpd );
    }

    public void testWagonGetFileList()
        throws Exception
    {
        File f = new File( FileTestUtils.createDir( "http-repository" ), "file-list" );
        f.mkdirs();
        
        super.testWagonGetFileList();
    }
}
