package g.apache.maven.wagon.providers.http;

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

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.jetty.Httpd;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class LightweightHttpWagonTest
    extends PlexusTestCase
{
    private Wagon wagon;

    public LightweightHttpWagonTest( String testName )
    {
        super( testName );       
    }


    protected void setUp()
        throws Exception
    {
        super.setUp();
        wagon = ( Wagon ) lookup( Wagon.ROLE, "http" );
    }

    public void testHttpWagon()
    {

        try
        {
            Repository repository = new Repository( "test", "http://www.ibiblio.org/maven" );

            wagon.connect( repository );

            File file = File.createTempFile( "maven-http-wagon", ".test" );

            file.deleteOnExit();

            wagon.get( "maven/poms/maven-1.0.pom", file );

            assertTrue( file.exists()  );

            assertTrue( file.length() > 0  );
        }
        catch ( Exception e )
        {

            e.printStackTrace();

            fail( e.getMessage() );
        }


    }


}
