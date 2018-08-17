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

import java.io.IOException;
import java.io.OutputStream;


/**
 * Abstract wrapper around OutputStream to allow lazy initialization through {@link #initialize()} method
 *
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 * @author <a href="mailto:erikhakan@gmail.com">Erik Håkansson</a>
 *
 * @param <T> type of OutputStream
 *
 */
public abstract class LazyOutputStream<T extends OutputStream>
    extends OutputStream
{

    private T delegee;

    public void close()
        throws IOException
    {
        if ( delegee != null )
        {
            delegee.close();
        }
    }

    public boolean equals( Object obj )
    {
        return delegee.equals( obj );
    }

    public void flush()
        throws IOException
    {
        if ( delegee != null )
        {
            delegee.flush();
        }
    }

    public int hashCode()
    {
        return delegee.hashCode();
    }

    public String toString()
    {
        return delegee.toString();
    }

    public void write( byte[] b )
        throws IOException
    {
        if ( delegee == null )
        {
            initialize();
        }

        delegee.write( b );
    }

    /**
     * @see OutputStream#write(byte[], int, int)
     */
    public void write( byte[] b, int off, int len )
        throws IOException
    {
        if ( delegee == null )
        {
            initialize();
        }
        delegee.write( b, off, len );
    }

    /**
     * @param b
     * @throws IOException
     */
    public void write( int b )
        throws IOException
    {
        if ( delegee == null )
        {
            initialize();
        }
        delegee.write( b );
    }

    protected abstract void initialize() throws IOException;

    protected T getDelegee()
    {
        return delegee;
    }

    protected void setDelegee( T delegee )
    {
        this.delegee = delegee;
    }
}
