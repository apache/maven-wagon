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

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;

/**
 * User: jdumay Date: 24/01/2008 Time: 18:15:53
 */
public abstract class HttpWagonHttpServerTestCase
    extends PlexusTestCase
{
    protected final int httpServerPort = 10008;

    private Server server;

    protected ResourceHandler resourceHandler;

    protected Context context;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        server = new Server( httpServerPort );

        context = new Context( server, "/", Context.SESSIONS );

        resourceHandler = new ResourceHandler();
        server.addHandler( resourceHandler );
    }

    protected Wagon getWagon()
        throws Exception
    {
        return (Wagon) lookup( HttpWagon.ROLE );
    }

    protected void startServer()
        throws Exception
    {
        server.start();
    }

    protected void stopServer()
        throws Exception
    {
        server.stop();
    }
}
