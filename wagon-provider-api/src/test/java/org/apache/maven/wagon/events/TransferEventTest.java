package org.apache.maven.wagon.events;

import junit.framework.TestCase;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class TransferEventTest extends TestCase
{

    /*
	 * @see TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Constructor for TransferEventTest.
     * 
     * @param arg0 
     */
    public TransferEventTest( final String arg0 )
    {
        super( arg0 );
    }

    /*
	 * Class to test for void TransferEvent(Wagon, Repository, String, int,
	 * int)
	 */
    public void testTransferEventProperties()
    {

        final Wagon wagon = new FileWagon();
        final Repository repo = new Repository();
        try
        {
            wagon.connect( repo );
        }
        catch ( Exception e )
        {
          fail( e.getMessage() );
        }
        final long timestamp = System.currentTimeMillis();
        final Exception exception = new AuthenticationException( "dummy" );

        TransferEvent event =
                new TransferEvent(
                        wagon,
                        "mm",
                        TransferEvent.TRANSFER_COMPLETED,
                        TransferEvent.REQUEST_GET );

        assertEquals( wagon, event.getWagon() );
        assertEquals( repo, event.getWagon().getRepository() );
        assertEquals( "mm", event.getResource() );
        assertEquals( TransferEvent.TRANSFER_COMPLETED, event.getEventType() );
        assertEquals( TransferEvent.REQUEST_GET, event.getRequestType() );

        event = new TransferEvent( wagon,  "mm", exception );

        assertEquals( wagon, event.getWagon() );
        assertEquals( repo, event.getWagon().getRepository() );
        assertEquals( "mm", event.getResource() );
        assertEquals( TransferEvent.TRANSFER_ERROR, event.getEventType() );
        assertEquals( exception, event.getException() );



        event.setResource( null );
        assertEquals( null, event.getResource() );

        event.setResource( "/foo/baa" );
        assertEquals( "/foo/baa", event.getResource() );

        event.setException( null );
        assertEquals( null, event.getException() );

        event.setException( exception );
        assertEquals( exception, event.getException() );

        event.setTimestamp( timestamp );
        assertEquals( timestamp, event.getTimestamp() );

        event.setRequestType( TransferEvent.REQUEST_PUT );
        assertEquals( TransferEvent.REQUEST_PUT, event.getRequestType() );

        event.setRequestType( TransferEvent.REQUEST_GET );
        assertEquals( TransferEvent.REQUEST_GET, event.getRequestType() );

        try
        {
            event.setRequestType( -1 );
            fail( "Exception expected" );
        }
        catch ( IllegalArgumentException e )
        {
        }

        event.setEventType( TransferEvent.TRANSFER_COMPLETED );
        assertEquals( TransferEvent.TRANSFER_COMPLETED, event.getEventType() );

        event.setEventType( TransferEvent.TRANSFER_ERROR );
        assertEquals( TransferEvent.TRANSFER_ERROR, event.getEventType() );

        event.setEventType( TransferEvent.TRANSFER_STARTED );
        assertEquals( TransferEvent.TRANSFER_STARTED, event.getEventType() );

        event.setEventType( TransferEvent.TRANSFER_PROGRESS );
        assertEquals( TransferEvent.TRANSFER_PROGRESS, event.getEventType() );

        try
        {
            event.setEventType( -1 );
            fail( "Exception expected" );
        }
        catch ( IllegalArgumentException e )
        {
        }


        event.setProgress( ( long ) 10 );
        assertEquals( ( long ) 10, event.getProgress() );

    }

    public void testConstantValueConflict()
    {
        final int[] values =
                {
                    TransferEvent.TRANSFER_COMPLETED,
                    TransferEvent.TRANSFER_ERROR,
                    TransferEvent.TRANSFER_STARTED,
                    TransferEvent.TRANSFER_PROGRESS,
                    TransferEvent.REQUEST_GET,
                    TransferEvent.REQUEST_PUT};

        for ( int i = 0; i < values.length; i++ )
        {
            for ( int j = i + 1; j < values.length; j++ )
            {

                final String msg =
                        "Value confict at [i,j]=[" + i + "," + j + "]";
                assertTrue( msg, values[i] != values[j] );
            }
        }

    }

}
