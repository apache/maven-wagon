package org.apache.maven.wagon.observers;

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

import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.FileTestUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka </a>
 * @version $Id$
 */
public class ChecksumObserverTest extends TestCase
{

    public void testMd5SumObserver()
    {
        try
        {
            FileWagon wagon = new FileWagon();

            ChecksumObserver observer = new ChecksumObserver( );

            String repositoryPath = FileTestUtils.createUniqueDir( this ).getPath();

            FileTestUtils.generateFile( repositoryPath + "/maven-test-a-1.0.jar" ,  "michal maczka" );

            FileTestUtils.generateFile( repositoryPath + "/maven-test-a-1.0.jar.md5" , "2b5ade4335f0b3babdf4257b8ecec500"  );

            FileTestUtils.generateFile( repositoryPath + "/maven-test-b-1.0.jar" , "wagon wagon" );

            String repositoryUrl = "file://" + repositoryPath;

            Repository repository = new Repository( );
            
            repository.setUrl( repositoryUrl );

            wagon.connect( repository );
            
            File dest = FileTestUtils.createUniqueDir( this  );
            
            dest.mkdirs();

            System.out.println( "Dir: "  + dest );

            File a = new File ( dest, "maven-test-a-1.0.jar" );

            File aMd5 = new File ( dest, "maven-test-a-1.0.jar.md5" );

            File b = new File ( dest, "maven-test-b-1.0.jar" );

            File bMd5 = new File ( dest, "maven-test-b-1.0.jar" );

            wagon.addTransferListener( observer );
                                   
            wagon.get( "maven-test-a-1.0.jar", a );

            assertTrue( a.exists()  );

            assertTrue( aMd5.exists()  );
            
            assertEquals(  observer.getExpectedChecksum(), observer.getActualChecksum() );
            
            assertTrue( observer.cheksumIsValid()  );
            
            wagon.get( "maven-test-b-1.0.jar", b );
            
            assertFalse( observer.cheksumIsValid()  );

            assertTrue( bMd5.exists()  );


            // now try to put it to repository

            wagon.put( a, "maven-test-c-1.0.jar"  );

            File c = new File ( repositoryPath, "maven-test-b-1.0.jar" );

            File cMd5 = new File ( repositoryPath, "maven-test-c-1.0.jar.md5" );

            assertTrue( c.exists() );

            assertTrue( cMd5.exists() );

            assertEquals( "2b5ade4335f0b3babdf4257b8ecec500", FileUtils.fileRead( cMd5 )  );



        }
        catch ( Exception e )
        {            
            e.printStackTrace();
            
            fail( e.getMessage() );
        }
        

    }

}