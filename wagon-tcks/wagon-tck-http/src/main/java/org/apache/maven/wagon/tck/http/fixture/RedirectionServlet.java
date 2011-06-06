package org.apache.maven.wagon.tck.http.fixture;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectionServlet
    extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    private final String targetPath;

    private final int code;

    private final int maxRedirects;

    private int redirectCount = 0;

    private final String myPath;

    public RedirectionServlet( final int code, final String path )
    {
        this.code = code;
        this.targetPath = path;
        this.maxRedirects = 1;
        this.myPath = null;
    }

    public RedirectionServlet( final int code, final String myPath, final String targetPath, final int maxRedirects )
    {
        this.code = code;
        this.myPath = myPath;
        this.targetPath = targetPath;
        this.maxRedirects = maxRedirects;
    }

    public int getRedirectCount()
    {
        return redirectCount;
    }

    @Override
    protected void service( final HttpServletRequest req, final HttpServletResponse resp )
        throws ServletException, IOException
    {
        redirectCount++;

        if ( myPath == null )
        {
            resp.setStatus( code );
            resp.setHeader( "Location", targetPath );
        }
        else if ( maxRedirects < 0 )
        {
            resp.setStatus( code );
            resp.setHeader( "Location", myPath );
        }
        else if ( redirectCount <= maxRedirects )
        {
            resp.setStatus( code );
            resp.setHeader( "Location", myPath + "/" + redirectCount );
        }
        else
        {
            resp.setStatus( code );
            resp.setHeader( "Location", targetPath );
        }
    }

}
