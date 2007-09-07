package org.apache.maven.wagon.observers;

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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * TransferListeners which computes MD5 checksum on the fly when files are transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class ChecksumObserver
    implements TransferListener
{
    private MessageDigest digester = null;

    private String actualChecksum;

    public ChecksumObserver()
        throws NoSuchAlgorithmException
    {
        this( "MD5" );
    }

    /**
     * @param algorithm One of the algorithms supported by JDK: MD5, MD2 or SHA-1
     */
    public ChecksumObserver( String algorithm )
        throws NoSuchAlgorithmException
    {
        digester = MessageDigest.getInstance( algorithm );
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        // This space left intentionally blank
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferStarted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferStarted( TransferEvent transferEvent )
    {
        actualChecksum = null;

        digester.reset();
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferProgress(org.apache.maven.wagon.events.TransferEvent,byte[],int)
     */
    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        digester.update( buffer, 0, length );
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        actualChecksum = encode( digester.digest() );
    }

    public void transferError( TransferEvent transferEvent )
    {
        digester = null;
        actualChecksum = null;
    }

    public void debug( String message )
    {
        // left intentionally blank
    }

    /**
     * Returns md5 checksum which was computed during transfer
     *
     * @return
     */
    public String getActualChecksum()
    {
        return actualChecksum;
    }

    /**
     * Encodes a 128 bit or 160-bit byte array into a String.
     *
     * @param binaryData Array containing the digest
     * @return Encoded hex string, or null if encoding failed
     */
    protected String encode( byte[] binaryData )
    {
        if ( binaryData.length != 16 && binaryData.length != 20 )
        {
            int bitLength = binaryData.length * 8;
            throw new IllegalArgumentException( "Unrecognised length for binary data: " + bitLength + " bits" );
        }

        String retValue = "";

        for ( int i = 0; i < binaryData.length; i++ )
        {
            String t = Integer.toHexString( binaryData[i] & 0xff );

            if ( t.length() == 1 )
            {
                retValue += ( "0" + t );
            }
            else
            {
                retValue += t;
            }
        }

        return retValue.trim();
    }


}
