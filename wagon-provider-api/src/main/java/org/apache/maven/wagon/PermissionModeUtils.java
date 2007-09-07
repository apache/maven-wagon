package org.apache.maven.wagon;

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
 * Utility class for common operations for file/directory permissions.
 *
 * @author <a href="mailto:juam at users.sourceforge.net">Juan F. Codagnone</a>
 * @see PermissionModeUtils
 * @since Sep 3, 2005
 */
public class PermissionModeUtils
{
    /**
     * See the System Interfaces volume of IEEE Std 1003.1-2001, umask(1)
     *
     * @param modeStr permision mode (numeric or symbolic)
     * @return the mode that can be used with unmask to acomplish modeStr.
     */
    public static String getUserMaskFor( String modeStr )
    {
        String ret = null;

        try
        {
            int mode = Integer.valueOf( modeStr, 8 ).intValue();

            mode = mode % 8 + ( ( mode / 8 ) % 8 ) * 8 + ( ( mode / 64 ) % 8 ) * 64;

            ret = Integer.toOctalString( 0777 - mode );
        }
        catch ( final NumberFormatException e )
        {
            try
            {
                Integer.parseInt( modeStr );
            }
            catch ( final NumberFormatException e1 )
            {
                ret = modeStr;
            }
        }

        if ( ret == null )
        {
            throw new IllegalArgumentException( "The mode is a number but is not octal" );
        }

        return ret;
    }
}
