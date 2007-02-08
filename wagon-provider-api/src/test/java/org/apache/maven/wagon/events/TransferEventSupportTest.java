package org.apache.maven.wagon.events;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.wagon.WagonMock;
import org.apache.maven.wagon.Wagon;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: TransferEventSupportTest.java,v 1.2 2003/11/16 12:41:02 michal
 *          Exp $
 */
public class TransferEventSupportTest
    extends TestCase
{

    private TransferEventSupport eventSupport;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {

        super.setUp();
        eventSupport = new TransferEventSupport();

    }

    public void testTransferListenerRegistration()
    {
        final TransferListenerMock mock1 = new TransferListenerMock();
        eventSupport.addTransferListener( mock1 );

        assertTrue( eventSupport.hasTransferListener( mock1 ) );
        final TransferListenerMock mock2 = new TransferListenerMock();

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
        final TransferListenerMock mock1 = new TransferListenerMock();
        eventSupport.addTransferListener( mock1 );

        final TransferListenerMock mock2 = new TransferListenerMock();
        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new WagonMock();
        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferStarted( event );

        assertTrue( mock1.isTransferStartedCalled() );
        assertTrue( mock2.isTransferStartedCalled() );
        assertEquals( event, mock1.getTransferEvent() );
        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireTransferProgress()
    {
        final TransferListenerMock mock1 = new TransferListenerMock();

        eventSupport.addTransferListener( mock1 );

        final TransferListenerMock mock2 = new TransferListenerMock();

        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new WagonMock();

        final TransferEvent event = getEvent( wagon );

        // TODO: should be testing the buffer
        eventSupport.fireTransferProgress( event, null, 0 );

        assertTrue( mock1.isTransferProgressCalled() );

        assertTrue( mock2.isTransferProgressCalled() );

        assertEquals( event, mock1.getTransferEvent() );

        assertEquals( event, mock2.getTransferEvent() );
    }

    public void testFireTransferCompleted()
    {
        final TransferListenerMock mock1 = new TransferListenerMock();

        eventSupport.addTransferListener( mock1 );

        final TransferListenerMock mock2 = new TransferListenerMock();

        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new WagonMock();

        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferCompleted( event );

        assertTrue( mock1.isTransferCompletedCalled() );

        assertTrue( mock2.isTransferCompletedCalled() );

        assertEquals( event, mock1.getTransferEvent() );

        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireTransferError()
    {
        final TransferListenerMock mock1 = new TransferListenerMock();

        eventSupport.addTransferListener( mock1 );

        final TransferListenerMock mock2 = new TransferListenerMock();

        eventSupport.addTransferListener( mock2 );

        final Wagon wagon = new WagonMock();

        final TransferEvent event = getEvent( wagon );

        eventSupport.fireTransferError( event );

        assertTrue( mock1.isTransferErrorCalled() );

        assertTrue( mock2.isTransferErrorCalled() );

        assertEquals( event, mock1.getTransferEvent() );

        assertEquals( event, mock2.getTransferEvent() );

    }

    public void testFireDebug()
    {
        final TransferListenerMock mock1 = new TransferListenerMock();

        eventSupport.addTransferListener( mock1 );

        final TransferListenerMock mock2 = new TransferListenerMock();

        eventSupport.addTransferListener( mock2 );

        eventSupport.fireDebug( "mm" );

        assertTrue( mock1.isDebugCalled() );

        assertTrue( mock2.isDebugCalled() );

        assertEquals( "mm", mock1.getDebugMessage() );

        assertEquals( "mm", mock2.getDebugMessage() );

    }

    private TransferEvent getEvent( final Wagon wagon )
    {
        final TransferEvent event = new TransferEvent( wagon, null, null, TransferEvent.TRANSFER_COMPLETED,
                                                       TransferEvent.REQUEST_GET );
        return event;
    }

}
