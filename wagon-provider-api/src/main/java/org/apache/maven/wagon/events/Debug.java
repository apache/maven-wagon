package org.apache.maven.wagon.events;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class Debug
    implements SessionListener, TransferListener
{
    long timestamp;
    long transfer;

    /**
     * @see SessionListener#sessionOpening(SessionEvent)
     */
    public void sessionOpening( final SessionEvent sessionEvent )
    {
        //System.out.println( .getUrl() + " - Session: Opening  ");
    }

    /**
     * @see SessionListener#sessionOpened(SessionEvent)
     */
    public void sessionOpened( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Opened  " );
    }

    /**
     * @see SessionListener#sessionDisconnecting(SessionEvent)
     */
    public void sessionDisconnecting( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Disconnecting  " );

    }

    /**
     * @see SessionListener#sessionDisconnected(SessionEvent)
     */
    public void sessionDisconnected( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Disconnected" );
    }

    /**
     * @see SessionListener#sessionConnectionRefused(SessionEvent)
     */
    public void sessionConnectionRefused( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Connection refused" );

    }

    /**
     * @see SessionListener#sessionLoggedIn(SessionEvent)
     */
    public void sessionLoggedIn( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Logged in" );

    }

    /**
     * @see SessionListener#sessionLoggedOff(SessionEvent)
     */
    public void sessionLoggedOff( final SessionEvent sessionEvent )
    {
        System.out.println(
            sessionEvent.getWagon().getRepository().getUrl() +
            " - Session: Logged off" );

    }

    /**
     * @see TransferListener#debug(String)
     */
    public void debug( final String message )
    {
        System.out.println( message );

    }

    /**
     * @see TransferListener#transferStarted(TransferEvent)
     */
    public void transferStarted( final TransferEvent transferEvent )
    {
        timestamp = transferEvent.getTimestamp();
        transfer = 0;
        if( transferEvent.getRequestType() == TransferEvent.REQUEST_GET )
        {
            final String message = "Downloading: " +
                transferEvent.getResource() +
                " from " +
                transferEvent.getWagon().getRepository().getUrl();
            System.out.println( message );
            System.out.println( "" );
        }
        else
        {
            final String message = "Uploading: " +
                transferEvent.getResource() +
                " to " +
                transferEvent.getWagon().getRepository().getUrl();
            System.out.println( message );
            System.out.println( "" );

        }
    }

    /**
     * @see TransferListener#transferProgress(TransferEvent)
     */
    public void transferProgress( final TransferEvent transferEvent )
    {

        System.out.print( "#" );
        //String data = new String( transferEvent.getData(),0, transferEvent.getDataLength());
        //System.out.println(data);
        transfer += transferEvent.getProgress();
    }

    /**
     * @see TransferListener#transferCompleted(TransferEvent)
     */
    public void transferCompleted( final TransferEvent transferEvent )
    {
        final double duration =
            (double)( transferEvent.getTimestamp() - timestamp ) / 1000;

        System.out.println();
        final String message = "Transfer finished. " +
            transfer +
            " bytes copied in " +
            duration +
            " seconds";
        System.out.println( message );

    }

    /**
     * @see TransferListener#transferError(TransferEvent)
     */
    public void transferError( final TransferEvent transferEvent )
    {
        System.out.println( " Transfer error: " + transferEvent.getException() );

    }

    /**
     * @see SessionListener#sessionError(SessionEvent)
     */
    public void sessionError( final SessionEvent sessionEvent )
    {
        System.out.println( " Session error: " + sessionEvent.getException() );

    }

}
