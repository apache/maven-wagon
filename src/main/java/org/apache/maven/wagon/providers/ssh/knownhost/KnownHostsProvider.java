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

import org.apache.maven.wagon.TransferFailedException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import java.util.Properties;
import java.io.IOException;

/**
 * Provides a way to add Known Hosts fingerprints to the JSch transport
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 */
public interface KnownHostsProvider
{
    String ROLE = KnownHostsProvider.class.getName();

    String HOST_KEY_CHECKING = "StrictHostKeyChecking";

    /**
     * Adds to the session all the known hosts
     * 
     * @param sch JSch transport
     */
    void addKnownHosts( JSch sch, UserInfo userInfo )
        throws JSchException;

    void addConfiguration( Properties config );

    void storeKnownHosts( JSch sch );
}
