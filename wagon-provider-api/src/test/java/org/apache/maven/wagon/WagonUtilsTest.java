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
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class WagonUtilsTest extends TestCase
{

    public void testPutDirectory()
    {

        try
        {
            File inputDir = FileTestUtils.createUniqueDir( "inputDir" );

            File  dirA = new File( inputDir, "a" );

            dirA.mkdirs();

            File fileA = new File( dirA, "a.txt" );

            FileUtils.fileWrite( fileA.getPath(), "a" );

            File  dirAA = new File( dirA, "aa" );

            dirAA.mkdir();

            File fileAA = new File( dirAA, "aa.txt" );

            FileUtils.fileWrite( fileAA.getPath(), "aa" );

            File  dirB = new File( inputDir, "b" );

            dirB.mkdir();

            File fileB = new File( dirB, "b.txt" );

            FileUtils.fileWrite( fileB.getPath(), "b" );

            File outputDir = FileTestUtils.createUniqueDir( "outputDir" );

            Wagon wagon = new FileWagon();

            Debug debug = new Debug();

            wagon.addTransferListener( debug );

            wagon.addSessionListener( debug );

            Repository repository = new Repository();

            repository.setUrl( "file://" +outputDir );

            wagon.connect( repository );

            WagonUtils.putDirectory( inputDir, wagon,  false );

            List inputFiles = FileUtils.getFiles( inputDir, "**/*", null, false );

            List outputFiles = FileUtils.getFiles( outputDir, "**/*", null, false );

            assertEquals( inputFiles.size(), outputFiles.size() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            fail( e.getMessage() );
        }


    }



}
