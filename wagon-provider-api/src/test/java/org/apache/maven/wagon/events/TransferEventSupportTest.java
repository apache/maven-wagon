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
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
class TransferEventSupportTest {
    private final TransferEventSupport eventSupport = new TransferEventSupport();

    private Wagon wagon;

    @BeforeEach
    void setUp() {
        // TODO: actually test it gets called?
        wagon = mock(Wagon.class);
    }

    @Test
    void transferListenerRegistration() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);
        assertTrue(eventSupport.hasTransferListener(mock1));

        TransferListener mock2 = mock(TransferListener.class);
        assertFalse(eventSupport.hasTransferListener(mock2));

        eventSupport.addTransferListener(mock2);
        assertTrue(eventSupport.hasTransferListener(mock1));
        assertTrue(eventSupport.hasTransferListener(mock2));

        eventSupport.removeTransferListener(mock2);
        assertTrue(eventSupport.hasTransferListener(mock1));
        assertFalse(eventSupport.hasTransferListener(mock2));

        eventSupport.removeTransferListener(mock1);
        assertFalse(eventSupport.hasTransferListener(mock1));
    }

    @Test
    void fireTransferStarted() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);

        TransferListener mock2 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock2);

        TransferEvent event = getEvent(wagon);
        eventSupport.fireTransferStarted(event);

        verify(mock1).transferStarted(event);
        verify(mock2).transferStarted(event);
    }

    @Test
    void fireTransferProgress() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);

        TransferListener mock2 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock2);

        TransferEvent event = getEvent(wagon);
        byte[] buffer = "content".getBytes();

        eventSupport.fireTransferProgress(event, buffer, 0);

        verify(mock1).transferProgress(event, buffer, 0);
        verify(mock2).transferProgress(event, buffer, 0);
    }

    @Test
    void fireTransferCompleted() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);

        TransferListener mock2 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock2);

        TransferEvent event = getEvent(wagon);
        eventSupport.fireTransferCompleted(event);

        verify(mock1).transferCompleted(event);
        verify(mock2).transferCompleted(event);
    }

    @Test
    void fireTransferError() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);

        TransferListener mock2 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock2);

        TransferEvent event = getEvent(wagon);
        eventSupport.fireTransferError(event);

        verify(mock1).transferError(event);
        verify(mock2).transferError(event);
    }

    @Test
    void fireDebug() {
        TransferListener mock1 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock1);

        TransferListener mock2 = mock(TransferListener.class);
        eventSupport.addTransferListener(mock2);
        eventSupport.fireDebug("mm");

        verify(mock1).debug("mm");
        verify(mock2).debug("mm");
    }

    private TransferEvent getEvent(Wagon wagon) {
        return new TransferEvent(wagon, new Resource(), TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);
    }
}
