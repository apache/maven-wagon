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
        // here we can do we need to have possibility to transfer to memory:
        //Wagon wagon = transferEvent.getWagon();
        //
        //String resource transferEvent.getResource();
        //
        //String md5 = wagon.getToString( resource + ".md5" );
    }

    public void transferError( TransferEvent transferEvent )
    {
        // TODO Auto-generated method stub
        
    }

    public void debug( String message )
    {
       
        
    }
    
    public String getMd5Sum() 
    {
       String retValue = null;
       
       if ( md5Digester != null )
       {
           retValue = new String ( md5Digester.digest() );
       }
       
       return retValue;
       
    }
    
}
