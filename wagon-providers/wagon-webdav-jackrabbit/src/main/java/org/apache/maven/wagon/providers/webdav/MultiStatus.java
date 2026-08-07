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
package org.apache.maven.wagon.providers.webdav;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.apache.maven.wagon.providers.webdav.DavMethods.DAV_NAMESPACE;
import static org.apache.maven.wagon.providers.webdav.DavMethods.PROPERTY_RESOURCETYPE;
import static org.apache.maven.wagon.providers.webdav.DavMethods.XML_COLLECTION;

/**
 * The {@code 207 Multi-Status} body of a PROPFIND response, reduced to what this Wagon needs: the
 * href of each response, in document order, and whether that response describes a collection.
 * <p>
 * Responses keep their document order because {@code getFileList} relies on the first entry being
 * the requested collection itself, as mandated by
 * <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.9.1">RFC 4918 section 9.1</a>.
 *
 * @since 4.0.0
 */
final class MultiStatus {

    private final List<Response> responses;

    private MultiStatus(List<Response> responses) {
        this.responses = responses;
    }

    /**
     * A single {@code DAV:response} element.
     */
    static final class Response {
        private final String href;
        private final boolean collection;

        Response(String href, boolean collection) {
            this.href = href;
            this.collection = collection;
        }

        String getHref() {
            return href;
        }

        /**
         * Whether the {@code resourcetype} property carried a {@code collection} child. Only
         * {@code propstat} elements with a {@code 200} status are considered; a resource whose
         * {@code resourcetype} is absent or empty is not a collection.
         */
        boolean isCollection() {
            return collection;
        }
    }

    List<Response> getResponses() {
        return responses;
    }

    /**
     * Parses a multistatus document.
     *
     * @param in the response body, never {@code null}
     * @return the parsed responses, possibly empty but never {@code null}
     * @throws IOException if the body is not a well-formed multistatus document
     */
    static MultiStatus parse(InputStream in) throws IOException {
        Document document;
        try {
            document = newDocumentBuilder().parse(in);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Cannot parse multistatus response: " + e.getMessage(), e);
        }

        Element root = document.getDocumentElement();
        if (root == null || !isDavElement(root, "multistatus")) {
            throw new IOException("Expected a DAV:multistatus response body");
        }

        // an href must occur only once per RFC 4918, so later duplicates are dropped rather than
        // reported twice; a LinkedHashMap keeps the document order the callers depend on
        Map<String, Response> responses = new LinkedHashMap<>();
        for (Element response : childElements(root, "response")) {
            String href = null;
            for (Element hrefElement : childElements(response, "href")) {
                href = hrefElement.getTextContent();
                break;
            }
            if (href != null) {
                href = href.trim();
                responses.put(href, new Response(href, isCollection(response)));
            }
        }
        return new MultiStatus(Collections.unmodifiableList(new ArrayList<>(responses.values())));
    }

    /**
     * Looks for {@code resourcetype/collection} inside any {@code propstat} that reported a
     * {@code 200} status.
     */
    private static boolean isCollection(Element response) {
        for (Element propstat : childElements(response, "propstat")) {
            if (!isOkStatus(propstat)) {
                continue;
            }
            for (Element prop : childElements(propstat, "prop")) {
                for (Element resourceType : childElements(prop, PROPERTY_RESOURCETYPE)) {
                    if (!childElements(resourceType, XML_COLLECTION).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Reads the {@code status} child, whose text is a status line such as {@code HTTP/1.1 200 OK}.
     * A {@code propstat} without a status element is treated as successful, matching how lenient
     * servers are read elsewhere.
     */
    private static boolean isOkStatus(Element propstat) {
        List<Element> statusElements = childElements(propstat, "status");
        if (statusElements.isEmpty()) {
            return true;
        }
        String[] tokens = statusElements.get(0).getTextContent().trim().split("\\s+");
        for (String token : tokens) {
            if (token.length() == 3 && token.chars().allMatch(Character::isDigit)) {
                return "200".equals(token);
            }
        }
        return false;
    }

    private static List<Element> childElements(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && isDavElement((Element) child, localName)) {
                elements.add((Element) child);
            }
        }
        return elements;
    }

    /**
     * Matches on local name within the {@code DAV:} namespace. Servers that emit the WebDAV
     * elements without a namespace are tolerated, since some do.
     */
    private static boolean isDavElement(Element element, String localName) {
        String elementLocalName = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        int colon = elementLocalName.indexOf(':');
        if (colon >= 0) {
            elementLocalName = elementLocalName.substring(colon + 1);
        }
        if (!localName.equals(elementLocalName)) {
            return false;
        }
        String namespace = element.getNamespaceURI();
        return namespace == null || DAV_NAMESPACE.equals(namespace);
    }

    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // multistatus bodies come from a remote server, so no external entity resolution
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }
}
