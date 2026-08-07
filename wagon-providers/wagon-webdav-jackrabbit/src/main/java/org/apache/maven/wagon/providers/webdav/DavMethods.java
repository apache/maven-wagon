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

import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 * The handful of WebDAV requests this Wagon issues.
 * <p>
 * Only MKCOL and a deliberately narrow flavour of PROPFIND are needed: creating collections when
 * deploying, and discovering whether a resource is a collection and what it contains. See
 * <a href="http://www.webdav.org/specs/rfc4918.html">RFC 4918</a>.
 *
 * @since 4.0.0
 */
final class DavMethods {

    /** The WebDAV XML namespace. */
    static final String DAV_NAMESPACE = "DAV:";

    /** Name of the {@code resourcetype} property, used to tell collections from plain resources. */
    static final String PROPERTY_RESOURCETYPE = "resourcetype";

    /** Name of the {@code displayname} property. */
    static final String PROPERTY_DISPLAYNAME = "displayname";

    /** Child of {@code resourcetype} marking a resource as a collection. */
    static final String XML_COLLECTION = "collection";

    /** Apply the request to the resource itself only. */
    static final int DEPTH_0 = 0;

    /** Apply the request to the resource and its immediate children. */
    static final int DEPTH_1 = 1;

    private DavMethods() {
        // utility class
    }

    /**
     * MKCOL request, creating a single collection. The parent collection must already exist, so
     * callers deploying a nested path have to walk it from the top down.
     */
    static final class HttpMkcol extends HttpRequestBase {
        HttpMkcol(String uri) {
            setURI(java.net.URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "MKCOL";
        }
    }

    /**
     * PROPFIND request asking for a single named property in the {@code DAV:} namespace.
     * <p>
     * The response is a {@code 207 Multi-Status} document; hand it to
     * {@link MultiStatus#parse} to read it.
     */
    static final class HttpPropfind extends HttpEntityEnclosingRequestBase {
        HttpPropfind(String uri, String propertyName, int depth) {
            setURI(java.net.URI.create(uri));
            setHeader("Depth", String.valueOf(depth));
            setEntity(new StringEntity(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<D:propfind xmlns:D=\"" + DAV_NAMESPACE + "\">"
                            + "<D:prop><D:" + propertyName + "/></D:prop>"
                            + "</D:propfind>",
                    ContentType.create("text/xml", StandardCharsets.UTF_8)));
        }

        @Override
        public String getMethod() {
            return "PROPFIND";
        }
    }
}
