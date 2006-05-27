package org.apache.maven.wagon.events;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class SessionListenerMock
    implements SessionListener
{

    private boolean connectionOpenningCalled = false;

    private boolean debugCalled = false;

    private boolean connectionLoggedOffCalled = false;

    private boolean connectionLoggedInCalled = false;

    private boolean connectionRefusedCalled = false;

    private boolean connectionDisconnectedCalled = false;

    private boolean connectionDisconnectingCalled = false;

    private boolean connectionOpenedCalled = false;

    private SessionEvent sessionEvent;

    private String message;

    private boolean connectionErrorCalled;

    public boolean isSessionDisconnectedCalled()
    {
        return connectionDisconnectedCalled;
    }

    public boolean isSessionDisconnectingCalled()
    {
        return connectionDisconnectingCalled;
    }

    public boolean isSessionLoggedInCalled()
    {
        return connectionLoggedInCalled;
    }

    public boolean isSessionLoggedOffCalled()
    {
        return connectionLoggedOffCalled;
    }

    public boolean isSessionOpenedCalled()
    {
        return connectionOpenedCalled;
    }

    public boolean isSessionOpenningCalled()
    {
        return connectionOpenningCalled;
    }

    public boolean isSessionRefusedCalled()
    {
        return connectionRefusedCalled;
    }

    public boolean isDebugCalled()
    {
        return debugCalled;
    }

    public void reset()
    {
        connectionOpenningCalled = false;

        debugCalled = false;

        connectionLoggedOffCalled = false;

        connectionLoggedInCalled = false;

        connectionRefusedCalled = false;

        connectionDisconnectedCalled = false;

        connectionDisconnectingCalled = false;

        connectionOpenedCalled = false;

        sessionEvent = null;

        message = null;
    }

    public void sessionOpening( final SessionEvent connectionEvent )
    {
        connectionOpenningCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionOpened( final SessionEvent connectionEvent )
    {
        connectionOpenedCalled = true;

        this.sessionEvent = connectionEvent;

    }

    public void sessionDisconnecting( final SessionEvent connectionEvent )
    {
        connectionDisconnectingCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionDisconnected( final SessionEvent connectionEvent )
    {
        connectionDisconnectedCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionConnectionRefused( final SessionEvent connectionEvent )
    {
        connectionRefusedCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionLoggedIn( final SessionEvent connectionEvent )
    {
        connectionLoggedInCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionLoggedOff( final SessionEvent connectionEvent )
    {
        connectionLoggedOffCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void sessionError( final SessionEvent connectionEvent )
    {
        connectionErrorCalled = true;

        this.sessionEvent = connectionEvent;
    }

    public void debug( final String message )
    {
        debugCalled = true;

        this.message = message;
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public String getDebugMessage()
    {
        return message;
    }
}
