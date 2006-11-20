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

import org.apache.maven.wagon.providers.ssh.TestData;
import org.codehaus.plexus.util.StringInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit test for <code>StreamKnownHostsProviderTest</code>
 *
 * @author Juan F. Codagnone
 * @see StreamKnownHostsProviderTest
 * @since Sep 12, 2005
 */
public class StreamKnownHostsProviderTest
    extends AbstractKnownHostsProviderTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        okHostsProvider = new StreamKnownHostsProvider(
            new StringInputStream( TestData.getHostname() + " ssh-rsa " + TestData.getHostKey() + "\n" ) );
        failHostsProvider = getProvider( "fail.knownhosts" );
        changedHostsProvider = getProvider( "changed.knownhosts" );
    }

    private StreamKnownHostsProvider getProvider( String s )
        throws IOException
    {
        String prefix = "/org/apache/maven/wagon/providers/ssh/knownhost/";
        InputStream ok = getClass().getResourceAsStream( prefix + s );
        assertNotNull( ok );
        return new StreamKnownHostsProvider( ok );
    }
}
