package org.apache.maven.wagon.events;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.Wagon;

/**
 *
 * SessionEvent is used for notifing SessionListeners about
 * occurences of various sistutation releated.
 *
 * The session event is emitted by <code>Wagon</code> objects when
 *
 * <ul>
 *   <li>Before connection to the repository will be opened</li>
 *   <li>After connection to the repository was opened</li>
 *   <li>After wagon has logged-in to the repository</li>
 *   <li>After wagon has logged-off from the repository</li>
 *   <li>Before connection to the repository will be closed</li>
 *   <li>After connection to the repository was closed</li>
 * </ul>
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class SessionEvent extends WagonEvent
{

    /** A SESSION was closed. */
    public final static int SESSION_CLOSED = 1;

    /** A SESSION is about to be disconnected. */
    public final static int SESSION_DISCONNECTING = 2;

    /** A SESSION was disconnected (not currently used). */
    public final static int SESSION_DISCONNECTED = 3;

    /** A SESSION was refused. */
    public final static int SESSION_CONNECTION_REFUSED = 4;

    /** A SESSION is about to be opened. */
    public final static int SESSION_OPENING = 5;

    /** A SESSION was opened. */
    public final static int SESSION_OPENED = 6;

    /** A SESSION was opened. */
    public final static int SESSION_LOGGED_IN = 7;

    /** A SESSION was opened. */
    public final static int SESSION_LOGGED_OFF = 8;

    /** A SESSION was opened. */
    public final static int SESSION_ERROR_OCCURRED = 9;

    /** The type of the event. One of the SESSSION_XXX constans*/
    private int eventType;

    private Exception exception;

    /**
     * Creates new instance of SessionEvent
     * @param wagon  <code>Wagon<code> object which created this event
     * @param eventType the type of the event
     */
    public SessionEvent(
            final Wagon wagon,
            final int eventType )
    {
        super( wagon );
        this.eventType = eventType;

    }

    /**
     * Creates new instance of SessionEvent. Sets event type to <code>SESSION_ERROR_OCCURRED</code>
     * @param wagon  <code>Wagon<code> object which created this event
     * @param exception the exception
     */
    public SessionEvent(
            final Wagon wagon,
            final Exception exception )
    {
        super( wagon );
        this.exception = exception;
        this.eventType = SESSION_ERROR_OCCURRED;

    }

    /**
     * @return Returns the type.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * @return Returns the exception.
     */
    public Exception getException()
    {
        return exception;
    }

    /**
     * @param eventType The eventType to set.
     */
    public void setEventType( final int eventType )
    {
        switch ( eventType )
        {

            case SessionEvent.SESSION_CLOSED:
                break;
            case SessionEvent.SESSION_DISCONNECTED:
                break;
            case SessionEvent.SESSION_DISCONNECTING:
                break;
            case SessionEvent.SESSION_ERROR_OCCURRED:
                break;
            case SessionEvent.SESSION_LOGGED_IN:
                break;
            case SessionEvent.SESSION_LOGGED_OFF:
                break;
            case SessionEvent.SESSION_OPENED:
                break;
            case SessionEvent.SESSION_OPENING:
                break;
            case SessionEvent.SESSION_CONNECTION_REFUSED:
                break;
            default :
                throw new IllegalArgumentException(
                        "Illegal event type: " + eventType );
        }
        this.eventType = eventType;
    }

    /**
     * @param exception The exception to set.
     */
    public void setException( final Exception exception )
    {
        this.exception = exception;
    }

}
