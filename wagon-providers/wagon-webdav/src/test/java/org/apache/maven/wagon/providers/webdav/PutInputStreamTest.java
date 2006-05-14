package org.apache.maven.wagon.providers.webdav;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * PutInputStream Test
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class PutInputStreamTest
    extends TestCase
{
    private static final int BUFFER_SIZE = 8096;
    
    public void testStream() throws IOException, NoSuchAlgorithmException
    {
        File sourceFile = new File( FileTestUtils.getTestOutputDir(), "test-source" );
        FileUtils.fileWrite( sourceFile.getAbsolutePath(), "test-source.txt\n" );
        
        File destFile = new File( FileTestUtils.getTestOutputDir(), "test-dest" );
        
        Resource resource = new Resource();
        AbstractWagon wagon = new NullWagon();
        
        InputStream input = new PutInputStream(sourceFile, resource, wagon, wagon.getTransferEventSupport() );
        
        FileOutputStream output = new FileOutputStream(destFile);
        byte buf[] = new byte[BUFFER_SIZE];
        int n = -1;
        while( (n = input.read(buf)) != (-1))
        {
            output.write(buf, 0, n);
        }
        IOUtil.close(output);
        IOUtil.close(input);
        
        String sourceHash = generateHash(sourceFile);
        String destHash = generateHash(destFile);
        
        assertEquals("Hash comparision", sourceHash, destHash);
    }
    
    private String generateHash(File file) throws NoSuchAlgorithmException, IOException
    {
        MessageDigest md = MessageDigest.getInstance( "SHA1" );
        
        byte buffer[] = new byte[BUFFER_SIZE];
        FileInputStream fis = new FileInputStream(file);
        md.reset();
        
        int size = fis.read( buffer, 0, BUFFER_SIZE );
        while ( size >= 0 )
        {
            md.update( buffer, 0, size );
            size = fis.read( buffer, 0, BUFFER_SIZE );
        }
        
        return toHex(md.digest());
    }
    
    private String toHex( byte buf[] )
    {
        final char pseudo[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        char[] hash = new char[buf.length * 2];
        for ( int i = 0; i < buf.length; i++ )
        {
            hash[i * 2] = ( pseudo[( buf[i] & 0xF0 ) >> 4] );
            hash[( i * 2 ) + 1] = ( pseudo[( buf[i] & 0x0F )] );
        }
        return new String( hash );
    }
    
    
    public class NullWagon extends AbstractWagon
    {
        protected void closeConnection()
            throws ConnectionException
        {
                        
        }

        public void get( String resourceName, File destination )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
            
        }

        public boolean getIfNewer( String resourceName, File destination, long timestamp )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
            return false;
        }

        public void put( File source, String destination )
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
        {
            
        }

        public void openConnection()
            throws ConnectionException, AuthenticationException
        {

        }
    }
}
