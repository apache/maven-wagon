package org.apache.maven.wagon.events;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public interface TransferListener
{

    /**
     * @param transferEvent 
     */
    void transferStarted( TransferEvent transferEvent );

    /**
     * @param transferEvent 
     */
    void transferProgress( TransferEvent transferEvent, byte[] buffer, int length );

    /**
     * @param transferEvent 
     */
    void transferCompleted( TransferEvent transferEvent );

    /**
     * @param transferEvent 
     */
    void transferError( TransferEvent transferEvent );

    /**
     * @param message 
     */
    void debug( String message );

}
