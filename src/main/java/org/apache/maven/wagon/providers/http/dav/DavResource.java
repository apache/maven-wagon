package org.apache.maven.wagon.providers.http.dav;

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

import org.codehaus.plexus.util.StringUtils;

/**
 * DavResource
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DavResource
{
    private boolean collection = false;

    private String contentType;

    private String creationDate;

    private String etag;

    private String href;

    private String lastModified;

    private int status;

    public DavResource()
    {
        /* default */
    }

    public DavResource( String href )
    {
        this.href = href;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getCreationDate()
    {
        return creationDate;
    }

    public String getEtag()
    {
        return etag;
    }

    public String getHref()
    {
        return href;
    }

    public String getLastModified()
    {
        return lastModified;
    }

    public int getStatus()
    {
        return status;
    }

    public boolean isCollection()
    {
        return collection;
    }

    public void parseStatus( String statusLine )
    {
        if ( StringUtils.isNotEmpty( statusLine ) )
        {
            String chunks[] = StringUtils.split( statusLine, " " );
            if ( chunks.length >= 2 )
            {
                try
                {
                    status = Integer.parseInt( chunks[1], 10 );
                }
                catch ( NumberFormatException e )
                {
                    status = 0;
                }
            }
        }
    }

    public void setAsCollection()
    {
        this.collection = true;
    }

    public void setCollection( boolean collection )
    {
        this.collection = collection;
    }

    public void setContentType( String contentType )
    {
        this.contentType = contentType;
    }

    public void setCreationDate( String creationDate )
    {
        this.creationDate = creationDate;
    }

    public void setEtag( String etag )
    {
        this.etag = etag;
    }

    public void setHref( String href )
    {
        this.href = href;
    }

    public void setLastModified( String lastModified )
    {
        this.lastModified = lastModified;
    }

    public void setStatus( int status )
    {
        this.status = status;
    }
}
