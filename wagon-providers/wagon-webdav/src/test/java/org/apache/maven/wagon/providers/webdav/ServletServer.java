package org.apache.maven.wagon.providers.webdav;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.MultiException;

/**
 * Plexus Component to start a Jetty Server with servlet settings.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class ServletServer
    implements Initializable, Startable
{
    public static final String ROLE = ServletServer.class.getName();
    
    private Server server;

    private int port;

    private List contexts;

    public void initialize()
        throws InitializationException
    {
        server = new Server();

        SocketListener listener = new SocketListener();
        listener.setPort( port );

        server.addListener( listener );

        if ( contexts != null )
        {
            try
            {
                Iterator itcontext = contexts.iterator();

                while ( itcontext.hasNext() )
                {
                    Context wdc = (Context) itcontext.next();

                    ServletHttpContext context = (ServletHttpContext) server.getContext( wdc.getId() );

                    initContext( wdc, context );
                }
            }
            catch ( Exception e )
            {
                throw new InitializationException( "Unable to initialize.", e );
            }
        }
    }

    private void initContext( Context wdc, ServletHttpContext context )
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Iterator itpaths = wdc.getServlets().iterator();
        while ( itpaths.hasNext() )
        {
            Servlet servlet = (Servlet) itpaths.next();
            initServlet( context, servlet );
        }
    }

    private void initServlet( ServletHttpContext context, Servlet path )
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        ServletHolder servlet = context.addServlet( path.getId(), path.getPath(), path.getServlet() );

        Iterator itparams = path.getParameters().entrySet().iterator();
        while ( itparams.hasNext() )
        {
            Map.Entry entry = (Entry) itparams.next();
            servlet.setInitParameter( (String) entry.getKey(), (String) entry.getValue() );
        }
    }

    public void start()
        throws StartingException
    {
        try
        {
            server.start();
        }
        catch ( MultiException e )
        {
            throw new StartingException( "Error starting the jetty webdav server: ", e );
        }
    }

    public void stop()
        throws StoppingException
    {
        try
        {
            server.stop();
        }
        catch ( InterruptedException e )
        {
            throw new StoppingException( "Error stopping the jetty webdav server: ", e );
        }
    }
}
