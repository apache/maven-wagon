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

import junit.framework.TestCase;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListenerMock;
import org.apache.maven.wagon.events.TransferListenerMock;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class AbstractWagonTest
    extends TestCase
{
    private String basedir;

    private WagonMock wagon = null;

    private File destination;

    private File source;

    private String artifact;

    private SessionListenerMock sessionListener = null;

    private TransferListenerMock transferListener = null;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        destination = new File( basedir, "target/folder/subfolder" );

        source = new File( basedir, "pom.xml" );

        wagon = new WagonMock();

        sessionListener = new SessionListenerMock();

        wagon.addSessionListener( sessionListener );

        transferListener = new TransferListenerMock();

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

    public void testSessionOpenEvents()
        throws Exception
    {
        Repository repository = new Repository();

        wagon.connect( repository );

        assertEquals( repository, wagon.getRepository() );

        assertTrue( sessionListener.isSessionOpenningCalled() );

        assertTrue( sessionListener.isSessionOpenedCalled() );

        //!!
        //assertTrue( sessionListener.isSessionLoggedInCalled() );

        //!!
        //assertTrue( sessionListener.isSessionRefusedCalled() );

    }

    public void testSessionCloseEvents()
        throws Exception
    {
        wagon.disconnect();

        //!!
        //assertTrue( sessionListener.isSessionLoggedOffCalled() );

        assertTrue( sessionListener.isSessionDisconnectingCalled() );

        assertTrue( sessionListener.isSessionDisconnectedCalled() );
    }

    public void testGetTransferEvents()
        throws Exception
    {
        wagon.fireTransferDebug( "fetch debug message" );

        Repository repository = new Repository();

        wagon.connect( repository );

        assertEquals( "fetch debug message", transferListener.getDebugMessage() );

        assertTrue( transferListener.isDebugCalled() );

        wagon.get( artifact, destination );

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferCompletedCalled() );

        assertTrue( transferListener.isTransferProgressCalled() );

        assertEquals( 5, transferListener.getNumberOfProgressCalls() );
    }

    public void testGetError()
        throws Exception
    {
        TransferListenerMock transferListener = new TransferListenerMock();

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

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferErrorCalled() );

        assertFalse( transferListener.isTransferCompletedCalled() );
    }

    public void testPutTransferEvents()
        throws ConnectionException, AuthenticationException, ResourceDoesNotExistException, TransferFailedException,
        AuthorizationException
    {
        wagon.fireTransferDebug( "deploy debug message" );

        Repository repository = new Repository();

        wagon.connect( repository );

        wagon.put( source, artifact );

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferCompletedCalled() );

        assertTrue( transferListener.isDebugCalled() );

        assertTrue( transferListener.isTransferProgressCalled() );

        assertEquals( "deploy debug message", transferListener.getDebugMessage() );

        //!!
        //assertEquals( 5, transferListener.getNumberOfProgressCalls() );
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
