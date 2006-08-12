package org.apache.maven.wagon.shared.http;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.wagon.TransferFailedException;
import org.apache.xerces.parsers.DOMParser;
import org.codehaus.plexus.util.StringUtils;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Html File List Parser.
 */
public class HtmlFileListParser
{
    /**
     * Fetches a raw HTML from a provided InputStream, parses it, and returns the file list.
     * 
     * @param is the input stream.
     * @return the file list.
     * @throws TransferFailedException if there was a problem fetching the raw html.
     */
    public static List parseFileList( String baseurl, InputStream is )
        throws TransferFailedException
    {
        try
        {
            DOMParser parser = new DOMParser( new HTMLConfiguration() );
            parser.setFeature( "http://xml.org/sax/features/namespaces", false );

            parser.parse( new InputSource( is ) );

            Document doc = parser.getDocument();

            List links = new ArrayList();
            links = findAnchorLinks( links, baseurl, doc );

            return links;
        }
        catch ( SAXException e )
        {
            throw new TransferFailedException( "Unable to parse HTML.", e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unable to parse HTML.", e );
        }
    }

    private static List findAnchorLinks( List links, String baseurl, Node node )
    {
        String basepath = baseurl;
        
        int colslash = basepath.indexOf( "://" );
        if ( colslash > 0 )
        {
            int pathstart = basepath.indexOf( '/', colslash + 3 );
            if(pathstart > 0)
            {
                // slash starts path
                // "http://localhost:10007/test/path/" = "/test/path"
                basepath = baseurl.substring( pathstart );
            } else
            {
                // no path means top level.
                // "http://localhost:10007" = ""
                basepath = "";
            }
        }

        if ( node.getNodeName().equalsIgnoreCase( "a" ) )
        {
            if ( node.hasAttributes() )
            {
                String key;
                String value;
                NamedNodeMap attributes = node.getAttributes();
                for ( int i = 0; i < attributes.getLength(); i++ )
                {
                    key = attributes.item( i ).getNodeName().toLowerCase();
                    if ( "href".equals( key ) )
                    {
                        value = attributes.item( i ).getNodeValue();
                        if ( StringUtils.isNotEmpty( value ) )
                        {
                            value = StringUtils.trim( value );
                            if ( validFilename( value ) )
                            {
                                // simple filename.
                                links.add( value );
                            }
                            else
                            {
                                // Potentially Complex Filename.

                                // Starts with full URL base "http://www.ibiblio.org/maven2/"
                                if ( value.startsWith( baseurl ) )
                                {
                                    String tst = value.substring( baseurl.length() );
                                    if ( validFilename( tst ) )
                                    {
                                        links.add( tst );
                                        continue;
                                    }
                                }

                                // Starts with host relative base url "/maven2/"
                                if ( value.startsWith( basepath ) )
                                {
                                    String tst = value.substring( basepath.length() );
                                    if ( validFilename( tst ) )
                                    {
                                        links.add( tst );
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if ( node.hasChildNodes() )
        {
            NodeList nodes = node.getChildNodes();
            for ( int nodenum = 0; nodenum < nodes.getLength(); nodenum++ )
            {
                findAnchorLinks( links, baseurl, nodes.item( nodenum ) );
            }
        }

        return links;
    }

    private static boolean validFilename( String tst )
    {
        final Pattern badFilenames = Pattern.compile( "[:?&@*]" );

        if ( badFilenames.matcher( tst ).find() )
        {
            return false;
        }

        String tstpath = StringUtils.replace( tst, '\\', '/' );
        int pathparts = StringUtils.countMatches( tstpath, "/" );

        if ( pathparts > 1 )
        {
            return false;
        }
        else if ( ( pathparts == 1 ) && ( !tstpath.endsWith( "/" ) ) )
        {
            return false;
        }

        return true;
    }
}
