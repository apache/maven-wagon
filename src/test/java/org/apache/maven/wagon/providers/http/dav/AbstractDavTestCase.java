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

import it.could.webdav.DAVServlet;

import java.io.File;
import java.net.URI;

import org.apache.maven.wagon.providers.http.HttpWagonHttpServerTestCase;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * AbstractDavTestCase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AbstractDavTestCase
    extends HttpWagonHttpServerTestCase
{
    protected URI startDavServer( File serverRoot )
        throws Exception
    {
        super.setUp();
        ServletHandler servlets = new ServletHandler();
        ServletHolder servletHolder = servlets.addServlet( "/dav/*", DAVServlet.class.getName() );
        servletHolder.setInitParameter( "rootPath", serverRoot.getAbsolutePath() );
        context.addHandler( servlets );
        startServer();

        return new URI( "http://localhost:" + httpServerPort + "/dav/" );
    }

}
