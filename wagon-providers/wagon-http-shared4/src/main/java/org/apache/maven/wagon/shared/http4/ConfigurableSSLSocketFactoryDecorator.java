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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.params.HttpParams;
import org.codehaus.plexus.util.StringUtils;

class ConfigurableSSLSocketFactoryDecorator implements SchemeLayeredSocketFactory
{

    private final SchemeLayeredSocketFactory sslSocketFactory;

    public ConfigurableSSLSocketFactoryDecorator( SchemeLayeredSocketFactory sslSocketFactory )
    {
        super();
        this.sslSocketFactory = sslSocketFactory;
    }

    public Socket createSocket(final HttpParams params) throws IOException
    {
        return enableSslProtocols( this.sslSocketFactory.createSocket(params) );
    }

    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            int port,
            final HttpParams params) throws IOException, UnknownHostException
    {
        return enableSslProtocols(
            this.sslSocketFactory.createLayeredSocket(socket, target, port, params));
    }

    public Socket connectSocket(
            final Socket sock,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException
    {
        return this.sslSocketFactory.connectSocket(sock, remoteAddress, localAddress, params);
    }

    public boolean isSecure(final Socket sock) throws IllegalArgumentException
    {
        return this.sslSocketFactory.isSecure(sock);
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
