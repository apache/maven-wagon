package org.apache.maven.wagon.http;

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

import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public abstract class HttpWagonTestCase
    extends StreamingWagonTestCase
{
    private Server server;

    protected void setupWagonTestingFixtures()
        throws Exception
    {
        // File round trip testing

        File file = FileTestUtils.createUniqueFile( "local-repository", "test-resource" );

        file.delete();

        file.getParentFile().mkdirs();

        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        server = new Server( 0 );

        PutHandler putHandler = new PutHandler( repositoryDirectory );
        server.addHandler( putHandler );

        createContext( server, repositoryDirectory );

        addConnectors( server );

        server.start();

        testRepository.setUrl( getTestRepositoryUrl() );
    }

    @Override
    protected final int getTestRepositoryPort()
    {
        if ( server == null )
        {
            return 0;
        }
        return server.getConnectors()[0].getLocalPort();
    }

    protected void createContext( Server server, File repositoryDirectory )
        throws IOException
    {
        Context root = new Context( server, "/", Context.SESSIONS );
        root.setResourceBase( repositoryDirectory.getAbsolutePath() );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        root.addServlet( servletHolder, "/*" );
    }

    protected void tearDownWagonTestingFixtures()
        throws Exception
    {
        server.stop();
    }

    public void testWagonGetFileList()
        throws Exception
    {
        File dir = getRepositoryDirectory();
        FileUtils.deleteDirectory( dir );

        File f = new File( dir, "file-list" );
        f.mkdirs();

        super.testWagonGetFileList();
    }

    public void testHttpHeaders()
        throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( "User-Agent", "Maven-Wagon/1.0" );

        StreamingWagon wagon = (StreamingWagon) getWagon();

        setHttpHeaders( wagon, properties );

        Server server = new Server( 0 );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        addConnectors( server );
        server.start();

        wagon.connect(
            new Repository( "id", getProtocol() + "://localhost:" + server.getConnectors()[0].getLocalPort() ) );

        wagon.getToStream( "resource", new ByteArrayOutputStream() );

        wagon.disconnect();

        server.stop();

        assertEquals( "Maven-Wagon/1.0", handler.headers.get( "User-Agent" ) );
    }

    /**
     * test set of User-Agent as it's done by aether wagon connector with using setHttpHeaders
     */
    public void testHttpHeadersWithCommonMethods()
        throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( "User-Agent", "Maven-Wagon/1.0" );

        StreamingWagon wagon = (StreamingWagon) getWagon();

        Method setHttpHeaders = wagon.getClass().getMethod( "setHttpHeaders", Properties.class );
        setHttpHeaders.invoke( wagon, properties );

        Server server = new Server( 0 );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        addConnectors( server );
        server.start();

        wagon.connect(
            new Repository( "id", getProtocol() + "://localhost:" + server.getConnectors()[0].getLocalPort() ) );

        wagon.getToStream( "resource", new ByteArrayOutputStream() );

        wagon.disconnect();

        server.stop();

        assertEquals( "Maven-Wagon/1.0", handler.headers.get( "User-Agent" ) );
    }

    protected abstract void setHttpHeaders( StreamingWagon wagon, Properties properties );

    protected void addConnectors( Server server )
    {
    }

    protected String getRepositoryUrl( Server server )
    {
        int localPort = server.getConnectors()[0].getLocalPort();
        return getProtocol() + "://localhost:" + localPort;
    }

    public void testGetForbidden()
        throws Exception
    {
        try
        {
            runTestGet( HttpServletResponse.SC_FORBIDDEN );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testGet404()
        throws Exception
    {
        try
        {
            runTestGet( HttpServletResponse.SC_NOT_FOUND );
            fail();
        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
    }

    public void testList429()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();
        try
        {

            Server server = new Server( 0 );
            final AtomicBoolean called = new AtomicBoolean();

            AbstractHandler handler = new AbstractHandler()
            {
                public void handle( String s, HttpServletRequest request, HttpServletResponse response, int i )
                    throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( 200 );
                        ( (Request) request ).setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( 429 );
                        ( (Request) request ).setHandled( true );

                    }
                }
            };

            server.setHandler( handler );
            addConnectors( server );
            server.start();

            wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

            try
            {
                wagon.getFileList( "resource" );
            }
            finally
            {
                wagon.disconnect();

                server.stop();
            }

        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
        catch ( TransferFailedException e )
        {
            if ( wagon.getClass().getName().contains( "Lightweight" ) )
            {
                //we don't care about lightweight
                assertTrue( true );
            }
            else
            {
                fail();
            }

        }
    }

    public void testGet500()
        throws Exception
    {
        try
        {
            runTestGet( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }

    private void runTestGet( int status )
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server server = new Server( 0 );
        StatusHandler handler = new StatusHandler();
        handler.setStatusToReturn( status );
        server.setHandler( handler );
        addConnectors( server );
        server.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

        try
        {
            wagon.getToStream( "resource", new ByteArrayOutputStream() );
            fail();
        }
        finally
        {
            wagon.disconnect();

            server.stop();
        }
    }

    public void testResourceExistsForbidden()
        throws Exception
    {
        try
        {
            runTestResourceExists( HttpServletResponse.SC_FORBIDDEN );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testResourceExists404()
        throws Exception
    {
        try
        {
            assertFalse( runTestResourceExists( HttpServletResponse.SC_NOT_FOUND ) );
        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
    }

    public void testResourceExists500()
        throws Exception
    {
        try
        {
            runTestResourceExists( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }

    public void testResourceExists429()
        throws Exception
    {
        try
        {

            final AtomicBoolean called = new AtomicBoolean();

            AbstractHandler handler = new AbstractHandler()
            {
                public void handle( String s, HttpServletRequest request, HttpServletResponse response, int i )
                    throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                        ( (Request) request ).setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( 429 );
                        ( (Request) request ).setHandled( true );
                    }
                }
            };

            StreamingWagon wagon = (StreamingWagon) getWagon();
            Server server = new Server( 0 );
            server.setHandler( handler );
            addConnectors( server );
            server.start();
            wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

            try
            {
                wagon.resourceExists( "resource" );
            }
            finally
            {
                wagon.disconnect();

                server.stop();
            }

            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }


    private boolean runTestResourceExists( int status )
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server server = new Server( 0 );
        StatusHandler handler = new StatusHandler();
        handler.setStatusToReturn( status );
        server.setHandler( handler );
        addConnectors( server );
        server.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

        try
        {
            return wagon.resourceExists( "resource" );
        }
        finally
        {
            wagon.disconnect();

            server.stop();
        }
    }

    protected long getExpectedLastModifiedOnGet( Repository repository, Resource resource )
    {
        File file = new File( getRepositoryDirectory(), resource.getName() );
        return ( file.lastModified() / 1000 ) * 1000;
    }

    protected File getRepositoryDirectory()
    {
        return getTestFile( "target/test-output/http-repository" );
    }

    public void testGzipGet()
        throws Exception
    {
        Server server = new Server( getTestRepositoryPort() );

        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Context root = new Context( server, "/", Context.SESSIONS );
        root.setResourceBase( localRepositoryPath );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        servletHolder.setInitParameter( "gzip", "true" );
        root.addServlet( servletHolder, "/*" );
        addConnectors( server );
        server.start();

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            File sourceFile = new File( localRepositoryPath + "/gzip" );

            sourceFile.deleteOnExit();

            String resName = "gzip-res.txt";
            String sourceContent = writeTestFileGzip( sourceFile, resName );

            wagon.connect( testRepository );

            File destFile = FileTestUtils.createUniqueFile( getName(), getName() );

            destFile.deleteOnExit();

            wagon.get( "gzip/" + resName, destFile );

            wagon.disconnect();

            String destContent = FileUtils.fileRead( destFile );

            assertEquals( sourceContent, destContent );
        }
        finally
        {
            server.stop();
        }
    }

    public void testProxiedRequest()
        throws Exception
    {
        ProxyInfo proxyInfo = createProxyInfo();
        TestHeaderHandler handler = new TestHeaderHandler();

        runTestProxiedRequest( proxyInfo, handler );
    }

    public void testProxiedRequestWithAuthentication()
        throws Exception
    {
        ProxyInfo proxyInfo = createProxyInfo();
        proxyInfo.setUserName( "user" );
        proxyInfo.setPassword( "secret" );
        AuthorizingProxyHandler handler = new AuthorizingProxyHandler();

        runTestProxiedRequest( proxyInfo, handler );

        assertTrue( handler.headers.containsKey( "Proxy-Authorization" ) );

        if ( supportProxyPreemptiveAuthentication() )
        {
            assertEquals( 200, handler.handlerRequestResponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( 407, handler.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( 200, handler.handlerRequestResponses.get( 1 ).responseCode );
        }

    }

    public void testRedirectGetToStream()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server server = new Server( 0 );
        TestHeaderHandler handler = new TestHeaderHandler();

        server.setHandler( handler );
        addConnectors( server );
        server.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        String protocol = getProtocol();

        // protocol is wagon protocol but in fact dav is http(s)
        if ( protocol.equals( "dav" ) )
        {
            protocol = "http";
        }

        if ( protocol.equals( "davs" ) )
        {
            protocol = "https";
        }

        String redirectUrl = protocol + "://localhost:" + server.getConnectors()[0].getLocalPort();

        RedirectHandler redirectHandler = new RedirectHandler( "Found", 303, redirectUrl, null );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( redirectServer ) ) );

        File tmpResult = File.createTempFile( "foo", "get" );

        FileOutputStream fileOutputStream = new FileOutputStream( tmpResult );

        try
        {
            wagon.getToStream( "resource", fileOutputStream );
            fileOutputStream.flush();
            fileOutputStream.close();
            String found = FileUtils.fileRead( tmpResult );
            assertEquals( "found:'" + found + "'", "Hello, World!", found );

            assertEquals( 1, handler.handlerRequestResponses.size() );
            assertEquals( 200, handler.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( 1, redirectHandler.handlerRequestResponses.size() );
            assertEquals( 302, redirectHandler.handlerRequestResponses.get( 0 ).responseCode );
        }
        finally
        {
            wagon.disconnect();

            server.stop();

            tmpResult.delete();
        }
    }

    public void testRedirectGet()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server server = new Server( 0 );
        TestHeaderHandler handler = new TestHeaderHandler();

        server.setHandler( handler );
        addConnectors( server );
        server.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        String protocol = getProtocol();

        // protocol is wagon protocol but in fact dav is http(s)
        if ( protocol.equals( "dav" ) )
        {
            protocol = "http";
        }

        if ( protocol.equals( "davs" ) )
        {
            protocol = "https";
        }

        String redirectUrl = protocol + "://localhost:" + server.getConnectors()[0].getLocalPort();

        RedirectHandler redirectHandler = new RedirectHandler( "Found", 303, redirectUrl, null );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( redirectServer ) ) );

        File tmpResult = File.createTempFile( "foo", "get" );

        try
        {
            wagon.get( "resource", tmpResult );
            String found = FileUtils.fileRead( tmpResult );
            assertEquals( "found:'" + found + "'", "Hello, World!", found );

            assertEquals( 1, handler.handlerRequestResponses.size() );
            assertEquals( 200, handler.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( 1, redirectHandler.handlerRequestResponses.size() );
            assertEquals( 302, redirectHandler.handlerRequestResponses.get( 0 ).responseCode );
        }
        finally
        {
            wagon.disconnect();

            server.stop();

            tmpResult.delete();
        }
    }


    public void testRedirectPutFromStreamWithFullUrl()
        throws Exception
    {
        Server realServer = new Server( 0 );

        addConnectors( realServer );

        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        String protocol = getProtocol();

        // protocol is wagon protocol but in fact dav is http(s)
        if ( protocol.equals( "dav" ) )
        {
            protocol = "http";
        }

        if ( protocol.equals( "davs" ) )
        {
            protocol = "https";
        }

        String redirectUrl = protocol + "://localhost:" + realServer.getConnectors()[0].getLocalPort();

        RedirectHandler redirectHandler = new RedirectHandler( "Found", 303, redirectUrl, repositoryDirectory );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();
            Repository repository = new Repository( "foo", getRepositoryUrl( redirectServer ) );
            wagon.connect( repository );

            File sourceFile = new File( repositoryDirectory, "test-secured-put-resource" );
            sourceFile.delete();
            assertFalse( sourceFile.exists() );

            File tempFile = File.createTempFile( "wagon", "tmp" );
            tempFile.deleteOnExit();
            String content = "put top secret";
            FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

            FileInputStream fileInputStream = new FileInputStream( tempFile );
            try
            {
                wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
            }
            finally
            {
                fileInputStream.close();
                tempFile.delete();

            }

            assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

            checkRequestResponseForRedirectPutFromStreamWithFullUrl( putHandler, redirectHandler );
        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }

    protected void checkRequestResponseForRedirectPutFromStreamWithFullUrl( PutHandler putHandler,
                                                                            RedirectHandler redirectHandler )
    {
        assertEquals( "found:" + putHandler.handlerRequestResponses, 1, putHandler.handlerRequestResponses.size() );
        assertEquals( "found:" + putHandler.handlerRequestResponses, 201,
                      putHandler.handlerRequestResponses.get( 0 ).responseCode );
        assertEquals( "found:" + redirectHandler.handlerRequestResponses, 1,
                      redirectHandler.handlerRequestResponses.size() );
        assertEquals( "found:" + redirectHandler.handlerRequestResponses, 302,
                      redirectHandler.handlerRequestResponses.get( 0 ).responseCode );
    }

    public void testRedirectPutFromStreamRelativeUrl()
        throws Exception
    {
        Server realServer = new Server( 0 );
        addConnectors( realServer );
        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "Found", 303, "/redirectRequest/foo", repositoryDirectory );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();
            Repository repository = new Repository( "foo", getRepositoryUrl( redirectServer ) );
            wagon.connect( repository );

            File sourceFile = new File( repositoryDirectory, "/redirectRequest/foo/test-secured-put-resource" );
            sourceFile.delete();
            assertFalse( sourceFile.exists() );

            File tempFile = File.createTempFile( "wagon", "tmp" );
            tempFile.deleteOnExit();
            String content = "put top secret";
            FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

            FileInputStream fileInputStream = new FileInputStream( tempFile );
            try
            {
                wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
            }
            finally
            {
                fileInputStream.close();
                tempFile.delete();

            }

            assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

            checkRequestResponseForRedirectPutFromStreamWithRelativeUrl( putHandler, redirectHandler );

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }

    protected void checkRequestResponseForRedirectPutFromStreamWithRelativeUrl( PutHandler putHandler,
                                                                                RedirectHandler redirectHandler )
    {
        assertEquals( "found:" + putHandler.handlerRequestResponses, 0, putHandler.handlerRequestResponses.size() );

        assertEquals( "found:" + redirectHandler.handlerRequestResponses, 2,
                      redirectHandler.handlerRequestResponses.size() );
        assertEquals( "found:" + redirectHandler.handlerRequestResponses, 302,
                      redirectHandler.handlerRequestResponses.get( 0 ).responseCode );
        assertEquals( "found:" + redirectHandler.handlerRequestResponses, 201,
                      redirectHandler.handlerRequestResponses.get( 1 ).responseCode );

    }

    public void testRedirectPutFileWithFullUrl()
        throws Exception
    {
        Server realServer = new Server( 0 );

        addConnectors( realServer );

        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        String protocol = getProtocol();

        // protocol is wagon protocol but in fact dav is http(s)
        if ( protocol.equals( "dav" ) )
        {
            protocol = "http";
        }

        if ( protocol.equals( "davs" ) )
        {
            protocol = "https";
        }

        String redirectUrl = protocol + "://localhost:" + realServer.getConnectors()[0].getLocalPort();

        RedirectHandler redirectHandler = new RedirectHandler( "Found", 303, redirectUrl, repositoryDirectory );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();
            Repository repository = new Repository( "foo", getRepositoryUrl( redirectServer ) );
            wagon.connect( repository );

            File sourceFile = new File( repositoryDirectory, "test-secured-put-resource" );
            sourceFile.delete();
            assertFalse( sourceFile.exists() );

            File tempFile = File.createTempFile( "wagon", "tmp" );
            tempFile.deleteOnExit();
            String content = "put top secret";
            FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

            try
            {
                wagon.put( tempFile, "test-secured-put-resource" );
            }
            finally
            {
                tempFile.delete();
            }

            assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }


    public void testRedirectPutFileRelativeUrl()
        throws Exception
    {
        Server realServer = new Server( 0 );
        addConnectors( realServer );
        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( 0 );

        addConnectors( redirectServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "Found", 303, "/redirectRequest/foo", repositoryDirectory );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();
            Repository repository = new Repository( "foo", getRepositoryUrl( redirectServer ) );
            wagon.connect( repository );

            File sourceFile = new File( repositoryDirectory, "/redirectRequest/foo/test-secured-put-resource" );
            sourceFile.delete();
            assertFalse( sourceFile.exists() );

            File tempFile = File.createTempFile( "wagon", "tmp" );
            tempFile.deleteOnExit();
            String content = "put top secret";
            FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

            try
            {
                wagon.put( tempFile, "test-secured-put-resource" );
            }
            finally
            {
                tempFile.delete();
            }

            assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }


    public static class RedirectHandler
        extends AbstractHandler
    {
        String reason;

        int retCode;

        String redirectUrl;

        File repositoryDirectory;

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        RedirectHandler( String reason, int retCode, String redirectUrl, File repositoryDirectory )
        {
            this.reason = reason;
            this.retCode = retCode;
            this.redirectUrl = redirectUrl;
            this.repositoryDirectory = repositoryDirectory;
        }

        public void handle( String s, HttpServletRequest req, HttpServletResponse resp, int i )
            throws IOException, ServletException
        {
            if ( req.getRequestURI().contains( "redirectRequest" ) )
            {
                PutHandler putHandler = new PutHandler( this.repositoryDirectory );
                putHandler.handle( s, req, resp, i );
                handlerRequestResponses.add(
                    new HandlerRequestResponse( req.getMethod(), ( (Response) resp ).getStatus(),
                                                req.getRequestURI() ) );
                return;
            }
            resp.setStatus( this.retCode );
            resp.sendRedirect( this.redirectUrl + "/" + req.getRequestURI() );
            handlerRequestResponses.add(
                new HandlerRequestResponse( req.getMethod(), ( (Response) resp ).getStatus(), req.getRequestURI() ) );
        }
    }


    private void runTestProxiedRequest( ProxyInfo proxyInfo, TestHeaderHandler handler )
        throws Exception
    {
        // what an UGLY hack!
        // but apparently jetty needs some time to free up resources
        // <5s: broken test :(
        Thread.sleep( 5001L );

        Server proxyServer = new Server( 0 );

        proxyServer.setHandler( handler );

        proxyServer.start();

        proxyInfo.setPort( proxyServer.getConnectors()[0].getLocalPort() );

        System.out.println(
            "start proxy on host/port " + proxyInfo.getHost() + "/" + proxyInfo.getPort() + " with non proxyHosts "
                + proxyInfo.getNonProxyHosts() );

        while ( !proxyServer.isRunning() || !proxyServer.isStarted() )
        {
            Thread.sleep( 10 );
        }

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", "http://www.example.com/" );

            String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
            File sourceFile = new File( localRepositoryPath, "test-proxied-resource" );
            FileUtils.fileWrite( sourceFile.getAbsolutePath(), "content" );

            wagon.connect( testRepository, proxyInfo );

            try
            {
                wagon.getToStream( "test-proxied-resource", new ByteArrayOutputStream() );

                assertTrue( handler.headers.containsKey( "Proxy-Connection" ) );
            }
            finally
            {
                System.setProperty( "http.proxyHost", "" );
                System.setProperty( "http.proxyPort", "" );
                wagon.disconnect();
            }
        }
        finally
        {
            proxyServer.stop();
        }
    }

    private ProxyInfo createProxyInfo()
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( "localhost" );
        proxyInfo.setNonProxyHosts( null );
        proxyInfo.setType( "http" );
        return proxyInfo;
    }

    public void testSecuredGetUnauthorized()
        throws Exception
    {
        try
        {
            runTestSecuredGet( null );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredGetWrongPassword()
        throws Exception
    {
        try
        {
            AuthenticationInfo authInfo = new AuthenticationInfo();
            authInfo.setUserName( "user" );
            authInfo.setPassword( "admin" );
            runTestSecuredGet( authInfo );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredGet()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredGet( authInfo );
    }


    public void runTestSecuredGet( AuthenticationInfo authInfo )
        throws Exception
    {
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = createSecurityServer( localRepositoryPath );

        server.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            File sourceFile = new File( localRepositoryPath, "test-secured-resource" );
            FileUtils.fileWrite( sourceFile.getAbsolutePath(), "top secret" );

            wagon.connect( testRepository, authInfo );

            File file = File.createTempFile( "wagon-test", "txt" );

            try
            {
                wagon.get( "test-secured-resource", file );
            }
            finally
            {
                wagon.disconnect();
            }

            FileInputStream in = new FileInputStream( file );

            assertEquals( "top secret", IOUtil.toString( in ) );

            TestSecurityHandler securityHandler = (TestSecurityHandler) ( (Context) server.getHandler() ).getHandler();
            testPreemptiveAuthenticationGet( securityHandler, supportPreemptiveAuthenticationGet() );

        }
        finally
        {
            server.stop();
        }
    }


    public void testSecuredGetToStream()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredGetToStream( authInfo );
    }

    public void runTestSecuredGetToStream( AuthenticationInfo authInfo )
        throws Exception
    {
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = createSecurityServer( localRepositoryPath );

        server.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            File sourceFile = new File( localRepositoryPath, "test-secured-resource" );
            FileUtils.fileWrite( sourceFile.getAbsolutePath(), "top secret" );

            wagon.connect( testRepository, authInfo );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try
            {
                wagon.getToStream( "test-secured-resource", out );
            }
            finally
            {
                wagon.disconnect();
            }

            assertEquals( "top secret", out.toString( "US-ASCII" ) );

            TestSecurityHandler securityHandler = (TestSecurityHandler) ( (Context) server.getHandler() ).getHandler();
            testPreemptiveAuthenticationGet( securityHandler, supportPreemptiveAuthenticationGet() );
        }
        finally
        {
            server.stop();
        }
    }

    public void testSecuredResourceExistsUnauthorized()
        throws Exception
    {
        try
        {
            runTestSecuredResourceExists( null );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredResourceExistsWrongPassword()
        throws Exception
    {
        try
        {
            AuthenticationInfo authInfo = new AuthenticationInfo();
            authInfo.setUserName( "user" );
            authInfo.setPassword( "admin" );
            runTestSecuredResourceExists( authInfo );
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredResourceExists()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredResourceExists( authInfo );
    }

    public void runTestSecuredResourceExists( AuthenticationInfo authInfo )
        throws Exception
    {
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = createSecurityServer( localRepositoryPath );

        server.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            File sourceFile = new File( localRepositoryPath, "test-secured-resource-exists" );
            FileUtils.fileWrite( sourceFile.getAbsolutePath(), "top secret" );

            wagon.connect( testRepository, authInfo );

            try
            {
                assertTrue( wagon.resourceExists( "test-secured-resource-exists" ) );

                assertFalse( wagon.resourceExists( "test-secured-resource-not-exists" ) );
            }
            finally
            {
                wagon.disconnect();
            }
        }
        finally
        {
            server.stop();
        }
    }

    private Server createSecurityServer( String localRepositoryPath )
    {
        Server server = new Server( 0 );

        SecurityHandler sh = createSecurityHandler();

        Context root = new Context( Context.SESSIONS );
        root.setContextPath( "/" );
        root.addHandler( sh );
        root.setResourceBase( localRepositoryPath );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        root.addServlet( servletHolder, "/*" );

        server.setHandler( root );
        addConnectors( server );
        return server;
    }


    private String writeTestFileGzip( File parent, String child )
        throws IOException
    {
        File file = new File( parent, child );
        file.getParentFile().mkdirs();
        file.deleteOnExit();
        OutputStream out = new FileOutputStream( file );
        try
        {
            out.write( child.getBytes() );
        }
        finally
        {
            out.close();
        }

        file = new File( parent, child + ".gz" );
        file.deleteOnExit();
        String content;
        out = new FileOutputStream( file );
        out = new GZIPOutputStream( out );
        try
        {
            // write out different data than non-gz file, so we can
            // assert the gz version was returned
            content = file.getAbsolutePath();
            out.write( content.getBytes() );
        }
        finally
        {
            out.close();
        }

        return content;
    }

    public void testPutForbidden()
        throws Exception
    {
        try
        {
            runTestPut( HttpServletResponse.SC_FORBIDDEN );
            fail();
        }
        catch ( AuthorizationException e )
        {
            assertTrue( true );
        }
    }

    public void testPut404()
        throws Exception
    {
        try
        {
            runTestPut( HttpServletResponse.SC_NOT_FOUND );
            fail();
        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
    }

    public void testPut500()
        throws Exception
    {
        try
        {
            runTestPut( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }

    public void testPut429()
        throws Exception
    {

        try
        {

            StreamingWagon wagon = (StreamingWagon) getWagon();
            Server server = new Server( 0 );
            final AtomicBoolean called = new AtomicBoolean();

            AbstractHandler handler = new AbstractHandler()
            {
                public void handle( String s, HttpServletRequest request, HttpServletResponse response, int i )
                    throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                        ( (Request) request ).setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( 429 );
                        ( (Request) request ).setHandled( true );
                    }
                }
            };

            server.setHandler( handler );
            addConnectors( server );
            server.start();

            wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

            File tempFile = File.createTempFile( "wagon", "tmp" );
            tempFile.deleteOnExit();
            FileUtils.fileWrite( tempFile.getAbsolutePath(), "content" );

            try
            {
                wagon.put( tempFile, "resource" );
                fail();
            }
            finally
            {
                wagon.disconnect();

                server.stop();

                tempFile.delete();
            }

        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }


    private void runTestPut( int status )
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server server = new Server( 0 );
        StatusHandler handler = new StatusHandler();
        handler.setStatusToReturn( status );
        server.setHandler( handler );
        addConnectors( server );
        server.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

        File tempFile = File.createTempFile( "wagon", "tmp" );
        tempFile.deleteOnExit();
        FileUtils.fileWrite( tempFile.getAbsolutePath(), "content" );

        try
        {
            wagon.put( tempFile, "resource" );
            fail();
        }
        finally
        {
            wagon.disconnect();

            server.stop();

            tempFile.delete();
        }
    }

    public void testSecuredPutUnauthorized()
        throws Exception
    {
        try
        {
            runTestSecuredPut( null );
            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredPutWrongPassword()
        throws Exception
    {
        try
        {
            AuthenticationInfo authInfo = new AuthenticationInfo();
            authInfo.setUserName( "user" );
            authInfo.setPassword( "admin" );
            runTestSecuredPut( authInfo );
            fail();
        }
        catch ( TransferFailedException e )
        {
            assertTrue( true );
        }
    }

    public void testSecuredPut()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredPut( authInfo );
    }

    public void runTestSecuredPut( AuthenticationInfo authInfo )
        throws Exception
    {
        runTestSecuredPut( authInfo, 1 );
    }

    public void runTestSecuredPut( AuthenticationInfo authInfo, int putNumber )
        throws Exception
    {
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = new Server( 0 );

        TestSecurityHandler sh = createSecurityHandler();

        PutHandler putHandler = new PutHandler( new File( localRepositoryPath ) );

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers( new Handler[]{ sh, putHandler } );

        server.setHandler( handlers );
        addConnectors( server );
        server.start();

        StreamingWagon wagon = (StreamingWagon) getWagon();
        Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );
        wagon.connect( testRepository, authInfo );
        try
        {
            for ( int i = 0; i < putNumber; i++ )
            {
                File sourceFile = new File( localRepositoryPath, "test-secured-put-resource" );
                sourceFile.delete();
                assertFalse( sourceFile.exists() );

                File tempFile = File.createTempFile( "wagon", "tmp" );
                tempFile.deleteOnExit();
                FileUtils.fileWrite( tempFile.getAbsolutePath(), "put top secret" );

                try
                {
                    wagon.put( tempFile, "test-secured-put-resource" );
                }
                finally
                {
                    tempFile.delete();
                }

                assertEquals( "put top secret", FileUtils.fileRead( sourceFile.getAbsolutePath() ) );
            }
        }
        finally
        {
            wagon.disconnect();
            server.stop();
        }
        assertEquals( putNumber, putHandler.putCallNumber );
        testPreemptiveAuthenticationPut( sh, supportPreemptiveAuthenticationPut() );
    }

    public void testNonSecuredPutFromStream()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredPutFromStream( authInfo, 1, false );
    }

    public void testSecuredPutFromStream()
        throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( "user" );
        authInfo.setPassword( "secret" );
        runTestSecuredPutFromStream( authInfo, 1, true );
    }

    public void runTestSecuredPutFromStream( AuthenticationInfo authInfo, int putNumber, boolean addSecurityHandler )
        throws Exception
    {
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = new Server( 0 );

        TestSecurityHandler sh = createSecurityHandler();

        PutHandler putHandler = new PutHandler( new File( localRepositoryPath ) );

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers( addSecurityHandler ? new Handler[]{ sh, putHandler } : new Handler[]{ putHandler } );

        server.setHandler( handlers );
        addConnectors( server );
        server.start();

        StreamingWagon wagon = (StreamingWagon) getWagon();
        Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );
        if ( addSecurityHandler )
        {
            wagon.connect( testRepository, authInfo );
        }
        else
        {
            wagon.connect( testRepository );
        }
        try
        {
            for ( int i = 0; i < putNumber; i++ )
            {
                File sourceFile = new File( localRepositoryPath, "test-secured-put-resource" );
                sourceFile.delete();
                assertFalse( sourceFile.exists() );

                File tempFile = File.createTempFile( "wagon", "tmp" );
                tempFile.deleteOnExit();
                String content = "put top secret";
                FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

                FileInputStream fileInputStream = new FileInputStream( tempFile );
                try
                {
                    wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
                }
                finally
                {
                    fileInputStream.close();
                    tempFile.delete();

                }

                assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );
            }
        }
        finally
        {
            wagon.disconnect();
            server.stop();
        }
        assertEquals( putNumber, putHandler.putCallNumber );
        if ( addSecurityHandler )
        {
            testPreemptiveAuthenticationPut( sh, supportPreemptiveAuthenticationPut() );
        }

        // ensure we didn't use chunked transfer which doesn't work on ngnix
        for ( DeployedResource deployedResource : putHandler.deployedResources )
        {
            if ( StringUtils.equalsIgnoreCase( "chunked", deployedResource.transferEncoding ) )
            {
                fail( "deployedResource use chunked: " + deployedResource );
            }
        }
    }


    protected abstract boolean supportPreemptiveAuthenticationPut();

    protected abstract boolean supportPreemptiveAuthenticationGet();

    protected abstract boolean supportProxyPreemptiveAuthentication();

    protected void testPreemptiveAuthenticationGet( TestSecurityHandler sh, boolean preemptive )
    {
        testPreemptiveAuthentication( sh, preemptive );
    }

    protected void testPreemptiveAuthenticationPut( TestSecurityHandler sh, boolean preemptive )
    {
        testPreemptiveAuthentication( sh, preemptive );
    }

    protected void testPreemptiveAuthentication( TestSecurityHandler sh, boolean preemptive )
    {

        if ( preemptive )
        {
            assertEquals( "not 1 security handler use " + sh.handlerRequestResponses, 1,
                          sh.handlerRequestResponses.size() );
            assertEquals( 200, sh.handlerRequestResponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( "not 2 security handler use " + sh.handlerRequestResponses, 2,
                          sh.handlerRequestResponses.size() );
            assertEquals( 401, sh.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( 200, sh.handlerRequestResponses.get( 1 ).responseCode );

        }
    }

    static class StatusHandler
        extends AbstractHandler
    {
        private int status;

        public void setStatusToReturn( int status )
        {
            this.status = status;
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            if ( status != 0 )
            {
                response.setStatus( status );
                ( (Request) request ).setHandled( true );
            }
        }
    }

    static class DeployedResource
    {
        String httpMethod;

        String requestUri;

        String contentLength;

        String transferEncoding;

        public DeployedResource()
        {
            // no op
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "DeployedResource" );
            sb.append( "{httpMethod='" ).append( httpMethod ).append( '\'' );
            sb.append( ", requestUri='" ).append( requestUri ).append( '\'' );
            sb.append( ", contentLength='" ).append( contentLength ).append( '\'' );
            sb.append( ", transferEncoding='" ).append( transferEncoding ).append( '\'' );
            sb.append( '}' );
            return sb.toString();
        }
    }

    public static class PutHandler
        extends AbstractHandler
    {
        private final File resourceBase;

        public List<DeployedResource> deployedResources = new ArrayList<DeployedResource>();

        public int putCallNumber = 0;

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        public PutHandler( File repositoryDirectory )
        {
            this.resourceBase = repositoryDirectory;
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            Request base_request =
                request instanceof Request ? (Request) request : HttpConnection.getCurrentConnection().getRequest();

            if ( base_request.isHandled() || !"PUT".equals( base_request.getMethod() ) )
            {
                return;
            }

            base_request.setHandled( true );

            File file = new File( resourceBase, URLDecoder.decode( request.getPathInfo() ) );
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream( file );
            ServletInputStream in = request.getInputStream();
            try
            {
                IOUtil.copy( in, out );
            }
            finally
            {
                in.close();
                out.close();
            }
            putCallNumber++;
            DeployedResource deployedResource = new DeployedResource();

            deployedResource.httpMethod = request.getMethod();
            deployedResource.requestUri = request.getRequestURI();
            deployedResource.transferEncoding = request.getHeader( "Transfer-Encoding" );
            deployedResource.contentLength = request.getHeader( "Content-Length" );
            deployedResources.add( deployedResource );

            response.setStatus( HttpServletResponse.SC_CREATED );

            handlerRequestResponses.add(
                new HandlerRequestResponse( request.getMethod(), ( (Response) response ).getStatus(),
                                            request.getRequestURI() ) );
        }
    }

    private static class AuthorizingProxyHandler
        extends TestHeaderHandler
    {

        List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            System.out.println( " handle proxy request" );
            if ( request.getHeader( "Proxy-Authorization" ) == null )
            {
                handlerRequestResponses.add(
                    new HandlerRequestResponse( request.getMethod(), 407, request.getRequestURI() ) );
                response.setStatus( 407 );
                response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );

                ( (Request) request ).setHandled( true );
                return;
            }
            handlerRequestResponses.add(
                new HandlerRequestResponse( request.getMethod(), 200, request.getRequestURI() ) );
            super.handle( target, request, response, dispatch );
        }
    }

    private static class TestHeaderHandler
        extends AbstractHandler
    {
        public Map<String, String> headers = Collections.emptyMap();

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        public TestHeaderHandler()
        {
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            headers = new HashMap<String, String>();
            for ( Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements(); )
            {
                String name = e.nextElement();
                headers.put( name, request.getHeader( name ) );
            }

            response.setContentType( "text/plain" );
            response.setStatus( HttpServletResponse.SC_OK );
            response.getWriter().print( "Hello, World!" );

            handlerRequestResponses.add(
                new HandlerRequestResponse( request.getMethod(), ( (Response) response ).getStatus(),
                                            request.getRequestURI() ) );

            ( (Request) request ).setHandled( true );
        }

    }

    protected TestSecurityHandler createSecurityHandler()
    {
        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[]{ "admin" } );
        constraint.setAuthenticate( true );

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint( constraint );
        cm.setPathSpec( "/*" );

        TestSecurityHandler sh = new TestSecurityHandler();
        HashUserRealm hashUserRealm = new HashUserRealm( "MyRealm" );
        hashUserRealm.put( "user", "secret" );
        hashUserRealm.addUserToRole( "user", "admin" );
        sh.setUserRealm( hashUserRealm );
        sh.setConstraintMappings( new ConstraintMapping[]{ cm } );
        return sh;
    }

    public static class TestSecurityHandler
        extends SecurityHandler
    {

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        @Override
        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            String method = request.getMethod();
            super.handle( target, request, response, dispatch );

            handlerRequestResponses.add(
                new HandlerRequestResponse( method, ( (Response) response ).getStatus(), request.getRequestURI() ) );
        }

    }

    public static class HandlerRequestResponse
    {
        public String method;

        public int responseCode;

        public String requestUri;

        private HandlerRequestResponse( String method, int responseCode, String requestUri )
        {
            this.method = method;
            this.responseCode = responseCode;
            this.requestUri = requestUri;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "HandlerRequestResponse" );
            sb.append( "{method='" ).append( method ).append( '\'' );
            sb.append( ", responseCode=" ).append( responseCode );
            sb.append( ", requestUri='" ).append( requestUri ).append( '\'' );
            sb.append( '}' );
            return sb.toString();
        }
    }
}
