package org.apache.maven.wagon.providers.webdav;

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

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>WebDavWagon</p>
 * <p/>
 * <p>Allows using a webdav remote repository for downloads and deployments</p>
 *
 * @author <a href="mailto:hisidro@exist.com">Henry Isidro</a>
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="dav"
 * instantiation-strategy="per-lookup"
 */
public class WebDavWagon
    extends AbstractHttpClientWagon
{
    protected static final String CONTINUE_ON_FAILURE_PROPERTY = "wagon.webdav.continueOnFailure";

    private final boolean continueOnFailure = Boolean.getBoolean( CONTINUE_ON_FAILURE_PROPERTY );

    /**
     * Defines the protocol mapping to use.
     * <p/>
     * First string is the user definition way to define a webdav url,
     * the second string is the internal representation of that url.
     * <p/>
     * NOTE: The order of the mapping becomes the search order.
     */
    private static final String[][] protocolMap =
        new String[][]{ { "dav:http://", "http://" },    /* maven 2.0.x url string format. (violates URI spec) */
            { "dav:https://", "https://" },  /* maven 2.0.x url string format. (violates URI spec) */
            { "dav+http://", "http://" },    /* URI spec compliant (protocol+transport) */
            { "dav+https://", "https://" },  /* URI spec compliant (protocol+transport) */
            { "dav://", "http://" },         /* URI spec compliant (protocol only) */
            { "davs://", "https://" }        /* URI spec compliant (protocol only) */ };

    /**
     * This wagon supports directory copying
     *
     * @return <code>true</code> always
     */
    public boolean supportsDirectoryCopy()
    {
        return true;
    }

    /**
     * Create directories in server as needed.
     * They are created one at a time until the whole path exists.
     *
     * @param dir path to be created in server from repository basedir
     * @throws IOException
     * @throws HttpException
     * @throws TransferFailedException
     */
    protected void mkdirs( String dir )
        throws IOException
    {
        Repository repository = getRepository();
        String basedir = repository.getBasedir();

        String baseUrl = repository.getProtocol() + "://" + repository.getHost();
        if ( repository.getPort() != WagonConstants.UNKNOWN_PORT )
        {
            baseUrl += ":" + repository.getPort();
        }

        // create relative path that will always have a leading and trailing slash
        String relpath = FileUtils.normalize( getPath( basedir, dir ) + "/" );

        PathNavigator navigator = new PathNavigator( relpath );

        // traverse backwards until we hit a directory that already exists (OK/NOT_ALLOWED), or that we were able to
        // create (CREATED), or until we get to the top of the path
        int status = SC_NULL;
        do
        {
            String url = baseUrl + "/" + navigator.getPath();
            status = doMkCol( url );
            if ( status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED
                || status == HttpStatus.SC_METHOD_NOT_ALLOWED )
            {
                break;
            }
        }
        while ( navigator.backward() );

        // traverse forward creating missing directories
        while ( navigator.forward() )
        {
            String url = baseUrl + "/" + navigator.getPath();
            status = doMkCol( url );
            if ( status != HttpStatus.SC_OK && status != HttpStatus.SC_CREATED )
            {
                throw new IOException( "Unable to create collection: " + url + "; status code = " + status );
            }
        }
    }

    private int doMkCol( String url )
        throws IOException
    {
        MkColMethod method = null;
        try
        {
            method = new MkColMethod( url );
            return execute( method );
        }
        finally
        {
            if ( method != null )
            {
                method.releaseConnection();
            }
        }
    }

    /**
     * Copy a directory from local system to remote webdav server
     *
     * @param sourceDirectory      the local directory
     * @param destinationDirectory the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        for ( File file : sourceDirectory.listFiles() )
        {
            if ( file.isDirectory() )
            {
                putDirectory( file, destinationDirectory + "/" + file.getName() );
            }
            else
            {
                String target = destinationDirectory + "/" + file.getName();

                put( file, target );
            }
        }
    }

    private boolean isDirectory( String url )
        throws IOException, DavException
    {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add( DavPropertyName.create( DavConstants.PROPERTY_RESOURCETYPE ) );

        PropFindMethod method = null;
        try
        {
            method = new PropFindMethod( url, nameSet, DavConstants.DEPTH_0 );
            execute( method );
            if ( method.succeeded() )
            {
                MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();
                MultiStatusResponse response = multiStatus.getResponses()[0];
                DavPropertySet propertySet = response.getProperties( HttpStatus.SC_OK );
                DavProperty<?> property = propertySet.get( DavConstants.PROPERTY_RESOURCETYPE );
                if ( property != null )
                {
                    Node node = (Node) property.getValue();
                    return node.getLocalName().equals( DavConstants.XML_COLLECTION );
                }
            }
            return false;
        }
        finally
        {
            if ( method != null )
            {
                method.releaseConnection();
            }
        }
    }

    public List<String> getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String repositoryUrl = repository.getUrl();
        String url = repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + destinationDirectory;

        PropFindMethod method = null;
        try
        {
            if ( isDirectory( url ) )
            {
                DavPropertyNameSet nameSet = new DavPropertyNameSet();
                nameSet.add( DavPropertyName.create( DavConstants.PROPERTY_DISPLAYNAME ) );

                method = new PropFindMethod( url, nameSet, DavConstants.DEPTH_1 );
                int status = execute( method );
                if ( method.succeeded() )
                {
                    ArrayList<String> dirs = new ArrayList<String>();
                    MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();

                    for ( int i = 0; i < multiStatus.getResponses().length; i++ )
                    {

                        MultiStatusResponse response = multiStatus.getResponses()[i];

                        String entryUrl = response.getHref();
                        String fileName = PathUtils.filename( URLDecoder.decode( entryUrl ) );
                        if ( entryUrl.endsWith( "/" ) )
                        {
                            if ( i == 0 )
                            {
                                //by design jackrabbit webdav sticks parent directory as the first entry
                                // so we need to ignore this entry
                                // http://www.nabble.com/Extra-entry-in-get-file-list-with-jackrabbit-webdav-td21262786.html
                                // http://www.webdav.org/specs/rfc4918.html#rfc.section.9.1
                                continue;
                            }

                            //extract "dir/" part of "path.to.dir/"
                            fileName = PathUtils.filename( PathUtils.dirname( URLDecoder.decode( entryUrl ) ) ) + "/";
                        }

                        if ( !StringUtils.isEmpty( fileName ) )
                        {
                            dirs.add( fileName );
                        }
                    }
                    return dirs;
                }

                if ( status == HttpStatus.SC_NOT_FOUND )
                {
                    throw new ResourceDoesNotExistException( "Destination directory does not exist: " + url );
                }
            }
        }
        catch ( DavException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( e.getMessage(), e );
        }
        finally
        {
            if ( method != null )
            {
                method.releaseConnection();
            }
        }
        throw new ResourceDoesNotExistException(
            "Destination path exists but is not a " + "WebDAV collection (directory): " + url );
    }

    public String getURL( Repository repository )
    {
        String url = repository.getUrl();

        // Process mappings first.
        for ( String[] entry : protocolMap )
        {
            String protocol = entry[0];
            if ( url.startsWith(protocol) )
            {
                return entry[1] + url.substring( protocol.length() );
            }
        }

        // No mapping trigger? then just return as-is.
        return url;
    }


    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        try
        {
            super.put( source, resourceName );
        }
        catch ( TransferFailedException e )
        {
            if ( continueOnFailure )
            {
                // TODO use a logging mechanism here or a fireTransferWarning
                System.out.println(
                    "WARN: Skip unable to transfer '" + resourceName + "' from '" + source.getPath() + "' due to "
                        + e.getMessage() );
            }
            else
            {
                throw e;
            }
        }
    }
}
