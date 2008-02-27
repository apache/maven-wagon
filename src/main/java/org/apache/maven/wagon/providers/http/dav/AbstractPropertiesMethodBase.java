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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.codehaus.plexus.util.IOUtil;
import org.xml.sax.SAXException;

/**
 * AbstractPropertiesMethodBase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractPropertiesMethodBase
    extends HttpMethodBase
{

    public static final int DEPTH_0 = 0;

    public static final int DEPTH_1 = 1;

    public static final int DEPTH_INFINITY = Integer.MAX_VALUE;

    private int depth = 0;

    private MultiStatus multiStatus;

    public AbstractPropertiesMethodBase()
    {
        super();
    }

    public AbstractPropertiesMethodBase( String uri )
        throws IllegalArgumentException, IllegalStateException
    {
        super( uri );
    }

    public MultiStatus getMultiStatus()
    {
        return multiStatus;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth( int depth )
    {
        this.depth = depth;
    }

    protected void readResponseBody( HttpState state, HttpConnection conn )
        throws IOException, HttpException
    {
        super.readResponseBody( state, conn );

        InputStream input = getResponseBodyAsStream();
        if ( input == null )
        {
            return;
        }

        try
        {
            this.multiStatus = MultiStatus.parse( input );
        }
        catch ( SAXException e )
        {
            throw new IOException( "Unable to parse multistatus: " + e.getMessage() );
        }
        catch ( ParserConfigurationException e )
        {
            throw new IOException( "Unable to parse multistatus: " + e.getMessage() );
        }
        finally
        {
            IOUtil.close( input );
        }
    }
}
