package org.apache.maven.wagon.observers;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonUtils;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.util.IOUtil;


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
    
    private String expectedMd5;
    
    private String actualMd5;
    
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
           
           int len = transferEvent.getDataLength();
                      
           md5Digester.update( data, 0, len );
           
        }                
        
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        
        if ( md5Digester == null )
        {
            return;
        }
        actualMd5 = encode ( md5Digester.digest() );
        
        InputStream inputStream = null;
        
        try
        {
            int type = transferEvent.getRequestType();
            
            Wagon wagon = transferEvent.getWagon();
            
            String resource = transferEvent.getResource();
            
            if ( type  == TransferEvent.REQUEST_GET )
            {
                                            
               expectedMd5 = WagonUtils.toString( resource + ".md5 ", wagon ).trim();               
            }
            else
            {
                //It's PUT put request
                
                WagonUtils.fromString( resource + ".md5 ", wagon, actualMd5 );
                
            }            
            
        }
        catch ( Exception e )
        {
            // ignore it. Observers should not throw any exceptions
        }    
        finally
        {
            if ( inputStream != null )
            {
                 IOUtil.close( inputStream );
            }
        }
        
            
    }

    public void transferError( TransferEvent transferEvent )
    {
        
        
    }

    public void debug( String message )
    {
       
        
    }
    
    
    
    /**
     * Returns the md5 checksum downloaded from the server
     *   
     * @return
     */
    public String getExpectedMd5Sum() 
    {       
       return expectedMd5;
    }
   
    
    /**
     * Returns md5 checksum which was computed during transfer
     * @return
     */
    public String getActualMd5Sum() 
    {       
       return actualMd5;
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

        String retValue = "";

        for ( int i = 0; i < 16; i++ )
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
    
    
    public boolean cheksumIsValid()
    {
        boolean retValue = false;
        
        if ( actualMd5 != null && expectedMd5 !=null )
        {
             retValue = actualMd5.equals( expectedMd5 );
        }
        
        return retValue;
    }
    
    
    
    
}
