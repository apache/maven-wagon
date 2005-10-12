package org.apache.maven.wagon;

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

import junit.framework.TestCase;
import org.apache.maven.wagon.PermissionModeUtils;

/**
 * Unit test for PermissionModeUtils class
 *
 * @author <a href="mailto:juam at users.sourceforge.net">Juan F. Codagnone</a>
 * @see PermissionModeUtils
 * @since Sep 3, 2005
 */
public class PermissionModeUtilsTest
    extends TestCase
{
    /**
     * @throws Exception on error
     */
    public void testNumeric()
        throws Exception
    {
        final String[][] tests = {
            {"0", "777"},
            {"0000", "777"},
            {"770", "7"},
            {"0770", "7"},
            {"0123", "654"},
            {"9", null},
            {"678", null},
            {"ug+rwX,o-rwX", "ug+rwX,o-rwX"},
            {"1770", "7"},
            {"14770", "7"},

        };

        for ( int i = 0; i < tests.length; i++ )
        {
            String umask = null;

            try
            {
                umask = PermissionModeUtils.getUserMaskFor( tests[ i ][ 0 ] );
            }
            catch ( IllegalArgumentException e )
            {
                // nothing to do
            }

            assertEquals( tests[ i ][ 1 ], umask );
        }
    }
}
