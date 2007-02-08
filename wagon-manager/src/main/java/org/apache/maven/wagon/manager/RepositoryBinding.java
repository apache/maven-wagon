package org.apache.maven.wagon.manager;

/*
 * Copyright 2001-2007 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

/**
 * RepositoryBinding - maps an ID to a Repository, with optional {@link AuthenticationInfo} and {@link ProxyInfo}.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @todo move these into wagon-provider-api later?
 */
public class RepositoryBinding
{
    private String id;
    private Repository repository;
    private AuthenticationInfo authenticationInfo;
    private ProxyInfo proxyInfo;
    private Wagon wagon;
    
    public RepositoryBinding( String id, Repository repository )
    {
        super();
        this.id = id;
        this.repository = repository;
    }

    public AuthenticationInfo getAuthenticationInfo()
    {
        return authenticationInfo;
    }

    public void setAuthenticationInfo( AuthenticationInfo authenticationInfo )
    {
        this.authenticationInfo = authenticationInfo;
    }

    public ProxyInfo getProxyInfo()
    {
        return proxyInfo;
    }

    public void setProxyInfo( ProxyInfo proxyInfo )
    {
        this.proxyInfo = proxyInfo;
    }

    public String getId()
    {
        return id;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public Wagon getWagon()
    {
        return wagon;
    }

    public void setWagon( Wagon wagon )
    {
        this.wagon = wagon;
    }
}
