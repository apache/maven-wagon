package org.apache.maven.wagon.tck.http;

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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.maven.wagon.tck.http.Assertions.assertFileContentsFromResource;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.tck.http.fixture.ErrorCodeServlet;
import org.apache.maven.wagon.tck.http.fixture.LatencyServlet;
import org.apache.maven.wagon.tck.http.fixture.ProxyConnectionVerifierFilter;
import org.apache.maven.wagon.tck.http.fixture.RedirectionServlet;
import org.apache.maven.wagon.tck.http.fixture.ServerFixture;
import org.apache.maven.wagon.tck.http.fixture.ServletExceptionServlet;
import org.apache.maven.wagon.tck.http.util.ValueHolder;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

public class GetWagonTests
    extends HttpWagonTests
{

    @Test
    public void basic()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        testSuccessfulGet( "base.txt" );
    }

    @Test
    @Ignore( "FIX ME!" )
    public void proxied()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getServerFixture().addFilter( "*", new ProxyConnectionVerifierFilter() );

        ProxyInfo info = newProxyInfo();
        if ( !initTest( null, info ) )
        {
            return;
        }

        File target = newTempFile();
        getWagon().get( "base.txt", target );

        assertFileContentsFromResource( ServerFixture.SERVER_ROOT_RESOURCE_PATH, "base.txt", target,
                                        "Downloaded file doesn't match original." );
    }

    @Test
    public void highLatencyHighTimeout()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getServerFixture().addServlet( "/slow/*", new LatencyServlet( 2000 ) );
        testSuccessfulGet( "slow/large.txt", "large.txt" );
    }

    @Test
    public void highLatencyLowTimeout()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Servlet servlet = new LatencyServlet( 2000 );
        getServerFixture().addServlet( "/slow/*", servlet );
        testSuccessfulGet( "slow/large.txt", "large.txt" );
    }

    @Test
    public void inifiniteLatencyTimeout()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !isSupported() )
        {
            return;
        }

        final ValueHolder<Boolean> holder = new ValueHolder<Boolean>( false );

        Runnable r = new Runnable()
        {
            public void run()
            {
                Servlet servlet = new LatencyServlet( -1 );
                addNotificationTarget( servlet );

                getServerFixture().addServlet( "/infinite/*", servlet );
                try
                {
                    if ( !initTest( null, null ) )
                    {
                        return;
                    }

                    if ( getWagon() instanceof StreamWagon )
                    {
                        System.out.println( "Connection timeout is: " + ( (StreamWagon) getWagon() ).getTimeout() );
                    }

                    File target = newTempFile();
                    getWagon().get( "infinite/", target );

                    fail( "Should have failed to transfer due to transaction timeout." );
                }
                catch ( ConnectionException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( AuthenticationException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( TransferFailedException e )
                {
                    // expected
                    holder.setValue( true );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( AuthorizationException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( ComponentConfigurationException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( IOException e )
                {
                    throw new IllegalStateException( e );
                }
            }
        };

        Thread t = new Thread( r );
        t.start();

        try
        {
            System.out.println( "Waiting 60 seconds for wagon timeout." );
            t.join( 30000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        System.out.println( "Interrupting thread." );
        t.interrupt();

        assertTrue( "TransferFailedException should have been thrown.", holder.getValue() );
    }

    @Test
    public void nonExistentHost()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        ResourceDoesNotExistException, AuthorizationException
    {
        if ( !initTest( "http://dummy-host", null, null ) )
        {
            return;
        }

        File target = newTempFile();
        try
        {
            getWagon().get( "base.txt", target );
            fail( "Expected error related to host lookup failure." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    @Test
    public void oneLevelPermanentMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getServerFixture().addServlet( "/moved.txt",
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_PERMANENTLY, "/base.txt" ) );

        testSuccessfulGet( "moved.txt" );
    }

    @Test
    public void oneLevelTemporaryMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        getServerFixture().addServlet( "/moved.txt",
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_TEMPORARILY, "/base.txt" ) );

        testSuccessfulGet( "moved.txt" );
    }

    @Test
    public void sixLevelPermanentMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_PERMANENTLY, myPath,
                                                               targetPath, 6 ) );

        testSuccessfulGet( myPath );
    }

    @Test
    public void sixLevelTemporaryMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_TEMPORARILY, myPath,
                                                               targetPath, 6 ) );

        testSuccessfulGet( myPath );
    }

    @Test
    public void infinitePermanentMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_PERMANENTLY, myPath,
                                                               targetPath, -1 ) );

        try
        {
            testSuccessfulGet( myPath );
            fail( "Expected failure as a result of too many redirects." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    @Test
    public void infiniteTemporaryMove()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_TEMPORARILY, myPath,
                                                               targetPath, -1 ) );

        try
        {
            testSuccessfulGet( myPath );
            fail( "Expected failure as a result of too many redirects." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    /**
     * NOTE: This test depends on a {@link WagonTestCaseConfigurator} configuration to limit redirects to 20. In the
     * case of the Sun HTTP implementation, this is the default limit.
     */
    @Test
    public void permanentMove_TooManyRedirects_limit20()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_PERMANENTLY, myPath,
                                                               targetPath, -1 ) );

        try
        {
            testSuccessfulGet( myPath );
            fail( "Expected failure as a result of too many redirects." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    /**
     * NOTE: This test depends on a {@link WagonTestCaseConfigurator} configuration to limit redirects to 20. In the
     * case of the Sun HTTP implementation, this is the default limit.
     */
    @Test
    public void temporaryMove_TooManyRedirects_limit20()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        ResourceDoesNotExistException, AuthorizationException
    {
        String myPath = "moved.txt";
        String targetPath = "/base.txt";

        getServerFixture().addServlet(
                                       "/" + myPath,
                                       new RedirectionServlet( HttpServletResponse.SC_MOVED_TEMPORARILY, myPath,
                                                               targetPath, -1 ) );

        try
        {
            testSuccessfulGet( myPath );
            fail( "Expected failure as a result of too many redirects." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }

    @Test
    public void missing()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, AuthorizationException
    {
        if ( !initTest( null, null ) )
        {
            return;
        }

        File target = newTempFile();
        try
        {
            getWagon().get( "404.txt", target );
            fail( "should have received a 404, meaning the resource doesn't exist." );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expected
        }
    }

    @Test
    public void error()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        AuthorizationException, ResourceDoesNotExistException
    {
        testErrorHandling( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
    }

    @Test
    public void proxyTimeout()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        AuthorizationException, ResourceDoesNotExistException
    {
        testErrorHandling( HttpServletResponse.SC_GATEWAY_TIMEOUT );
    }

    @Test
    public void forbidden()
        throws ConnectionException, ComponentConfigurationException, IOException, ResourceDoesNotExistException,
        TransferFailedException
    {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName( "user" );
        info.setPassword( "password" );

        getServerFixture().addUser( info.getUserName(), "password" );

        getServerFixture().addServlet( "/403.txt",
                                       new ErrorCodeServlet( HttpServletResponse.SC_FORBIDDEN, "Expected 403" ) );

        testAuthFailure( "403.txt", info );
    }

    @Test
    public void successfulAuthentication()
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName( "user" );
        info.setPassword( "password" );

        getServerFixture().addUser( info.getUserName(), info.getPassword() );

        if ( !initTest( info, null ) )
        {
            return;
        }

        File target = newTempFile();
        getWagon().get( "protected/base.txt", target );

        assertFileContentsFromResource( ServerFixture.SERVER_ROOT_RESOURCE_PATH, "base.txt", target,
                                        "Downloaded file doesn't match original." );
    }

    @Test
    public void unsuccessfulAuthentication()
        throws ConnectionException, ComponentConfigurationException, IOException, TransferFailedException,
        ResourceDoesNotExistException
    {
        AuthenticationInfo info = new AuthenticationInfo();
        info.setUserName( "user" );
        info.setPassword( "password" );

        getServerFixture().addUser( info.getUserName(), "anotherPassword" );

        testAuthFailure( "protected/base.txt", info );
    }

    protected void testAuthFailure( final String path, final AuthenticationInfo info )
        throws ConnectionException, ComponentConfigurationException, IOException, TransferFailedException,
        ResourceDoesNotExistException
    {
        boolean authFailure = false;
        try
        {
            if ( !initTest( info, null ) )
            {
                return;
            }
        }
        catch ( AuthenticationException e )
        {
            // expected
            authFailure = true;
        }

        File target = newTempFile();
        try
        {
            getWagon().get( path, target );
        }
        catch ( AuthorizationException e )
        {
            // expected
            authFailure = true;
        }

        assertTrue( "Authentication/Authorization should have failed.", authFailure );
    }

    protected void testSuccessfulGet( final String path )
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        testSuccessfulGet( path, "base.txt" );
    }

    protected void testSuccessfulGet( final String path, final String checkPath )
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        if ( !initTest( null, null ) )
        {
            return;
        }

        if ( getWagon() instanceof StreamWagon )
        {
            System.out.println( "Connection timeout is: " + ( (StreamWagon) getWagon() ).getTimeout() );
        }

        File target = newTempFile();
        getWagon().get( path, target );

        assertFileContentsFromResource( ServerFixture.SERVER_ROOT_RESOURCE_PATH, checkPath, target,
                                        "Downloaded file doesn't match original." );
    }

    protected void testErrorHandling( final int code )
        throws ConnectionException, AuthenticationException, ComponentConfigurationException, IOException,
        AuthorizationException, ResourceDoesNotExistException
    {
        if ( code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR )
        {
            getServerFixture().addServlet( "/" + code + ".txt", new ServletExceptionServlet( "Expected " + code ) );
        }
        else
        {
            getServerFixture().addServlet( "/" + code + ".txt", new ErrorCodeServlet( code, "Expected " + code ) );
        }

        if ( !initTest( null, null ) )
        {
            return;
        }

        File target = newTempFile();
        try
        {
            getWagon().get( code + ".txt", target );
            fail( "should have received a " + code + " error code, meaning the resource doesn't exist." );
        }
        catch ( TransferFailedException e )
        {
            // expected
        }
    }
}
