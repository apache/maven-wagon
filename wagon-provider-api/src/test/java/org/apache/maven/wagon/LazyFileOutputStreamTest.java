package org.apache.maven.wagon;

import org.apache.maven.wagon.LazyFileOutputStream;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a> 
 * @version $Id$ 
 */
public class LazyFileOutputStreamTest extends TestCase
{

     public void testFileCreation()
     {
         try
         {
             File file = File.createTempFile( "wagon", "tmp" );
             
             file.deleteOnExit();
             
             assertFalse( file.exists() );
             
             LazyFileOutputStream stream = new LazyFileOutputStream( file );
             
             assertFalse( file.exists() );
             
             String exptected = "michal";
             
             stream.write( exptected.getBytes() );
             
             stream.close();
             
             assertTrue( file.exists() );
             
             String actual = FileUtils.fileRead( file );
             
             assertEquals( exptected, actual );
             
             
         }
         catch( Exception e )
         {
             e.printStackTrace();
             
             fail( e.getMessage() );    
         }
     
     }
     
}
