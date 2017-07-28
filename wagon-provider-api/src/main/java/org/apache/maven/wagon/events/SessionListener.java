package org.apache.maven.wagon.events;

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

/**
 * Interface for classes which wants to receive and respond to any session update events.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public interface SessionListener
{

    /**
     * This method will be called when Wagon is about to open
     * connection to the repository.
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_OPENING}
     *
     * @param sessionEvent the session event
     */
    void sessionOpening( SessionEvent sessionEvent );

    /**
     * This method will be called when Wagon has successfully connected to
     * to the repository.
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_OPENED}
     *
     * @param sessionEvent the session event
     */
    void sessionOpened( SessionEvent sessionEvent );

    /**
     * This method will be called when Wagon has closed connection to
     * to the repository.
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_DISCONNECTING}
     *
     * @param sessionEvent the session event
     */
    void sessionDisconnecting( SessionEvent sessionEvent );

    /**
     * This method will be called when Wagon has closed connection to
     * the repository.
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_DISCONNECTED}
     *
     * @param sessionEvent the session event
     */
    void sessionDisconnected( SessionEvent sessionEvent );

    /**
     * This method will be called when Wagon when connection to
     * the repository was refused.
     * <p/>
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_CONNECTION_REFUSED}
     *
     * @param sessionEvent the session event
     */
    void sessionConnectionRefused( SessionEvent sessionEvent );

    /**
     * This method will be called by Wagon when Wagon managed
     * to login to the repository.
     *
     * @param sessionEvent the session event
     */
    void sessionLoggedIn( SessionEvent sessionEvent );

    /**
     * This method will be called by Wagon has logged off
     * from the repository.
     * <p/>
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_LOGGED_OFF}
     *
     * @param sessionEvent the session event
     */
    void sessionLoggedOff( SessionEvent sessionEvent );

    /**
     * This method will be called by Wagon when an error occurred.
     * <p/>
     * The type of the event should
     * be set to {@link SessionEvent#SESSION_ERROR_OCCURRED}
     *
     * @param sessionEvent the session event
     */
    void sessionError( SessionEvent sessionEvent );

    /**
     * This method allows to send arbitrary debug messages.
     *
     * @param message the debug message
     */
    void debug( String message );

}