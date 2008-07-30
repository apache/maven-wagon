package org.apache.maven.wagon.proxy;

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

import java.util.StringTokenizer;

/**
 * @author <a href="mailto:lafeuil@gmail.com">Thomas Champagne</a>
 */
public final class ProxyUtils
{
    private ProxyUtils()
    {
    }

    /**
     * Check if the specified host is in the list of non proxy hosts.
     * 
     * @param proxy the proxy info object contains set of properties.
     * @param targetHost the target hostname
     * @return true if the hostname is in the list of non proxy hosts, false otherwise.
     */
    public static boolean validateNonProxyHosts( ProxyInfo proxy, String targetHost )
    {
        if ( targetHost == null )
        {
            targetHost = new String();
        }
        if ( proxy == null )
        {
            return false;
        }
        String nonProxyHosts = proxy.getNonProxyHosts();
        if ( nonProxyHosts == null )
        {
            return false;
        }

        StringTokenizer tokenizer = new StringTokenizer( nonProxyHosts, "|" );

        while ( tokenizer.hasMoreTokens() )
        {
            String pattern = tokenizer.nextToken();
            pattern = pattern.replaceAll( "\\.", "\\\\." ).replaceAll( "\\*", ".*" );
            if ( targetHost.matches( pattern ) )
            {
                return true;
            }
        }
        return false;
    }
}
