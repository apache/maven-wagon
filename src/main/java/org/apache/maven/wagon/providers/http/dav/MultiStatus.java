package org.apache.maven.wagon.providers.http.dav;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * MultiStatus
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MultiStatus
{
    private static DocumentBuilderFactory documentFactory;

    private static Element getChildElement( Element elem, String childNodeName )
    {
        NodeList nodelist = elem.getChildNodes();
        int len = nodelist.getLength();
        for ( int i = 0; i < len; i++ )
        {
            Node node = nodelist.item( i );
            if ( ( node.getNodeType() == Node.ELEMENT_NODE ) && ( childNodeName.equals( node.getNodeName() ) ) )
            {
                return (Element) node;
            }
        }

        return null;
    }

    private static List/*<Element>*/getChildElements( Element elem, String childNodeName )
    {
        List/*<Element>*/elems = new ArrayList/*<Element>*/();

        NodeList nodelist = elem.getChildNodes();
        int len = nodelist.getLength();
        for ( int i = 0; i < len; i++ )
        {
            Node node = nodelist.item( i );
            if ( ( node.getNodeType() == Node.ELEMENT_NODE ) && ( childNodeName.equals( node.getNodeName() ) ) )
            {
                elems.add( node );
            }
        }

        return elems;
    }

    private static String getChildText( Element elem, String childNodeName )
    {
        return getElementText( getChildElement( elem, childNodeName ) );
    }

    private static String getElementText( Element elem )
    {
        if ( elem == null )
        {
            return null;
        }

        StringBuffer text = new StringBuffer();
        NodeList nodelist = elem.getChildNodes();
        int len = nodelist.getLength();
        for ( int i = 0; i < len; i++ )
        {
            Node node = nodelist.item( i );
            if ( node.getNodeType() == Node.TEXT_NODE )
            {
                text.append( node.getNodeValue() );
            }
        }

        if ( text.length() <= 0 )
        {
            return null;
        }

        return text.toString();
    }

    public static MultiStatus parse( InputStream stream )
        throws IOException, SAXException, ParserConfigurationException
    {
        if ( documentFactory == null )
        {
            documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setNamespaceAware( true );
            documentFactory.setValidating( false );
        }
        DocumentBuilder db = documentFactory.newDocumentBuilder();
        Document doc = db.parse( stream );

        MultiStatus multistatus = new MultiStatus();

        Element root = doc.getDocumentElement();
        String namespaceURI = root.getNamespaceURI();

        String rootNodeName = root.getNodeName();

        if ( !"DAV:".equals( namespaceURI ) )
        {
            /* Typically means that the response code was valid, but the response contents 
             * are not in the form of DAV response.
             * This can happen if you request props via PROPFIND on a non-existant URL and the
             * server responds with a custom 404 handler (for example)
             */
            // TODO: getLogger().info( "Encountered invalid namespace <" + namespaceURI + ">, expected <DAV:>" );
            return null;
        }

        String prefix = root.getPrefix();

        if ( prefix != null )
        {
            rootNodeName = rootNodeName.substring( prefix.length() + 1 );
        }

        if ( !"multistatus".equals( rootNodeName ) )
        {
            /* Typically means that the response code was valid, but the response contents 
             * are not in the form of DAV response.
             * This can happen if you request props via PROPFIND on a non-existant URL and the
             * server responds with a custom 404 handler (for example)
             */
            // TODO: getLogger().info( "Encountered unexpected <" + rootNodeName + ">, expected <" + "multistatus" + ">" );
            return null;
        }

        List/*<Element>*/responses = getChildElements( root, prefix + ":response" );

        Element propstat;
        Element prop;
        Element resourceType;
        Element status;
        for ( Iterator itresponses = responses.iterator(); itresponses.hasNext(); )
        {
            Element response = (Element) itresponses.next();
            DavResource resource = new DavResource();

            resource.setHref( getChildText( response, prefix + ":href" ) );

            propstat = getChildElement( response, prefix + ":propstat" );
            if ( propstat != null )
            {
                prop = getChildElement( propstat, prefix + ":prop" );
                if ( prop != null )
                {
                    resource.setContentType( getChildText( prop, prefix + ":getcontentType" ) );
                    resource.setEtag( getChildText( prop, prefix + ":getetag" ) );
                    resource.setCreationDate( getChildText( prop, prefix + ":creationdate" ) );
                    resource.setLastModified( getChildText( prop, prefix + ":getlastmodified" ) );

                    resourceType = getChildElement( prop, prefix + ":resourcetype" );
                    if ( resourceType != null )
                    {
                        if ( getChildElement( resourceType, prefix + ":collection" ) != null )
                        {
                            resource.setAsCollection();
                        }
                    }
                }

                status = getChildElement( propstat, prefix + ":status" );
                if ( status != null )
                {
                    resource.parseStatus( getElementText( status ) );
                }
            }

            multistatus.addResource( resource );
        }

        return multistatus;
    }

    private Map/*<String, DavResource>*/resources = new HashMap/*<String, DavResource>*/();

    public MultiStatus()
    {
        /* ignore */
    }

    public void addResource( DavResource resource )
    {
        this.resources.put( resource.getHref(), resource );
    }

    public List/*<DavResource>*/getCollectionResources()
    {
        List/*<DavResource>*/dirs = new ArrayList/*<DavResource>*/();

        for ( Iterator it = resources.values().iterator(); it.hasNext(); )
        {
            DavResource resource = (DavResource) it.next();
            if ( resource.isCollection() )
            {
                dirs.add( resource );
            }
        }

        return dirs;
    }

    public List/*<DavResource>*/getFileResources()
    {
        List/*<DavResource>*/files = new ArrayList/*<DavResource>*/();

        for ( Iterator it = resources.values().iterator(); it.hasNext(); )
        {
            DavResource resource = (DavResource) it.next();
            if ( !resource.isCollection() )
            {
                files.add( resource );
            }
        }

        return files;
    }

    public DavResource getResource( String href )
    {
        return (DavResource) resources.get( href );
    }

    public Collection/*<DavResource>*/getResources()
    {
        return resources.values();
    }
}
