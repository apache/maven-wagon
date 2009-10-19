package org.apache.maven.wagon.tck.http.fixture;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletExceptionServlet
    extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    private final String message;

    public ServletExceptionServlet( final String message )
    {
        this.message = message;
    }

    @Override
    protected void service( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        throw new ServletException( message );
    }

}
