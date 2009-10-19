package org.apache.maven.wagon.tck.http.fixture;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ProxyConnectionVerifierFilter
    implements Filter
{

    public void destroy()
    {
    }

    @SuppressWarnings( "unchecked" )
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        // HttpServletResponse response = (HttpServletResponse) resp;

        Enumeration<String> kEn = request.getHeaderNames();
        for ( String key : Collections.list( kEn ) )
        {
            if ( key == null )
            {
                continue;
            }

            Enumeration<String> vEn = request.getHeaders( key );
            if ( vEn != null )
            {
                for ( String val : Collections.list( vEn ) )
                {
                    System.out.println( key + ": " + val );
                }
            }
        }

        chain.doFilter( req, resp );
    }

    public void init( final FilterConfig filterConfig )
        throws ServletException
    {
    }

}
