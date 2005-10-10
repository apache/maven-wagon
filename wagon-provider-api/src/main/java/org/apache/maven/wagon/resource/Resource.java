package org.apache.maven.wagon.resource;

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
}
