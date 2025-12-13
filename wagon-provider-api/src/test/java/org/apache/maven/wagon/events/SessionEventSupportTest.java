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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
class SessionEventSupportTest {

    private final SessionEventSupport eventSupport = new SessionEventSupport();

    private Wagon wagon;

    @BeforeEach
    void setUp() {
        // TODO: actually test it gets called?
        wagon = mock(Wagon.class);
    }

    @Test
    void sessionListenerRegistration() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);
        assertTrue(eventSupport.hasSessionListener(mock1));

        SessionListener mock2 = mock(SessionListener.class);
        assertFalse(eventSupport.hasSessionListener(mock2));

        eventSupport.addSessionListener(mock2);
        assertTrue(eventSupport.hasSessionListener(mock1));
        assertTrue(eventSupport.hasSessionListener(mock2));

        eventSupport.removeSessionListener(mock2);
        assertTrue(eventSupport.hasSessionListener(mock1));
        assertFalse(eventSupport.hasSessionListener(mock2));

        eventSupport.removeSessionListener(mock1);
        assertFalse(eventSupport.hasSessionListener(mock1));
    }

    @Test
    void fireSessionDisconnected() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionDisconnected(event);

        verify(mock1).sessionDisconnected(event);
        verify(mock2).sessionDisconnected(event);
    }

    @Test
    void fireSessionDisconneting() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionDisconnecting(event);

        verify(mock1).sessionDisconnecting(event);
        verify(mock2).sessionDisconnecting(event);
    }

    @Test
    void fireSessionLoggedIn() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionLoggedIn(event);

        verify(mock1).sessionLoggedIn(event);
        verify(mock2).sessionLoggedIn(event);
    }

    @Test
    void fireSessionLoggedOff() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionLoggedOff(event);

        verify(mock1).sessionLoggedOff(event);
        verify(mock2).sessionLoggedOff(event);
    }

    @Test
    void fireSessionOpened() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionOpened(event);

        verify(mock1).sessionOpened(event);
        verify(mock2).sessionOpened(event);
    }

    @Test
    void fireSessionOpenning() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionOpening(event);

        verify(mock1).sessionOpening(event);
        verify(mock2).sessionOpening(event);
    }

    @Test
    void fireSessionRefused() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        SessionEvent event = new SessionEvent(wagon, 1);
        eventSupport.fireSessionConnectionRefused(event);

        verify(mock1).sessionConnectionRefused(event);
        verify(mock2).sessionConnectionRefused(event);
    }

    @Test
    void fireDebug() {
        SessionListener mock1 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = mock(SessionListener.class);
        eventSupport.addSessionListener(mock2);

        eventSupport.fireDebug("mm");

        verify(mock1).debug("mm");
        verify(mock2).debug("mm");
    }
}
