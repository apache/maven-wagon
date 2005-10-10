package org.apache.maven.wagon.providers.ssh.knownhost;

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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringInputStream;

import java.io.InputStream;
import java.io.IOException;

/**
 * Provides known hosts from a file
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 */
public class StreamKnownHostsProvider
    extends AbstractKnownHostsProvider
{
    /**
     * the known hosts, in the openssh format
     */
    private final String contents;

    public StreamKnownHostsProvider( InputStream stream )
        throws IOException
    {
        try
        {
            StringOutputStream stringOutputStream = new StringOutputStream();
            IOUtil.copy( stream, stringOutputStream );
            this.contents = stringOutputStream.toString();
        }
        finally
        {
            IOUtil.close( stream );
        }
    }

    /**
     * @see KnownHostsProvider#addKnownHosts(com.jcraft.jsch.JSch, UserInfo)
     */
    public void addKnownHosts( JSch sch, UserInfo userInfo )
        throws JSchException
    {
        sch.setKnownHosts( new StringInputStream( contents ) );
    }
}
