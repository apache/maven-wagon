package org.apache.maven.wagon.observers;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.repository.Repository;

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

            String url = "file://" + System.getProperty( "basedir" )
                    + "/target/test-classes/repository";

            Repository repository = new Repository( );
            
            repository.setUrl( url );

            wagon.connect( repository );
            
            File temp = new File( System.getProperty( "basedir" ), "target/test-output" );
            
            temp.mkdirs();
            
            File dest = new File ( temp, "maven-test-a-1.0.jar" );
                        
            wagon.addTransferListener( observer );
                                   
            wagon.get( "maven-test-a-1.0.jar", dest );
            
            File destMd5 = new File ( temp, "maven-test-a-1.0.jar.md5" );
            
            assertTrue( destMd5.exists()  );
            
            assertEquals(  observer.getExpectedChecksum(), observer.getActualChecksum() );
            
            assertTrue( observer.cheksumIsValid()  );
            
            wagon.get( "maven-test-b-1.0.jar", dest );
            
            assertFalse( observer.cheksumIsValid()  );
            
        }       
        catch ( Exception e )
        {            
            e.printStackTrace();
            
            fail( e.getMessage() );
        }
        

    }

}