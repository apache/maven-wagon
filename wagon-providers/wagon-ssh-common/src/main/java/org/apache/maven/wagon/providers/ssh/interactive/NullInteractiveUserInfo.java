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

/**
 * Dummy Implementation for <code>InteractiveUserInfo</code>, nice for
 * non-Interactive environments
 *
 * @author Juan F. Codagnone
 * @see org.apache.maven.wagon.providers.ssh.interactive.InteractiveUserInfo
 * @since Sep 12, 2005
 */
public class NullInteractiveUserInfo
    implements InteractiveUserInfo
{
    private final boolean promptYesNoResult;

    /**
     * @see #NullInteractiveUserInfo(boolean)
     */
    public NullInteractiveUserInfo()
    {
        this( false ); // the safest value
    }

    /**
     * Creates a <code>NullInteractiveUserInfo</code> with a hardcoded
     * prompYesNo result
     *
     * @param promptYesNoResult the hardcoded result
     */
    public NullInteractiveUserInfo( final boolean promptYesNoResult )
    {
        this.promptYesNoResult = promptYesNoResult;
    }

    /**
     * @see InteractiveUserInfo#promptYesNo(java.lang.String)
     */
    public boolean promptYesNo( final String message )
    {
        return promptYesNoResult;
    }

    /**
     * @see InteractiveUserInfo#showMessage(java.lang.String)
     */
    public void showMessage( final String message )
    {
        // nothing to do
    }

    public String promptPassword( String message )
    {
        return null;
    }

    public String promptPassphrase( String message )
    {
        return null;
    }
}
