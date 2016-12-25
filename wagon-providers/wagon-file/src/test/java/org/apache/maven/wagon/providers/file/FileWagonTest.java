package org.apache.maven.wagon.providers.file;

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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class FileWagonTest
    extends StreamingWagonTestCase
{
    protected String getProtocol()
    {
        return "file";
    }

    protected String getTestRepositoryUrl()
        throws IOException
    {
        File file = FileTestUtils.createUniqueDir( getName() + ".file-repository." );

        return "file://" + file.getPath();
    }

    @Override
    protected int getTestRepositoryPort() {
        // file transfer dont needs a port
        return 0;
    }

    /**
     * This test is introduced to allow for null file wagons.
     * Which is used heavily in the maven component ITs.
     *
     * @throws ConnectionException
     * @throws AuthenticationException
     */
    public void testNullFileWagon() throws ConnectionException, AuthenticationException
    {
        Wagon wagon = new FileWagon();
        Resource resource = new Resource();
        resource.setContentLength( 100000 );
        Repository repository = new Repository();
        wagon.connect( repository );
        wagon.disconnect();
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        return new File( repository.getBasedir(), resource.getName() ).lastModified();
    }

    public void testResourceExists()
        throws Exception
    {
        String url = "file://" + getBasedir();

        Wagon wagon = new FileWagon();
        Repository repository = new Repository( "someID", url );
        wagon.connect( repository );

        assertTrue( wagon.resourceExists( "target" ) );
        assertTrue( wagon.resourceExists( "target/" ) );
        assertTrue( wagon.resourceExists( "pom.xml" ) );

        assertFalse( wagon.resourceExists( "pom.xml/" ) );

        wagon.disconnect();
    }
}
