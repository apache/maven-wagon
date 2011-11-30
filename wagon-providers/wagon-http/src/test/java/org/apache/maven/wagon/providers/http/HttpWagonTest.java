package org.apache.maven.wagon.providers.http;

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

import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.apache.maven.wagon.shared.http4.HttpConfiguration;
import org.apache.maven.wagon.shared.http4.HttpMethodConfiguration;

import java.util.Properties;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class HttpWagonTest
    extends HttpWagonTestCase
{
    protected String getProtocol()
    {
        return "http";
    }

    protected String getTestRepositoryUrl()
    {
        return getProtocol() + "://localhost:" + getTestRepositoryPort();
    }

    protected void setHttpHeaders( StreamingWagon wagon, Properties properties )
    {
        HttpConfiguration config = new HttpConfiguration();

        HttpMethodConfiguration methodConfiguration = new HttpMethodConfiguration();
        methodConfiguration.setHeaders( properties );
        config.setAll( methodConfiguration );
        ( (HttpWagon) wagon ).setHttpConfiguration( config );
    }

    public void testDefaultPooledConnectionManager()
        throws Exception
    {
        HttpWagon wagon = (HttpWagon) lookup( Wagon.class, "http" );
        assertTrue( wagon.getConnectionManager() instanceof ThreadSafeClientConnManager );

    }

    @Override
    protected boolean supportPreemptiveAuthentication()
    {
        return true;
    }
}
