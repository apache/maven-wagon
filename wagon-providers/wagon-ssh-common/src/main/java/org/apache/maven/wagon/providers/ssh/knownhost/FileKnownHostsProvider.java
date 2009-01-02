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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;

/**
 * Provides known hosts from a file
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 * 
 * @plexus.component role="org.apache.maven.wagon.providers.ssh.knownhost.KnownHostsProvider"
 *    role-hint="file"
 *    instantiation-strategy="per-lookup"
 */
public class FileKnownHostsProvider
    extends StreamKnownHostsProvider
{
    private final File file;

    /**
     * Creates the FileKnownHostsProvider.
     *
     * @param file the file that holds the known hosts, in the openssh format
     * @throws IOException
     */
    public FileKnownHostsProvider( File file )
        throws IOException
    {
        super( file.exists() ? (InputStream) new FileInputStream( file ) : new ByteArrayInputStream( "".getBytes() ) );
        this.file = file;
    }

    /**
     * Creates a FileKnownHostsProvider using as file openssh knwon_host
     *
     * @throws IOException
     * @see #FileKnownHostsProvider(File)
     */
    public FileKnownHostsProvider()
        throws IOException
    {
        this( new File( System.getProperty( "user.home" ), ".ssh/known_hosts" ) );
    }

    public void storeKnownHosts( String contents )
        throws IOException
    {
        Set hosts = this.loadKnownHosts( contents );
        
        if ( ! this.knownHosts.equals( hosts ) )
        {
            file.getParentFile().mkdirs();
            FileUtils.fileWrite( file.getAbsolutePath(), contents );
            this.knownHosts = hosts;
        }
    }
    
    public File getFile()
    {
        return file;
    }
}
