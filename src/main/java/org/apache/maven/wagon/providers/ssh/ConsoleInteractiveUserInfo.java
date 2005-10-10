package org.apache.maven.wagon.providers.ssh;

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

import org.codehaus.plexus.components.inputhandler.InputHandler;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;

/**
 * Shows messages to System.out, and ask replies using an InputHandler 
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 */
public class ConsoleInteractiveUserInfo
    implements InteractiveUserInfo
{
    private InputHandler inputHandler;

    public ConsoleInteractiveUserInfo()
    {
    }

    public ConsoleInteractiveUserInfo( InputHandler inputHandler )
    {
        this.inputHandler = inputHandler;
    }

    /** @see InteractiveUserInfo#promptYesNo(java.lang.String) */
    public boolean promptYesNo( String message )
    {
        boolean ret = false;

        showMessage( message );
        String answer;
        try
        {
            answer = inputHandler.readLine();
            if ( !StringUtils.isEmpty( answer ) )
            {
                answer = answer.toLowerCase();

                // TODO: localization and i18n? 
                if ( answer.startsWith( "y" ) )
                {
                    ret = true;
                }
                else if ( answer.startsWith( "n" ) )
                {
                    ret = false;

                }
            }
        }
        catch ( final IOException e )
        {
            // TODO log?
            
            // nothing to do
        }

        return ret;
    }

    /** @see InteractiveUserInfo#showMessage(java.lang.String) */
    public void showMessage( String message )
    {
        System.out.println( message );
    }

    public void setInputHandler( InputHandler inputHandler )
    {
        this.inputHandler = inputHandler;
    }
}
