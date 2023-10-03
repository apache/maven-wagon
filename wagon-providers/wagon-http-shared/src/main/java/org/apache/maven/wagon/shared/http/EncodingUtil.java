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
package org.apache.maven.wagon.shared.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Encoding utility.
 *
 * @since 2.7
 */
public class EncodingUtil {
    /**
     * Parses and returns an encoded version of the given URL string.
     *
     * @param url Raw/decoded string form of a URL to parse/encode.
     * @return Parsed/encoded {@link URI} that represents the string form URL passed in.
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @deprecated to be removed with 4.0.0
     */
    @Deprecated
    public static URI encodeURL(String url) throws MalformedURLException, URISyntaxException {
        URL urlObject = new URL(url);

        URI uriEncoded = new URI(
                urlObject.getProtocol(), //
                urlObject.getAuthority(), //
                urlObject.getPath(), //
                urlObject.getQuery(), //
                urlObject.getRef());

        return uriEncoded;
    }

    /**
     * Parses and returns an encoded version of the given URL string.
     * Wraps the {@link MalformedURLException} and {@link URISyntaxException} in case the passed URL is invalid.
     *
     * @param url Raw/decoded string form of a URL to parse/encode.
     * @return Parsed/encoded URI (as string) that represents the
     * @throws IllegalArgumentException in case the URL string is invalid.
     * @deprecated To be remvoed with 4.0.0
     */
    @Deprecated
    public static String encodeURLToString(String url) {
        try {
            return encodeURL(url).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Error parsing url: %s", url), e);
        }
    }

    /**
     * Parses and returns an encoded version of the given URL string alongside the given path.
     *
     * @param baseUrl Base URL to use when constructing the final URL. This has to be a valid URL already.
     * @param path Additional unencoded path to append at the end of the base path.
     * @return Composed URL (base + path) already encoded, separating the individual path segments by "/".
     * @since TODO
     */
    public static String encodeURLToString(String baseUrl, String path) {
        String[] pathSegments = path == null ? new String[0] : path.split("/");

        String encodedUrl = encodeURLToString(baseUrl, pathSegments);
        if (path != null && path.endsWith("/")) {
            return encodedUrl + "/";
        }

        return encodedUrl;
    }

    /**
     * Parses and returns an encoded version of the given URL string alongside the given path segments.
     *
     * @param baseUrl Base URL to use when constructing the final URL. This has to be a valid URL already.
     * @param pathSegments Additional unencoded path segments to append at the end of the base path.
     * @return Composed URL (base + path) already encoded, separating the individual path segments by "/".
     * @since TODO
     */
    public static String encodeURLToString(String baseUrl, String... pathSegments) {
        StringBuilder url = new StringBuilder(baseUrl);

        String[] segments = pathSegments == null ? new String[0] : pathSegments;

        String path = URLEncodedUtils.formatSegments(segments);

        if (url.toString().endsWith("/") && !path.isEmpty()) {
            url.deleteCharAt(url.length() - 1);
        }

        url.append(path);

        return url.toString();
    }
}
