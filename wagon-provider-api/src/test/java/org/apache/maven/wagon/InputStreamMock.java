package org.apache.maven.wagon;

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

import java.io.IOException;
import java.io.InputStream;


/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class InputStreamMock
    extends InputStream
{

    private boolean closed = false;

    boolean forcedError = false;

    public void close()
    {
        closed = true;
    }

    /**
     * @return Returns the closed.
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * @return Returns the forcedError.
     */
    public boolean isForcedError()
    {
        return forcedError;
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read()
        throws IOException
    {
        if ( forcedError )
        {
            throw new IOException( "Mock exception" );
        }
        return 0;
    }

    /**
     * @param forcedError The forcedError to set.
     */
    public void setForcedError( final boolean forcedError )
    {
        this.forcedError = forcedError;
    }

}
