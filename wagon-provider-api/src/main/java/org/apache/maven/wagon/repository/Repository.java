package org.apache.maven.wagon.repository;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import java.io.Serializable;


/**
 * This class is an abstraction of the location from/to resources
 * can be transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 * @todo [BP] some things are specific to certain wagons (eg key stuff in authInfo, permissions)
 */
public class Repository
    implements Serializable
{
    private String id;

    private String name;

    private String host;

    private int port = WagonConstants.UNKNOWN_PORT;

    private String basedir;

    private String protocol;

    private String url;

    private RepositoryPermissions permissions;

    private AuthenticationInfo authenticationInfo;


    public Repository()
    {

    }


    public Repository( String id, String url )
    {
        setId( id );

        setUrl( url );
    }


    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }


    public AuthenticationInfo getAuthenticationInfo()
    {
        return authenticationInfo;
    }

    public void setAuthenticationInfo( AuthenticationInfo authenticationInfo )
    {
        this.authenticationInfo = authenticationInfo;
    }

    public String getBasedir()
    {
        return basedir;
    }

    public void setBasedir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public void setUrl( String url )
    {
        this.url = url;

        // TODO [BP]: refactor out the PathUtils URL stuff into a class like java.net.URL, so you only parse once
        //  can't use URL class as is because it won't recognise our protocols, though perhaps we could attempt to
        //  register handlers for scp, etc?

        this.host = PathUtils.host( url );

        this.protocol = PathUtils.protocol( url );

        this.port = PathUtils.port( url );

        this.basedir = PathUtils.basedir( url );

        String username = PathUtils.user( url );

        if ( username != null )
        {
            if ( authenticationInfo == null )
            {
                authenticationInfo = new AuthenticationInfo();
            }
            authenticationInfo.setUserName( username );

            String password = PathUtils.password( url );

            if ( password != null )
            {
                authenticationInfo.setPassword( password );
            }
        }
    }

    public String getUrl()
    {
        if ( url != null )
        {
            return url;
        }

        StringBuffer sb = new StringBuffer();

        sb.append( protocol );

        sb.append( "://" );

        sb.append( host );

        if ( port != WagonConstants.UNKNOWN_PORT )
        {
            sb.append( ":" );

            sb.append( port );
        }

        sb.append( basedir );

        return sb.toString();
    }

    public String getHost()
    {
        if ( host == null )
        {
            return "localhost";
        }
        return host;
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        if ( getName() != null )
        {
            return "[" + getName() + "] -> " + getUrl();
        }

        return getUrl();
    }

    public String getProtocol()
    {
        return protocol;
    }

    public RepositoryPermissions getPermissions()
    {
        return permissions;
    }

    public void setPermissions( RepositoryPermissions permissions )
    {
        this.permissions = permissions;
    }
}
