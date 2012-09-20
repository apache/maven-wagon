package org.apache.maven.wagon.events;

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

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class TransferListenerMock
    implements TransferListener
{

    private String debugMessage;

    private boolean debugCalled;

    private TransferEvent transferEvent;

    private boolean transferCompletedCalled;

    private boolean transferProgressCalled;

    private boolean transferStartedCalled;

    private int numberOfProgressCalls;

    private boolean transferErrorCalled;

    private boolean transferInitiatedCalled;


    public boolean isTransferInitiatedCalled()
    {
        return transferInitiatedCalled;
    }

    /**
     * @return Returns the debugCalled.
     */
    public boolean isDebugCalled()
    {
        return debugCalled;
    }

    /**
     * @return Returns the transferCompletedCalled.
     */
    public boolean isTransferCompletedCalled()
    {
        return transferCompletedCalled;
    }

    /**
     * @return Returns the transferEvent.
     */
    public TransferEvent getTransferEvent()
    {
        return transferEvent;
    }

    /**
     * @return Returns the transferProgressCalled.
     */
    public boolean isTransferProgressCalled()
    {
        return transferProgressCalled;
    }

    /**
     * @return Returns the transferStartedCalled.
     */
    public boolean isTransferStartedCalled()
    {
        return transferStartedCalled;
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        this.transferEvent = transferEvent;
        transferInitiatedCalled = true;
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferStarted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferStarted( final TransferEvent transferEvent )
    {
        this.transferEvent = transferEvent;
        transferStartedCalled = true;

    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferProgress(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferProgress( final TransferEvent transferEvent, byte[] buffer, int length )
    {
        this.transferEvent = transferEvent;
        transferProgressCalled = true;
        numberOfProgressCalls++;

    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferCompleted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferCompleted( final TransferEvent transferEvent )
    {
        this.transferEvent = transferEvent;
        transferCompletedCalled = true;

    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#debug(java.lang.String)
     */
    public void debug( final String message )
    {
        debugMessage = message;
        debugCalled = true;

    }

    /**
     * @return
     */
    public String getDebugMessage()
    {

        return debugMessage;
    }

    /**
     * @return Returns the numberOfprogressCalls.
     */
    public int getNumberOfProgressCalls()
    {
        return numberOfProgressCalls;
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferError(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferError( final TransferEvent transferEvent )
    {
        this.transferEvent = transferEvent;
        transferErrorCalled = true;

    }

    /**
     * @return Returns the transferErrorCalled.
     */
    public boolean isTransferErrorCalled()
    {
        return transferErrorCalled;
    }

}
