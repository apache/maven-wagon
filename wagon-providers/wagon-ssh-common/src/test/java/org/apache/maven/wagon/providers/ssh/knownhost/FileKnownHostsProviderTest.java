package org.apache.maven.wagon.providers.ssh.knownhost;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;

import junit.framework.TestCase;

public class FileKnownHostsProviderTest
    extends TestCase
{
    private File basedir = new File( System.getProperty( "basedir", "." ) );
    
    private File testKnownHostsFile;
    
    
    private FileKnownHostsProvider provider;
    
    public void setUp()
        throws Exception
    {
        File readonlyKnownHostFile = new File( basedir, "src/test/resources/known_hosts" );
        testKnownHostsFile = new File( basedir, "target/known_hosts" );
        testKnownHostsFile.delete();
        FileUtils.copyFile( readonlyKnownHostFile, testKnownHostsFile );
       
        provider = new FileKnownHostsProvider( testKnownHostsFile );
       
    }
    
    public void testStoreKnownHostsNoChange()
        throws Exception
    {
        long timestamp = this.testKnownHostsFile.lastModified();
        //file with the same contents, but with entries swapped
        File sameKnownHostFile = new File( basedir, "src/test/resources/known_hosts_same" );
        String contents = FileUtils.fileRead( sameKnownHostFile );
        
        Thread.sleep( 50 );
        provider.storeKnownHosts( contents );
        assertEquals( "known_hosts file is rewritten", timestamp, testKnownHostsFile.lastModified() );
    }
    
    public void testStoreKnownHostsWithChange()
        throws Exception
    {
        long timestamp = this.testKnownHostsFile.lastModified();
        File sameKnownHostFile = new File( basedir, "src/test/resources/known_hosts_same" );
        String contents = FileUtils.fileRead( sameKnownHostFile );
        contents += "1 2 3";
        
        Thread.sleep( 50 );
        provider.storeKnownHosts( contents );
        assertTrue( "known_hosts file is not rewritten", timestamp != testKnownHostsFile.lastModified() );
    }
    
}
