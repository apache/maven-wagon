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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The class allows registration and removal of event listners of type
 * TransferListener and dispatch of those events to those listeners
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public final class TransferEventSupport
{

    /** registred listeners */
    private final List listeners = new ArrayList();

    /**
     * Adds the listener to the collection of listeners
     * who will be notifed when any transfer event occurs
     * in this <code>Wagon</code> object.
     * <br/>
     * If listener is <code>null</code>, no exception is thrown and no action is performed
     *
     * @param listener the transfer listener
     *
     * @see #removeTransferListener(org.apache.maven.wagon.events.TransferListener)
     * @see TransferListener
     */
    public void addTransferListener( final TransferListener listener )
    {
        listeners.add( listener );
    }

     /**
     * Removes the transfer listener from the collection of listeners so
     * it no longer receives transfer events.
     * <br/>
     * If listener is <code>null</code> or specified listener was not added
     * to this <code>TransferEventSupport</code> object
     * no exception is thrown and no action is performed
     *
     * @param listener the transfer listener
     *
     * @see #addTransferListener(TransferListener)
     */
    public void removeTransferListener( final TransferListener listener )
    {
        listeners.remove( listener );
    }

    /**
     * Returns whether the specified instance of transfer
     * listener was added to the collection of listeners
     * who will be notifed when an transfer event occurs
     *
     * @param listener the transfer listener
     *
     * @return <code>true<code>
     *         if given listner was added to the collection of listeners
     *         <code>false</code> otherwise
     *
     * @see org.apache.maven.wagon.events.TransferEvent
     * @see #addTransferListener(TransferListener)
     */
    public boolean hasTransferListener( final TransferListener listener )
    {
        return listeners.contains( listener );
    }


     /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registred listeners (calls method {@link TransferListener#transferStarted(TransferEvent)} on all of them}.
     * The Event should be of type {@link TransferEvent#TRANSFER_COMPLETED}
     *
     * @param transferEvent the TransferEvent which will be dispached to listeners
     */
    public void fireTransferStarted( final TransferEvent transferEvent )
    {
        for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
        {
            final TransferListener listener = ( TransferListener ) iter.next();
            listener.transferStarted( transferEvent );
        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registred listeners (calls method {@link TransferListener#transferProgress(TransferEvent)} on all of them}.
     * The Event should be of type {@link TransferEvent#TRANSFER_PROGRESS}.
     *
     * @param transferEvent the TransferEvent which will be dispached to listeners
     */
    public void fireTransferProgress( final TransferEvent transferEvent )
    {
        for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
        {
            final TransferListener listener = ( TransferListener ) iter.next();
            listener.transferProgress( transferEvent );

        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registred listeners (calls method {@link TransferListener#transferCompleted(TransferEvent)} on all of them}.
     * The Event should be of type {@link TransferEvent#TRANSFER_COMPLETED}
     *
     * @param transferEvent the TransferEvent which will be dispached to listeners
     */
    public void fireTransferCompleted( final TransferEvent transferEvent )
    {
        for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
        {
            final TransferListener listener = ( TransferListener ) iter.next();
            listener.transferCompleted( transferEvent );

        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registred listeners (calls method {@link TransferListener#transferError(TransferEvent)}  on all of them.
     * The Event should be of type {@link TransferEvent#TRANSFER_ERROR} and it is expected that
     * {@link TransferEvent#getException()} } method will return not null value
     *
     * @param transferEvent the TransferEvent which will be dispached to listeners
     */
    public void fireTransferError( final TransferEvent transferEvent )
    {
        for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
        {
            final TransferListener listener = ( TransferListener ) iter.next();
            listener.transferError( transferEvent );

        }
    }

     /**
     * Dispatches the given debug message
     * to all registred listeners (calls method {@link TransferListener#debug(String)} on all of them.
     *
     * @param message the debug message which will be dispached to listeners
     */
    public void fireDebug( final String message )
    {

        for ( Iterator iter = listeners.iterator(); iter.hasNext(); )
        {
            final TransferListener listener = ( TransferListener ) iter.next();
            listener.debug( message );

        }
    }

}
