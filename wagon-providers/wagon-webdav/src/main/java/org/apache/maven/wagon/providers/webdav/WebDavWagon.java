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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.methods.DepthSupport;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import java.util.TimeZone;

/**
 * <p>WebDavWagon</p>
 * 
 * <p>Allows using a webdav remote repository for downloads and deployments</p>
 * 
 * @author <a href="mailto:hisidro@exist.com">Henry Isidro</a>
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="dav"
 *   instantiation-strategy="per-lookup"
 */
public class WebDavWagon
    extends AbstractWagon
{
    private static final TimeZone TIMESTAMP_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private static String wagonVersion;

    private DateFormat dateFormat = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );

    private CorrectedWebdavResource webdavResource;

    public WebDavWagon()
    {
        dateFormat.setTimeZone( TIMESTAMP_TIME_ZONE );

        if ( wagonVersion == null )
        {
            URL pomUrl = this.getClass()
                .getResource( "/META-INF/maven/org.apache.maven.wagon/wagon-webdav/pom.properties" );
            if ( pomUrl == null )
            {
                wagonVersion = "";
            }
            else
            {
                Properties props = new Properties();
                try
                {
                    props.load( pomUrl.openStream() );
                    wagonVersion = props.getProperty( "version" );
                }
                catch ( IOException e )
                {
                    wagonVersion = "";
                }
            }
        }
    }

    /**
     * Opens a connection via web-dav resource
     * 
     * @throws AuthenticationException 
     * @throws ConnectionException
     */
    public void openConnection()
        throws AuthenticationException, ConnectionException
    {
        final boolean hasProxy = ( proxyInfo != null && proxyInfo.getUserName() != null );
        final boolean hasAuthentication = ( authenticationInfo != null && authenticationInfo.getUserName() != null );

        String url = getURL( repository );

        repository.setUrl( url );

        HttpURL httpURL = null;

        try
        {
            httpURL = urlToHttpURL( url );

            if ( hasAuthentication )
            {
                String userName = authenticationInfo.getUserName();
                String password = authenticationInfo.getPassword();

                if ( userName != null && password != null )
                {
                    httpURL.setUserinfo( userName, password );
                }
            }

            CorrectedWebdavResource.setDefaultAction( CorrectedWebdavResource.NOACTION );
            webdavResource = new CorrectedWebdavResource( httpURL );

            if ( hasProxy )
            {
                
                
                webdavResource.setProxy( proxyInfo.getHost(), proxyInfo.getPort() );
                if ( !StringUtils.isEmpty( proxyInfo.getUserName() ) )
                {
                    UsernamePasswordCredentials proxyCredentials = new UsernamePasswordCredentials();
                    proxyCredentials.setUserName( proxyInfo.getUserName() );
                    proxyCredentials.setPassword( proxyInfo.getPassword() );
                    webdavResource.setProxyCredentials( proxyCredentials );
                }
            }
        }
        catch ( HttpException he )
        {
            throw new ConnectionException( "Connection Exception: " + url + " " + he.getReasonCode() + " "
                + HttpStatus.getStatusText( he.getReasonCode() ), he );
        }
        catch ( URIException urie )
        {
            throw new ConnectionException( "Connection Exception: " + urie.getReason(), urie );
        }
        catch ( IOException ioe )
        {
            throw new ConnectionException( "Connection Exception: " + ioe.getMessage(), ioe );
        }
    }

    /**
     * Closes the connection
     * 
     * @throws ConnectionException 
     */
    public void closeConnection()
        throws ConnectionException
    {
        try
        {
            if ( webdavResource != null )
            {
                webdavResource.close();
            }
        }
        catch ( IOException ioe )
        {
            throw new ConnectionException( "Connection Exception: " + ioe.getMessage(), ioe );
        }
        finally
        {
            webdavResource = null;
        }
    }

    /**
     * Puts a file into the remote repository
     *
     * @param source the file to transfer
     * @param resourceName the name of the resource
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    public void put( File source, String resourceName )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Repository repository = getRepository();

        resourceName = StringUtils.replace( resourceName, "\\", "/" );
        String dir = PathUtils.dirname( resourceName );
        dir = StringUtils.replace( dir, "\\", "/" );

        String dest = repository.getUrl();
        Resource resource = new Resource( resourceName );

        if ( dest.endsWith( "/" ) )
        {
            dest = dest + resource.getName();
        }
        else
        {
            dest = dest + "/" + resource.getName();
        }

        firePutInitiated( resource, source );

        mkdirs( dir );

        try
        {
            // Put source into destination path.
            firePutStarted( resource, source );

            InputStream is = new PutInputStream( source, resource, this, getTransferEventSupport() );
            boolean success = webdavResource.putMethod( dest, is, (int) source.length() );
            int statusCode = webdavResource.getStatusCode();

            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_CREATED:
                    break;

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + dest );

                case HttpStatus.SC_NOT_FOUND:
                    // should never happen as the destination is created before or fail
                    throw new TransferFailedException( "Destination does not exist: " + repository.getUrl() );

                case HttpStatus.SC_LENGTH_REQUIRED:
                    throw new TransferFailedException( "Transfer failed, server requires Content-Length." );

                    //add more entries here
                default:
                    if ( !success )
                    {
                        throw new TransferFailedException( "Failed to transfer file: " + dest + ". Return code is: "
                            + statusCode + " " + HttpStatus.getStatusText( statusCode ) );
                    }
            }
        }
        catch ( FileNotFoundException e )
        {
            throw new TransferFailedException( "Specified source file does not exist: " + source, e );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            String msg = "PUT request for: " + resource + " to " + source.getName() + " failed";

            throw new TransferFailedException( msg, e );
        }
        firePutCompleted( resource, source );
    }

    /**
     * Create directories in server as needed.
     * They are created one at a time until the whole path exists.
     *
     * @param dir path to be created in server from repository basedir
     * @throws TransferFailedException
     */
    private void mkdirs( String dir )
        throws TransferFailedException
    {
        Repository repository = getRepository();
        String basedir = repository.getBasedir();
        String destinationPath = webdavResource.getPath();

        String relpath = FileUtils.normalize( getPath( basedir, dir ) + "/" );
        String currentPath = relpath;

        Stack directoriesToBeCreated = new Stack();

        try
        {
            while ( currentPath != null )
            {
                webdavResource.setPath( currentPath );

                // set basic properties like 'resourcetype' so we can query for
                // exists() and isCollection()
                try
                {
                    webdavResource.setProperties( WebdavResource.BASIC, DepthSupport.DEPTH_0 );
                }
                catch ( HttpException e )
                {
                    // ignore exceptions thrown when the path does not exist ( 404 errors )
                }

                if ( webdavResource.exists() )
                {
                    if ( webdavResource.isCollection() )
                    {
                        /* path exists and it's a directory */
                        break;
                    }
                    else
                    {
                        throw new TransferFailedException(
                                                           "Destination path exists and is not a WebDAV collection (directory): "
                                                               + webdavResource.toString() );
                    }
                }

                /* if dest resource path does not exist, create it later */
                directoriesToBeCreated.push( currentPath );

                /* go down a folder */
                currentPath += "/../";
                currentPath = FileUtils.normalize( currentPath );
            }

            // mkcolMethod() cannot create a directory hierarchy at once,
            // it has to create each directory one at a time
            while ( !directoriesToBeCreated.empty() )
            {
                currentPath = (String) directoriesToBeCreated.pop();
                webdavResource.setPath( currentPath );

                try
                {
                    boolean destinationCreated = webdavResource.mkcolMethod( currentPath );
                    if ( !destinationCreated )
                    {
                        throw new TransferFailedException( "Destination folder could not be created: "
                            + webdavResource.toString() );
                    }
                }
                catch ( IOException e )
                {
                    throw new TransferFailedException( "Failed to create destination WebDAV collection (directory): "
                        + webdavResource.toString(), e );
                }
            }

            webdavResource.setPath( destinationPath );

        }
        catch ( HttpException e )
        {
            throw new TransferFailedException( "Unknown error creating destination WebDAV collection (directory): "
                + webdavResource.toString() + ". Server returned error: "
                + HttpStatus.getStatusText( e.getReasonCode() ), e );
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unknown error creating destination WebDAV collection (directory): "
                + webdavResource.toString(), e );
        }
    }

    /**
     * Converts a String url to an HttpURL
     *
     * @param url String url to convert to an HttpURL
     * @return an HttpURL object created from the String url
     * @throws URIException
     */
    private HttpURL urlToHttpURL( String url )
        throws URIException
    {
        if ( url.startsWith( "https" ) )
        {
            return new HttpsURL( url );
        }
        else
        {
            return new HttpURL( url );
        }
    }

    /**
     * Determine which URI to use at the prompt.
     * @see FileUtils#normalize(String)
     *
     * @param uri the path to be set.
     * @return the normalized path.
     */
    private String checkUri( String uri )
        throws IOException
    {

        if ( webdavResource == null )
        {
            throw new IOException( "Not connected yet." );
        }

        if ( uri == null )
        {
            uri = webdavResource.getPath();
        }

        return FileUtils.normalize( uri );
    }

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        get( resourceName, destination, 0 );

    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return get( resourceName, destination, timestamp );
    }

    /**
     * Get a file from remote server
     * 
     * @param resourceName
     * @param destination
     * @param timestamp the timestamp to check against, only downloading if newer. If <code>0</code>, always download
     * @return <code>true</code> if newer version was downloaded, <code>false</code> otherwise.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    public boolean get( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        Resource resource = new Resource( resourceName );

        fireGetInitiated( resource, destination );

        String url = getRepository().getUrl() + "/" + resourceName;

        webdavResource.addRequestHeader( "X-wagon-provider", "wagon-webdav" );
        webdavResource.addRequestHeader( "X-wagon-version", wagonVersion );

        webdavResource.addRequestHeader( "Cache-control", "no-cache" );
        webdavResource.addRequestHeader( "Cache-store", "no-store" );
        webdavResource.addRequestHeader( "Pragma", "no-cache" );
        webdavResource.addRequestHeader( "Expires", "0" );
        webdavResource.addRequestHeader( "Accept-Encoding", "gzip" );

        if ( timestamp > 0 )
        {
            webdavResource.addRequestHeader( "If-Modified-Since", dateFormat.format( new Date( timestamp ) ) );
        }

        InputStream is = null;
        OutputStream output = new LazyFileOutputStream( destination );
        try
        {
            is = webdavResource.getMethodData( url );
            getTransfer( resource, destination, is );
        }
        catch ( HttpException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );
            throw new TransferFailedException( "Failed to transfer file: " + url + ".  Return code is: "
                + e.getReasonCode(), e );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( !deleted )
                {
                    destination.deleteOnExit();
                }
            }

            int statusCode = webdavResource.getStatusCode();
            switch ( statusCode )
            {
                case HttpStatus.SC_NOT_MODIFIED:
                    return false;

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denied to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                default:
                    throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                        + statusCode, e );
            }
        }
        finally
        {
            IOUtil.close( is );
            IOUtil.close( output );
        }

        int statusCode = webdavResource.getStatusCode();

        switch ( statusCode )
        {
            case HttpStatus.SC_OK:
                return true;

            case HttpStatus.SC_NOT_MODIFIED:
                return false;

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denied to: " + url );

            case HttpStatus.SC_UNAUTHORIZED:
                throw new AuthorizationException( "Not authorized." );

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                throw new AuthorizationException( "Not authorized by proxy." );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

            default:
                throw new TransferFailedException( "Failed to transfer file: " + url + ". Return code is: "
                    + statusCode );
        }

    }

    private String getURL( Repository repository )
    {
        String url = repository.getUrl();
        String s = "dav:";
        if ( url.startsWith( s ) )
        {
            return url.substring( s.length() );
        }
        else
        {
            return url;
        }
    }

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
     * Copy a directory from local system to remote webdav server
     * 
     * @param sourceDirectory the local directory
     * @param destinationDirectory the remote destination
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     */
    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {

        File[] listFiles = sourceDirectory.listFiles();

        for ( int i = 0; i < listFiles.length; i++ )
        {
            if ( listFiles[i].isDirectory() )
            {
                putDirectory( listFiles[i], destinationDirectory + "/" + listFiles[i].getName() );
            }
            else
            {
                String target = destinationDirectory + "/" + listFiles[i].getName();

                put( listFiles[i], target );
            }
        }

    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        webdavResource.addRequestHeader( "X-wagon-provider", "wagon-webdav" );
        webdavResource.addRequestHeader( "X-wagon-version", wagonVersion );

        webdavResource.addRequestHeader( "Cache-control", "no-cache" );
        webdavResource.addRequestHeader( "Cache-store", "no-store" );
        webdavResource.addRequestHeader( "Pragma", "no-cache" );
        webdavResource.addRequestHeader( "Expires", "0" );

        String basedir = repository.getBasedir();

        if ( !destinationDirectory.endsWith( "/" ) )
        {
            destinationDirectory += "/";
        }

        String cleanDestDir = StringUtils.replace( destinationDirectory, "\\", "/" );
        String dir = PathUtils.dirname( cleanDestDir );
        dir = StringUtils.replace( dir, "\\", "/" );

        String oldpath = webdavResource.getPath();
        String relpath = getPath( basedir, dir );

        try
        {
            // Test if dest resource path exist.
            String cdpath = checkUri( relpath + "/" );
            webdavResource.setPath( cdpath );

            try
            {
                webdavResource.setProperties( WebdavResource.NAME, DepthSupport.DEPTH_0 );

                /* seems this is not needed as Webdav client causes a 404 error in webdavResource.setProperties */
                if ( !webdavResource.exists() )
                {
                    throw new ResourceDoesNotExistException( "Destination directory does not exist: " + cdpath );
                }

                if ( !webdavResource.isCollection() )
                {
                    throw new ResourceDoesNotExistException( "Destination path exists but is not a "
                        + "WebDAV collection (directory): " + cdpath );
                }

                String[] entries = webdavResource.list();

                List filelist = new ArrayList();
                if ( entries != null )
                {
                    filelist.addAll( Arrays.asList( entries ) );
                }

                return filelist;
            }
            catch ( HttpException e )
            {
                if ( e.getReasonCode() == HttpStatus.SC_NOT_FOUND )
                {
                    throw new ResourceDoesNotExistException( "Destination directory does not exist: " + cdpath, e );
                }
                else
                {
                    throw new TransferFailedException( "Unable to obtain file list from WebDAV collection, "
                        + "HTTP error: " + HttpStatus.getStatusText( e.getReasonCode() ), e );
                }
            }
            finally
            {
                webdavResource.setPath( oldpath );
            }

        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Unable to obtain file list from WebDAV collection.", e );
        }
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        try
        {
            String parentDirectory = PathUtils.dirname( resourceName );
            List entries = getFileList( parentDirectory );
            return entries.contains( resourceName );
        }
        catch ( ResourceDoesNotExistException e )
        {
            return false;
        }
    }
}
