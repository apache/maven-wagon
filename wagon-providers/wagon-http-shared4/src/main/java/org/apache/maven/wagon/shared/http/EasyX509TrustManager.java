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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
public class EasyX509TrustManager
    implements X509TrustManager
{
    private X509TrustManager standardTrustManager = null;


    protected static SSLContext createEasySSLContext()
        throws IOException
    {
        try
        {
            SSLContext context = SSLContext.getInstance( "SSL" );
            context.init( null, new TrustManager[]{ new EasyX509TrustManager( null ) }, null );
            return context;
        }
        catch ( Exception e )
        {
            throw new IOException( e.getMessage(), e );
        }
    }

    /**
     * Constructor for EasyX509TrustManager.
     */
    public EasyX509TrustManager( KeyStore keystore )
        throws NoSuchAlgorithmException, KeyStoreException
    {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        factory.init( keystore );
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if ( trustmanagers.length == 0 )
        {
            throw new NoSuchAlgorithmException( "no trust manager found" );
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[], String authType)
     */
    public void checkClientTrusted( X509Certificate[] certificates, String authType )
        throws CertificateException
    {
        System.out.println( "checkClientTrusted" );
        standardTrustManager.checkClientTrusted( certificates, authType );
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[], String authType)
     */
    public void checkServerTrusted( X509Certificate[] certificates, String authType )
        throws CertificateException
    {

        if ( ( certificates != null ) && ( certificates.length == 1 ) )
        {
            try
            {
                certificates[0].checkValidity();
            }
            catch ( CertificateExpiredException e )
            {
                if ( !AbstractHttpClientWagon.IGNORE_SSL_VALIDITY_DATES )
                {
                    throw e;
                }
            }
            catch ( CertificateNotYetValidException e )
            {
                if ( !AbstractHttpClientWagon.IGNORE_SSL_VALIDITY_DATES )
                {
                    throw e;
                }
            }
        }
        else
        {
            standardTrustManager.checkServerTrusted( certificates, authType );
        }
    }

    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers()
    {
        return this.standardTrustManager.getAcceptedIssuers();
    }
}
