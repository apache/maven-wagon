package org.apache.maven.wagon.manager;

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

import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.manager.stats.TransferStatistics;
import org.apache.maven.wagon.manager.stats.WagonStatistics;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.webdav.DavServerManager;
import org.codehaus.plexus.webdav.servlet.basic.BasicWebDavServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * WagonManagerTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class WagonManagerTest
    extends PlexusTestCase
{
    private static final String DAV_CONTEXT = "/dav";

    private static final int DAV_PORT = 51119;

    private static final int HTTP_PORT = 51117;

    private File httpRootDir;

    private File davRootDir;

    private File localDownloadDir;

    private WagonManager wagonManager;

    private Server httpServer;

    private Server davServer;

    private File testDataDir;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        httpRootDir = setupDir( "/target/test-repos/http-test-server/" );
        davRootDir = setupDir( "/target/test-repos/dav-test-server/" );
        localDownloadDir = setupDir( "/target/test-repos/downloads/" );

        System.setProperty( "DEBUG", "" );
        System.setProperty( "org.mortbay.log.class", "org.slf4j.impl.SimpleLogger" );

        httpServer = createHttpServer();
        davServer = createDavServer();

        wagonManager = (WagonManager) lookup( WagonManager.ROLE, "default" );

        testDataDir = new File( getBasedir(), "src/test/data" );
    }

    private Server createHttpServer()
        throws Exception
    {
        // Setup the Jetty Server.

        Server server = new Server( HTTP_PORT );
        Context rootJettyContext = new Context( server, "/", Context.SESSIONS );

        rootJettyContext.setContextPath( "/" );
        rootJettyContext.setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );

        ServletHandler servletHandler = rootJettyContext.getServletHandler();

        ServletHolder holder = servletHandler.addServletWithMapping( DefaultServlet.class, "/*" );

        holder.setInitParameter( "resourceBase", httpRootDir.getAbsolutePath() );
        holder.setInitParameter( "dirAllowed", "true" );

        server.start();

        return server;
    }

    private Server createDavServer()
        throws Exception
    {
        /* kick start the dav management on the right foot */
        DavServerManager manager = (DavServerManager) lookup( DavServerManager.ROLE, "simple" );
        assertNotNull( manager );

        /* create the jetty server */
        Server server = new Server( DAV_PORT );
        Context rootJettyContext = new Context( server, "/", Context.SESSIONS );

        rootJettyContext.setContextPath( "/" );
        rootJettyContext.setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );

        ServletHandler servletHandler = rootJettyContext.getServletHandler();

        ServletHolder holder = servletHandler.addServletWithMapping( BasicWebDavServlet.class, DAV_CONTEXT + "/*" );

        holder.setInitParameter( "dav.root", davRootDir.getAbsolutePath() );

        server.start();

        return server;
    }

    private File setupDir( String rootPath )
        throws IOException
    {
        File rootDir = new File( getBasedir(), rootPath );

        // Clean up from old tests.
        if ( rootDir.exists() )
        {
            FileUtils.deleteDirectory( rootDir );
        }

        // Create dir
        rootDir.mkdirs();

        return rootDir;
    }

    protected void tearDown()
        throws Exception
    {
        release( wagonManager );

        if ( httpServer != null )
        {
            try
            {
                httpServer.stop();
            }
            catch ( Exception e )
            {
                /* do nothing */
            }
        }

        if ( davServer != null )
        {
            try
            {
                davServer.stop();
            }
            catch ( Exception e )
            {
                /* do nothing */
            }
        }

        super.tearDown();
    }

    private void assertExistInHttpServer( String path )
    {
        File file = new File( httpRootDir, path );
        if ( !file.exists() )
        {
            fail( "Missing expected file in http server home [" + file.getAbsolutePath() + "]" );
        }
    }

    private void assertExistInDavServer( String path )
    {
        File file = new File( davRootDir, path );
        if ( !file.exists() )
        {
            fail( "Missing expected file in dav server home [" + file.getAbsolutePath() + "]" );
        }
    }

    public void testSupportedProtocols()
        throws MalformedURLException
    {
        Set protocols = wagonManager.getProtocols();
        assertNotNull( protocols );

        String expectedProtocols[] = new String[] { "file", "ftp", "http", "https", "scp", "scpexe", "sftp", "dav" };
        
        assertEquals( "Supported Protocols : " + protocols, expectedProtocols.length, protocols.size() );
    }

    public void testRepositoryPut()
        throws Exception
    {
        Repository testRepo = createTestRepository();
        Repository davRepo = createDavRepository();

        wagonManager.addRepository( testRepo );
        wagonManager.addRepository( davRepo );

        Wagon davWagon = wagonManager.getWagon( "davrepo" );
        Wagon httpWagon = wagonManager.getWagon( "testrepo" );

        assertNotNull( davWagon );
        assertNotNull( httpWagon );

        try
        {
            davWagon.connect();

            String destFilename = "daytrader-streamer-2.0-SNAPSHOT.jar";
            String destPath = "org/apache/geronimo/daytrader-streamer/2.0-SNAPSHOT/" + destFilename;

            davWagon.put( new File( testDataDir, destFilename ), destPath );

            assertExistInDavServer( destPath );
        }
        finally
        {
            davWagon.disconnect();
            wagonManager.releaseWagon( davWagon );
        }

        try
        {
            httpWagon.connect();

            String destFilename = "daytrader-streamer-2.0-SNAPSHOT.jar";
            String destPath = "org/apache/geronimo/daytrader-streamer/2.0-SNAPSHOT/" + destFilename;

            httpWagon.put( new File( testDataDir, destFilename ), destPath );
            fail( "Should not have been able to perform a PUT on the http repository." );
        }
        catch ( TransferFailedException e )
        {
            /* expected path */
        }
        finally
        {
            httpWagon.disconnect();
            wagonManager.releaseWagon( httpWagon );
        }
    }

    public void testStatistics()
        throws Exception
    {
        Repository testRepo = createTestRepository();
        Repository davRepo = createDavRepository();

        wagonManager.addRepository( testRepo );
        wagonManager.addRepository( davRepo );

        Wagon davWagon = wagonManager.getWagon( "davrepo" );
        Wagon httpWagon = wagonManager.getWagon( "testrepo" );

        assertNotNull( davWagon );
        assertNotNull( httpWagon );

        // Put create some put requests on the dav protocol

        try
        {
            davWagon.connect();

            String destFilename = "daytrader-streamer-2.0-SNAPSHOT.jar";
            String destPath = "org/apache/geronimo/daytrader-streamer/2.0-SNAPSHOT/" + destFilename;

            davWagon.put( new File( testDataDir, destFilename ), destPath );
            assertExistInDavServer( destPath );

            File destFile = new File( localDownloadDir, destFilename );

            davWagon.get( destPath, destFile );
        }
        finally
        {
            davWagon.disconnect();
            wagonManager.releaseWagon( davWagon );
        }

        // Lets create some get requests on the http protocol.

        FileUtils.copyDirectory( testDataDir, httpRootDir );

        try
        {
            httpWagon.connect();

            String filenames[] = new String[] {
                "daytrader-streamer-2.0-SNAPSHOT-javadoc.jar",
                "daytrader-streamer-2.0-SNAPSHOT-sources.jar",
                "daytrader-streamer-2.0-SNAPSHOT.pom" };

            for ( int i = 0; i < filenames.length; i++ )
            {
                httpWagon.get( filenames[i], new File( localDownloadDir, filenames[i] ) );
            }
        }
        finally
        {
            httpWagon.disconnect();
            wagonManager.releaseWagon( httpWagon );
        }

        // Now lets get the statistics.

        WagonStatistics wagonStats = wagonManager.getStatistics();
        assertNotNull( "Wagon Statistics should not be null.", wagonStats );
        
        TransferStatistics totals = wagonStats.getTotalTransferStatistics();
        assertNotNull( "Total Transfer Statistics should not be null.", totals );
        
        wagonStats.dump();

        assertEquals( 4, totals.getCountResourcesFetched() );
        assertEquals( 1, totals.getCountResourcesSent() );
        assertEquals( 5, totals.getCountResourcesTransferred() );
        assertEquals( 234605, totals.getBytesFetched() );
        assertEquals( 38056, totals.getBytesSent() );
        assertEquals( 272661, totals.getBytesTransferred() );
    }

    /**
     * Tests #connect and #get on a Mirror.
     */
    public void testMirroredWagon() throws Exception
	{
        final Repository testRepo = createTestRepository();
        wagonManager.addRepository( testRepo );
        wagonManager.addRepositoryMirror( "testmirrorrepo", "testrepo", "http://localhost:" + HTTP_PORT + "/" );
        final Wagon httpWagon = wagonManager.getWagon( "testmirrorrepo" );
        
        try {
            httpWagon.connect();
            
            FileUtils.copyDirectory( testDataDir, httpRootDir );
            
            final String destFilename = "daytrader-streamer-2.0-SNAPSHOT-javadoc.jar";
            httpWagon.get( destFilename, new File( localDownloadDir, destFilename ));
        } finally {
            httpWagon.disconnect();
            wagonManager.releaseWagon( httpWagon );
        }
	}
    
    private Repository createDavRepository()
    {
        Repository repo = new Repository( "davrepo", "dav:http://localhost:" + DAV_PORT + DAV_CONTEXT + "/" );
        return repo;
    }

    private Repository createTestRepository()
    {
        Repository repo = new Repository( "testrepo", "http://localhost:" + HTTP_PORT + "/" );
        return repo;
    }
}
