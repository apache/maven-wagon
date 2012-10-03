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

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class FtpWagonTest
    extends StreamingWagonTestCase
{
    static private FtpServer server;

    protected String getProtocol()
    {
        return "ftp";
    }

    @Override
    protected int getTestRepositoryPort() {
        return 10023;
    }

    protected void setupWagonTestingFixtures()
        throws Exception
    {
        File ftpHomeDir = getRepositoryDirectory();
        if ( !ftpHomeDir.exists() )
        {
            ftpHomeDir.mkdirs();
        }

        if (server == null)
        {
            FtpServerFactory serverFactory = new FtpServerFactory();

            ListenerFactory factory = new ListenerFactory();

            // set the port of the listener
            factory.setPort(getTestRepositoryPort());

            // replace the default listener
            serverFactory.addListener("default", factory.createListener());

            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            UserManager um = userManagerFactory.createUserManager();

            BaseUser user = new BaseUser();
            user.setName("admin");
            user.setPassword("admin");

            List<Authority> authorities = new ArrayList<Authority>();
            authorities.add( new WritePermission() );

            user.setAuthorities( authorities );

            user.setHomeDirectory( ftpHomeDir.getAbsolutePath() );


            um.save(user);

            serverFactory.setUserManager( um );

            server = serverFactory.createServer();

            // start the server
            server.start();

        }
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
        server.stop();
        server = null;
    }

    protected String getTestRepositoryUrl()
    {
        return "ftp://localhost:" + getTestRepositoryPort();
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
            fail();
        }
        catch ( AuthenticationException e )
        {
            assertTrue( true );
        }
    }

    public void testDefaultUserName()
        throws Exception
    {
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
    }

    /**
     * This is a unit test to show WAGON-265
     */
    public void testPutDirectoryCreation()
        throws Exception
    {
        setupRepositories();

        setupWagonTestingFixtures();

        Wagon wagon = getWagon();

        if ( wagon.supportsDirectoryCopy() )
        {
            // do the cleanup first
            File destDir = new File( getRepositoryDirectory(), "dirExists" );
            FileUtils.deleteDirectory(destDir);
            destDir.mkdirs();
            destDir = new File( destDir, "not_yet_existing/also_not" );

            File sourceDir = new File( getRepositoryDirectory(), "testDirectory" );

            FileUtils.deleteDirectory(sourceDir);
            sourceDir.mkdir();

            File testRes = new File( sourceDir, "test-resource-1.txt" );
            testRes.createNewFile();

            // This is the difference to our normal use case:
            // the directory specified in the repo string doesn't yet exist!

            testRepository.setUrl( testRepository.getUrl() + "/dirExists/not_yet_existing/also_not" );

            wagon.connect( testRepository, getAuthInfo() );

            wagon.putDirectory( sourceDir, "testDirectory" );

            destFile = FileTestUtils.createUniqueFile(getName(), getName());

            destFile.deleteOnExit();

            wagon.get( "testDirectory/test-resource-1.txt", destFile );

            wagon.disconnect();
        }

        tearDownWagonTestingFixtures();


    }
}
