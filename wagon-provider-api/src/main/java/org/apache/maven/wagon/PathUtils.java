package org.apache.maven.wagon;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.util.StringTokenizer;

/**
 * Various path (URL) manipulation routines
 * 
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class PathUtils
{
    /**
     * Returns the directory path portion of a file specification string.
     * Matches the equally named unix command.
     * 
     * @return The directory portion excluding the ending file separator.
     */
    public static String dirname( final String path )
    {
        final int i = path.lastIndexOf( "/" );

        return ( ( i >= 0 ) ? path.substring( 0, i ) : "" );
    }

    /**
     * Returns the filename portion of a file specification string.
     * 
     * @return The filename string with extension.
     */
    public static String filename( final String path )
    {
        final int i = path.lastIndexOf( "/" );
        return ( ( i >= 0 ) ? path.substring( i + 1 ) : path );
    }

    public static String[] dirnames( final String path )
    {
        final String dirname = PathUtils.dirname( path );
        return split( dirname, "/", -1 );

    }

    private static String[] split(
            final String str,
            final String separator,
            final int max )
    {
        final StringTokenizer tok;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();
        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        final String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                final String endToken = tok.nextToken();
                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );
                list[i] = str.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }

    /**
     * Return the host name (Removes protocol and path from the URL) E.g: for input
     * <code>http://www.codehause.org</code> this method will return <code>www.apache.org</code>
     * 
     * @param url the url
     * 
     * @return the host name
     */
    public static String host( final String url )
    {
        if ( url == null )
        {
            return "localhost";
        }

        final String protocol = PathUtils.protocol( url );
        if ( protocol == null || protocol.equals( "file" ) )
        {
            return "";
        }
        String host = url.substring( url.indexOf( "://" ) + 3 ).trim();

        int pos = host.indexOf( "/" );
        if ( pos > 0 )
        {
            host = host.substring( 0, pos );
        }
        pos = host.indexOf( ":" );
        if ( pos > 0 )
        {
            host = host.substring( 0, pos );
        }
        return host;
    }

    /**
     * /**
     * Return the protocol name.
     * <br/>
     * E.g: for input
     * <code>http://www.codehause.org</code> this method will return <code>http</code>
     * 
     * @param url the url
     * 
     * @return the host name
     */
    public static String protocol( final String url )
    {

        final int pos = url.indexOf( "://" );
        if ( pos == -1 )
        {
            return "";
        }
        return url.substring( 0, pos ).trim();
    }

    /**
     * @param url 
     * 
     * @return 
     */
    public static int port( final String url )
    {

        final String protocol = PathUtils.protocol( url );
        if ( protocol == null || protocol.equals( "file" ) )
        {
            return WagonConstants.UNKNOWN_PORT;
        }
        final String host = PathUtils.host( url );
        if ( host == null )
        {
            return WagonConstants.UNKNOWN_PORT;
        }

        final String prefix = protocol + "://" + host;
        final int start = prefix.length();

        if ( url.length() > start && url.charAt( start ) == ':' )
        {
            int end = url.indexOf( '/', start );
            if ( end == -1 )
            {
                end = url.length();
            }
            return Integer.parseInt( url.substring( start + 1, end ) );
        }
        else
        {
            return WagonConstants.UNKNOWN_PORT;
        }

    }

    /**
     * @param url 
     * 
     * @return 
     */
    public static String basedir( final String url )
    {
        final String protocol = PathUtils.protocol( url );
        String retValue = null;
        if ( protocol.equals( "file" ) )
        {
            retValue = url.substring( protocol.length() + 3 );
        }
        else
        {
            final String host = PathUtils.host( url );
            final int port = PathUtils.port( url );
            final int pos;
            if ( port != WagonConstants.UNKNOWN_PORT )
            {
                pos = ( protocol + "://" + host + ":" + port + "/" ).length();

            }
            else
            {
                pos = ( protocol + "://" + host + "/" ).length();
            }
            if ( url.length() > pos )
            {
                retValue = url.substring( pos );

            }

        }

        if ( retValue == null )
        {
            retValue = "";
        }
        return retValue.trim();
    }

}
