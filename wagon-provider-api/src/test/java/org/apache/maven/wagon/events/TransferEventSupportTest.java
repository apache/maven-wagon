package org.apache.maven.wagon.events;

import junit.framework.TestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: TransferEventSupportTest.java,v 1.2 2003/11/16 12:41:02 michal
 *          Exp $
 */
public class TransferEventSupportTest extends TestCase
{

    private TransferEventSupport eventSupport;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {

        super.setUp();
        eventSupport = new TransferEventSupport();

    }

    public void testTransferListenerRegistration()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        assertTrue( eventSupport.hasTransferListener( mock1 ) );
        final MockTransferListener mock2 = new MockTransferListener();

        assertFalse( eventSupport.hasTransferListener( mock2 ) );

        eventSupport.addTransferListener( mock2 );

        assertTrue( eventSupport.hasTransferListener( mock1 ) );
        assertTrue( eventSupport.hasTransferListener( mock2 ) );

        eventSupport.removeTransferListener( mock2 );

        assertTrue( eventSupport.hasTransferListener( mock1 ) );
        assertFalse( eventSupport.hasTransferListener( mock2 ) );

        eventSupport.removeTransferListener( mock1 );
        assertFalse( eventSupport.hasTransferListener( mock1 ) );
    }

    public void testFireTransferStarted()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        final MockTransferListener mock2 = new MockTransferListener();
        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new FileWagon();
        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferStarted( event );

        assertTrue( mock1.isTransferStartedCalled() );
        assertTrue( mock2.isTransferStartedCalled() );
        assertEquals( event, mock1.getTransferEvent() );
        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireTransferProgress()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        final MockTransferListener mock2 = new MockTransferListener();
        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new FileWagon();
        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferProgress( event );

        assertTrue( mock1.isTransferProgressCalled() );
        assertTrue( mock2.isTransferProgressCalled() );
        assertEquals( event, mock1.getTransferEvent() );
        assertEquals( event, mock2.getTransferEvent() );
    }

    public void testFireTransferCompleted()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        final MockTransferListener mock2 = new MockTransferListener();
        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new FileWagon();
        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferCompleted( event );

        assertTrue( mock1.isTransferCompletedCalled() );
        assertTrue( mock2.isTransferCompletedCalled() );
        assertEquals( event, mock1.getTransferEvent() );
        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireTransferError()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        final MockTransferListener mock2 = new MockTransferListener();
        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new FileWagon();
        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferError( event );

        assertTrue( mock1.isTransferErrorCalled() );
        assertTrue( mock2.isTransferErrorCalled() );
        assertEquals( event, mock1.getTransferEvent() );
        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireDebug()
    {
        final MockTransferListener mock1 = new MockTransferListener();
        eventSupport.addTransferListener( mock1 );

        final MockTransferListener mock2 = new MockTransferListener();
        eventSupport.addTransferListener( mock2 );

        eventSupport.fireDebug( "mm" );

        assertTrue( mock1.isDebugCalled() );
        assertTrue( mock2.isDebugCalled() );
        assertEquals( "mm", mock1.getDebugMessage() );
        assertEquals( "mm", mock2.getDebugMessage() );

    }

    private TransferEvent getEvent( final Wagon wagon )
    {
        final TransferEvent event =
                new TransferEvent(
                        wagon,
                        null,
                        TransferEvent.TRANSFER_COMPLETED,
                        TransferEvent.REQUEST_GET );
        return event;
    }

}
