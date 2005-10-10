package org.apache.maven.wagon.providers.ssh.knownhost;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.codehaus.plexus.util.IOUtil;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * Provides known hosts from a file
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
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

    public void storeKnownHosts( JSch sch )
    {
        PrintWriter w = null;
        try
        {
            w = new PrintWriter( new FileWriter( file ) );

            HostKeyRepository hkr = sch.getHostKeyRepository();
            HostKey[] keys = hkr.getHostKey();

            for ( int i = 0; i < keys.length; i++ )
            {
                HostKey key = keys[i];
                w.println( key.getHost() + " " + key.getType() + " " + key.getKey() );
            }
        }
        catch ( IOException e )
        {
            // TODO: log it
        }
        finally
        {
            IOUtil.close( w );
        }

        super.storeKnownHosts( sch );
    }

    public File getFile()
    {
        return file;
    }
}
