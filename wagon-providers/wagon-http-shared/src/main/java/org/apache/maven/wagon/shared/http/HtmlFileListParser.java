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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.wagon.TransferFailedException;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.codehaus.plexus.util.StringUtils;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.DefaultFilter;

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
    public static List/* <String> */parseFileList( String baseurl, InputStream stream )
        throws TransferFailedException
    {
        try
        {
            // Use URI object to get benefits of proper absolute and relative path resolution for free
            URI baseURI = new URI( baseurl );

            Parser handler = new Parser( baseURI );

            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setDocumentHandler( handler );
            parser.setFeature( "http://cyberneko.org/html/features/augmentations", true );
            parser.setProperty( "http://cyberneko.org/html/properties/names/elems", "upper" );
            parser.setProperty( "http://cyberneko.org/html/properties/names/attrs", "upper" );
            parser.parse( new XMLInputSource( null, baseurl, baseURI.toString(), stream, "UTF-8" ) );

            return new ArrayList( handler.getLinks() );

        }
        catch ( URISyntaxException e )
        {
            throw new TransferFailedException( "Unable to parse as URI: " + baseurl );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "I/O error: " + e.getMessage(), e );
        }
    }

    private static class Parser
        extends DefaultFilter
    {
        // Apache Fancy Index Sort Headers
        private static final Pattern APACHE_INDEX_SKIP = Pattern.compile( "\\?[CDMNS]=.*" );

        // URLs with excessive paths.
        private static final Pattern URLS_WITH_PATHS = Pattern.compile( "/[^/]*/" );

        // URLs that to a parent directory.
        private static final Pattern URLS_TO_PARENT = Pattern.compile( "\\.\\./" );

        // mailto urls
        private static final Pattern MAILTO_URLS = Pattern.compile( "mailto:.*" );

        private static final Pattern[] SKIPS =
            new Pattern[] { APACHE_INDEX_SKIP, URLS_WITH_PATHS, URLS_TO_PARENT, MAILTO_URLS };
        
        private Set links = new HashSet();

        private URI baseURI;

        public Parser( URI baseURI )
        {
            this.baseURI = baseURI;
        }

        public Set getLinks()
        {
            return links;
        }

        public void startElement( QName element, XMLAttributes attrs, Augmentations augs )
        {
            if ( "A".equals( element.rawname ) )
            {
                String href = attrs.getValue( "HREF" );
                if ( href != null )
                {
                    String link = cleanLink( baseURI, href );
                    if ( isAcceptableLink( link ) )
                    {
                        links.add( link );
                    }
                }
            }
        }

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

                ret = URLDecoder.decode( ret, "UTF-8" );
            }
            catch ( URISyntaxException e )
            {
            }
            catch ( UnsupportedEncodingException e )
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

            for ( int i = 0; i < SKIPS.length; i++ )
            {
                if ( SKIPS[i].matcher( link ).find() )
                {
                    return false;
                }
            }

            return true;
        }
    }
}
