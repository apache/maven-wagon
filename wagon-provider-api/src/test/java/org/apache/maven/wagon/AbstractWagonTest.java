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
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.easymock.IAnswer;

import static org.easymock.EasyMock.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class AbstractWagonTest
    extends TestCase
{
    private static class TestWagon
        extends AbstractWagon
    {
        protected void closeConnection()
            throws ConnectionException
        {
        }

        protected void openConnectionInternal()
            throws ConnectionException, AuthenticationException
        {
        }

        public void get( String resourceName, File destination )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
        }

        public boolean getIfNewer( String resourceName, File destination, long timestamp )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
            return false;
        }

        public void put( File source, String destination )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
        }
    }

    private String basedir;

    private WagonMock wagon = null;

    private File destination;

    private File source;

    private String artifact;

    private SessionListener sessionListener = null;

    private TransferListener transferListener = null;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        destination = new File( basedir, "target/folder/subfolder" );

        source = new File( basedir, "pom.xml" );

        wagon = new WagonMock();

        sessionListener = createMock( SessionListener.class );

        wagon.addSessionListener( sessionListener );

        transferListener = createMock( TransferListener.class );

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

    public void testNoProxyConfiguration()
        throws ConnectionException, AuthenticationException
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

    public void testNullProxyConfiguration()
        throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository();
        wagon.connect( repository, (ProxyInfo) null );
        assertNull( wagon.getProxyInfo() );
        assertNull( wagon.getProxyInfo( "http", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "http", "localhost" ) );

        wagon.connect( repository );
        assertNull( wagon.getProxyInfo() );
        assertNull( wagon.getProxyInfo( "http", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "http", "localhost" ) );

        wagon.connect( repository, new AuthenticationInfo() );
        assertNull( wagon.getProxyInfo() );
        assertNull( wagon.getProxyInfo( "http", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "dav", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "scp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "ftp", "www.example.com" ) );
        assertNull( wagon.getProxyInfo( "http", "localhost" ) );
    }

    public void testLegacyProxyConfiguration()
        throws ConnectionException, AuthenticationException
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

    public void testProxyConfiguration()
        throws ConnectionException, AuthenticationException
    {
        final ProxyInfo httpProxyInfo = new ProxyInfo();
        httpProxyInfo.setType( "http" );

        final ProxyInfo socksProxyInfo = new ProxyInfo();
        socksProxyInfo.setType( "http" );

        ProxyInfoProvider proxyInfoProvider = new ProxyInfoProvider()
        {
            public ProxyInfo getProxyInfo( String protocol )
            {
                if ( "http".equals( protocol ) || "dav".equals( protocol ) )
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

        sessionListener.sessionOpening( anyObject( SessionEvent.class ) );
        sessionListener.sessionOpened( anyObject( SessionEvent.class ) );
        replay( sessionListener );

        wagon.connect( repository );

        verify( sessionListener );

        assertEquals( repository, wagon.getRepository() );
    }

    public void testSessionConnectionRefusedEventConnectionException()
        throws Exception
    {
        final WagonException exception = new ConnectionException( "" );

        try
        {
            runTestSessionConnectionRefusedEvent( exception );
            fail();
        }
        catch ( ConnectionException e )
        {
            assertTrue( true );
        }
    }

    public void testSessionConnectionRefusedEventAuthenticationException()
        throws Exception
    {
        final WagonException exception = new AuthenticationException( "" );

        try
        {
            runTestSessionConnectionRefusedEvent( exception );
            fail();
        }
        catch ( AuthenticationException e )
        {
            assertTrue( true );
        }
    }

    private void runTestSessionConnectionRefusedEvent( final WagonException exception )
        throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository();

        sessionListener.sessionOpening( anyObject( SessionEvent.class ) );
        sessionListener.sessionConnectionRefused( anyObject( SessionEvent.class ) );
        replay( sessionListener );

        Wagon wagon = new TestWagon()
        {
            protected void openConnectionInternal()
                throws ConnectionException, AuthenticationException
            {
                if ( exception instanceof ConnectionException )
                {
                    throw (ConnectionException) exception;
                }
                if ( exception instanceof AuthenticationException )
                {
                    throw (AuthenticationException) exception;
                }
            }
        };
        wagon.addSessionListener( sessionListener );

        try
        {
            wagon.connect( repository );
            fail();
        }
        finally
        {
            verify( sessionListener );

            assertEquals( repository, wagon.getRepository() );
        }
    }

    public void testSessionCloseEvents()
        throws Exception
    {
        sessionListener.sessionDisconnecting( anyObject( SessionEvent.class ) );
        sessionListener.sessionDisconnected( anyObject( SessionEvent.class ) );
        replay( sessionListener );

        wagon.disconnect();

        verify( sessionListener );
    }

    public void testSessionCloseRefusedEventConnectionException()
        throws Exception
    {
        Repository repository = new Repository();

        sessionListener.sessionDisconnecting( anyObject( SessionEvent.class ) );
        sessionListener.sessionError( anyObject( SessionEvent.class ) );
        replay( sessionListener );

        Wagon wagon = new TestWagon()
        {
            protected void closeConnection()
                throws ConnectionException
            {
                throw new ConnectionException( "" );
            }
        };
        wagon.addSessionListener( sessionListener );

        try
        {
            wagon.disconnect();
            fail();
        }
        catch ( ConnectionException e )
        {
            assertTrue( true );
        }
        finally
        {
            verify( sessionListener );
        }
    }

    public void testGetTransferEvents()
        throws Exception
    {
        transferListener.debug( "fetch debug message" );
        transferListener.transferInitiated( anyObject( TransferEvent.class ) );
        transferListener.transferStarted( anyObject( TransferEvent.class ) );
        transferListener.debug( anyString() );
        expectLastCall().anyTimes();
        transferListener.transferProgress( anyObject( TransferEvent.class ), anyObject( byte[].class ), anyInt() );
        expectLastCall().times( 5 );
        transferListener.transferCompleted( anyObject( TransferEvent.class ) );
        replay( transferListener );

        wagon.fireTransferDebug( "fetch debug message" );

        Repository repository = new Repository();
        wagon.connect( repository );

        wagon.get( artifact, destination );

        verify( transferListener );
    }

    public void testGetError()
        throws Exception
    {
        transferListener.transferInitiated( anyObject( TransferEvent.class ) );
        transferListener.transferStarted( anyObject( TransferEvent.class ) );
        transferListener.debug( anyString() );
        expectLastCall().anyTimes();
        transferListener.transferError( anyObject( TransferEvent.class ) );
        replay( transferListener );

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

        verify( transferListener );
    }

    public void testPutTransferEvents()
        throws ConnectionException, AuthenticationException, ResourceDoesNotExistException, TransferFailedException,
        AuthorizationException
    {
        transferListener.debug( "deploy debug message" );
        transferListener.transferInitiated( anyObject( TransferEvent.class ) );
        transferListener.transferStarted( anyObject( TransferEvent.class ) );
        transferListener.transferProgress( anyObject( TransferEvent.class ), anyObject( byte[].class ), anyInt() );
        transferListener.transferCompleted( anyObject( TransferEvent.class ) );
        replay( transferListener );

        wagon.fireTransferDebug( "deploy debug message" );

        Repository repository = new Repository();

        wagon.connect( repository );

        wagon.put( source, artifact );

        verify( transferListener );
    }

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

    public void testRepositoryPermissionsOverride()
        throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository();

        RepositoryPermissions original = new RepositoryPermissions();
        original.setFileMode( "664" );
        repository.setPermissions( original );

        RepositoryPermissions override = new RepositoryPermissions();
        override.setFileMode( "644" );
        wagon.setPermissionsOverride( override );

        wagon.connect( repository );

        assertEquals( override, repository.getPermissions() );
        assertEquals( "644", repository.getPermissions().getFileMode() );
    }

    public void testRepositoryUserName()
        throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository( "id", "http://bporter:password@www.example.com/path/to/resource" );

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName( "brett" );
        authenticationInfo.setPassword( "pass" );
        wagon.connect( repository, authenticationInfo );

        assertEquals( authenticationInfo, wagon.getAuthenticationInfo() );
        assertEquals( "brett", authenticationInfo.getUserName() );
        assertEquals( "pass", authenticationInfo.getPassword() );
    }

    public void testRepositoryUserNameNotGivenInCredentials()
        throws ConnectionException, AuthenticationException
    {
        Repository repository = new Repository( "id", "http://bporter:password@www.example.com/path/to/resource" );

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        wagon.connect( repository, authenticationInfo );

        assertEquals( authenticationInfo, wagon.getAuthenticationInfo() );
        assertEquals( "bporter", authenticationInfo.getUserName() );
        assertEquals( "password", authenticationInfo.getPassword() );
    }

    public void testConnectNullRepository()
        throws ConnectionException, AuthenticationException
    {
        try
        {
            wagon.connect( null );
            fail();
        }
        catch ( IllegalStateException e )
        {
            assertTrue( true );
        }
    }

    public void testPostProcessListeners()
        throws TransferFailedException, IOException
    {
        File tempFile = File.createTempFile( "wagon", "tmp" );
        tempFile.deleteOnExit();
        String content = "content";
        FileUtils.fileWrite( tempFile.getAbsolutePath(), content );

        Resource resource = new Resource( "resource" );

        transferListener.transferInitiated( anyObject( TransferEvent.class ) );
        transferListener.transferStarted( anyObject( TransferEvent.class ) );
        TransferEvent event =
            new TransferEvent( wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT );
        event.setLocalFile( tempFile );
        transferListener.transferProgress( eq( event ), anyObject( byte[].class ), eq( content.length() ) );
        ProgressAnswer answer = new ProgressAnswer();
        expectLastCall().andAnswer( answer );
        transferListener.transferCompleted( anyObject( TransferEvent.class ) );
        replay( transferListener );

        wagon.postProcessListeners( resource, tempFile, TransferEvent.REQUEST_PUT );

        assertEquals( content.length(), answer.getSize() );
        assertEquals( new String( content.getBytes() ), new String( answer.getBytes() ) );

        tempFile.delete();
    }

    static final class ProgressAnswer implements IAnswer
    {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private int size;
        
        public Object answer() throws Throwable
        {
            byte[] buffer = (byte[]) getCurrentArguments()[1];
            int length = (Integer) getCurrentArguments()[2];
            baos.write( buffer, 0, length );
            size += length;
            return null;
        }

        public int getSize()
        {
            return size;
        }

        public byte[] getBytes()
        {
            return baos.toByteArray();
        }
    }
}
