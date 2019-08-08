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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Encoding utility.
 *
 * @since 2.7
 */
public class EncodingUtil
{
    /**
     * Parses and returns an encoded version of the given URL string.
     *
     * @param url Raw/decoded string form of a URL to parse/encode.
     * @return Parsed/encoded {@link URI} that represents the string form URL passed in.
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    public static URI encodeURL( final String url )
        throws MalformedURLException, URISyntaxException
    {
        final URL urlObject = new URL( url );

        final URI uriEncoded =
            new URI( urlObject.getProtocol(), //
                     urlObject.getAuthority(), //
                     urlObject.getPath(), //
                     urlObject.getQuery(), //
                     urlObject.getRef() );

        // WAGON-566
        return uriEncoded.normalize();
    }

    /**
     * Parses and returns an encoded version of the given URL string.
     * Wraps the {@link MalformedURLException} and {@link URISyntaxException} in case the passed URL is invalid.
     *
     * @param url Raw/decoded string form of a URL to parse/encode.
     * @return Parsed/encoded URI (as string) that represents the
     * @throws IllegalArgumentException in case the URL string is invalid.
     */
    public static String encodeURLToString( final String url )
    {
        try
        {
            return encodeURL( url ).toString();
        }
        catch ( final Exception e )
        {
            throw new IllegalArgumentException( String.format( "Error parsing url: %s", url ), e );
        }
    }

    /**
     * Parses and returns an encoded version of the given URL string alongside the given paths.
     *
     * @param baseUrl Base URL to use when constructing the final URL, ie: scheme://authority/initial.path.
     * @param paths   Additional path(s) to append at the end of the base path.
     * @return Composed URL (base + paths) already encoded, separating the individual path components by "/".
     * @since TODO
     */
    public static String encodeURLToString( final String baseUrl, final String... paths )
    {
        final StringBuilder url = new StringBuilder( baseUrl );

        final String[] parts = paths == null ? //
            new String[0] : //
            paths.length == 1 ? paths[0].split( "/" ) : paths;

        for ( final String part : parts )
        {
            if ( !url.toString().endsWith( "/" ) )
            {
                url.append( '/' );
            }

            url.append( part );
        }

        return encodeURLToString( url.toString() );
    }
}
