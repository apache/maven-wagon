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

import java.util.ArrayList;
import java.util.List;

/**
 * The class allows registration and deregistration of session listeners
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public final class SessionEventSupport {
    /**
     * registered listeners
     */
    private final List<SessionListener> listeners = new ArrayList<>();

    /**
     * Adds the listener to the collection of listeners
     * who will be notified when any session event occurs
     * in this <code>Wagon</code> object.
     * <br/>
     * If listener is <code>null</code>, no exception is thrown and no action is performed
     *
     * @param listener the transfer listener
     * @see #removeSessionListener(SessionListener)
     * @see TransferListener
     */
    public void addSessionListener(final SessionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the session listener from the collection of listeners so
     * it no longer receives session events.
     * <br/>
     * If listener is <code>null</code> or specified listener was not added
     * to this <code>SessionEventSupport</code> object
     * no exception is thrown and no action is performed
     *
     * @param listener the session listener
     * @see #addSessionListener(org.apache.maven.wagon.events.SessionListener)
     */
    public void removeSessionListener(final SessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns whether the specified instance of session
     * listener was added to the collection of listeners
     * who will be notified when an session event occurs
     *
     * @param listener the session listener
     * @return <code>true<code>
     *         if given listener was added to the collection of listeners
     *         <code>false</code> otherwise
     * @see org.apache.maven.wagon.events.SessionListener
     * @see #addSessionListener(org.apache.maven.wagon.events.SessionListener)
     */
    public boolean hasSessionListener(final SessionListener listener) {
        return listeners.contains(listener);
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionDisconnected(SessionEvent)} on all of
     * them}. The Event should be of type {@link SessionEvent#SESSION_DISCONNECTED}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionDisconnected(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionDisconnected(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionDisconnecting(SessionEvent)} } on all of
     * them}. The Event should be of type {@link SessionEvent#SESSION_DISCONNECTING}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionDisconnecting(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionDisconnecting(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionLoggedIn(SessionEvent)} on all of them}.
     * The Event should be of type {@link SessionEvent#SESSION_LOGGED_IN}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionLoggedIn(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionLoggedIn(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionLoggedOff(SessionEvent)} on all of
     * them}. The Event should be of type {@link SessionEvent#SESSION_LOGGED_OFF}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionLoggedOff(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionLoggedOff(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionOpened(SessionEvent)} on all of them}.
     * The Event should be of type {@link SessionEvent#SESSION_OPENED}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionOpened(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionOpened(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionOpening(SessionEvent)} on all of them}.
     * The Event should be of type {@link SessionEvent#SESSION_OPENING}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionOpening(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionOpening(sessionEvent);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionConnectionRefused(SessionEvent)} on all
     * of them}. The Event should be of type {@link SessionEvent#SESSION_CONNECTION_REFUSED}
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionConnectionRefused(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionConnectionRefused(sessionEvent);
        }
    }

    /**
     * Dispatches the given debug message
     * to all registered listeners (calls method {@link SessionListener#debug(String)} on all of them}.
     *
     * @param message the debug message which will be dispatched to listeners
     */
    public void fireDebug(final String message) {
        for (SessionListener listener : listeners) {
            listener.debug(message);
        }
    }

    /**
     * Dispatches the given <code>SessionEvent</code>
     * to all registered listeners (calls method {@link SessionListener#sessionConnectionRefused(SessionEvent)} on all
     * of them}. The Event should be of type {@link SessionEvent#SESSION_ERROR_OCCURRED} and it is expected that
     * {@link SessionEvent#getException()}  method will return not null value
     *
     * @param sessionEvent the SessionEvent which will be dispatched to listeners
     */
    public void fireSessionError(final SessionEvent sessionEvent) {
        for (SessionListener listener : listeners) {
            listener.sessionError(sessionEvent);
        }
    }
}
