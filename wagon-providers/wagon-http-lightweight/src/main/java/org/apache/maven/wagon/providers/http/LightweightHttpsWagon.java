package org.apache.maven.wagon.providers.http;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * LIghtweightHttpsWagon 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="https"
 *   instantiation-strategy="per-lookup"
 */
public class LightweightHttpsWagon
    extends LightweightHttpWagon
{
    private String previousHttpsProxyHost;
    
    private String previousHttpsProxyPort;
    
    public LightweightHttpsWagon()
    {
        super();
    }

    public String getProtocol()
    {
        return "https";
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        previousHttpsProxyHost = System.getProperty( "https.proxyHost" );
        previousHttpsProxyPort = System.getProperty( "https.proxyPort" );
        
        final ProxyInfo proxyInfo = this.proxyInfo;
        if ( proxyInfo != null )
        {
            System.setProperty( "https.proxyHost", proxyInfo.getHost() );
            System.setProperty( "https.proxyPort", String.valueOf( proxyInfo.getPort() ) );
        }
        
        super.openConnection();
    }

    public void closeConnection()
        throws ConnectionException
    {
        super.closeConnection();
        
        if ( previousHttpsProxyHost != null )
        {
            System.setProperty( "https.proxyHost", previousHttpsProxyHost );
        }
        if ( previousHttpsProxyPort != null )
        {
            System.setProperty( "https.proxyPort", previousHttpsProxyPort );
        }
    }
}
