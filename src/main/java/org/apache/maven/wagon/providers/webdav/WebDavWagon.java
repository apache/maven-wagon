package org.apache.maven.wagon.providers.webdav;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>WebDavWagon</p>
 * 
 * <p>Allows using a webdav remote repository for downloads and deployments</p>
 * 
 * <p>TODO: webdav https server is not tested</p>
 * 
 * @author <a href="mailto:hisidro@exist.com">Henry Isidro</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public class WebDavWagon
    extends AbstractWagon
{
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    private CorrectedWebdavResource wdresource;

    private static String WAGON_VERSION;

    public WebDavWagon()
    {
        if ( WAGON_VERSION == null )
        {
            URL pomUrl = this.getClass()
                .getResource( "/META-INF/maven/org.apache.maven.wagon/wagon-webdav/pom.properties" );
            if ( pomUrl == null )
            {
                WAGON_VERSION = "";
            }
            else
            {
                Properties props = new Properties();
                try
                {
                    props.load( pomUrl.openStream() );
                    WAGON_VERSION = props.getProperty( "version" );
                    System.out.println( "WAGON_VERSION: " + WAGON_VERSION );
                }
                catch ( IOException e )
                {
                    WAGON_VERSION = "";
                }
            }
        }
    }

    /**
     * Opens a connection via web-dav resource
     * @throws AuthenticationException 
     */
    public void openConnection()
        throws AuthenticationException, ConnectionException
    {
        String url = getURL( repository );

        repository.setUrl( url );

        HttpURL httpURL = null;

        try
        {
            httpURL = urlToHttpURL( url );

            wdresource = new CorrectedWebdavResource( httpURL );
        }
        catch ( HttpException he )
        {
            if ( he.getReasonCode() == HttpStatus.SC_UNAUTHORIZED )
            {
                try
                {
                    httpURL.setUserinfo( authenticationInfo.getUserName(), authenticationInfo.getPassword() );

                    wdresource = new CorrectedWebdavResource( httpURL );
                }
                catch ( URIException urie )
                {
                    throw new AuthenticationException( "Authentication Exception: " + urie.getReason() );
                }
                catch ( IOException ioe )
                {
                    throw new ConnectionException( "Connection Exception: " + ioe.getMessage() );
                }
            }
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
     * @throws ConnectionException 
     */
    public void closeConnection()
        throws ConnectionException
    {
        try
        {
            wdresource.close();
        }
        catch ( IOException ioe )
        {
            throw new ConnectionException( "Connection Exception: " + ioe.getMessage() );
        }
        finally
        {
            wdresource = null;
        }
    }

    /**
     * Puts a file into the remote repository
     *
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

        String basedir = repository.getBasedir();

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
        String oldpath = wdresource.getPath();

        String relpath = getPath( basedir, dir );

        try
        {
            // Test if dest resource path exist.
            String cdpath = checkUri( relpath + "/" );
            wdresource.setPath( cdpath );

            if ( wdresource.exists() && !wdresource.isCollection() )
            {
                throw new TransferFailedException(
                                                   "Destination path exists and is not a WebDAV collection (directory): "
                                                       + cdpath );
            }

            wdresource.setPath( oldpath );
        }
        // if dest resource path does not exist, create it
        catch ( HttpException e )
        {
            if ( e.getReasonCode() == HttpStatus.SC_NOT_FOUND )
            {
                // mkcolMethod() cannot create a directory heirarchy at once,
                // it has to create each directory one at a time
                try
                {
                    String[] dirs = relpath.split( "/" );
                    String create_dir = "";

                    // start at 1 because first element of dirs[] from split() is ""
                    for ( int count = 1; count < dirs.length; count++ )
                    {
                        create_dir = create_dir + "/" + dirs[count];
                        wdresource.mkcolMethod( create_dir );
                    }
                    wdresource.setPath( oldpath );
                }
                catch ( IOException ioe )
                {
                    throw new TransferFailedException( "Failed to create destination WebDAV collection (directory): "
                        + relpath );
                }
            }
            else
            {
                throw new TransferFailedException( "Failed to create destination WebDAV collection (directory): "
                    + relpath );
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Failed to create destination WebDAV collection (directory): " + relpath );
        }

        try
        {
            // Put source into destination path.
            firePutStarted( resource, source );

            InputStream is = new PutInputStream( source, resource, this, getTransferEventSupport() );
            boolean success = wdresource.putMethod( dest, is, (int) source.length() );
            int statusCode = wdresource.getStatusCode();
            
            switch ( statusCode )
            {
                case HttpStatus.SC_OK:
                    break;

                case HttpStatus.SC_CREATED:
                    break;

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denided to: " + dest );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + dest + " does not exist" );

                case HttpStatus.SC_LENGTH_REQUIRED:
                    throw new ResourceDoesNotExistException( "Transfer failed, server requires Content-Length." );

                //add more entries here
                default:
                    if ( !success )
                    {
                        throw new TransferFailedException( "Failed to transfer file: " + dest + ". Return code is: "
                            + statusCode );
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
     * Converts a String url to an HttpURL
     *
     * @param url String url to conver to an HttpURL
     * @return an HttpURL object created from the String url
     * @throws URIException
     */
    private HttpURL urlToHttpURL( String url )
        throws URIException
    {
        return url.startsWith( "https" ) ? new HttpsURL( url ) : new HttpURL( url );
    }

    /**
     * Determine which URI to use at the prompt.
     *
     * @param uri the path to be set.
     * @return the absolute path.
     */
    private String checkUri( String uri )
        throws IOException
    {

        if ( wdresource == null )
        {
            throw new IOException( "Not connected yet." );
        }

        if ( uri == null )
        {
            uri = wdresource.getPath();
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

        wdresource.addRequestHeader( "X-wagon-provider", "wagon-webdav" );
        wdresource.addRequestHeader( "X-wagon-version", WAGON_VERSION );

        wdresource.addRequestHeader( "Cache-control", "no-cache" );
        wdresource.addRequestHeader( "Cache-store", "no-store" );
        wdresource.addRequestHeader( "Pragma", "no-cache" );
        wdresource.addRequestHeader( "Expires", "0" );

        if ( timestamp > 0 )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US );
            fmt.setTimeZone( GMT_TIME_ZONE );
            wdresource.addRequestHeader( "If-Modified-Since", fmt.format( new Date( timestamp ) ) );
        }

        InputStream is = null;
        OutputStream output = new LazyFileOutputStream( destination ); 
        try
        {
            is = wdresource.getMethodData( url );
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

            int statusCode = wdresource.getStatusCode();
            switch ( statusCode )
            {
                case HttpStatus.SC_NOT_MODIFIED:
                    return false;

                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException( "Access denided to: " + url );

                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException( "Not authorized." );

                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new AuthorizationException( "Not authorized by proxy." );

                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

                default:
                    throw new TransferFailedException( "Failed to trasfer file: " + url + ". Return code is: "
                                                       + statusCode );
            }                
        }
        finally
        {
            IOUtil.close( is );
            IOUtil.close( output );
        }

        int statusCode = wdresource.getStatusCode();

        switch ( statusCode )
        {
            case HttpStatus.SC_OK:
                return true;

            case HttpStatus.SC_NOT_MODIFIED:
                return false;

            case HttpStatus.SC_FORBIDDEN:
                throw new AuthorizationException( "Access denided to: " + url );

            case HttpStatus.SC_UNAUTHORIZED:
                throw new AuthorizationException( "Not authorized." );

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                throw new AuthorizationException( "Not authorized by proxy." );

            case HttpStatus.SC_NOT_FOUND:
                throw new ResourceDoesNotExistException( "File: " + url + " does not exist" );

            default:
                throw new TransferFailedException( "Failed to trasfer file: " + url + ". Return code is: "
                    + statusCode );
        }
        
    }

    private String getURL( Repository repository )
    {
        String url = repository.getUrl();
        if ( url.startsWith( "dav:" ) )
        {
            return url.substring( 4 );
        }
        else
        {
            return url;
        }
    }
}
