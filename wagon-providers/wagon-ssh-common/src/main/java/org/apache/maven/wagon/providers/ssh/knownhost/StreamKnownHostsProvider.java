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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides known hosts from a file
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 */
public class StreamKnownHostsProvider
    extends AbstractKnownHostsProvider
{

    public StreamKnownHostsProvider( InputStream stream )
        throws IOException
    {
        try
        {
            StringOutputStream stringOutputStream = new StringOutputStream();
            IOUtil.copy( stream, stringOutputStream );
            this.contents = stringOutputStream.toString();
            
            this.knownHosts = this.loadKnownHosts( this.contents );
        }
        finally
        {
            IOUtil.close( stream );
        }
    }
    
    protected Set<KnownHostEntry> loadKnownHosts( String contents )
        throws IOException
    {
        Set<KnownHostEntry> hosts = new HashSet<KnownHostEntry>();
        
        BufferedReader br = new BufferedReader( new StringReader( contents ) );
        
        String line = null;
        
        do 
        {
            line = br.readLine();
            if ( line != null )
            {
                String tokens[] = StringUtils.split( line );
                if ( tokens.length == 3 )
                {
                    hosts.add( new KnownHostEntry( tokens[0], tokens[1], tokens[2] ) );
                }
            }
            
        }
        while ( line != null );
        
        return hosts;
    }
}
