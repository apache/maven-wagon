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

import java.util.ArrayList;
import java.util.List;

/**
 * The class allows registration and removal of event listeners of type
 * TransferListener and dispatch of those events to those listeners
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public final class TransferEventSupport
{

    /**
     * registered listeners
     */
    private final List<TransferListener> listeners = new ArrayList<TransferListener>();

    /**
     * Adds the listener to the collection of listeners
     * who will be notified when any transfer event occurs
     * in this <code>Wagon</code> object.
     * <br/>
     * If listener is <code>null</code>, no exception is thrown and no action is performed
     *
     * @param listener the transfer listener
     * @see #removeTransferListener(org.apache.maven.wagon.events.TransferListener)
     * @see TransferListener
     */
    public synchronized void addTransferListener( final TransferListener listener )
    {
        if ( listener != null )
        {
            listeners.add( listener );
        }
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
     * @see #addTransferListener(TransferListener)
     */
    public synchronized void removeTransferListener( final TransferListener listener )
    {
        listeners.remove( listener );
    }

    /**
     * Returns whether the specified instance of transfer
     * listener was added to the collection of listeners
     * who will be notified when an transfer event occurs
     *
     * @param listener the transfer listener
     * @return <code>true<code>
     *         if given listener was added to the collection of listeners
     *         <code>false</code> otherwise
     * @see org.apache.maven.wagon.events.TransferEvent
     * @see #addTransferListener(TransferListener)
     */
    public synchronized boolean hasTransferListener( final TransferListener listener )
    {
        return listeners.contains( listener );
    }


    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registered listeners (calls method {@link TransferListener#transferStarted(TransferEvent)} on all of
     * them}. The Event should be of type {@link TransferEvent#TRANSFER_COMPLETED}
     *
     * @param transferEvent the TransferEvent which will be dispatched to listeners
     */
    public synchronized void fireTransferStarted( final TransferEvent transferEvent )
    {
        for ( TransferListener listener : listeners )
        {
            listener.transferStarted( transferEvent );
        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registered listeners (calls method {@link TransferListener#transferProgress(TransferEvent, byte[], int)}
     * on all of them). The Event should be of type {@link TransferEvent#TRANSFER_PROGRESS}.
     *
     * @param transferEvent the TransferEvent which will be dispatched to listeners
     * @param buffer        the buffer containing the additional content
     * @param length        the length of the content in the buffer
     */
    public synchronized void fireTransferProgress( final TransferEvent transferEvent, byte[] buffer, int length )
    {
        for ( TransferListener listener : listeners )
        {
            listener.transferProgress( transferEvent, buffer, length );

        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registered listeners (calls method {@link TransferListener#transferCompleted(TransferEvent)} on all of
     * them}. The Event should be of type {@link TransferEvent#TRANSFER_COMPLETED}
     *
     * @param transferEvent the TransferEvent which will be dispatched to listeners
     */
    public synchronized void fireTransferCompleted( final TransferEvent transferEvent )
    {
        for ( TransferListener listener : listeners )
        {
            listener.transferCompleted( transferEvent );

        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registered listeners (calls method {@link TransferListener#transferError(TransferEvent)}  on all of them.
     * The Event should be of type {@link TransferEvent#TRANSFER_ERROR} and it is expected that
     * {@link TransferEvent#getException()} } method will return not null value
     *
     * @param transferEvent the TransferEvent which will be dispatched to listeners
     */
    public synchronized void fireTransferError( final TransferEvent transferEvent )
    {
        for ( TransferListener listener : listeners )
        {
            listener.transferError( transferEvent );

        }
    }

    /**
     * Dispatches the given debug message
     * to all registered listeners (calls method {@link TransferListener#debug(String)} on all of them.
     *
     * @param message the debug message which will be dispatched to listeners
     */
    public synchronized void fireDebug( final String message )
    {

        for ( TransferListener listener : listeners )
        {
            listener.debug( message );

        }
    }

    /**
     * Dispatches the given <code>TransferEvent</code>
     * to all registered listeners (calls method {@link TransferListener#transferInitiated(TransferEvent)} on all of
     * them. The Event should be of type {@link TransferEvent#TRANSFER_INITIATED}.
     *
     * @param transferEvent the TransferEvent which will be dispatched to listeners
     */
    public synchronized void fireTransferInitiated( final TransferEvent transferEvent )
    {
        for ( TransferListener listener : listeners )
        {
            listener.transferInitiated( transferEvent );
        }
    }
}
