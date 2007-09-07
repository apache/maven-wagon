package org.apache.maven.wagon.manager.stats;

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

import org.apache.maven.wagon.Wagon;

/**
 * TransferStatistics 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class TransferStatistics
{
    private long bytesFetched;

    private long bytesSent;

    private int countFetchFailures;

    private int countSendFailures;

    private int countResourcesFetched;

    private int countResourcesSent;
    
    /**
     * Bytes that have been transferre (both sent and received).
     * 
     * Note: This information may not contain protocol overhead.
     * 
     * @return the total bytes sent and received.
     */
    public long getBytesTransferred()
    {
        return this.bytesSent + this.bytesFetched;
    }

    /**
     * Bytes that have been fetched.
     * 
     * Note: This information may not contain protocol overhead.
     * 
     * @return the total bytes received.
     */
    public long getBytesFetched()
    {
        return this.bytesFetched;
    }

    /**
     * Bytes that have been sent.
     * 
     * Note: This information may not contain protocol overhead.
     * 
     * @return the total bytes sent.
     */
    public long getBytesSent()
    {
        return this.bytesSent;
    }

    /**
     * Get count of resources that have been transferred (both sent and received).
     * 
     * This can be viewed as a transaction count too.
     * 
     * @return count of resource sent and received.
     */
    public int getCountResourcesTransferred()
    {
        return this.countResourcesSent + this.countResourcesFetched;
    }

    /**
     * Get count of resources that have been received.
     * 
     * This can be viewed as a transaction count too.
     * 
     * @return count of resource received.
     */
    public int getCountResourcesFetched()
    {
        return this.countResourcesFetched;
    }

    /**
     * Get count of resources that have been sent.
     * 
     * This can be viewed as a transaction count too.
     * 
     * @return count of resource sent.
     */
    public int getCountResourcesSent()
    {
        return this.countResourcesSent;
    }

    /**
     * Get count of {@link Wagon#get(String, java.io.File)} that resulted in failure.
     * 
     * Failure can be any of a raft of situations, including a missing resource, failure to connect, 
     * authentication failure, or even a proxy failure.
     * 
     * @return count of failures.
     */
    public int getCountFetchFailures()
    {
        return this.countFetchFailures;
    }

    /**
     * Get count of {@link Wagon#put(java.io.File, String)} and {@link Wagon#putDirectory(java.io.File, String) 
     * requests that resulted in failure.
     * 
     * Failure can be any of a raft of situations, including a failure to connect, authentication failure,
     * a required but missing resource on the server side, or even a proxy failure.
     * 
     * @return count of failures.
     */
    public int getCountSendFailures()
    {
        return this.countSendFailures;
    }

    public void increaseBytesFetched( long bytes )
    {
        this.bytesFetched += bytes;
    }

    public void increaseBytesSent( long bytes )
    {
        this.bytesSent += bytes;
    }

    public void increaseCountFetchFailures( int count )
    {
        this.countFetchFailures += count;
    }

    public void increaseCountResourcesFetched( int count )
    {
        this.countResourcesFetched += count;
    }

    public void increaseCountResourcesSent( int count )
    {
        this.countResourcesSent += count;
    }

    public void increaseCountSendFailures( int count )
    {
        this.countSendFailures += count;
    }
}
