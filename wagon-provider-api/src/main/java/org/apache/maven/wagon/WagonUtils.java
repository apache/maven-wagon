package org.apache.maven.wagon;

import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;


/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a> 
 * @version $Id$ 
 */
public class WagonUtils
{

    
    public static String toString( String resource, Wagon wagon  ) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        
        File file = null;
       
        try
        {                     
             file = File.createTempFile( "wagon", "tmp" );
            
             wagon.get( resource, file );
             
             String retValue = FileUtils.fileRead( file );
             
             return retValue;
        }        
        finally
        {           
             
             if ( file != null )
             {
                 boolean deleted = file.delete();
                 
                 if ( ! deleted )
                 {
                     file.deleteOnExit();    
                 }                
             }
        }
        
         
    }
    
    public static void fromString( String resource, Wagon wagon, String content  ) throws Exception
    {
        File file = null;
       
        try
        {     
            file = File.createTempFile( "wagon", "tmp" );
            
            
            //@todo this methos should trow something more sepcific then java.lang.Exception
            FileUtils.fileWrite( file.getPath(), content );
            
            wagon.put( file, resource );
                          
        }        
        finally
        {
            if ( file != null )
            {
                boolean deleted = file.delete();
                
                if ( ! deleted )
                {
                    file.deleteOnExit();    
                }
                
            }
        }
        
         
    }
        
    
}
