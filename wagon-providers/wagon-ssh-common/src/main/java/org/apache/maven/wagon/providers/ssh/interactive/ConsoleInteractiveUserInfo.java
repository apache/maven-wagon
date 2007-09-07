package org.apache.maven.wagon.providers.ssh.interactive;

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

import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.Arrays;

/**
 * Shows messages to System.out, and ask replies using an InputHandler
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 * 
 * @plexus.component role="org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo"
 *    instantiation-strategy="per-lookup"
 */
public class ConsoleInteractiveUserInfo
    implements InteractiveUserInfo
{
    /**
     * @plexus.requirement role-hint="default"
     */
    private Prompter prompter;

    public ConsoleInteractiveUserInfo()
    {
    }

    public ConsoleInteractiveUserInfo( Prompter prompter )
    {
        this.prompter = prompter;
    }

    /**
     * @see InteractiveUserInfo#promptYesNo(String)
     */
    public boolean promptYesNo( String message )
    {
        String ret;
        try
        {
            ret = prompter.prompt( message, Arrays.asList( new String[]{"yes", "no"} ) );
        }
        catch ( PrompterException e )
        {
            // no op
            ret = null;
        }
        return "yes".equalsIgnoreCase( ret );
    }

    /**
     * @see InteractiveUserInfo#showMessage(String)
     */
    public void showMessage( String message )
    {
        try
        {
            prompter.showMessage( message );
        }
        catch ( PrompterException e )
        {
            // no op
        }
    }

    public String promptPassword( String message )
    {
        try
        {
            return prompter.promptForPassword( message );
        }
        catch ( PrompterException e )
        {
            return null;
        }
    }

    public String promptPassphrase( String message )
    {
        return promptPassword( message );
    }
}
