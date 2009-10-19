package org.apache.maven.wagon.tck.http.fixture;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
            resp.setHeader( "Location", myPath );
        }
        else
        {
            resp.setStatus( code );
            resp.setHeader( "Location", targetPath );
        }
    }

}
