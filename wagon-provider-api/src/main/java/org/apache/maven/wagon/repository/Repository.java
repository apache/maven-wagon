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

import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;

import java.io.Serializable;


/**
 * This class is an abstraction of the location from/to resources
 * can be transfered.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
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

    private String layout;

    private String url;

    private ProxyInfo proxyInfo;

    private AuthenticationInfo authenticationInfo;


    public Repository(  )
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

    public ProxyInfo getProxyInfo()
    {
        return proxyInfo;
    }

    public void setProxyInfo( ProxyInfo proxyInfo )
    {
        this.proxyInfo = proxyInfo;
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

        this.host = PathUtils.host( url );

        this.protocol = PathUtils.protocol( url );

        this.port = PathUtils.port( url );

        this.basedir = PathUtils.basedir( url );
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

        sb.append( "/" );

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

    public String getLayout()
    {
        if ( layout == null )
        {
            return "${groupId}/${type}s/${artifactId}-${version}.${type}";
        }

        return layout;
    }

    public String artifactPath( Artifact artifact )
    {
        return interpolateLayout( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion() );
    }

    public String fullArtifactPath( Artifact artifact )
    {
        return getBasedir() + "/" + artifactPath( artifact );
    }

    public String artifactUrl( Artifact artifact )
    {
        return getUrl() + "/" + artifactPath( artifact );
    }

    private String interpolateLayout( String groupId, String artifactId, String type, String version )
    {
        String layout = getLayout();

        layout = StringUtils.replace( layout, "${groupId}", groupId );

        layout = StringUtils.replace( layout, "${artifactId}", artifactId );

        layout = StringUtils.replace( layout, "${type}", type );

        layout = StringUtils.replace( layout, "${version}", version );

        return layout;
    }
}
