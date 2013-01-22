package org.apache.maven.wagon.shared.http4;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;
import org.codehaus.plexus.util.StringUtils;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Olivier Lamy
 * @since 2.4
 */
public class ConfigurableSSLSocketFactory
    extends SSLSocketFactory
{
    public ConfigurableSSLSocketFactory( SSLContext sslContext, X509HostnameVerifier hostnameVerifier )
    {
        super( sslContext, hostnameVerifier );
    }

    @Override
    public Socket createSocket()
        throws IOException
    {
        return enableSslProtocols( super.createSocket() );
    }

    @Override
    public Socket createSocket( HttpParams params )
        throws IOException
    {
        return enableSslProtocols( super.createSocket( params ) );
    }

    @Override
    public Socket createSocket( Socket socket, String host, int port, boolean autoClose )
        throws IOException, UnknownHostException
    {
        return enableSslProtocols( super.createSocket( socket, host, port, autoClose ) );
    }

    @Override
    public Socket connectSocket( Socket socket, String host, int port, InetAddress localAddress, int localPort,
                                 HttpParams params )
        throws IOException, UnknownHostException, ConnectTimeoutException
    {
        return enableSslProtocols( super.connectSocket( socket, host, port, localAddress, localPort, params ) );
    }

    protected Socket enableSslProtocols( Socket socket )
    {
        String httpsProtocols = System.getProperty( "https.protocols" );
        if ( StringUtils.isNotEmpty( httpsProtocols ) )
        {
            String[] protocols = StringUtils.split( httpsProtocols, "," );
            if ( socket instanceof SSLSocket )
            {
                ( (SSLSocket) socket ).setEnabledProtocols( protocols );
            }
        }

        return socket;
    }


}
