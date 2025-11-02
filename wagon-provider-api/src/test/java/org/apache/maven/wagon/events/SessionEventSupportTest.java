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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class SessionEventSupportTest {

    private SessionEventSupport eventSupport;

    private Wagon wagon;

    /**
     * @see 
     */
    @BeforeEach
    public void setUp() throws Exception {

        eventSupport = new SessionEventSupport();

        // TODO: actually test it gets called?
        wagon = createNiceMock(Wagon.class);
    }

    @Test
    public void testSessionListenerRegistration() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        assertTrue(eventSupport.hasSessionListener(mock1));

        SessionListener mock2 = createMock(SessionListener.class);

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
    public void testFireSessionDisconnected() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionDisconnected(event);
        mock2.sessionDisconnected(event);

        replay(mock1, mock2);

        eventSupport.fireSessionDisconnected(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionDisconneting() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionDisconnecting(event);
        mock2.sessionDisconnecting(event);

        replay(mock1, mock2);

        eventSupport.fireSessionDisconnecting(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionLoggedIn() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionLoggedIn(event);
        mock2.sessionLoggedIn(event);

        replay(mock1, mock2);

        eventSupport.fireSessionLoggedIn(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionLoggedOff() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionLoggedOff(event);
        mock2.sessionLoggedOff(event);

        replay(mock1, mock2);

        eventSupport.fireSessionLoggedOff(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionOpened() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionOpened(event);
        mock2.sessionOpened(event);

        replay(mock1, mock2);

        eventSupport.fireSessionOpened(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionOpenning() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionOpening(event);
        mock2.sessionOpening(event);

        replay(mock1, mock2);

        eventSupport.fireSessionOpening(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireSessionRefused() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        final SessionEvent event = new SessionEvent(wagon, 1);

        mock1.sessionConnectionRefused(event);
        mock2.sessionConnectionRefused(event);

        replay(mock1, mock2);

        eventSupport.fireSessionConnectionRefused(event);

        verify(mock1, mock2);
    }

    @Test
    public void testFireDebug() {
        SessionListener mock1 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock1);

        SessionListener mock2 = createMock(SessionListener.class);

        eventSupport.addSessionListener(mock2);

        mock1.debug("mm");
        mock2.debug("mm");

        replay(mock1, mock2);

        eventSupport.fireDebug("mm");

        verify(mock1, mock2);
    }
}
