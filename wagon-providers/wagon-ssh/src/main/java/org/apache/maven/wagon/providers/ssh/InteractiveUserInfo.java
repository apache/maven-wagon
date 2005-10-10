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

/**
 * Interactive part for <code>UserInfo</code>
 *
 * @author Juan F. Codagnone
 * @since Sep 12, 2005
 * @see com.jcraft.jsch.UserInfo
 */
public interface InteractiveUserInfo
{
    String ROLE = InteractiveUserInfo.class.getName();

    /** @see com.jcraft.jsch.UserInfo#promptYesNo(java.lang.String) */
    boolean promptYesNo( String message );

    /*** @see com.jcraft.jsch.UserInfo#showMessage(java.lang.String) */
    void showMessage( String message );
}
