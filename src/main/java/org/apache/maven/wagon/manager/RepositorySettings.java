package org.apache.maven.wagon.manager;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * RepositoryConfiguration - A class to track the configuration of a Repository via a repository id.
 * The underlying repository implementation can be swapped out as needed by the application.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositorySettings
{
    private String id;

    private AuthenticationInfo authentication;

    private XmlPlexusConfiguration configuration;

    private RepositoryPermissions permissions;

    private ProxyInfo proxy;

    private List mirrors = new ArrayList();

    /**
     * If set, assumes this is configuration is a mirror.
     */
    private String mirrorOf = null;

    private boolean isBlacklisted = false;

    private boolean isEnabled = true;

    public RepositorySettings( String repoId )
    {
        this.id = repoId;
    }

    public void addMirror( String repoId )
    {
        this.mirrors.add( repoId );
    }

    public AuthenticationInfo getAuthentication()
    {
        return authentication;
    }

    public XmlPlexusConfiguration getConfiguration()
    {
        return configuration;
    }

    public String getId()
    {
        return id;
    }

    public String getMirrorOf()
    {
        return mirrorOf;
    }

    public List getMirrors()
    {
        return mirrors;
    }

    public RepositoryPermissions getPermissions()
    {
        return permissions;
    }

    public ProxyInfo getProxy()
    {
        return proxy;
    }

    public boolean hasMirror()
    {
        if ( isMirror() )
        {
            // Mirrors can't have mirrors. (too confusing)
            return false;
        }

        return !mirrors.isEmpty();
    }

    public boolean isBlacklisted()
    {
        return isBlacklisted;
    }

    public boolean isEnabled()
    {
        return isEnabled;
    }
    
    public boolean isMirror()
    {
        return StringUtils.isNotBlank( mirrorOf );
    }

    public void setAuthentication( AuthenticationInfo authentication )
    {
        this.authentication = authentication;
    }

    public void setAuthentication( String username, String password, String privateKey, String passPhrase )
    {
        AuthenticationInfo authn = new AuthenticationInfo();
        authn.setUserName( username );
        authn.setPassword( password );
        authn.setPrivateKey( privateKey );
        authn.setPassphrase( passPhrase );

        setAuthentication( authn );
    }

    public void setBlacklisted( boolean isBlacklisted )
    {
        this.isBlacklisted = isBlacklisted;
    }

    public void setConfiguration( XmlPlexusConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public void setEnabled( boolean isEnabled )
    {
        this.isEnabled = isEnabled;
    }

    protected void setMirrorOf( String repoId )
    {
        this.mirrorOf = repoId;
    }

    public void setMirrors( List mirrors )
    {
        this.mirrors = mirrors;
    }

    public void setPermissions( RepositoryPermissions permissions )
    {
        this.permissions = permissions;
    }

    public void setPermissions( String group, String fileMode, String dirMode )
    {
        RepositoryPermissions repoPerms = new RepositoryPermissions();
        repoPerms.setGroup( group );
        repoPerms.setFileMode( fileMode );
        repoPerms.setDirectoryMode( dirMode );

        setPermissions( repoPerms );
    }

    public void setProxy( ProxyInfo proxy )
    {
        this.proxy = proxy;
    }
}