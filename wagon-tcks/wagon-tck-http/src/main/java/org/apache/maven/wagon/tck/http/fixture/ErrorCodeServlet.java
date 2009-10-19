package org.apache.maven.wagon.tck.http.fixture;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ErrorCodeServlet
    extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    private final int code;

    private final String message;

    public ErrorCodeServlet( final int code, final String message )
    {
        this.code = code;
        this.message = message;
    }

    @Override
    protected void service( final HttpServletRequest req, final HttpServletResponse resp )
        throws ServletException, IOException
    {
        resp.sendError( code, message );
    }

}
