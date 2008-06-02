package org.apache.maven.wagon;

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

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionEventSupport;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferEventSupport;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of common facilties for Wagon providers.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public abstract class AbstractWagon
    implements Wagon
{
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    protected Repository repository;

    protected SessionEventSupport sessionEventSupport = new SessionEventSupport();

    protected TransferEventSupport transferEventSupport = new TransferEventSupport();

    protected AuthenticationInfo authenticationInfo;

    protected boolean interactive = true;
    
    private int connectionTimeout = 60000;
    
    private ProxyInfoProvider proxyInfoProvider;
    
    /** @deprecated */
    protected ProxyInfo proxyInfo;
    
    private RepositoryPermissions permissionsOverride;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public Repository getRepository()
    {
        return repository;
    }

    public ProxyInfo getProxyInfo()
    {
        return proxyInfoProvider.getProxyInfo( null );
    }

    public AuthenticationInfo getAuthenticationInfo()
    {
        return authenticationInfo;
    }

    // ----------------------------------------------------------------------
    // Connection
    // ----------------------------------------------------------------------

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        try
        {
            openConnectionInternal();
        }
        catch ( ConnectionException e )
        {
            fireSessionConnectionRefused();
            
            throw e;
        }
        catch ( AuthenticationException e )
        {
            fireSessionConnectionRefused();
            
            throw e;
        }
    }

    public void connect( Repository repository )
        throws ConnectionException, AuthenticationException
    {
        connect( repository, null, (ProxyInfoProvider) null );
    }

    public void connect( Repository repository, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        connect( repository, null, proxyInfo );
    }

    public void connect( Repository repository, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        connect( repository, null, proxyInfoProvider );
    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {
        connect( repository, authenticationInfo, (ProxyInfoProvider) null );
    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        final ProxyInfo proxy = proxyInfo;
        connect( repository, authenticationInfo, new ProxyInfoProvider()
        {
            public ProxyInfo getProxyInfo( String protocol )
            {
                if ( proxy.getType().equalsIgnoreCase( protocol ) )
                {
                    return proxy;
                }
                else
                {
                    return null;
                }
            }
        } );
        this.proxyInfo = proxyInfo;
    }
    
    public void connect( Repository repository, AuthenticationInfo authenticationInfo,
                         ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        if ( repository == null )
        {
            throw new IllegalStateException( "The repository specified cannot be null." );
        }

        if ( permissionsOverride != null )
        {
            repository.setPermissions( permissionsOverride );
        }
        
        this.repository = repository;

        if ( authenticationInfo == null )
        {
            authenticationInfo = new AuthenticationInfo();
        }

        if ( authenticationInfo.getUserName() == null )
        {
            // Get user/pass that were encoded in the URL.
            if ( repository.getUsername() != null )
            {
                authenticationInfo.setUserName( repository.getUsername() );
                if ( repository.getPassword() != null && authenticationInfo.getPassword() == null )
                {
                    authenticationInfo.setPassword( repository.getPassword() );
                }
            }
        }

        // TODO: Do these needs to be fields, or are they only used in openConnection()?
        this.authenticationInfo = authenticationInfo;
        
        fireSessionOpening();

        openConnection();

        fireSessionOpened();
    }

    protected abstract void openConnectionInternal()
        throws ConnectionException, AuthenticationException;

    public void disconnect()
        throws ConnectionException
    {
        fireSessionDisconnecting();

        try
        {
            closeConnection();
        }
        catch ( ConnectionException e )
        {
            fireSessionError( e );
            throw e;
        }

        fireSessionDisconnected();
    }

    protected abstract void closeConnection()
        throws ConnectionException;

    protected void createParentDirectories( File destination )
        throws TransferFailedException
    {
        File destinationDirectory = destination.getParentFile();
        try
        {
            destinationDirectory = destinationDirectory.getCanonicalFile();
        }
        catch ( IOException e )
        {
            // not essential to have a canonical file
        }
        if ( destinationDirectory != null && !destinationDirectory.exists() )
        {
            destinationDirectory.mkdirs();
            if ( !destinationDirectory.exists() )
            {
                throw new TransferFailedException(
                    "Specified destination directory cannot be created: " + destinationDirectory );
            }
        }
    }
    
    public void setTimeout( int timeoutValue )
    {
    	connectionTimeout = timeoutValue;
    }
    
    public int getTimeout()
    {
    	return connectionTimeout;
    }

    // ----------------------------------------------------------------------
    // Stream i/o
    // ----------------------------------------------------------------------

    protected void getTransfer( Resource resource, File destination, InputStream input )
        throws TransferFailedException
    {
        getTransfer( resource, destination, input, true, Integer.MAX_VALUE );
    }

    protected void getTransfer( Resource resource, OutputStream output, InputStream input )
        throws TransferFailedException
    {
        getTransfer( resource, output, input, true, Integer.MAX_VALUE );
    }

    protected void getTransfer( Resource resource, File destination, InputStream input, boolean closeInput,
                                int maxSize )
        throws TransferFailedException
    {
        // ensure that the destination is created only when we are ready to transfer
        fireTransferDebug( "attempting to create parent directories for destination: " + destination.getName() );
        createParentDirectories( destination );

        OutputStream output = new LazyFileOutputStream( destination );

        fireGetStarted( resource, destination );

        try
        {
            getTransfer( resource, output, input, closeInput, maxSize );
        }
        catch ( TransferFailedException e )
        {
            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( !deleted )
                {
                    destination.deleteOnExit();
                }
            }
            throw e;
        }

        fireGetCompleted( resource, destination );
    }

    protected void getTransfer( Resource resource, OutputStream output, InputStream input, boolean closeInput, int maxSize )
        throws TransferFailedException
    {
        try
        {
            transfer( resource, input, output, TransferEvent.REQUEST_GET, maxSize );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_GET );

            String msg = "GET request of: " + resource.getName() + " from " + repository.getName() + " failed";

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( closeInput )
            {
                IOUtil.close( input );
            }

            IOUtil.close( output );
        }
    }

    protected void putTransfer( Resource resource, File source, OutputStream output, boolean closeOutput )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        resource.setContentLength( source.length() );

        resource.setLastModified( source.lastModified() );

        firePutStarted( resource, source );

        transfer( resource, source, output, closeOutput );

        firePutCompleted( resource, source );
    }

    /**
     * Write from {@link File} to {@link OutputStream}
     * 
     * @since 1.0-beta-1
     * 
     * @param resource resource to transfer
     * @param source file to read from
     * @param output output stream
     * @param closeOutput whether the output stream should be closed or not
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException 
     * @throws AuthorizationException 
     */
    protected void transfer( Resource resource, File source, OutputStream output, boolean closeOutput )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        InputStream input = null;

        try
        {
            input = new FileInputStream( source );

            putTransfer( resource, input, output, closeOutput );
        }
        catch ( FileNotFoundException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            throw new TransferFailedException( "Specified source file does not exist: " + source, e );
        }
        finally
        {
            IOUtil.close( input );
        }
    }

    protected void putTransfer( Resource resource, InputStream input, OutputStream output, boolean closeOutput )
        throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException
    {
        try
        {
            transfer( resource, input, output, TransferEvent.REQUEST_PUT );
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, TransferEvent.REQUEST_PUT );

            String msg = "PUT request to: " + resource.getName() + " in " + repository.getName() + " failed";

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            if ( closeOutput )
            {
                IOUtil.close( output );
            }
        }
    }

    /**
     * Write from {@link InputStream} to {@link OutputStream}.
     * Equivalent to {@link #transfer(Resource, InputStream, OutputStream, int, int)} with a maxSize equal to {@link Integer#MAX_VALUE}
     * 
     * @param resource resource to transfer
     * @param input input stream
     * @param output output stream
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     * @throws IOException
     */
    protected void transfer( Resource resource, InputStream input, OutputStream output, int requestType )
        throws IOException
    {
        transfer( resource, input, output, requestType, Integer.MAX_VALUE );
    }

    /**
     * Write from {@link InputStream} to {@link OutputStream}.
     * Equivalent to {@link #transfer(Resource, InputStream, OutputStream, int, int)} with a maxSize equal to {@link Integer#MAX_VALUE}
     * 
     * @param resource resource to transfer
     * @param input input stream
     * @param output output stream
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     * @param maxSize size of the buffer
     * @throws IOException
     */
    protected void transfer( Resource resource, InputStream input, OutputStream output, int requestType, int maxSize )
        throws IOException
    {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, requestType );
        transferEvent.setTimestamp( System.currentTimeMillis() );

        int remaining = maxSize;
        while ( remaining > 0 )
        {
            int n = input.read( buffer, 0, Math.min( buffer.length, remaining ) );

            if ( n == -1 )
            {
                break;
            }

            fireTransferProgress( transferEvent, buffer, n );

            output.write( buffer, 0, n );

            remaining -= n;
        }
        output.flush();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void fireTransferProgress( TransferEvent transferEvent, byte[] buffer, int n )
    {
        transferEventSupport.fireTransferProgress( transferEvent, buffer, n );
    }

    protected void fireGetCompleted( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_COMPLETED,
                                                         TransferEvent.REQUEST_GET );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferCompleted( transferEvent );
    }

    protected void fireGetStarted( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_STARTED,
                                                         TransferEvent.REQUEST_GET );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferStarted( transferEvent );
    }

    protected void fireGetInitiated( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_INITIATED,
                                                         TransferEvent.REQUEST_GET );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferInitiated( transferEvent );
    }

    protected void firePutInitiated( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_INITIATED,
                                                         TransferEvent.REQUEST_PUT );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferInitiated( transferEvent );
    }

    protected void firePutCompleted( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_COMPLETED,
                                                         TransferEvent.REQUEST_PUT );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferCompleted( transferEvent );
    }

    protected void firePutStarted( Resource resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_STARTED,
                                                         TransferEvent.REQUEST_PUT );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferStarted( transferEvent );
    }

    protected void fireSessionDisconnected()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_DISCONNECTED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionDisconnected( sessionEvent );
    }

    protected void fireSessionDisconnecting()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_DISCONNECTING );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionDisconnecting( sessionEvent );
    }

    protected void fireSessionLoggedIn()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_LOGGED_IN );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionLoggedIn( sessionEvent );
    }

    protected void fireSessionLoggedOff()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_LOGGED_OFF );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionLoggedOff( sessionEvent );
    }

    protected void fireSessionOpened()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_OPENED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionOpened( sessionEvent );
    }

    protected void fireSessionOpening()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_OPENING );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionOpening( sessionEvent );
    }

    protected void fireSessionConnectionRefused()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_CONNECTION_REFUSED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionConnectionRefused( sessionEvent );
    }

    protected void fireSessionError( Exception exception )
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, exception );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionError( sessionEvent );

    }

    protected void fireTransferDebug( String message )
    {
        transferEventSupport.fireDebug( message );
    }

    protected void fireSessionDebug( String message )
    {
        sessionEventSupport.fireDebug( message );
    }

    public boolean hasTransferListener( TransferListener listener )
    {
        return transferEventSupport.hasTransferListener( listener );
    }

    public void addTransferListener( TransferListener listener )
    {
        transferEventSupport.addTransferListener( listener );
    }

    public void removeTransferListener( TransferListener listener )
    {
        transferEventSupport.removeTransferListener( listener );
    }

    public void addSessionListener( SessionListener listener )
    {
        sessionEventSupport.addSessionListener( listener );
    }

    public boolean hasSessionListener( SessionListener listener )
    {
        return sessionEventSupport.hasSessionListener( listener );
    }

    public void removeSessionListener( SessionListener listener )
    {
        sessionEventSupport.removeSessionListener( listener );
    }

    protected void fireTransferError( Resource resource, Exception e, int requestType )
    {
        TransferEvent transferEvent = new TransferEvent( this, resource, e, requestType );

        transferEventSupport.fireTransferError( transferEvent );
    }


    public SessionEventSupport getSessionEventSupport()
    {
        return sessionEventSupport;
    }

    public void setSessionEventSupport( SessionEventSupport sessionEventSupport )
    {
        this.sessionEventSupport = sessionEventSupport;
    }

    public TransferEventSupport getTransferEventSupport()
    {
        return transferEventSupport;
    }

    public void setTransferEventSupport( TransferEventSupport transferEventSupport )
    {
        this.transferEventSupport = transferEventSupport;
    }

    /**
     * This method is used if you are not streaming the transfer, to make sure any listeners dependent on state
     * (eg checksum observers) succeed.
     */
    protected void postProcessListeners( Resource resource, File source, int requestType )
        throws TransferFailedException
    {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, requestType );
        transferEvent.setTimestamp( System.currentTimeMillis() );
        transferEvent.setLocalFile( source );

        try
        {
            InputStream input = new FileInputStream( source );

            while ( true )
            {
                int n = input.read( buffer );

                if ( n == -1 )
                {
                    break;
                }

                fireTransferProgress( transferEvent, buffer, n );
            }
        }
        catch ( IOException e )
        {
            fireTransferError( resource, e, requestType );
            
            throw new TransferFailedException( "Failed to post-process the source file", e );
        }
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "The wagon you are using has not implemented putDirectory()" );
    }

    public boolean supportsDirectoryCopy()
    {
        return false;
    }

    public void createZip( List files, File zipName, File basedir )
        throws IOException
    {
        ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipName ) );

        try
        {
            for ( int i = 0; i < files.size(); i++ )
            {
                String file = (String) files.get( i );

                file = file.replace( '\\', '/' );

                writeZipEntry( zos, new File( basedir, file ), file );
            }
        }
        finally
        {
            IOUtil.close( zos );
        }
    }

    private void writeZipEntry( ZipOutputStream jar, File source, String entryName )
        throws IOException
    {
        byte[] buffer = new byte[1024];

        int bytesRead;

        FileInputStream is = new FileInputStream( source );

        try
        {
            ZipEntry entry = new ZipEntry( entryName );

            jar.putNextEntry( entry );

            while ( ( bytesRead = is.read( buffer ) ) != -1 )
            {
                jar.write( buffer, 0, bytesRead );
            }
        }
        finally
        {
            is.close();
        }
    }

    protected static String getPath( String basedir, String dir )
    {
        String path;
        path = basedir;
        if ( !basedir.endsWith( "/" ) && !dir.startsWith( "/" ) )
        {
            path += "/";
        }
        path += dir;
        return path;
    }

    public boolean isInteractive()
    {
        return interactive;
    }

    public void setInteractive( boolean interactive )
    {
        this.interactive = interactive;
    }

    public List getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        throw new UnsupportedOperationException( "The wagon you are using has not implemented getFileList()" );
    }

    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        throw new UnsupportedOperationException( "The wagon you are using has not implemented resourceExists()" );
    }

    protected ProxyInfo getProxyInfo( String protocol, String host )
    {
        if ( proxyInfoProvider != null )
        {
            ProxyInfo proxyInfo = proxyInfoProvider.getProxyInfo( protocol );
            if ( !ProxyUtils.validateNonProxyHosts( proxyInfo, host ) )
            {
                return proxyInfo;
            }
        }
        return null;
    }

    public RepositoryPermissions getPermissionsOverride()
    {
        return permissionsOverride;
    }

    public void setPermissionsOverride( RepositoryPermissions permissionsOverride )
    {
        this.permissionsOverride = permissionsOverride;
    }
}
