package org.apache.maven.wagon.tck.http.fixture;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyAuthenticationFilter
    implements Filter
{

    private final String username;

    private final String password;

    public ProxyAuthenticationFilter( final String username, final String password )
    {
        this.username = username;
        this.password = password;
    }

    public void destroy()
    {
    }

    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String header = request.getHeader( "Proxy-Authorization" );
        if ( header == null )
        {
            response.setStatus( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
            response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );
            return;
        }
        else
        {
            String data = header.substring( "BASIC ".length() );
            data = new String( Base64.decodeBase64( data ) );
            String[] creds = data.split( ":" );

            if ( !creds[0].equals( username ) || !creds[1].equals( password ) )
            {
                response.sendError( HttpServletResponse.SC_UNAUTHORIZED );
            }
        }

        chain.doFilter( req, resp );
    }

    public void init( final FilterConfig filterConfig )
        throws ServletException
    {
    }

}
