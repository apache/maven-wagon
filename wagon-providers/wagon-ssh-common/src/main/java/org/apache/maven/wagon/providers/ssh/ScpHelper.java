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

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.repository.RepositoryPermissions;

public class ScpHelper
{
    private ScpHelper()
    {
    }

    public static void createRemoteDirectories( String path, RepositoryPermissions permissions,
                                                CommandExecutor commandExecutor )
        throws TransferFailedException
    {
        try
        {
            String umaskCmd = null;
            if ( permissions != null )
            {
                String dirPerms = permissions.getDirectoryMode();

                if ( dirPerms != null )
                {
                    umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor( dirPerms );
                }
            }

            String mkdirCmd = "mkdir -p " + path;

            if ( umaskCmd != null )
            {
                mkdirCmd = umaskCmd + "; " + mkdirCmd;
            }

            commandExecutor.executeCommand( mkdirCmd );
        }
        catch ( CommandExecutionException e )
        {
            throw new TransferFailedException( "Error performing commands for file transfer", e );
        }
    }
}
