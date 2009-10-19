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

public class AuthSnoopFilter
    implements Filter
{

    public void destroy()
    {
    }

    public void doFilter( final ServletRequest req, final ServletResponse response, final FilterChain chain )
        throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        String authHeader = request.getHeader( "Authorization" );

        if ( authHeader != null )
        {
            System.out.println( "Authorization: " + authHeader );
            String data = authHeader.substring( "BASIC ".length() );
            String decoded = new String( Base64.decodeBase64( data ) );
            System.out.println( decoded );
            String[] creds = decoded.split( ":" );

            System.out.println( "User: " + creds[0] + "\nPassword: " + creds[1] );
        }
    }

    public void init( final FilterConfig filterConfig )
        throws ServletException
    {
    }

}
