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

/**
 * The exception is thrown when a connection
 * to repository cannot be estblished or open connection cannot be closed.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class ConnectionException
    extends WagonException
{


    /**
     * @see org.apache.maven.wagon.WagonException
     */
    public ConnectionException( final String message )
    {
        super( message );
    }

    /**
     * @see org.apache.maven.wagon.WagonException
     */
    public ConnectionException( final String message, final Throwable cause )
    {
        super( message, cause );
    }

}
