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

import org.apache.http.conn.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * Relaxed X509 certificate trust manager: can ignore invalid certificate date.
 *
 * @author Olivier Lamy
 * @since 2.0
 */
public class RelaxedTrustStrategy
    implements TrustStrategy
{
    private final boolean ignoreSSLValidityDates;

    public RelaxedTrustStrategy( boolean ignoreSSLValidityDates )
    {
        this.ignoreSSLValidityDates = ignoreSSLValidityDates;
    }

    public boolean isTrusted( X509Certificate[] certificates, String authType )
        throws CertificateException
    {
        if ( ( certificates != null ) && ( certificates.length > 0 ) )
        {
            for ( X509Certificate currentCertificate : certificates ) 
            {
                try
                {
                    currentCertificate.checkValidity();
                }
                catch ( CertificateExpiredException e )
                {
                    if ( !ignoreSSLValidityDates )
                    {
                        throw e;
                    }
                }
                catch ( CertificateNotYetValidException e )
                {
                    if ( !ignoreSSLValidityDates )
                    {
                        throw e;
                    }
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }

}
