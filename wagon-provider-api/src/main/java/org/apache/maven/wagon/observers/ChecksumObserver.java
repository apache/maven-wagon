package org.apache.maven.wagon.observers;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonUtils;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;


/**
 * 
 * TransferListeners which computes MD5 checksum on the fly when files are transfered.
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a> 
 * @version $Id$ 
 */
public class ChecksumObserver implements TransferListener
{
    
    private String algorithm;
        
    private MessageDigest digester;
    
    private String expectedChecksum;
    
    private String actualChecksum;
    
    private boolean transferingMd5 = false;
    
    
    private static Map algorithmExtensionMap = new HashMap();
    
    static
    {
       algorithmExtensionMap.put( "MD5", ".md5" );  
       
       algorithmExtensionMap.put( "MD2", ".md2" );
       
       algorithmExtensionMap.put( "SHA-1", ".sha1" );
       
    }
    
    
    public ChecksumObserver()
    {
       this( "MD5" );    
    }
    
    /**
     * 
     * @param algorithm One of the algorithms supported by JDK: MD5, MD2 or SHA-1
     */
    public ChecksumObserver( String algorithm )
    {
         this.algorithm = algorithm;    
    }
    
    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferStarted(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferStarted( TransferEvent transferEvent )
    {

        if ( transferingMd5 )
        {
             return;
        }
        
        expectedChecksum = null;
        
        actualChecksum = null;
        
        
        try
        {
            digester = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e)
        {
         
        }
        
    }

    /**
     * @see org.apache.maven.wagon.events.TransferListener#transferProgress(org.apache.maven.wagon.events.TransferEvent)
     */
    public void transferProgress( TransferEvent transferEvent )
    {
        if ( digester != null )
        {
            byte[] data = transferEvent.getData();
                                
            int len = transferEvent.getDataLength();

            digester.update( data, 0, len );

        }
        
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        
        if ( digester == null )
        {        
            return;
        }

        Wagon wagon = transferEvent.getWagon();
        
        actualChecksum = encode ( digester.digest() );
        
        digester = null;
        
        InputStream inputStream = null;
        
        transferingMd5 = true;
        
        try
        {
            int type = transferEvent.getRequestType();
                                    
            String resource = transferEvent.getResource();
            
            String extension = ( String ) algorithmExtensionMap.get( algorithm );
            
            if ( type  == TransferEvent.REQUEST_GET )
            {
                
                //we will fetch md5 cheksum from server and
                // read its content into memory
                File artifactFile = transferEvent.getLocalFile();
                
                File md5File = new File( artifactFile.getPath() + extension );
                
                String  md5Resource = resource + extension;
                
                wagon.get( md5Resource, md5File );
               
                expectedChecksum = FileUtils.fileRead( md5File  ).trim();               
            }
            else
            {
                //It's PUT put request we will also put md5 checksum
                // which was computed on the fly
                WagonUtils.fromString( resource + extension , wagon, actualChecksum );
                
            }            
            
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }    
        finally
        {
            if ( inputStream != null )
            {
                 IOUtil.close( inputStream );
            }            
            
            transferingMd5 = false;
        }
        
            
    }

    public void transferError( TransferEvent transferEvent )
    { 
        digester = null;  
    }

    public void debug( String message )
    {
       
        
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
     * @return
     */
    public String getActualChecksum() 
    {       
       return actualChecksum;
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
        
        if ( actualChecksum != null && expectedChecksum !=null )
        {
             retValue = actualChecksum.equals( expectedChecksum );
        }
        
        return retValue;
    }

   
    
    
    
    
}
