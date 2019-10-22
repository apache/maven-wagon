package org.apache.maven.wagon.providers.http;

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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.impl.execchain.RetryExec;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.junit.Ignore;
import org.junit.Test;

public class AbstractHttpClientWagonTest
{
    @Ignore("This test is validating nothing and require internet connection which we should avoid so ignore it")
    public void test()
        throws Exception
    {
        AbstractHttpClientWagon wagon = new AbstractHttpClientWagon()
        {
        };

        Repository repository = new Repository( "central", "http://repo.maven.apache.org/maven2" );

        wagon.connect( repository );

        Resource resource = new Resource();

        resource.setName( "junit/junit/maven-metadata.xml" );

        InputData inputData = new InputData();

        inputData.setResource( resource );

        wagon.fillInputData( inputData );

        wagon.disconnect();
    }

    @Test
    public void retryableConfigurationDefaultTest() throws Exception
    {
        doTestHttpClient( new Runnable()
        {
            @Override
            public void run()
            {
                final HttpRequestRetryHandler handler = getCurrentHandler();
                assertNotNull( handler );
                assertThat( handler, instanceOf( DefaultHttpRequestRetryHandler.class ) );
                final DefaultHttpRequestRetryHandler impl = DefaultHttpRequestRetryHandler.class.cast(handler);
                assertEquals( 3, impl.getRetryCount() );
                assertFalse( impl.isRequestSentRetryEnabled() );
            }
        });
    }

    @Test
    public void retryableConfigurationCountTest() throws Exception
    {
        doTestHttpClient( new Runnable()
        {
            @Override
            public void run()
            {
                System.setProperty( "maven.wagon.http.retryHandler.class", "default" );
                System.setProperty( "maven.wagon.http.retryHandler.count", "5" );

                final HttpRequestRetryHandler handler = getCurrentHandler();
                assertNotNull( handler );
                assertThat( handler, instanceOf( DefaultHttpRequestRetryHandler.class ) );
                final DefaultHttpRequestRetryHandler impl = DefaultHttpRequestRetryHandler.class.cast(handler);
                assertEquals( 5, impl.getRetryCount() );
                assertFalse( impl.isRequestSentRetryEnabled() );
            }
        });
    }

    @Test
    public void retryableConfigurationSentTest() throws Exception
    {
        doTestHttpClient( new Runnable()
        {
            @Override
            public void run()
            {
                System.setProperty( "maven.wagon.http.retryHandler.class", "default" );
                System.setProperty( "maven.wagon.http.retryHandler.requestSentEnabled", "true" );

                final HttpRequestRetryHandler handler = getCurrentHandler();
                assertNotNull( handler );
                assertThat( handler, instanceOf( DefaultHttpRequestRetryHandler.class ) );
                final DefaultHttpRequestRetryHandler impl = DefaultHttpRequestRetryHandler.class.cast(handler);
                assertEquals( 3, impl.getRetryCount() );
                assertTrue( impl.isRequestSentRetryEnabled() );
            }
        });
    }

    @Test
    public void retryableConfigurationExceptionsTest() throws Exception
    {
        doTestHttpClient( new Runnable()
        {
            @Override
            public void run()
            {
                System.setProperty( "maven.wagon.http.retryHandler.class", "default" );
                System.setProperty( "maven.wagon.http.retryHandler.nonRetryableClasses", IOException.class.getName() );

                final HttpRequestRetryHandler handler = getCurrentHandler();
                assertNotNull( handler );
                assertThat( handler, instanceOf( DefaultHttpRequestRetryHandler.class ) );
                final DefaultHttpRequestRetryHandler impl = DefaultHttpRequestRetryHandler.class.cast(handler);
                assertEquals( 3, impl.getRetryCount() );
                assertFalse( impl.isRequestSentRetryEnabled() );

                try
                {
                    final Field nonRetriableClasses = handler.getClass().getSuperclass()
                            .getDeclaredField( "nonRetriableClasses" );
                    if ( !nonRetriableClasses.isAccessible() )
                    {
                        nonRetriableClasses.setAccessible(true);
                    }
                    final Set<?> exceptions = Set.class.cast( nonRetriableClasses.get(handler) );
                    assertEquals( 1, exceptions.size() );
                    assertTrue( exceptions.contains( IOException.class ) );
                }
                catch ( final Exception e )
                {
                    fail( e.getMessage() );
                }
            }
        });
    }

