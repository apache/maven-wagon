package org.apache.maven.wagon;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MockWagon
    extends StreamWagon
{
    private boolean errorInputStream;

    public MockWagon()
    {
    }

    public MockWagon( boolean errorInputStream )
    {
        this.errorInputStream = errorInputStream;
    }



    public InputStream getInputStream( String resource )
        throws TransferFailedException
    {
        if ( errorInputStream )
        {
            MockInputStream is = new MockInputStream();

            is.setForcedError( true );

            return is;
        }

        byte[] buffer = new byte[1024 * 4 * 5];

        ByteArrayInputStream is = new ByteArrayInputStream( buffer );

        return is;
    }

    public OutputStream getOutputStream( String resource )
        throws TransferFailedException
    {
        if ( errorInputStream )
        {
            MockOutputStream os = new MockOutputStream();

            os.setForcedError( true );

            return os;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        return os;
    }

    public void openConnection()
    {
    }

    public void closeConnection()
    {
    }
}
