package org.apache.maven.wagon.observers;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.util.FileUtils;
import org.apache.maven.wagon.util.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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

    private String expectedChecksum;

    private String actualChecksum;

    private static Map algorithmExtensionMap = new HashMap();

    private final String extension;

    static
    {
        algorithmExtensionMap.put( "MD5", ".md5" );

        algorithmExtensionMap.put( "MD2", ".md2" );

        algorithmExtensionMap.put( "SHA-1", ".sha1" );
    }

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
        if ( !algorithmExtensionMap.containsKey( algorithm ) )
        {
            throw new NoSuchAlgorithmException( "Checksum algorithm not recognised by this class: " + algorithm );
        }

        digester = MessageDigest.getInstance( algorithm );
        extension = (String) algorithmExtensionMap.get( algorithm );
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
        expectedChecksum = null;

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
        Wagon wagon = transferEvent.getWagon();

        actualChecksum = encode( digester.digest() );

        InputStream inputStream = null;

        String checksumResource = transferEvent.getResource().getName() + extension;

        int type = transferEvent.getRequestType();

        try
        {
            if ( type == TransferEvent.REQUEST_GET )
            {

                //we will fetch md5 cheksum from server and
                // read its content into memory
                File artifactFile = transferEvent.getLocalFile();

                File checksumFile = new File( artifactFile.getPath() + extension );

                wagon.removeTransferListener( this );
                wagon.get( checksumResource, checksumFile );
                wagon.addTransferListener( this );

                expectedChecksum = FileUtils.fileRead( checksumFile ).trim();
            }
            else
            {
                //It's PUT put request we will also put md5 checksum
                // which was computed on the fly

                File file = File.createTempFile( "wagon", "tmp" );
                file.deleteOnExit();

                try
                {
                    FileUtils.fileWrite( file.getPath(), actualChecksum );

                    wagon.removeTransferListener( this );
                    wagon.put( file, checksumResource );
                    wagon.addTransferListener( this );
                }
                finally
                {
                    file.delete();
                }
            }

        }
        catch ( ResourceDoesNotExistException e )
        {
            transferError( new TransferEvent( wagon, new Resource( checksumResource ), e, type ) );
        }
        catch ( WagonException e )
        {
            transferError( new TransferEvent( wagon, new Resource( checksumResource ), e, type ) );
        }
        catch ( IOException e )
        {
            transferError( new TransferEvent( wagon, new Resource( checksumResource ), e, type ) );
        }
        finally
        {
            IoUtils.close( inputStream );

        }

    }

    public void transferError( TransferEvent transferEvent )
    {
        digester = null;
    }

    public void debug( String message )
    {
        // left intentionally blank
    }

    /**
     * Returns the md5 checksum downloaded from the server
     *
     * @return
     */
    public String getExpectedChecksum()
    {
        return expectedChecksum;
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
