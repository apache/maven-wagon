package org.apache.maven.wagon;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;


/**
 * Variant of FileOutputStream which creates the file only when first portion 
 * of data is written.  
 * 
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a> 
 * @version $Id$ 
 */
public class LazyFileOutputStream extends OutputStream
{

    private File file;
    
    private FileOutputStream delegee;
    
    
    public LazyFileOutputStream( String filename )
    {
        this.file =  new File( filename );    
    }
    
    public LazyFileOutputStream( File file )
    {
        this.file = file;    
    }
    
    
    public void close() throws IOException
    {
        if ( delegee != null)
        {
            delegee.close();
        }
    }
    
    
    public boolean equals( Object obj )
    {
        return delegee.equals( obj );
    }
    
    
    public void flush() throws IOException
    {
        delegee.flush();
    }
    
    
    public FileChannel getChannel()
    {
        return delegee.getChannel();
    }
    
    
    public FileDescriptor getFD() throws IOException
    {
        return delegee.getFD();
    }
    
    public int hashCode()
    {
        return delegee.hashCode();
    }
    
    
    public String toString()
    {
        return delegee.toString();
    }
    
    public void write( byte[] b ) throws IOException
    {
        if ( delegee  == null)
        {
            initialize();
        }
        
        delegee.write( b );
    }
    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write( byte[] b, int off, int len ) throws IOException
    {
        if ( delegee == null)
        {
          initialize();    
        }
        
        delegee.write( b, off, len );
    }
    /**
     * @param b
     * @throws java.io.IOException
     */
    public void write( int b ) throws IOException
    {
        if ( delegee == null)
        {
            initialize();    
        }
        
        delegee.write( b );
    }


    /**
     * 
     */
    private void initialize() throws FileNotFoundException
    {        
        delegee = new FileOutputStream( file );         
    }
}
