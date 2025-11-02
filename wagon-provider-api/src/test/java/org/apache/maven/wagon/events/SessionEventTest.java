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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class SessionEventTest {
    /*
     * Class to test for void SESSIONEvent(Wagon, Repository, String, int,
     * int)
     */
    @Test
    public void testSessionEventProperties() throws ConnectionException, AuthenticationException {

        final Wagon wagon = EasyMock.createMock(Wagon.class);
        final Repository repo = new Repository();

        wagon.connect(repo);

        final long timestamp = System.currentTimeMillis();
        final Exception exception = new AuthenticationException("dummy");

        SessionEvent event = new SessionEvent(wagon, SessionEvent.SESSION_CLOSED);

        assertEquals(wagon, event.getWagon());

        assertEquals(SessionEvent.SESSION_CLOSED, event.getEventType());

        event = new SessionEvent(wagon, exception);

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

        try {
            event.setEventType(-1);
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testConstantValueConflict() {
        final int[] values = {
            SessionEvent.SESSION_CLOSED, SessionEvent.SESSION_DISCONNECTED,
            SessionEvent.SESSION_DISCONNECTING, SessionEvent.SESSION_ERROR_OCCURRED,
            SessionEvent.SESSION_LOGGED_IN, SessionEvent.SESSION_LOGGED_OFF,
            SessionEvent.SESSION_OPENED, SessionEvent.SESSION_OPENING,
            SessionEvent.SESSION_CONNECTION_REFUSED
        };

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {

                final String msg = "Value confict at [i,j]=[" + i + "," + j + "]";
                assertTrue(values[i] != values[j], msg);
            }
        }
    }
}
