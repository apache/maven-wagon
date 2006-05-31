package org.apache.maven.wagon.providers.webdav;

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

import java.io.File;
import java.io.IOException;

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.WagonTestCase;

/**
 * WebDAV Wagon Test 
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class WebDavWagonTest
    extends WagonTestCase
{
    private ServletServer server;

    protected String getTestRepositoryUrl()
        throws IOException
    {
        return "dav:http://localhost:10007/dav/newfolder/folder2";
    }

    protected String getProtocol()
    {
        return "dav";
    }

    protected void setupWagonTestingFixtures()
        throws Exception
    {
        if ( System.getProperty( "basedir" ) == null )
        {
            fail( "System property 'basedir' must be set for the web server to run properly" );
        }

        File file = FileTestUtils.createUniqueFile( "dav-repository", "test-resource" );

        file.delete();

        File davDir = file.getParentFile();
        davDir.mkdirs();

        server = (ServletServer) lookup( ServletServer.ROLE );
    }

    protected void tearDownWagonTestingFixtures()
        throws Exception
    {
        release( server );
    }
}
