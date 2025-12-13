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
package org.apache.maven.wagon.observers;

import java.io.File;

import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonMock;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ChecksumObserverTest {
    private Wagon wagon;

    @BeforeEach
    void setUp() throws Exception {

        wagon = new WagonMock(true);

        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        wagon.disconnect();
    }

    @Test
    void subsequentTransfersAfterTransferError() throws Exception {
        TransferListener listener = new ChecksumObserver();

        wagon.addTransferListener(listener);

        File testFile = File.createTempFile("wagon", "tmp");
        testFile.deleteOnExit();

        try {
            wagon.get("resource", testFile);
            fail();
        } catch (TransferFailedException ignored) {
        }

        try {
            wagon.get("resource", testFile);
            fail();
        } catch (TransferFailedException ignored) {
        }

        testFile.delete();
    }

    @Test
    void checksum() throws Exception {
        ChecksumObserver listener = new ChecksumObserver("SHA-1");

        Resource resource = new Resource("resource");

        TransferEvent transferEvent =
                new TransferEvent(wagon, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET);

        listener.transferInitiated(transferEvent);

        transferEvent = new TransferEvent(wagon, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET);

        listener.transferStarted(transferEvent);

        transferEvent = new TransferEvent(wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_GET);

        listener.transferProgress(transferEvent, "checksum\n".getBytes(), 9);

        transferEvent = new TransferEvent(wagon, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);

        listener.transferCompleted(transferEvent);

        assertEquals("2e5daf0201ddeb068a62d5e08da18657ab2c6be9", listener.getActualChecksum());
    }
}
