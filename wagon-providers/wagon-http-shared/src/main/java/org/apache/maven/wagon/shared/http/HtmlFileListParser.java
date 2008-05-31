package org.apache.maven.wagon.shared.http;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Html File List Parser.
 */
public class HtmlFileListParser
{
    private static final Set/*<Pattern>*/skips = new HashSet/*<Pattern>*/();

    static
    {
        // Apache Fancy Index Sort Headers
        skips.add( Pattern.compile( "\\?[CDMNS]=.*" ) );

        // URLs with excessive paths.
        skips.add( Pattern.compile( "/[^/]*/" ) );

        // URLs that to a parent directory.
        skips.add( Pattern.compile( "\\.\\./" ) );

        // mailto urls
        skips.add( Pattern.compile( "mailto:.*" ) );
    }

    /**
     * Fetches a raw HTML from a provided InputStream, parses it, and returns the file list.
     * 
     * @param is the input stream.
     * @return the file list.
     * @throws TransferFailedException if there was a problem fetching the raw html.
     */
    public static List/*<String>*/parseFileList( String baseurl, InputStream stream )
        throws TransferFailedException
    {
        try
        {
            // Use URI object to get benefits of proper absolute and relative path resolution for free
            URI baseURI = new URI( baseurl );

            DOMParser parser = new DOMParser();
            parser.setFeature( "http://cyberneko.org/html/features/augmentations", true );
            parser.setProperty( "http://cyberneko.org/html/properties/names/elems", "upper" );
            parser.setProperty( "http://cyberneko.org/html/properties/names/attrs", "upper" );
            parser.parse( new InputSource( stream ) );

            Set/*<String>*/links = new HashSet/*<String>*/();

            recursiveLinkCollector( parser.getDocument(), baseURI, links );

            return new ArrayList( links );

        }
        catch ( URISyntaxException e )
        {
            throw new TransferFailedException( "Unable to parse as URI: " + baseurl );
        }
        catch ( SAXNotRecognizedException e )
        {
            throw new TransferFailedException( "Unable to setup XML/SAX: " + e.getMessage(), e );
        }
        catch ( SAXNotSupportedException e )
        {
            throw new TransferFailedException( "XML/SAX not supported?: " + e.getMessage(), e );
        }
        catch ( SAXException e )
        {
            throw new TransferFailedException( "XML/SAX error: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "I/O error: " + e.getMessage(), e );
        }
    }

    private static void recursiveLinkCollector( Node node, URI baseURI, Set/*<String>*/links )
    {
        if ( node.getNodeType() == Node.ELEMENT_NODE )
        {
            //            System.out.println("Element <" + node.getNodeName() + dumpAttributes((Element) node) + ">");
            if ( "A".equals( node.getNodeName() ) )
            {
                Element anchor = (Element) node;
                NamedNodeMap nodemap = anchor.getAttributes();
                Node href = nodemap.getNamedItem( "HREF" );
                if ( href != null )
                {
                    String link = cleanLink( baseURI, href.getNodeValue() );
                    //                    System.out.println("HREF (" + href.getNodeValue() + " => " + link + ")");
                    if ( isAcceptableLink( link ) )
                    {
                        links.add( link );
                    }
                }
            }
        }

        Node child = node.getFirstChild();
        while ( child != null )
        {
            recursiveLinkCollector( child, baseURI, links );
            child = child.getNextSibling();
        }
    }

    //    private String dumpAttributes(Element elem) {
    //        StringBuffer buf = new StringBuffer();
    //        NamedNodeMap nodemap = elem.getAttributes();
    //        int len = nodemap.getLength();
    //        for (int i = 0; i < len; i++) {
    //            Node att = nodemap.item(i);
    //            buf.append(" ");
    //            buf.append(att.getNodeName()).append("=\"");
    //            buf.append(att.getNodeValue()).append("\"");
    //        }
    //        return buf.toString();
    //    }

    private static String cleanLink( URI baseURI, String link )
    {
        if ( StringUtils.isEmpty( link ) )
        {
            return "";
        }

        String ret = link;

        try
        {
            URI linkuri = new URI( ret );
            URI relativeURI = baseURI.relativize( linkuri ).normalize();
            ret = relativeURI.toASCIIString();
            if ( ret.startsWith( baseURI.getPath() ) )
            {
                ret = ret.substring( baseURI.getPath().length() );
            }
            // TODO: Fix string escaping properly.
            ret = StringUtils.replace( ret, "%20", " " );
            ret = StringUtils.replace( ret, "+", " " );
        }
        catch ( URISyntaxException e )
        {
        }

        return ret;
    }
    
    

    private static boolean isAcceptableLink( String link )
    {
        if ( StringUtils.isEmpty( link ) )
        {
            return false;
        }

        for ( Iterator it = skips.iterator(); it.hasNext(); )
        {
            Pattern skipPat = (Pattern) it.next();
            if ( skipPat.matcher( link ).find() )
            {
                return false;
            }
        }

        return true;
    }
}
