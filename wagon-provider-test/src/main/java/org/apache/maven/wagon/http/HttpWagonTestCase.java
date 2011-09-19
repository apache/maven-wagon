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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringOutputStream;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * @version $Id: LightweightHttpWagonTest.java 680764 2008-07-29 16:45:51Z brett $
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

        server = new Server( 10007 );

        PutHandler putHandler = new PutHandler( repositoryDirectory );
        server.addHandler( putHandler );

        createContext( server, repositoryDirectory );

        addConnectors( server );

        server.start();
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

        wagon.getToStream( "resource", new StringOutputStream() );

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
        throws Exception, ConnectionException, AuthenticationException, TransferFailedException,
        ResourceDoesNotExistException, AuthorizationException
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
            wagon.getToStream( "resource", new StringOutputStream() );
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

    private boolean runTestResourceExists( int status )
        throws Exception, ConnectionException, AuthenticationException, TransferFailedException,
        ResourceDoesNotExistException, AuthorizationException
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
        Server server = new Server( 10008 );

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
        TestHeaderHandler handler = new AuthorizingProxyHandler();

        runTestProxiedRequest( proxyInfo, handler );

        assertTrue( handler.headers.containsKey( "Proxy-Authorization" ) );
    }

    private void runTestProxiedRequest( ProxyInfo proxyInfo, TestHeaderHandler handler )
        throws Exception, IOException, ConnectionException, AuthenticationException, ResourceDoesNotExistException,
        TransferFailedException, AuthorizationException
    {
        Server proxyServer = new Server( 10007 );

        proxyServer.setHandler( handler );
        proxyServer.start();

        proxyInfo.setPort( 10007 );

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", "http://www.example.com/" );

            String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
            File sourceFile = new File( localRepositoryPath, "test-proxied-resource" );
            FileUtils.fileWrite( sourceFile.getAbsolutePath(), "content" );

            wagon.connect( testRepository, proxyInfo );

            StringOutputStream out = new StringOutputStream();
            try
            {
                wagon.getToStream( "test-proxied-resource", out );

                assertTrue( handler.headers.containsKey( "Proxy-Connection" ) );
            }
            finally
            {
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

            StringOutputStream out = new StringOutputStream();
            try
            {
                wagon.getToStream( "test-secured-resource", out );
            }
            finally
            {
                wagon.disconnect();
            }

            assertEquals( "top secret", out.toString() );
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
        out.write( child.getBytes() );
        out.close();

        file = new File( parent, child + ".gz" );
        file.deleteOnExit();
        out = new FileOutputStream( file );
        out = new GZIPOutputStream( out );
        // write out different data than non-gz file, so we can
        // assert the gz version was returned
        String content = file.getAbsolutePath();
        out.write( content.getBytes() );
        out.close();
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

    private void runTestPut( int status )
        throws Exception, ConnectionException, AuthenticationException, TransferFailedException,
        ResourceDoesNotExistException, AuthorizationException
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
        String localRepositoryPath = FileTestUtils.getTestOutputDir().toString();
        Server server = new Server( 0 );

        TestSecurityHandler sh = createSecurityHandler();

        PutHandler handler = new PutHandler( new File( localRepositoryPath ) );

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers( new Handler[]{ sh, handler } );

        server.setHandler( handlers );
        addConnectors( server );
        server.start();

        try
        {
            StreamingWagon wagon = (StreamingWagon) getWagon();

            Repository testRepository = new Repository( "id", getRepositoryUrl( server ) );

            wagon.connect( testRepository, authInfo );

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
                wagon.disconnect();
                tempFile.delete();
            }

            assertEquals( "put top secret", FileUtils.fileRead( sourceFile.getAbsolutePath() ) );
            assertEquals( 1, handler.fileUploaded );
            testPreemptiveAuthentication( sh );
        }
        finally
        {
            server.stop();
        }
    }

    protected abstract boolean supportPreemptiveAuthentication();

    protected void testPreemptiveAuthentication( TestSecurityHandler sh )
    {

        if ( supportPreemptiveAuthentication() )
        {
            assertEquals( "not 1 security handler use " + sh.securityHandlerRequestReponses, 1,
                          sh.securityHandlerRequestReponses.size() );
            assertEquals( 200, sh.securityHandlerRequestReponses.get( 0 ).responseCode );
        }
        else
        {
            assertEquals( "not 2 security handler use " + sh.securityHandlerRequestReponses, 2,
                          sh.securityHandlerRequestReponses.size() );
            assertEquals( 401, sh.securityHandlerRequestReponses.get( 0 ).responseCode );
            assertEquals( 200, sh.securityHandlerRequestReponses.get( 1 ).responseCode );

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

    static class PutHandler
        extends AbstractHandler
    {
        private final File resourceBase;

        private int fileUploaded = 0;

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
                fileUploaded++;
            }
            finally
            {
                in.close();
                out.close();
            }

            response.setStatus( HttpServletResponse.SC_CREATED );
        }
    }

    private static class AuthorizingProxyHandler
        extends TestHeaderHandler
    {
        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            if ( request.getHeader( "Proxy-Authorization" ) == null )
            {
                response.setStatus( 407 );
                response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );

                ( (Request) request ).setHandled( true );
                return;
            }
            super.handle( target, request, response, dispatch );
        }
    }

    private static class TestHeaderHandler
        extends AbstractHandler
    {
        private Map headers;

        public TestHeaderHandler()
        {
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            headers = new HashMap();
            for ( Enumeration e = request.getHeaderNames(); e.hasMoreElements(); )
            {
                String name = (String) e.nextElement();
                headers.put( name, request.getHeader( name ) );
            }

            response.setContentType( "text/plain" );
            response.setStatus( HttpServletResponse.SC_OK );
            response.getWriter().println( "Hello, World!" );

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

        public List<SecurityHandlerRequestReponse> securityHandlerRequestReponses =
            new ArrayList<SecurityHandlerRequestReponse>();

        @Override
        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            String method = request.getMethod();
            super.handle( target, request, response, dispatch );
            System.out.println( "method in SecurityHandler: " + method );

            securityHandlerRequestReponses.add(
                new SecurityHandlerRequestReponse( method, ( (Response) response ).getStatus() ) );
        }

    }

    public static class SecurityHandlerRequestReponse
    {
        public String method;

        public int responseCode;

        private SecurityHandlerRequestReponse( String method, int responseCode )
        {
            this.method = method;
            this.responseCode = responseCode;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "SecurityHandlerRequestReponse" );
            sb.append( "{method='" ).append( method ).append( '\'' );
            sb.append( ", responseCode=" ).append( responseCode );
            sb.append( '}' );
            return sb.toString();
        }
    }
}
