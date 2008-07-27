package org.apache.maven.wagon;

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
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.IOUtil;
import org.easymock.MockControl;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: AbstractWagonTest.java 630808 2008-02-25 11:01:41Z brett $
 */
public class AbstractWagonTest
    extends TestCase
{
    private String basedir;

    private WagonMock wagon = null;

    private File destination;

    private File source;

    private String artifact;

    private SessionListener sessionListener = null;

    private TransferListener transferListener = null;

    private MockControl transferListenerControl;

    private MockControl sessionListenerControl;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        destination = new File( basedir, "target/folder/subfolder" );

        source = new File( basedir, "pom.xml" );

        wagon = new WagonMock();

        sessionListenerControl = MockControl.createControl( SessionListener.class ); 
        sessionListener = (SessionListener) sessionListenerControl.getMock();

        wagon.addSessionListener( sessionListener );

        transferListenerControl = MockControl.createControl( TransferListener.class ); 
        transferListener = (TransferListener) transferListenerControl.getMock();

        wagon.addTransferListener( transferListener );

    }

    public void testSessionListenerRegistration()
    {
        assertTrue( wagon.hasSessionListener( sessionListener ) );

        wagon.removeSessionListener( sessionListener );

        assertFalse( wagon.hasSessionListener( sessionListener ) );
    }

    public void testTransferListenerRegistration()
    {
        assertTrue( wagon.hasTransferListener( transferListener ) );

        wagon.removeTransferListener( transferListener );

        assertFalse( wagon.hasTransferListener( transferListener ) );
    }

    public void testNoProxyConfiguration() throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository();
        wagon.connect( repository );
        assertNull( wagon.getProxyInfo() );
        assertNull( wagon.getProxyInfo( "http", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "http", "localhost" ) );
    }

    public void testLegacyProxyConfiguration() throws ConnectionException, AuthenticationException
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType( "http" );
        
        Repository repository = new Repository();
        wagon.connect( repository, proxyInfo );
        assertEquals( proxyInfo, wagon.getProxyInfo() );
        assertEquals( proxyInfo, wagon.getProxyInfo( "http", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
    }

    public void testProxyConfiguration() throws ConnectionException, AuthenticationException
    {
        final ProxyInfo httpProxyInfo = new ProxyInfo();
        httpProxyInfo.setType( "http" );
        
        final ProxyInfo socksProxyInfo = new ProxyInfo(); 
        socksProxyInfo.setType( "http" );
        
        ProxyInfoProvider proxyInfoProvider = new ProxyInfoProvider()
        {
            public ProxyInfo getProxyInfo( String protocol )
            {
                if ( "http".equals( protocol ) || "dav".equals( protocol ))
                {
                    return httpProxyInfo;
                }
                else if ( "scp".equals( protocol ) )
                {
                    return socksProxyInfo;
                }
                return null;
            }
        };
        
        Repository repository = new Repository();
        wagon.connect( repository, proxyInfoProvider );
        assertNull( wagon.getProxyInfo() );
        assertEquals( httpProxyInfo, wagon.getProxyInfo( "http", "www.example.com" ) );
        assertEquals( httpProxyInfo, wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertEquals( socksProxyInfo, wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
    }

    public void testSessionOpenEvents()
        throws Exception
    {
        Repository repository = new Repository();

        sessionListenerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        sessionListener.sessionOpening( null );
        sessionListener.sessionOpened( null );
        sessionListenerControl.replay();
        
        wagon.connect( repository );
        
        sessionListenerControl.verify();

        assertEquals( repository, wagon.getRepository() );
    }

    public void testSessionCloseEvents()
        throws Exception
    {
        sessionListenerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        sessionListener.sessionDisconnecting( null );
        sessionListener.sessionDisconnected( null );
        sessionListenerControl.replay();
        
        wagon.disconnect();
        
        sessionListenerControl.verify();
    }

    public void testGetTransferEvents()
        throws Exception
    {
        transferListener.debug( "fetch debug message" );
        transferListenerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        transferListener.transferInitiated( null );
        transferListener.transferStarted( null );
        transferListener.debug( null );
        transferListenerControl.setVoidCallable( MockControl.ZERO_OR_MORE );
        transferListener.transferProgress( null, null, 0 );
        transferListenerControl.setVoidCallable( 5 );
        transferListener.transferCompleted( null );
        transferListenerControl.replay();
        
        wagon.fireTransferDebug( "fetch debug message" );

        Repository repository = new Repository();
        wagon.connect( repository );

        wagon.get( artifact, destination );
        
        transferListenerControl.verify();
    }

    public void testGetError()
        throws Exception
    {
        transferListenerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        transferListener.transferInitiated( null );
        transferListener.transferStarted( null );        
        transferListener.debug( null );
        transferListenerControl.setVoidCallable( MockControl.ZERO_OR_MORE );
        transferListener.transferError( null );
        transferListenerControl.replay();
        
        try
        {
            Repository repository = new Repository();

            WagonMock wagon = new WagonMock( true );

            wagon.addTransferListener( transferListener );

            wagon.connect( repository );

            wagon.get( artifact, destination );

            fail( "Transfer error was expected during deploy" );
        }
        catch ( TransferFailedException expected )
        {
            assertTrue( true );
        }

        transferListenerControl.verify();
    }

    public void testPutTransferEvents()
        throws ConnectionException, AuthenticationException, ResourceDoesNotExistException, TransferFailedException,
        AuthorizationException
    {
        transferListener.debug( "deploy debug message" );
        transferListenerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        transferListener.transferInitiated( null );
        transferListener.transferStarted( null );
        transferListener.transferProgress( null, null, 0 );
        transferListener.transferCompleted( null );
        transferListenerControl.replay();
        
        wagon.fireTransferDebug( "deploy debug message" );

        Repository repository = new Repository();

        wagon.connect( repository );

        wagon.put( source, artifact );

        transferListenerControl.verify();
    }

    /*
    public void testPutError()
    {
        MockInputStream mockInputStream = new MockInputStream();

        //forced io error!
        mockInputStream.setForcedError( true );

        StreamSource result = new StreamSource( mockInputStream );

        PutRequest command = new PutRequest( result, "my favourite resource" );

        try
        {
            wagon.transfer( command );

            fail( "Transfer error was expected during fetch" );
        }
        catch ( Exception e )
        {
        }

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferErrorCalled() );

        assertFalse( transferListener.isTransferCompletedCalled() );
    }
    */

    public void testStreamShutdown()
    {
        IOUtil.close( (InputStream) null );

        IOUtil.close( (OutputStream) null );

        InputStreamMock inputStream = new InputStreamMock();

        assertFalse( inputStream.isClosed() );

        IOUtil.close( inputStream );

        assertTrue( inputStream.isClosed() );

        OutputStreamMock outputStream = new OutputStreamMock();

        assertFalse( outputStream.isClosed() );

        IOUtil.close( outputStream );

        assertTrue( outputStream.isClosed() );
    }
}
