package org.apache.maven.wagon.providers.ssh.jsch.interactive;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.jcraft.jsch.UIKeyboardInteractive;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * A conservative <code>UIKeyboardInteractive</code> that avoids real
 * user interaction :). This implementation expects only one prompt with the
 * word password in it.
 * <p/>
 * <code>UIKeyboardInteractive</code> are usefull when you don't use user with
 * password authentication with a server that use keyboard-interactive and
 * doesn't allow password method <code>PasswordAuthentication no</code>.
 *
 * @author Juan F. Codagnone
 * @since Sep 21, 2005
 */
public class TraditionalUIKeyboardInteractive
    implements UIKeyboardInteractive
{
    private final AuthenticationInfo authInfo;

    public TraditionalUIKeyboardInteractive( AuthenticationInfo authInfo )
    {
        this.authInfo = authInfo;
    }

    /**
     * @see UIKeyboardInteractive#promptKeyboardInteractive(String,String,
     *String,String[],boolean[])
     */
    public String[] promptKeyboardInteractive( String destination, String name, String instruction, String[] prompt,
                                               boolean[] echo )
    {

        String[] ret;

        if ( prompt.length == echo.length && prompt.length == 1 && !echo[0] &&
            prompt[0].toLowerCase().indexOf( "password" ) > -1 )
        {

            ret = new String[1];
            ret[0] = authInfo.getPassword();
        }
        else
        {
            // jsch-0.1.21/examples/UserAuthKI.java returns null to cancel
            ret = null;
        }

        return ret;
    }
}
