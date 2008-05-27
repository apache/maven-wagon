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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * A proxy that let you merge a <code>UserInfo</code> and a
 * <code>UIKeyboardInteractive</code>
 *
 * @author Juan F. Codagnone
 * @since Sep 22, 2005
 */
public class UserInfoUIKeyboardInteractiveProxy
    implements UserInfo, UIKeyboardInteractive
{
    private final UIKeyboardInteractive interactive;

    private final UserInfo userInfo;

    public UserInfoUIKeyboardInteractiveProxy( UserInfo userInfo, UIKeyboardInteractive interactive )
    {
        this.userInfo = userInfo;
        this.interactive = interactive;
    }

    /**
     * @see com.jcraft.jsch.UIKeyboardInteractive#promptKeyboardInteractive(String,String,String,String[],boolean[])
     */
    public String[] promptKeyboardInteractive( String destination, String name, String instruction, String[] prompt,
                                               boolean[] echo )
    {
        if ( userInfo.getPassword() != null )
        {
            prompt[0] = "Keyboard interactive required, supplied password is ignored\n" + prompt[0];
        }
        return interactive.promptKeyboardInteractive( destination, name, instruction, prompt, echo );
    }

    /**
     * @see com.jcraft.jsch.UserInfo#getPassphrase()
     */
    public String getPassphrase()
    {
        return userInfo.getPassphrase();
    }

    /**
     * @see com.jcraft.jsch.UserInfo#getPassword()
     */
    public String getPassword()
    {
        return userInfo.getPassword();
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptPassword(String)
     */
    public boolean promptPassword( String arg0 )
    {
        return userInfo.promptPassword( arg0 );
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptPassphrase(String)
     */
    public boolean promptPassphrase( String arg0 )
    {
        return userInfo.promptPassphrase( arg0 );
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptYesNo(String)
     */
    public boolean promptYesNo( String arg0 )
    {
        return userInfo.promptYesNo( arg0 );
    }

    /**
     * @see com.jcraft.jsch.UserInfo#showMessage(String)
     */
    public void showMessage( String arg0 )
    {
        userInfo.showMessage( arg0 );
    }

}
