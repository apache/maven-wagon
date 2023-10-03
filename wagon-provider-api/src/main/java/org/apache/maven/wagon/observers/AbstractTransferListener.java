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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

/**
 * TransferListeners which computes MD5 checksum on the fly when files are transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public abstract class AbstractTransferListener implements TransferListener {
    public void transferInitiated(TransferEvent transferEvent) {}

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferStarted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferStarted(TransferEvent transferEvent) {}

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferProgress(org.apache.maven.wagon.events.TransferEvent,byte[],int)
     */
    public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {}

    public void transferCompleted(TransferEvent transferEvent) {}

    public void transferError(TransferEvent transferEvent) {}

    public void debug(String message) {}
}
