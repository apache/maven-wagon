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
package org.apache.maven.wagon.providers.ssh.jsch.interactive;

import com.jcraft.jsch.UIKeyboardInteractive;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * UIKeyboardInteractive that use plexus-prompter.
 *
 * <code>UIKeyboardInteractive</code> are usefull when you don't use user with
 * password authentication with a server that use keyboard-interactive and
 * doesn't allow password method <code>PasswordAuthentication no</code>.
 *
 * @author <a href="mailto:juam at users.sourceforge.net">Juan F. Codagnone</a>
 * @since Sep 22, 2005
 *
 * @plexus.component role="com.jcraft.jsch.UIKeyboardInteractive"
 */
public class PrompterUIKeyboardInteractive implements UIKeyboardInteractive {
    /**
     * @plexus.requirement role-hint="default"
     */
    private volatile Prompter prompter;

    public PrompterUIKeyboardInteractive() {}

    public PrompterUIKeyboardInteractive(Prompter promper) {
        this.prompter = promper;
    }

    /**
     * @see UIKeyboardInteractive#promptKeyboardInteractive(String,String,
     *String,String[],boolean[])
     */
    public String[] promptKeyboardInteractive(
            String destination, String name, String instruction, String[] prompt, boolean[] echo) {

        if (prompt.length != echo.length) {
            // jcsh is buggy?
            throw new IllegalArgumentException("prompt and echo size arrays are different!");
        }
        String[] ret = new String[prompt.length];

        try {

            for (int i = 0; i < ret.length; i++) {
                if (echo[i]) {
                    ret[i] = prompter.prompt(prompt[i]);
                } else {
                    ret[i] = prompter.promptForPassword(prompt[i]);
                }
            }
        } catch (PrompterException e) {
            // TODO: log
            // the user canceled?
            ret = null;
        }

        return ret;
    }
}
