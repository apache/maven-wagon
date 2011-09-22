package org.apache.maven.wagon.providers.ssh;

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

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class TestData
{
    public static String getTempDirectory()
    {
        return System.getProperty( "java.io.tmpdir", "target" );
    }

    public static String getTestRepositoryUrl( int port )
    {
        return "scp://" + getHostname() + ":" + port + getRepoPath();
    }

    public static String getRepoPath()
    {
        return getTempDirectory() + "/wagon-ssh-test/" + getUserName();
    }

    public static String getUserName()
    {
        return System.getProperty( "test.user", System.getProperty( "user.name" ) );
    }

    public static String getUserPassword()
    {
        return "comeonFrance!:-)";
    }

    public static File getPrivateKey()
    {
        return new File( System.getProperty( "sshKeysPath", "src/test/ssh-keys" ), "id_rsa" );
    }

    public static String getHostname()
    {
        return System.getProperty( "test.host", "localhost" );
    }

    public static String getHostKey()
    {
        try
        {
            return FileUtils.fileRead(
                new File( System.getProperty( "sshKeysPath" ), "id_rsa.pub" ).getPath() ).substring(
                "ssh-rsa".length() ).trim();
        }
        catch ( IOException e )
        {
            return null;
        }
    }
}
