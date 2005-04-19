package org.apache.maven.wagon.observers;

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

import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

import java.io.PrintStream;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class Debug
    implements SessionListener, TransferListener
{
    private PrintStream out;

    long timestamp;

    long transfer;


    public Debug()
    {
        this( System.out );
    }

    public Debug( PrintStream out )
    {
        this.out = out;
    }

    /**
     * @see SessionListener#sessionOpening(SessionEvent)
     */
    public void sessionOpening( final SessionEvent sessionEvent )
    {
        //out.println( .getUrl() + " - Session: Opening  ");
    }

    /**
     * @see SessionListener#sessionOpened(SessionEvent)
     */
    public void sessionOpened( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Opened  " );
    }

    /**
     * @see SessionListener#sessionDisconnecting(SessionEvent)
     */
    public void sessionDisconnecting( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Disconnecting  " );

    }

    /**
     * @see SessionListener#sessionDisconnected(SessionEvent)
     */
    public void sessionDisconnected( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Disconnected" );
    }

    /**
     * @see SessionListener#sessionConnectionRefused(SessionEvent)
     */
    public void sessionConnectionRefused( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Connection refused" );

    }

    /**
     * @see SessionListener#sessionLoggedIn(SessionEvent)
     */
    public void sessionLoggedIn( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Logged in" );

    }

    /**
     * @see SessionListener#sessionLoggedOff(SessionEvent)
     */
    public void sessionLoggedOff( final SessionEvent sessionEvent )
    {
        out.println( sessionEvent.getWagon().getRepository().getUrl() + " - Session: Logged off" );

    }

    /**
     * @see TransferListener#debug(String)
     */
    public void debug( final String message )
    {
        out.println( message );

    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        // This space left intentionally blank
    }

    /**
     * @see TransferListener#transferStarted(TransferEvent)
     */
    public void transferStarted( final TransferEvent transferEvent )
    {
        timestamp = transferEvent.getTimestamp();

        transfer = 0;

        if ( transferEvent.getRequestType() == TransferEvent.REQUEST_GET )
        {
            final String message = "Downloading: " + transferEvent.getResource().getName() + " from " +
                transferEvent.getWagon().getRepository().getUrl();

            out.println( message );

            out.println( "" );
        }
        else
        {
            final String message = "Uploading: " + transferEvent.getResource().getName() + " to " +
                transferEvent.getWagon().getRepository().getUrl();

            out.println( message );

            out.println( "" );

        }
    }

    /**
     * @see TransferListener#transferProgress(TransferEvent,byte[],int)
     */
    public void transferProgress( final TransferEvent transferEvent, byte[] buffer, int length )
    {

        out.print( "#" );
        //String data = new String( transferEvent.getData(),0, transferEvent.getDataLength());
        //out.println(data);
        transfer += length;
    }

    /**
     * @see TransferListener#transferCompleted(TransferEvent)
     */
    public void transferCompleted( final TransferEvent transferEvent )
    {
        final double duration = (double) ( transferEvent.getTimestamp() - timestamp ) / 1000;

        out.println();

        final String message = "Transfer finished. " + transfer + " bytes copied in " + duration + " seconds";

        out.println( message );

    }

    /**
     * @see TransferListener#transferError(TransferEvent)
     */
    public void transferError( final TransferEvent transferEvent )
    {
        out.println( " Transfer error: " + transferEvent.getException() );

    }

    /**
     * @see SessionListener#sessionError(SessionEvent)
     */
    public void sessionError( final SessionEvent sessionEvent )
    {
        out.println( " Session error: " + sessionEvent.getException() );

    }

}
