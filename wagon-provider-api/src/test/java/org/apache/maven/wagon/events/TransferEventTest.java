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
import org.apache.maven.wagon.resource.Resource;
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
class TransferEventTest {
    /*
     * Class to test for void TransferEvent(Wagon, Repository, String, int, int)
     */
    @Test
    void transferEventProperties() throws Exception {
        Wagon wagon = Mockito.mock(Wagon.class);
        Repository repo = new Repository("fake", "http://fake");
        wagon.connect(repo);

        Resource resource = new Resource();
        resource.setName("mm");

        TransferEvent event1 =
                new TransferEvent(wagon, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);

        assertEquals(wagon, event1.getWagon());
        assertEquals("mm", event1.getResource().getName());
        assertEquals(TransferEvent.TRANSFER_COMPLETED, event1.getEventType());
        assertEquals(TransferEvent.REQUEST_GET, event1.getRequestType());

        Resource res = new Resource();
        res.setName("mm");
        Exception exception = new AuthenticationException("dummy");

        TransferEvent event = new TransferEvent(wagon, res, exception, TransferEvent.REQUEST_GET);

        assertEquals(wagon, event.getWagon());
        assertEquals("mm", event.getResource().getName());
        assertEquals(TransferEvent.TRANSFER_ERROR, event.getEventType());
        assertEquals(TransferEvent.REQUEST_GET, event.getRequestType());
        assertEquals(exception, event.getException());

        event.setResource(null);
        assertNull(event.getResource());

        res.setName("/foo/baa");
        event.setResource(res);
        assertEquals("/foo/baa", event.getResource().getName());

        long timestamp = System.currentTimeMillis();
        event.setTimestamp(timestamp);
        assertEquals(timestamp, event.getTimestamp());

        event.setRequestType(TransferEvent.REQUEST_PUT);
        assertEquals(TransferEvent.REQUEST_PUT, event.getRequestType());

        event.setRequestType(TransferEvent.REQUEST_GET);
        assertEquals(TransferEvent.REQUEST_GET, event.getRequestType());

        assertThrows(IllegalArgumentException.class, () -> event.setRequestType(-1));

        event.setEventType(TransferEvent.TRANSFER_COMPLETED);
        assertEquals(TransferEvent.TRANSFER_COMPLETED, event.getEventType());

        event.setEventType(TransferEvent.TRANSFER_ERROR);
        assertEquals(TransferEvent.TRANSFER_ERROR, event.getEventType());

        event.setEventType(TransferEvent.TRANSFER_STARTED);
        assertEquals(TransferEvent.TRANSFER_STARTED, event.getEventType());

        event.setEventType(TransferEvent.TRANSFER_PROGRESS);
        assertEquals(TransferEvent.TRANSFER_PROGRESS, event.getEventType());

        assertThrows(IllegalArgumentException.class, () -> event.setEventType(-1));
    }

    @Test
    void constantValueConflict() {
        final int[] values = {
            TransferEvent.TRANSFER_COMPLETED, TransferEvent.TRANSFER_ERROR,
            TransferEvent.TRANSFER_STARTED, TransferEvent.TRANSFER_PROGRESS,
            TransferEvent.REQUEST_GET, TransferEvent.REQUEST_PUT
        };

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j], "Value conflict at [i,j]=[" + i + "," + j + "]");
            }
        }
    }
}
