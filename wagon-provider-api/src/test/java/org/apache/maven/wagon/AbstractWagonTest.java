package org.apache.maven.wagon;

import junit.framework.TestCase;
import org.apache.maven.wagon.artifact.Artifact;
import org.apache.maven.wagon.artifact.DefaultArtifact;
import org.apache.maven.wagon.events.MockSessionListener;
import org.apache.maven.wagon.events.MockTransferListener;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class AbstractWagonTest extends TestCase
{
    private String basedir;
    private MockWagon wagon = null;
    private Artifact artifact;
    private File destination;
    private File source;
    private MockSessionListener sessionListener = null;
    private MockTransferListener transferListener = null;

    protected void setUp() throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        artifact = new DefaultArtifact( "groupId", "artifactId", "version", "type", "extension" );

        destination = new File( basedir, "folder/subfolder" );

        source = new File( basedir, "project.xml" );

        wagon = new MockWagon();

        sessionListener = new MockSessionListener();

        wagon.addSessionListener( sessionListener );

        transferListener = new MockTransferListener();

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
    {
        wagon.fireTransferDebug( "fetch debug message" );

        try
        {
            Repository repository = new Repository();

            wagon.connect( repository );

            wagon.get( artifact, destination );
        }

        catch ( Exception e )
        {
            e.printStackTrace();

            fail( e.getMessage() );
        }

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferCompletedCalled() );

        assertTrue( transferListener.isDebugCalled() );

        assertTrue( transferListener.isTransferProgressCalled() );

        assertEquals( "fetch debug message", transferListener.getDebugMessage() );

        assertEquals( 5, transferListener.getNumberOfProgressCalls() );
    }

    public void testGetError()
    {
        MockTransferListener transferListener = new MockTransferListener();

        try
        {
            Repository repository = new Repository();

            MockWagon wagon = new MockWagon( true );

            wagon.addTransferListener( transferListener );

            wagon.connect( repository );

            wagon.get( artifact, destination );

            fail( "Transfer error was expected during deploy" );
        }
        catch ( Exception e )
        {
        }

        assertTrue( transferListener.isTransferStartedCalled() );

        assertTrue( transferListener.isTransferErrorCalled() );

        assertFalse( transferListener.isTransferCompletedCalled() );
    }

    public void testPutTransferEvents()
    {
        wagon.fireTransferDebug( "deploy debug message" );

        try
        {
            Repository repository = new Repository();

            wagon.connect( repository );

            wagon.put( source, artifact );
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

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
        wagon.shutdownStream( (InputStream) null );

        wagon.shutdownStream( (OutputStream) null );

        MockInputStream inputStream = new MockInputStream();

        assertFalse( inputStream.isClosed() );

        wagon.shutdownStream( inputStream );

        assertTrue( inputStream.isClosed() );

        MockOutputStream outputStream = new MockOutputStream();

        assertFalse( outputStream.isClosed() );

        wagon.shutdownStream( outputStream );

        assertTrue( outputStream.isClosed() );
    }
}
