package org.apache.maven.wagon.observers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

/**
 * 
 * TransferListeners which computes MD5 checksum on the fly when files are transfered.
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a> 
 * @version $Id$ 
 */
public class Md5SumObserver implements TransferListener
{
    private MessageDigest md5Digester;
    
    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferStarted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferStarted( TransferEvent transferEvent )
    {
        try
        {
            md5Digester = MessageDigest.getInstance( "MD5" );
        }
        catch ( NoSuchAlgorithmException e)
        {
           // ignore
        }
        
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferProgress(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferProgress( TransferEvent transferEvent )
    {
        if ( md5Digester != null )
        {
           byte[] data = transferEvent.getData();
           
           md5Digester.update( data );
           
        }                
        
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        
    }

    public void transferError( TransferEvent transferEvent )
    {
        
        
    }

    public void debug( String message )
    {
       
        
    }
    
    public String getMd5Sum() 
    {
       String retValue = null;
       
       if ( md5Digester != null )
       {
           retValue = encode ( md5Digester.digest() );
       }
       
       return retValue;
       
    }
    
    /**
     * Encodes a 128 bit (16 bytes) byte array into a 32 character String.
     * XXX I think it should at least throw an IllegalArgumentException rather than return null
     *
     * @param binaryData Array containing the digest
     * @return Encoded hex string, or null if encoding failed
     */
    protected String encode( byte[] binaryData )
    {
        if ( binaryData.length != 16 )
        {
            return null;
        }

        String result = "";

        for ( int i = 0; i < 16; i++ )
        {
            String t = Integer.toHexString( binaryData[i] & 0xff );

            if ( t.length() == 1 )
            {
                result += ( "0" + t );
            }
            else
            {
                result += t;
            }
        }

        return result;
    }
    
    
}
