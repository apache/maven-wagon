package org.apache.maven.wagon.events;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class MockSessionListener implements SessionListener
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
