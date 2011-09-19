package org.apache.jackrabbit.webdav.client.methods;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * <code>XmlRequestEntity</code>...
 */
public class XmlRequestEntity
    implements RequestEntity
{

    private static Logger log = LoggerFactory.getLogger( XmlRequestEntity.class );

    private final RequestEntity delegatee;

    public XmlRequestEntity( Document xmlDocument )
        throws IOException
    {
        super();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
            transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
            transformer.setOutputProperty( OutputKeys.INDENT, "no" );
            transformer.transform( new DOMSource( xmlDocument ), new StreamResult( out ) );
        }
        catch ( TransformerException e )
        {
            log.error( "XML serialization failed", e );
            IOException exception = new IOException( "XML serialization failed" );
            exception.initCause( e );
            throw exception;
        }

        delegatee = new StringRequestEntity( out.toString(), "text/xml", "UTF-8" );
    }

    public boolean isRepeatable()
    {
        return delegatee.isRepeatable();
    }

    public String getContentType()
    {
        return delegatee.getContentType();
    }

    public void writeRequest( OutputStream out ) throws IOException
    {
        delegatee.writeRequest( out );
    }

    public long getContentLength()
    {
        return delegatee.getContentLength();
    }
}