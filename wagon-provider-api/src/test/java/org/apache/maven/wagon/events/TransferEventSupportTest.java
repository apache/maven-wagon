package org.apache.maven.wagon.events;

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
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.resource.Resource;
import org.easymock.EasyMock;

import static org.easymock.EasyMock.*;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public class TransferEventSupportTest
    extends TestCase
{
    private TransferEventSupport eventSupport;

    private Wagon wagon;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        eventSupport = new TransferEventSupport();
        
        // TODO: actually test it gets called?
        wagon = EasyMock.createNiceMock( Wagon.class );
    }

    public void testTransferListenerRegistration()
    {
        TransferListener mock1 = createMock( TransferListener.class );
        eventSupport.addTransferListener( mock1 );

        assertTrue( eventSupport.hasTransferListener( mock1 ) );
        TransferListener mock2 = createMock( TransferListener.class );

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
        TransferListener mock1 = createMock( TransferListener.class );
        eventSupport.addTransferListener( mock1 );

        TransferListener mock2 = createMock( TransferListener.class );
        eventSupport.addTransferListener( mock2 );

        final TransferEvent event = getEvent( wagon );

        mock1.transferStarted( event );
        mock2.transferStarted( event );

        replay( mock1, mock2 );

        eventSupport.fireTransferStarted( event );

        verify( mock1, mock2 );
    }

    public void testFireTransferProgress()
    {
        TransferListener mock1 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock1 );

        TransferListener mock2 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock2 );

        final TransferEvent event = getEvent( wagon );
        final byte[] buffer = "content".getBytes();

        mock1.transferProgress( event, buffer, 0 );
        mock2.transferProgress( event, buffer, 0 );

        replay( mock1, mock2 );

        eventSupport.fireTransferProgress( event, buffer , 0 );

        verify( mock1, mock2 );
    }

    public void testFireTransferCompleted()
    {
        TransferListener mock1 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock1 );

        TransferListener mock2 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock2 );

        final TransferEvent event = getEvent( wagon );

        mock1.transferCompleted( event );
        mock2.transferCompleted( event );

        replay( mock1, mock2 );

        eventSupport.fireTransferCompleted( event );

        verify( mock1, mock2 );
    }

    public void testFireTransferError()
    {
        TransferListener mock1 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock1 );

        TransferListener mock2 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock2 );

        final TransferEvent event = getEvent( wagon );
        
        mock1.transferError( event );
        mock2.transferError( event );

        replay( mock1, mock2 );

        eventSupport.fireTransferError( event );

        verify( mock1, mock2 );
    }

    public void testFireDebug()
    {
        TransferListener mock1 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock1 );

        TransferListener mock2 = createMock( TransferListener.class );

        eventSupport.addTransferListener( mock2 );

        mock1.debug( "mm" );
        mock2.debug( "mm" );

        replay( mock1, mock2 );
        
        eventSupport.fireDebug( "mm" );

        verify( mock1, mock2 );
    }

    private TransferEvent getEvent( final Wagon wagon )
    {
        return new TransferEvent( wagon, new Resource(), TransferEvent.TRANSFER_COMPLETED,
                                                       TransferEvent.REQUEST_GET );
    }

}
