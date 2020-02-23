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
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public abstract class HttpWagonTestCase
    extends StreamingWagonTestCase
{
    public static final int SC_TOO_MANY_REQUESTS = 429;

    private Server server;
    private ServerConnector connector;

    protected int getLocalPort( Server server )
    {
        Connector connector = server.getConnectors()[0];
        return ( ( ServerConnector ) connector ).getLocalPort();
    }

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

        server = new Server( );
        //connector = new ServerConnector( server, new HttpConnectionFactory( new HttpConfiguration() ) );
        //server.addConnector( connector );
        connector = addConnector( server );

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        ServletContextHandler context = createContext( server, repositoryDirectory );
        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler( putHandler );
        handlers.addHandler( context );
        server.setHandler( handlers );

        server.start();
    }

    protected final int getTestRepositoryPort()
    {
        if ( server == null )
        {
            return 0;
        }
        return connector.getLocalPort();
    }

    protected ServletContextHandler createContext( Server server, File repositoryDirectory )
        throws IOException
    {
        ServletContextHandler root = new ServletContextHandler( ServletContextHandler.SESSIONS );
        root.setResourceBase( repositoryDirectory.getAbsolutePath() );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        root.addServlet( servletHolder, "/*" );
        return root;
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

        Server server = new Server(  );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        ServerConnector serverConnector = addConnector( server );
        server.start();

        wagon.connect(
            new Repository( "id", getProtocol() + "://localhost:" + serverConnector.getLocalPort() ) );

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

        Server server = new Server( );
        ServerConnector serverConnector = addConnector( server );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        addConnector( server );
        server.start();

        wagon.connect(
            new Repository( "id", getProtocol() + "://localhost:" + serverConnector.getLocalPort() ) );

        wagon.getToStream( "resource", new ByteArrayOutputStream() );

        wagon.disconnect();

        server.stop();

        assertEquals( "Maven-Wagon/1.0", handler.headers.get( "User-Agent" ) );
    }

    public void testUserAgentHeaderIsPresentByDefault()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();
        Server server = new Server(  );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        addConnector( server );
        server.start();
        wagon.connect( new Repository( "id", getProtocol() + "://localhost:" + getLocalPort( server ) ) );
        wagon.getToStream( "resource", new ByteArrayOutputStream() );
        wagon.disconnect();
        server.stop();

        assertNotNull( "default User-Agent header of wagon provider should be present",
                       handler.headers.get( "User-Agent" ) );
    }

    public void testUserAgentHeaderIsPresentOnlyOnceIfSetMultipleTimes()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        // 1. set User-Agent header via HttpConfiguration
        Properties headers1 = new Properties();
        headers1.setProperty( "User-Agent", "test-user-agent" );
        setHttpHeaders( wagon, headers1 );

        // 2. redundantly set User-Agent header via setHttpHeaders()
        Properties headers2 = new Properties();
        headers2.setProperty( "User-Agent", "test-user-agent" );
        Method setHttpHeaders = wagon.getClass().getMethod( "setHttpHeaders", Properties.class );
        setHttpHeaders.invoke( wagon, headers2 );

        Server server = new Server(  );
        TestHeaderHandler handler = new TestHeaderHandler();
        server.setHandler( handler );
        addConnector( server );
        server.start();
        wagon.connect( new Repository( "id", getProtocol() + "://localhost:" + getLocalPort( server ) ) );
        wagon.getToStream( "resource", new ByteArrayOutputStream() );
        wagon.disconnect();
        server.stop();

        assertEquals( "test-user-agent", handler.headers.get( "User-Agent" ) );

    }

    protected abstract void setHttpHeaders( StreamingWagon wagon, Properties properties );

    protected ServerConnector addConnector( Server server )
    {
        ServerConnector serverConnector =
            new ServerConnector( server, new HttpConnectionFactory( new HttpConfiguration() ) );
        server.addConnector( serverConnector );
        return serverConnector;
    }

    protected String getRepositoryUrl( Server server )
    {
        int localPort = getLocalPort( server );
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

            Server server = new Server(  );
            final AtomicBoolean called = new AtomicBoolean();

            AbstractHandler handler = new AbstractHandler()
            {
                public void handle( String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response ) throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( HttpServletResponse.SC_OK );
                        baseRequest.setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( SC_TOO_MANY_REQUESTS );
                        baseRequest.setHandled( true );

                    }
                }
            };

            server.setHandler( handler );
            ServerConnector serverConnector = addConnector( server );
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

        Server server = createStatusServer( status );
        server.start();

        String baseUrl = getRepositoryUrl( server );
        String resourceName = "resource";
        String serverReasonPhrase = HttpStatus.getCode( status ).getMessage();

        wagon.connect( new Repository( "id", baseUrl ) );

        try
        {
            wagon.getToStream( "resource", new ByteArrayOutputStream() );
            fail();
        }
        catch ( Exception e )
        {
            verifyWagonExceptionMessage( e, status, baseUrl + "/" + resourceName, serverReasonPhrase );
            throw e;
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
                public void handle( String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response ) throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                        baseRequest.setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( SC_TOO_MANY_REQUESTS );
                        baseRequest.setHandled( true );
                    }
                }
            };

            StreamingWagon wagon = (StreamingWagon) getWagon();
            Server server = new Server(  );
            server.setHandler( handler );
            addConnector( server );
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

        Server server = createStatusServer( status );
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
        Server server = new Server( );

        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        ServletContextHandler root = new ServletContextHandler( ServletContextHandler.SESSIONS );
        root.setResourceBase( localRepositoryPath );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        servletHolder.setInitParameter( "gzip", "true" );
        root.addServlet( servletHolder, "/*" );
        addConnector( server );
        server.setHandler( root );
        server.start();

        try
        {
            Wagon wagon = getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            File sourceFile = new File( localRepositoryPath + "/gzip" );

            sourceFile.deleteOnExit();

            String resName = "gzip-res.txt";
            String sourceContent = writeTestFile( sourceFile, resName, "gzip" );

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

    /* This test cannot be enabled because we cannot tell GzipFilter to compress with deflate only
    public void testDeflateGet()
            throws Exception
        {
            Server server = new Server( );

            String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
            ServletContextHandler root = new ServletContextHandler( ServletContextHandler.SESSIONS );
            root.setResourceBase( localRepositoryPath );
            ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
            root.addServlet( servletHolder, "/*" );
            FilterHolder filterHolder = new FilterHolder( new GzipFilter() );
            root.addFilter( filterHolder, "/deflate/*", EnumSet.of( DispatcherType.REQUEST ) );
            addConnector( server );
            server.setHandler( root );
            server.start();

            try
            {
                Wagon wagon = getWagon();

                Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

                File sourceFile = new File( localRepositoryPath + "/deflate" );

                sourceFile.deleteOnExit();

                String resName = "deflate-res.txt";
                String sourceContent = writeTestFile( sourceFile, resName, null );

                wagon.connect( testRepository );

                File destFile = FileTestUtils.createUniqueFile( getName(), getName() );

                destFile.deleteOnExit();

                wagon.get( "deflate/" + resName, destFile );

                wagon.disconnect();

                String destContent = FileUtils.fileRead( destFile );

                assertEquals( sourceContent, destContent );
            }
            finally
            {
                server.stop();
            }
        }*/

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
            assertEquals( HttpServletResponse.SC_OK, handler.handlerRequestResponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
                          handler.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( HttpServletResponse.SC_OK, handler.handlerRequestResponses.get( 1 ).responseCode );
        }

    }

    public void testProxiedRequestWithAuthenticationWithProvider()
        throws Exception
    {
        final ProxyInfo proxyInfo = createProxyInfo();
        proxyInfo.setUserName( "user" );
        proxyInfo.setPassword( "secret" );
        AuthorizingProxyHandler handler = new AuthorizingProxyHandler();

        ProxyInfoProvider proxyInfoProvider = new ProxyInfoProvider()
        {
            public ProxyInfo getProxyInfo( String protocol )
            {
                return proxyInfo;
            }
        };
        runTestProxiedRequestWithProvider( proxyInfoProvider, handler );

        assertTrue( handler.headers.containsKey( "Proxy-Authorization" ) );

        if ( supportProxyPreemptiveAuthentication() )
        {
            assertEquals( HttpServletResponse.SC_OK, handler.handlerRequestResponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
                          handler.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( HttpServletResponse.SC_OK, handler.handlerRequestResponses.get( 1 ).responseCode );
        }

    }

    public void testRedirectGetToStream()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server realServer = new Server(  );
        TestHeaderHandler handler = new TestHeaderHandler();

        realServer.setHandler( handler );
        addConnector( realServer );
        realServer.start();

        Server redirectServer = new Server(  );

        addConnector( redirectServer );

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

        String redirectUrl = protocol + "://localhost:" + getLocalPort( realServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, redirectUrl, null );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( redirectServer ) ) );

        File tmpResult = File.createTempFile( "foo", "get" );

        try ( FileOutputStream fileOutputStream = new FileOutputStream( tmpResult ) )
        {
            wagon.getToStream( "resource", fileOutputStream );
            fileOutputStream.flush();
            fileOutputStream.close();
            String found = FileUtils.fileRead( tmpResult );
            assertEquals( "found:'" + found + "'", "Hello, World!", found );

            checkHandlerResult( redirectHandler.handlerRequestResponses, HttpServletResponse.SC_SEE_OTHER );
            checkHandlerResult( handler.handlerRequestResponses, HttpServletResponse.SC_OK );
        }
        finally
        {
            wagon.disconnect();

            redirectServer.stop();
            realServer.stop();

            tmpResult.delete();
        }
    }

    public void testRedirectGet()
        throws Exception
    {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        Server realServer = new Server( );
        TestHeaderHandler handler = new TestHeaderHandler();

        realServer.setHandler( handler );
        addConnector( realServer );
        realServer.start();

        Server redirectServer = new Server( );

        addConnector( redirectServer );

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

        String redirectUrl = protocol + "://localhost:" + getLocalPort( realServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, redirectUrl, null );

        redirectServer.setHandler( redirectHandler );

        redirectServer.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( redirectServer ) ) );

        File tmpResult = File.createTempFile( "foo", "get" );

        try
        {
            wagon.get( "resource", tmpResult );
            String found = FileUtils.fileRead( tmpResult );
            assertEquals( "found:'" + found + "'", "Hello, World!", found );

            checkHandlerResult( redirectHandler.handlerRequestResponses, HttpServletResponse.SC_SEE_OTHER );
            checkHandlerResult( handler.handlerRequestResponses, HttpServletResponse.SC_OK );
        }
        finally
        {
            wagon.disconnect();

            redirectServer.stop();
            realServer.stop();

            tmpResult.delete();
        }
    }


    public void testRedirectPutFromStreamWithFullUrl()
        throws Exception
    {
        Server realServer = new Server( );

        addConnector( realServer );

        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( );

        addConnector( redirectServer );

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

        String redirectUrl = protocol + "://localhost:" + getLocalPort( realServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, redirectUrl, repositoryDirectory );

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

            try ( FileInputStream fileInputStream = new FileInputStream( tempFile ) )
            {
                wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
                assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

                checkRequestResponseForRedirectPutWithFullUrl( redirectHandler, putHandler );
            }
            finally
            {
                wagon.disconnect();
                tempFile.delete();
            }

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }

    protected void checkRequestResponseForRedirectPutWithFullUrl( RedirectHandler redirectHandler,
                                                                  PutHandler putHandler )
    {
        checkHandlerResult( redirectHandler.handlerRequestResponses, HttpServletResponse.SC_SEE_OTHER );
        checkHandlerResult( putHandler.handlerRequestResponses, HttpServletResponse.SC_CREATED );
    }

    public void testRedirectPutFromStreamRelativeUrl()
        throws Exception
    {
        Server realServer = new Server( );
        addConnector( realServer );
        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( );

        addConnector( redirectServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, "/redirectRequest/foo",
                                 repositoryDirectory );

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

            try ( FileInputStream fileInputStream = new FileInputStream( tempFile ) )
            {
                wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
                assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

                checkRequestResponseForRedirectPutWithRelativeUrl( redirectHandler, putHandler );
            }
            finally
            {
                wagon.disconnect();
                tempFile.delete();
            }

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }

    protected void checkRequestResponseForRedirectPutWithRelativeUrl( RedirectHandler redirectHandler,
                                                                      PutHandler putHandler )
    {
        checkHandlerResult( redirectHandler.handlerRequestResponses, HttpServletResponse.SC_SEE_OTHER,
                            HttpServletResponse.SC_CREATED );
        checkHandlerResult( putHandler.handlerRequestResponses );
    }

    protected void checkHandlerResult( List<HandlerRequestResponse> handlerRequestResponses,
                                       int... expectedResponseCodes )
    {
        boolean success = true;
        if ( handlerRequestResponses.size() == expectedResponseCodes.length )
        {
            for ( int i = 0; i < expectedResponseCodes.length; i++ )
            {
                success &= ( expectedResponseCodes[i] == handlerRequestResponses.get( i ).responseCode );
            }
        }

        if ( !success )
        {
            fail( "expected " + expectedResponseCodes + ", got " + handlerRequestResponses );
        }
    }

    public void testRedirectPutFileWithFullUrl()
        throws Exception
    {
        Server realServer = new Server( );

        addConnector( realServer );

        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( );

        addConnector( redirectServer );

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

        String redirectUrl = protocol + "://localhost:" + getLocalPort( realServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, redirectUrl, repositoryDirectory );

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
                assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

                checkRequestResponseForRedirectPutWithFullUrl( redirectHandler, putHandler );
            }
            finally
            {
                wagon.disconnect();
                tempFile.delete();
            }

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
        Server realServer = new Server( );
        addConnector( realServer );
        File repositoryDirectory = getRepositoryDirectory();
        FileUtils.deleteDirectory( repositoryDirectory );
        repositoryDirectory.mkdirs();

        PutHandler putHandler = new PutHandler( repositoryDirectory );

        realServer.setHandler( putHandler );

        realServer.start();

        Server redirectServer = new Server( );

        addConnector( redirectServer );

        RedirectHandler redirectHandler =
            new RedirectHandler( "See Other", HttpServletResponse.SC_SEE_OTHER, "/redirectRequest/foo",
                                 repositoryDirectory );

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
                assertEquals( content, FileUtils.fileRead( sourceFile.getAbsolutePath() ) );

                checkRequestResponseForRedirectPutWithRelativeUrl( redirectHandler, putHandler );
            }
            finally
            {
                wagon.disconnect();
                tempFile.delete();
            }

        }
        finally
        {
            realServer.stop();
            redirectServer.stop();
        }
    }


    /**
     *
     */
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
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

        public void handle( String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            if ( request.getRequestURI().contains( "redirectRequest" ) )
            {
                PutHandler putHandler = new PutHandler( this.repositoryDirectory );
                putHandler.handle( target, baseRequest, request, response );
                handlerRequestResponses.add(
                    new HandlerRequestResponse( request.getMethod(), response.getStatus(),
                                                request.getRequestURI() ) );
                return;
            }
            response.setStatus( this.retCode );
            response.setHeader( "Location", this.redirectUrl + request.getRequestURI() );
            baseRequest.setHandled( true );

            handlerRequestResponses.add(
                new HandlerRequestResponse( request.getMethod(), response.getStatus(),
                                            request.getRequestURI() ) );
        }


    }


    private void runTestProxiedRequest( ProxyInfo proxyInfo, TestHeaderHandler handler )
        throws Exception
    {
        // what an UGLY hack!
        // but apparently jetty needs some time to free up resources
        // <5s: broken test :(
        // CHECKSTYLE_OFF: MagicNumber
        Thread.sleep( 5001L );
        // CHECKSTYLE_ON: MagicNumber

        Server proxyServer = new Server( );
        ServerConnector serverConnector =
            new ServerConnector( proxyServer, new HttpConnectionFactory( new HttpConfiguration() ) );
        proxyServer.addConnector( serverConnector );
        proxyServer.setHandler( handler );

        proxyServer.start();

        proxyInfo.setPort( getLocalPort( proxyServer ) );

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

    private void runTestProxiedRequestWithProvider( ProxyInfoProvider proxyInfoProvider, TestHeaderHandler handler )
        throws Exception
    {
        // what an UGLY hack!
        // but apparently jetty needs some time to free up resources
        // <5s: broken test :(
        // CHECKSTYLE_OFF: MagicNumber
        Thread.sleep( 5001L );
        // CHECKSTYLE_ON: MagicNumber

        Server proxyServer = new Server( );
        ServerConnector serverConnector =
            new ServerConnector( proxyServer, new HttpConnectionFactory( new HttpConfiguration() ) );
        proxyServer.addConnector( serverConnector );

        proxyServer.setHandler( handler );

        proxyServer.start();

        proxyInfoProvider.getProxyInfo( null ).setPort( getLocalPort( proxyServer ) );

        System.out.println( "start proxy on host/port " + proxyInfoProvider.getProxyInfo( null ).getHost() + "/"
                                + proxyInfoProvider.getProxyInfo( null ).getPort() + " with non proxyHosts "
                                + proxyInfoProvider.getProxyInfo( null ).getNonProxyHosts() );

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

            wagon.connect( testRepository, proxyInfoProvider );

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

            /*
             * We need to wait a bit for all Jetty workers/threads to complete their work. Otherwise
             * we may suffer from race conditions where handlerRequestResponses list is not completely
             * populated and its premature iteration in testPreemptiveAuthenticationGet will lead to
             * a test failure.
             */
            // CHECKSTYLE_OFF: MagicNumber
            Thread.sleep ( 2000L );
            // CHECKSTYLE_ON: MagicNumber

            TestSecurityHandler securityHandler = server.getChildHandlerByClass( TestSecurityHandler.class );
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

            /*
             * We need to wait a bit for all Jetty workers/threads to complete their work. Otherwise
             * we may suffer from race conditions where handlerRequestResponses list is not completely
             * populated and its premature iteration in testPreemptiveAuthenticationGet will lead to
             * a test failure.
             */
            // CHECKSTYLE_OFF: MagicNumber
            Thread.sleep ( 2000L );
            // CHECKSTYLE_ON: MagicNumber

            TestSecurityHandler securityHandler = server.getChildHandlerByClass( TestSecurityHandler.class );
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
        Server server = new Server( );

        SecurityHandler sh = createSecurityHandler();

        ServletContextHandler root = new ServletContextHandler( ServletContextHandler.SESSIONS
            | ServletContextHandler.SECURITY );
        root.setResourceBase( localRepositoryPath );
        root.setSecurityHandler( sh );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        root.addServlet( servletHolder, "/*" );

        server.setHandler( root );
        addConnector( server );
        return server;
    }

    private Server createStatusServer( int status )
    {
        Server server = new Server( );
        StatusHandler handler = new StatusHandler();
        handler.setStatusToReturn( status );
        server.setHandler( handler );
        addConnector( server );
        return server;
    }


    private String writeTestFile( File parent, String child, String compressionType )
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

        String ext = "";
        if ( "gzip".equals( compressionType ) )
        {
            ext = ".gz";
        }
        if ( "deflate".equals( compressionType ) )
        {
            ext = ".deflate";
        }

        file = new File( parent, child + ext );
        file.deleteOnExit();
        String content;
        out = new FileOutputStream( file );
        if ( "gzip".equals( compressionType ) )
        {
            out = new GZIPOutputStream( out );
        }
        if ( "deflate".equals( compressionType ) )
        {
            out = new DeflaterOutputStream( out );
        }
        try
        {
            // write out different data than non-compressed file, so we can
            // assert the compressed version was returned
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
            Server server = new Server( );
            final AtomicBoolean called = new AtomicBoolean();

            AbstractHandler handler = new AbstractHandler()
            {
                public void handle( String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response ) throws IOException, ServletException
                {
                    if ( called.get() )
                    {
                        response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                        baseRequest.setHandled( true );
                    }
                    else
                    {
                        called.set( true );
                        response.setStatus( SC_TOO_MANY_REQUESTS );
                        baseRequest.setHandled( true );
                    }
                }
            };

            server.setHandler( handler );
            addConnector( server );
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

        Server server = createStatusServer( status );
        server.start();

        wagon.connect( new Repository( "id", getRepositoryUrl( server ) ) );

        File tempFile = File.createTempFile( "wagon", "tmp" );
        tempFile.deleteOnExit();
        FileUtils.fileWrite( tempFile.getAbsolutePath(), "content" );

        String baseUrl = getRepositoryUrl( server );
        String resourceName = "resource";
        String serverReasonPhrase = HttpStatus.getCode( status ).getMessage();

        try
        {
            wagon.put( tempFile, resourceName );
            fail();
        }
        catch ( Exception e )
        {
            verifyWagonExceptionMessage( e, status, baseUrl + "/" + resourceName, serverReasonPhrase );
            throw e;
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
        catch ( AuthorizationException e )
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
        catch ( AuthorizationException e )
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
        Server server = new Server( );

        TestSecurityHandler sh = createSecurityHandler();

        PutHandler putHandler = new PutHandler( new File( localRepositoryPath ) );

        sh.setHandler( putHandler );
        server.setHandler( sh );
        addConnector( server );
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
        Server server = new Server( );

        TestSecurityHandler sh = createSecurityHandler();

        PutHandler putHandler = new PutHandler( new File( localRepositoryPath ) );

        if ( addSecurityHandler )
        {
            sh.setHandler( putHandler );
            server.setHandler( sh );
        }
        else
        {
            server.setHandler( putHandler );
        }
        addConnector( server );
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

                try ( FileInputStream fileInputStream = new FileInputStream( tempFile ) )
                {
                    wagon.putFromStream( fileInputStream, "test-secured-put-resource", content.length(), -1 );
                }
                finally
                {
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
        testPreemptiveAuthentication( sh, preemptive, HttpServletResponse.SC_OK );
    }

    protected void testPreemptiveAuthenticationPut( TestSecurityHandler sh, boolean preemptive )
    {
        testPreemptiveAuthentication( sh, preemptive, HttpServletResponse.SC_CREATED );
    }

    protected void testPreemptiveAuthentication( TestSecurityHandler sh, boolean preemptive, int statusCode )
    {

        if ( preemptive )
        {
            assertEquals( "not 1 security handler use " + sh.handlerRequestResponses, 1,
                          sh.handlerRequestResponses.size() );
            assertEquals( statusCode, sh.handlerRequestResponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( "not 2 security handler use " + sh.handlerRequestResponses, 2,
                          sh.handlerRequestResponses.size() );
            assertEquals( HttpServletResponse.SC_UNAUTHORIZED, sh.handlerRequestResponses.get( 0 ).responseCode );
            assertEquals( statusCode, sh.handlerRequestResponses.get( 1 ).responseCode );

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

        public void handle( String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            if ( status != 0 )
            {
                response.setStatus( status );
                baseRequest.setHandled( true );
            }
        }
    }

    static class DeployedResource
    {
        String httpMethod;

        String requestUri;

        String contentLength;

        String transferEncoding;

        DeployedResource()
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

    /**
     *
     */
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
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

        public void handle( String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            if ( baseRequest.isHandled() || !"PUT".equals( baseRequest.getMethod() ) )
            {
                return;
            }

            baseRequest.setHandled( true );

            File file = new File( resourceBase, URLDecoder.decode( request.getPathInfo() ) );
            file.getParentFile().mkdirs();
            OutputStream out = null;
            InputStream in = null;
            try
            {
                in = request.getInputStream();
                out = new FileOutputStream( file );
                IOUtil.copy( in, out );
                out.close();
                out = null;
                in.close();
                in = null;
            }
            finally
            {
                IOUtil.close( in );
                IOUtil.close( out );
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

        public void handle( String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            System.out.println( " handle proxy request" );
            if ( request.getHeader( "Proxy-Authorization" ) == null )
            {
                handlerRequestResponses.add(
                    new HandlerRequestResponse( request.getMethod(),
                                                HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
                                                request.getRequestURI() ) );
                response.setStatus( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
                response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );

                baseRequest.setHandled( true );
                return;
            }
            handlerRequestResponses.add(
                new HandlerRequestResponse( request.getMethod(), HttpServletResponse.SC_OK, request.getRequestURI() ) );
            super.handle( target, baseRequest, request, response );
        }
    }

    /**
     *
     */
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    private static class TestHeaderHandler
        extends AbstractHandler
    {
        public Map<String, String> headers = Collections.emptyMap();

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        TestHeaderHandler()
        {
        }

        public void handle( String target, Request baseRrequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            headers = new HashMap<String, String>();
            for ( Enumeration<String> e = baseRrequest.getHeaderNames(); e.hasMoreElements(); )
            {
                String name = e.nextElement();
                Enumeration headerValues = baseRrequest.getHeaders( name );
                // as per HTTP spec http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html
                // multiple values for the same header key are concatenated separated by comma
                // otherwise we wouldn't notice headers with same key added multiple times
                StringBuffer combinedHeaderValue = new StringBuffer();
                for ( int i = 0; headerValues.hasMoreElements(); i++ )
                {
                    if ( i > 0 )
                    {
                        combinedHeaderValue.append( "," );
                    }
                    combinedHeaderValue.append( headerValues.nextElement() );
                }
                headers.put( name, combinedHeaderValue.toString() );
            }

            response.setContentType( "text/plain" );
            response.setStatus( HttpServletResponse.SC_OK );
            response.getWriter().print( "Hello, World!" );

            handlerRequestResponses.add(
                new HandlerRequestResponse( baseRrequest.getMethod(), ( (Response) response ).getStatus(),
                                            baseRrequest.getRequestURI() ) );

            baseRrequest.setHandled( true );
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
        HashLoginService hashLoginService = new HashLoginService( "MyRealm" );
        hashLoginService.putUser( "user", new Password( "secret" ), new String[] { "admin" } );
        sh.setLoginService( hashLoginService );
        sh.setConstraintMappings( new ConstraintMapping[]{ cm } );
        sh.setAuthenticator ( new BasicAuthenticator() );
        return sh;
    }

    /**
     *
     */
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public static class TestSecurityHandler
        extends ConstraintSecurityHandler
    {

        public List<HandlerRequestResponse> handlerRequestResponses = new ArrayList<HandlerRequestResponse>();

        @Override
        public void handle( String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response ) throws IOException, ServletException
        {
            String method = request.getMethod();
            super.handle( target, baseRequest, request, response );

            handlerRequestResponses.add(
                new HandlerRequestResponse( method, ( (Response) response ).getStatus(), request.getRequestURI() ) );
        }
    }

    /**
     *
     */
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
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

    /**
     * Verify a WagonException message contains required format and context based on the status code we expected to
     * trigger it in the first place.
     * <p>
     * This implementation represents the most desired assertions, but HttpWagonTestCase sub-classes could override
     * this method if a specific wagon representation makes it impossible to meet these assertions.
     *
     * @param e               an instance of {@link WagonException}
     * @param forStatusCode   the response status code that triggered the exception
     * @param forUrl          the url that triggered the exception
     * @param forReasonPhrase the optional status line reason phrase the server returned
     */
    protected void verifyWagonExceptionMessage( Exception e, int forStatusCode, String forUrl, String forReasonPhrase )
    {
        // TODO: handle AuthenticationException for Wagon.connect() calls
        assertNotNull( e );
        try
        {
            assertTrue( "only verify instances of WagonException", e instanceof WagonException );

            String reasonPhrase;
            String assertMessageForBadMessage = "exception message not described properly";
            switch ( forStatusCode )
            {
                case HttpServletResponse.SC_NOT_FOUND:
                    // TODO: add test for 410: Gone?
                    assertTrue( "404 not found response should throw ResourceDoesNotExistException",
                            e instanceof ResourceDoesNotExistException );
                    reasonPhrase = StringUtils.isEmpty( forReasonPhrase ) ? " Not Found" : ( " " + forReasonPhrase );
                    assertEquals( assertMessageForBadMessage, "Resource missing at " + forUrl + " 404"
                            + reasonPhrase, e.getMessage() );
                    break;

                case HttpServletResponse.SC_UNAUTHORIZED:
                    // FIXME assumes Wagon.get()/put() returning 401 instead of Wagon.connect()
                    assertTrue( "401 Unauthorized should throw AuthorizationException since "
                                    + " AuthenticationException is not explicitly declared as thrown from wagon "
                                    + "methods",
                            e instanceof AuthorizationException );
                    reasonPhrase = StringUtils.isEmpty( forReasonPhrase ) ? " Unauthorized" : ( " " + forReasonPhrase );
                    assertEquals( assertMessageForBadMessage, "Authentication failed for " + forUrl + " 401"
                            + reasonPhrase, e.getMessage() );
                    break;

                case HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED:
                    assertTrue( "407 Proxy authentication required should throw AuthorizationException",
                            e instanceof AuthorizationException );
                    reasonPhrase = StringUtils.isEmpty( forReasonPhrase ) ? " Proxy Authentication Required"
                            : ( " " + forReasonPhrase );
                    assertEquals( assertMessageForBadMessage, "HTTP proxy server authentication failed for "
                            + forUrl + " 407" + reasonPhrase, e.getMessage() );
                    break;

                case HttpServletResponse.SC_FORBIDDEN:
                    assertTrue( "403 Forbidden should throw AuthorizationException",
                            e instanceof AuthorizationException );
                    reasonPhrase = StringUtils.isEmpty( forReasonPhrase ) ? " Forbidden" : ( " " + forReasonPhrase );
                    assertEquals( assertMessageForBadMessage, "Authorization failed for " + forUrl + " 403"
                            + reasonPhrase, e.getMessage() );
                    break;

                default:
                    assertTrue( "transfer failures should at least be wrapped in a TransferFailedException", e
                                    instanceof TransferFailedException );
                    assertTrue( "expected status code for transfer failures should be >= 400",
                            forStatusCode >= HttpServletResponse.SC_BAD_REQUEST );
                    reasonPhrase = forReasonPhrase == null ? "" : " " + forReasonPhrase;
                    assertEquals( assertMessageForBadMessage, "Transfer failed for " + forUrl + " "
                            + forStatusCode + reasonPhrase, e.getMessage() );
                    break;
            }
        }
        catch ( AssertionError assertionError )
        {
            logger.error( "Exception which failed assertions: ", e );
            throw assertionError;
        }

    }

}
