package org.apache.maven.wagon.events;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.MockWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class TransferEventTest
    extends TestCase
{

    public TransferEventTest( final String name )
    {
        super( name );
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }


    /*
     * Class to test for void TransferEvent(Wagon, Repository, String, int,
     * int)
    */
    public void testTransferEventProperties()
        throws ConnectionException, AuthenticationException
    {

        final Wagon wagon = new MockWagon();

        final Repository repo = new Repository();

        wagon.connect( repo );

        final long timestamp = System.currentTimeMillis();

        final Exception exception = new AuthenticationException( "dummy" );

        Resource resource = new Resource();

        resource.setName( "mm" );

        TransferEvent event = new TransferEvent( wagon, resource, TransferEvent.TRANSFER_COMPLETED,
                                                 TransferEvent.REQUEST_GET );

        assertEquals( wagon, event.getWagon() );

        assertEquals( repo, event.getWagon().getRepository() );

        assertEquals( "mm", event.getResource().getName() );

        assertEquals( TransferEvent.TRANSFER_COMPLETED, event.getEventType() );

        assertEquals( TransferEvent.REQUEST_GET, event.getRequestType() );

        Resource res = new Resource();

        res.setName( "mm" );

        event = new TransferEvent( wagon, res, exception, TransferEvent.REQUEST_GET );

        assertEquals( wagon, event.getWagon() );

        assertEquals( repo, event.getWagon().getRepository() );

        assertEquals( "mm", event.getResource().getName() );

        assertEquals( TransferEvent.TRANSFER_ERROR, event.getEventType() );

        assertEquals( TransferEvent.REQUEST_GET, event.getRequestType() );

        assertEquals( exception, event.getException() );

        event.setResource( null );

        assertEquals( null, event.getResource() );

        res.setName( "/foo/baa" );

        event.setResource( res );

        assertEquals( "/foo/baa", event.getResource().getName() );

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
            assertTrue( true );
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
            assertTrue( true );
        }

    }

    public void testConstantValueConflict()
    {
        final int[] values = {TransferEvent.TRANSFER_COMPLETED, TransferEvent.TRANSFER_ERROR,
                              TransferEvent.TRANSFER_STARTED, TransferEvent.TRANSFER_PROGRESS,
                              TransferEvent.REQUEST_GET, TransferEvent.REQUEST_PUT};

        for ( int i = 0; i < values.length; i++ )
        {
            for ( int j = i + 1; j < values.length; j++ )
            {

                final String msg = "Value confict at [i,j]=[" + i + "," + j + "]";

                assertTrue( msg, values[i] != values[j] );
            }
        }

    }

}
