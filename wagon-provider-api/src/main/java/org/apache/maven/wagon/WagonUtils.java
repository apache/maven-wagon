package org.apache.maven.wagon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a> 
 * @version $Id$ 
 */
public class WagonUtils
{

    public static byte[] toByteArray( String resource, Wagon wagon  ) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        InputStream inputStream = null;
       
        try
        {        
             inputStream = wagon.getInputStream( resource );
             
             byte[] retValue = IOUtil.toByteArray( inputStream );
             
             return retValue;
        }        
        finally
        {
             IOUtil.close( inputStream );
        }
        
         
    }
    
    public static void fromByteArray( String resource, Wagon wagon, byte[] buffer  ) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        OutputStream outputStream = null;
       
        try
        {        
             outputStream = wagon.getOutputStream( resource );
             
             IOUtil.copy( buffer, outputStream );
             
             
        }        
        finally
        {
             IOUtil.close( outputStream );
        }
        
         
    }
    
    
    
    public static String toString( String resource, Wagon wagon  ) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    
        byte[] buffer = toByteArray( resource, wagon );
        
        String retValue = new String( buffer );
        
        return retValue;
         
    }
    
    public static void fromString( String resource, Wagon wagon, String  str ) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
    
        byte[] buffer = str.getBytes();
        
        fromByteArray( resource, wagon, buffer  );
        
        
         
    }
    
    
    
}
