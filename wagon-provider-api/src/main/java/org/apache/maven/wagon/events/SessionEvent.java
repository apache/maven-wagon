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
package org.apache.maven.wagon.events;

import org.apache.maven.wagon.Wagon;

/**
 * SessionEvent is used for notifying SessionListeners about
 * occurrences of various situations related.
 * <p/>
 * The session event is emitted by <code>Wagon</code> objects when
 * <p/>
 * <ul>
 * <li>Before connection to the repository will be opened</li>
 * <li>After connection to the repository was opened</li>
 * <li>After wagon has logged-in to the repository</li>
 * <li>After wagon has logged-off from the repository</li>
 * <li>Before connection to the repository will be closed</li>
 * <li>After connection to the repository was closed</li>
 * </ul>
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class SessionEvent extends WagonEvent {

    /**
     * A SESSION was closed.
     */
    public static final int SESSION_CLOSED = 1;

    /**
     * A SESSION is about to be disconnected.
     */
    public static final int SESSION_DISCONNECTING = 2;

    /**
     * A SESSION was disconnected (not currently used).
     */
    public static final int SESSION_DISCONNECTED = 3;

    /**
     * A SESSION was refused.
     */
    public static final int SESSION_CONNECTION_REFUSED = 4;

    /**
     * A SESSION is about to be opened.
     */
    public static final int SESSION_OPENING = 5;

    /**
     * A SESSION was opened.
     */
    public static final int SESSION_OPENED = 6;

    /**
     * A SESSION was opened.
     */
    public static final int SESSION_LOGGED_IN = 7;

    /**
     * A SESSION was opened.
     */
    public static final int SESSION_LOGGED_OFF = 8;

    /**
     * A SESSION was opened.
     */
    public static final int SESSION_ERROR_OCCURRED = 9;

    /**
     * The type of the event. One of the SESSSION_XXX constants
     */
    private int eventType;

    private Exception exception;

    /**
     * Creates new instance of SessionEvent
     *
     * @param wagon     <code>Wagon<code> object which created this event
     * @param eventType the type of the event
     */
    public SessionEvent(final Wagon wagon, final int eventType) {
        super(wagon);
        this.eventType = eventType;
    }

    /**
     * Creates new instance of SessionEvent. Sets event type to <code>SESSION_ERROR_OCCURRED</code>
     *
     * @param wagon     <code>Wagon<code> object which created this event
     * @param exception the exception
     */
    public SessionEvent(final Wagon wagon, final Exception exception) {
        super(wagon);
        this.exception = exception;
        this.eventType = SESSION_ERROR_OCCURRED;
    }

    /**
     * @return Returns the type.
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * @return Returns the exception.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @param eventType The eventType to set.
     */
    public void setEventType(final int eventType) {
        switch (eventType) {
            case SessionEvent.SESSION_CLOSED:
            case SessionEvent.SESSION_DISCONNECTED:
            case SessionEvent.SESSION_DISCONNECTING:
            case SessionEvent.SESSION_ERROR_OCCURRED:
            case SessionEvent.SESSION_LOGGED_IN:
            case SessionEvent.SESSION_LOGGED_OFF:
            case SessionEvent.SESSION_OPENED:
            case SessionEvent.SESSION_OPENING:
            case SessionEvent.SESSION_CONNECTION_REFUSED:
                break;
            default:
                throw new IllegalArgumentException("Illegal event type: " + eventType);
        }
        this.eventType = eventType;
    }

    /**
     * @param exception The exception to set.
     */
    public void setException(final Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SessionEvent[");

        switch (this.eventType) {
            case SessionEvent.SESSION_CLOSED:
                sb.append("CONNECTION_CLOSED");
                break;
            case SessionEvent.SESSION_DISCONNECTED:
                sb.append("CONNECTION_DISCONNECTED");
                break;
            case SessionEvent.SESSION_DISCONNECTING:
                sb.append("CONNECTION_DISCONNECTING");
                break;
            case SessionEvent.SESSION_ERROR_OCCURRED:
                sb.append("CONNECTION_ERROR_OCCURRED");
                break;
            case SessionEvent.SESSION_LOGGED_IN:
                sb.append("CONNECTION_LOGGED_IN");
                break;
            case SessionEvent.SESSION_LOGGED_OFF:
                sb.append("CONNECTION_LOGGED_OFF");
                break;
            case SessionEvent.SESSION_OPENED:
                sb.append("CONNECTION_OPENED");
                break;
            case SessionEvent.SESSION_OPENING:
                sb.append("CONNECTION_OPENING");
                break;
            case SessionEvent.SESSION_CONNECTION_REFUSED:
                sb.append("CONNECTION_CONNECTION_REFUSED");
                break;
            default:
                sb.append(eventType);
        }
        sb.append("|");

        sb.append(this.getWagon().getRepository()).append("|");
        sb.append(this.source);

        if (exception != null) {
            sb.append("|");
            sb.append(exception.getClass().getName()).append(":");
            sb.append(exception.getMessage());
        }

        sb.append("]");

        return sb.toString();
    }
}
