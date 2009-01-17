package org.apache.maven.wagon.providers.ssh.knownhost;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
