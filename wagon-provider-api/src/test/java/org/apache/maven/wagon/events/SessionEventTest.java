package org.apache.maven.wagon.events;

/*
 * ====================================================================
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The end-user documentation
 * included with the redistribution, if any, must include the following
 * acknowledgment: "This product includes software developed by the Apache
 * Software Foundation (http://www.apache.org/)." Alternately, this
 * acknowledgment may appear in the software itself, if and wherever such
 * third-party acknowledgments normally appear. 4. The names "Apache" and
 * "Apache Software Foundation" and "Apache Maven" must not be used to endorse
 * or promote products derived from this software without prior written
 * permission. For written permission, please contact apache@apache.org. 5.
 * Products derived from this software may not be called "Apache", "Apache
 * Maven", nor may "Apache" appear in their name, without prior written
 * permission of the Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation. For more information on the
 * Apache Software Foundation, please see <http://www.apache.org/> .
 * 
 * ====================================================================
 */

import junit.framework.TestCase;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.MockWagon;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class SessionEventTest extends TestCase
{

    /*
	 * @see TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Constructor for SESSIONEventTest.
     * 
     * @param arg0 
     */
    public SessionEventTest( final String arg0 )
    {
        super( arg0 );
    }

    /*
	 * Class to test for void SESSIONEvent(Wagon, Repository, String, int,
	 * int)
	 */
    public void testSessionEventProperties()
    {

        final Wagon wagon = new MockWagon();
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

        SessionEvent event =
                new SessionEvent(
                        wagon,
                        SessionEvent.SESSION_CLOSED );

        assertEquals( wagon, event.getWagon() );
        assertEquals( repo, event.getWagon().getRepository() );

        assertEquals( SessionEvent.SESSION_CLOSED, event.getEventType() );


        event = new SessionEvent( wagon, exception );

        assertEquals( wagon, event.getWagon() );
        assertEquals( repo, event.getWagon().getRepository() );
        assertEquals( SessionEvent.SESSION_ERROR_OCCURRED, event.getEventType() );
        assertEquals( exception, event.getException() );




        event.setException( null );
        assertEquals( null, event.getException() );

        event.setException( exception );
        assertEquals( exception, event.getException() );

        event.setTimestamp( timestamp );
        assertEquals( timestamp, event.getTimestamp() );

        event.setEventType( SessionEvent.SESSION_CLOSED );
        assertEquals( SessionEvent.SESSION_CLOSED, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_DISCONNECTED );
        assertEquals( SessionEvent.SESSION_DISCONNECTED, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_DISCONNECTING );
        assertEquals( SessionEvent.SESSION_DISCONNECTING, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_ERROR_OCCURRED );
        assertEquals( SessionEvent.SESSION_ERROR_OCCURRED, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_LOGGED_IN );
        assertEquals( SessionEvent.SESSION_LOGGED_IN, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_LOGGED_OFF );
        assertEquals( SessionEvent.SESSION_LOGGED_OFF, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_OPENED );
        assertEquals( SessionEvent.SESSION_OPENED, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_OPENING );
        assertEquals( SessionEvent.SESSION_OPENING, event.getEventType() );

        event.setEventType( SessionEvent.SESSION_CONNECTION_REFUSED );
        assertEquals( SessionEvent.SESSION_CONNECTION_REFUSED, event.getEventType() );


        try
        {
            event.setEventType( -1 );
            fail( "Exception expected" );
        }
        catch ( IllegalArgumentException e )
        {
        }


    }

    public void testConstantValueConflict()
    {
        final int[] values =
                {
                    SessionEvent.SESSION_CLOSED,
                    SessionEvent.SESSION_DISCONNECTED,
                    SessionEvent.SESSION_DISCONNECTING,
                    SessionEvent.SESSION_ERROR_OCCURRED,
                    SessionEvent.SESSION_LOGGED_IN,
                    SessionEvent.SESSION_LOGGED_OFF,
                    SessionEvent.SESSION_OPENED,
                    SessionEvent.SESSION_OPENING,
                    SessionEvent.SESSION_CONNECTION_REFUSED};

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
