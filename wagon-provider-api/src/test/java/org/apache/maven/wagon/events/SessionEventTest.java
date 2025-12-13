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
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
class SessionEventTest {

    @Test
    void sessionEventProperties() throws Exception {

        Wagon wagon = Mockito.mock(Wagon.class);
        Repository repo = new Repository("fake", "http://fake");

        wagon.connect(repo);

        long timestamp = System.currentTimeMillis();
        Exception exception = new AuthenticationException("dummy");

        SessionEvent event1 = new SessionEvent(wagon, SessionEvent.SESSION_CLOSED);
        assertEquals(wagon, event1.getWagon());
        assertEquals(SessionEvent.SESSION_CLOSED, event1.getEventType());

        SessionEvent event = new SessionEvent(wagon, exception);
        assertEquals(wagon, event.getWagon());
        assertEquals(SessionEvent.SESSION_ERROR_OCCURRED, event.getEventType());
        assertEquals(exception, event.getException());

        event.setException(null);
        assertNull(event.getException());

        event.setException(exception);
        assertEquals(exception, event.getException());

        event.setTimestamp(timestamp);
        assertEquals(timestamp, event.getTimestamp());

        event.setEventType(SessionEvent.SESSION_CLOSED);
        assertEquals(SessionEvent.SESSION_CLOSED, event.getEventType());

        event.setEventType(SessionEvent.SESSION_DISCONNECTED);
        assertEquals(SessionEvent.SESSION_DISCONNECTED, event.getEventType());

        event.setEventType(SessionEvent.SESSION_DISCONNECTING);
        assertEquals(SessionEvent.SESSION_DISCONNECTING, event.getEventType());

        event.setEventType(SessionEvent.SESSION_ERROR_OCCURRED);
        assertEquals(SessionEvent.SESSION_ERROR_OCCURRED, event.getEventType());

        event.setEventType(SessionEvent.SESSION_LOGGED_IN);
        assertEquals(SessionEvent.SESSION_LOGGED_IN, event.getEventType());

        event.setEventType(SessionEvent.SESSION_LOGGED_OFF);
        assertEquals(SessionEvent.SESSION_LOGGED_OFF, event.getEventType());

        event.setEventType(SessionEvent.SESSION_OPENED);
        assertEquals(SessionEvent.SESSION_OPENED, event.getEventType());

        event.setEventType(SessionEvent.SESSION_OPENING);
        assertEquals(SessionEvent.SESSION_OPENING, event.getEventType());

        event.setEventType(SessionEvent.SESSION_CONNECTION_REFUSED);
        assertEquals(SessionEvent.SESSION_CONNECTION_REFUSED, event.getEventType());

        assertThrows(IllegalArgumentException.class, () -> event.setEventType(-1));
    }

    @Test
    void constantValueConflict() {
        final int[] values = {
            SessionEvent.SESSION_CLOSED, SessionEvent.SESSION_DISCONNECTED,
            SessionEvent.SESSION_DISCONNECTING, SessionEvent.SESSION_ERROR_OCCURRED,
            SessionEvent.SESSION_LOGGED_IN, SessionEvent.SESSION_LOGGED_OFF,
            SessionEvent.SESSION_OPENED, SessionEvent.SESSION_OPENING,
            SessionEvent.SESSION_CONNECTION_REFUSED
        };

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j], "Value conflict at [i,j]=[" + i + "," + j + "]");
            }
        }
    }
}
