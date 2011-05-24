package org.apache.maven.wagon.resource;

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

import org.apache.maven.wagon.WagonConstants;

/**
 * Describes resources which can be downloaded from the repository
 * or uploaded to repository.
 * <p/>
 * This class contains minimal set of informations, which
 * are needed to reuse wagon in maven 1.
 *
 * @author <a href="michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */

public class Resource
{
    private String name;

    private long lastModified;

    private long contentLength = WagonConstants.UNKNOWN_LENGTH;

    public Resource()
    {

    }

    public Resource( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Returns the value of the last-modified header field.
     * The result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the date the resource  was last modified, or WagonConstants.UNKNOWN_LENGTH
     *         if not known.
     */
    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified( long lastModified )
    {
        this.lastModified = lastModified;
    }

    public long getContentLength()
    {
        return contentLength;
    }

    public void setContentLength( long contentLength )
    {
        this.contentLength = contentLength;
    }

    public String toString()
    {
        return name;
    }
    
    public String inspect()
    {
        return name + "[len = " + contentLength + "; mod = " + lastModified + "]";
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) ( contentLength ^ ( contentLength >>> 32 ) );
        result = prime * result + (int) ( lastModified ^ ( lastModified >>> 32 ) );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final Resource other = (Resource) obj;
        if ( contentLength != other.contentLength )
        {
            return false;
        }
        if ( lastModified != other.lastModified )
        {
            return false;
        }
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        return true;
    }
}
