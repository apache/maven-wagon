package org.apache.maven.wagon.providers.ftp;

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

import java.io.File;

import org.apache.ftpserver.interfaces.FtpServerInterface;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class FtpWagonTest
    extends StreamingWagonTestCase
{
    private FtpServerInterface server;

    protected String getProtocol()
    {
        return "ftp";
    }

    protected void setupWagonTestingFixtures()
        throws Exception
    {
        server = (FtpServerInterface) lookup( FtpServerInterface.ROLE );
    }

    protected void createDirectory( Wagon wagon, String resourceToCreate, String dirName )
        throws Exception
    {
        super.createDirectory( wagon, resourceToCreate, dirName );

        getRepositoryDirectory().mkdirs();
    }

    protected void tearDownWagonTestingFixtures()
        throws Exception
    {
        release( server );
    }

    protected String getTestRepositoryUrl()
    {
        return "ftp://localhost:10023";
    }

    public AuthenticationInfo getAuthInfo()
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();

        authInfo.setUserName( "admin" );

        authInfo.setPassword( "admin" );

        return authInfo;
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        File file = new File( getRepositoryDirectory(), resource.getName() );

        // granularity for FTP is minutes
        return ( file.lastModified() / 60000 ) * 60000;
    }

    private File getRepositoryDirectory()
    {
        return getTestFile( "target/test-output/local-repository" );
    }

    public void testNoPassword()
        throws Exception
    {
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName( "me" );
        try
        {
            getWagon().connect( new Repository( "id", getTestRepositoryUrl() ), authenticationInfo );
        }
        catch ( AuthenticationException e )
        {
            assertTrue( true );
        }
    }

    public void testDefaultUserName()
        throws Exception
    {
        setupWagonTestingFixtures();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setPassword( "secret" );
        try
        {
            getWagon().connect( new Repository( "id", getTestRepositoryUrl() ), authenticationInfo );
            fail();
        }
        catch ( AuthenticationException e )
        {
            assertEquals( System.getProperty( "user.name" ), authenticationInfo.getUserName() );
        }

        tearDownWagonTestingFixtures();
    }
}