    private HttpRequestRetryHandler getCurrentHandler()
    {
        try
        {
            final Class<?> impl = Thread.currentThread().getContextClassLoader().loadClass(
                        "org.apache.maven.wagon.shared.http.AbstractHttpClientWagon" );

            final CloseableHttpClient httpClient = CloseableHttpClient.class.cast(
                    impl.getMethod("getHttpClient").invoke(null) );

            final Field redirectExec = httpClient.getClass().getDeclaredField( "execChain" );
            if ( !redirectExec.isAccessible() )
            {
                redirectExec.setAccessible( true );
            }
            final RedirectExec redirectExecInstance = RedirectExec.class.cast(
                    redirectExec.get( httpClient ) );

            final Field requestExecutor = redirectExecInstance.getClass().getDeclaredField( "requestExecutor" );
            if ( !requestExecutor.isAccessible() )
            {
                requestExecutor.setAccessible( true );
            }
            final RetryExec requestExecutorInstance = RetryExec.class.cast(
                    requestExecutor.get( redirectExecInstance ) );

            final Field retryHandler = requestExecutorInstance.getClass().getDeclaredField( "retryHandler" );
            if ( !retryHandler.isAccessible() )
            {
                retryHandler.setAccessible( true );
            }
            return HttpRequestRetryHandler.class.cast( retryHandler.get( requestExecutorInstance ) );
        }
        catch ( final Exception e )
        {
            throw new IllegalStateException(e);
        }
    }

    private void doTestHttpClient( final Runnable test ) throws Exception
    {
        final String classpath = System.getProperty( "java.class.path" );
        final String[] paths = classpath.split( File.pathSeparator );
        final Collection<URL> urls = new ArrayList<>( paths.length );
        for ( final String path : paths )
        {
            try
            {
                urls.add( new File( path ).toURI().toURL() );
            }
            catch ( final MalformedURLException e )
            {
                fail( e.getMessage() );
            }
        }
        final URLClassLoader loader = new URLClassLoader( urls.toArray( new URL[ paths.length ] ) , new ClassLoader()
        {
            @Override
            protected Class<?> loadClass( final String name, final boolean resolve ) throws ClassNotFoundException
            {
                if ( name.startsWith( "org.apache.maven.wagon.shared.http" ) )
                {
                    throw new ClassNotFoundException( name );
                }
                return super.loadClass( name, resolve );
            }
        });
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader( loader );

        final String originalClass = System.getProperty( "maven.wagon.http.retryHandler.class", "default" );
        final String originalSentEnabled = System.getProperty(
                "maven.wagon.http.retryHandler.requestSentEnabled", "false" );
        final String originalCount = System.getProperty( "maven.wagon.http.retryHandler.count", "3" );
        final String originalExceptions = System.getProperty( "maven.wagon.http.retryHandler.nonRetryableClasses",
                InterruptedIOException.class.getName() + ","
                    + UnknownHostException.class.getName() + ","
                    + ConnectException.class.getName() + ","
                    + SSLException.class.getName());
        try
        {
            test.run();
        }
        finally
        {
            loader.close();
            thread.setContextClassLoader( contextClassLoader );
            System.setProperty(  "maven.wagon.http.retryHandler.class", originalClass );
            System.setProperty(  "maven.wagon.http.retryHandler.requestSentEnabled", originalSentEnabled );
            System.setProperty(  "maven.wagon.http.retryHandler.count", originalCount );
            System.setProperty(  "maven.wagon.http.retryHandler.nonRetryableClasses", originalExceptions );
        }
    }
}
