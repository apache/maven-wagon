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
package org.apache.maven.wagon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.List;

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

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Implementation of common facilities for Wagon providers.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
public abstract class AbstractWagon implements Wagon {
    protected static final int DEFAULT_BUFFER_SIZE = 4 * 1024;
    protected static final int MAXIMUM_BUFFER_SIZE = 512 * 1024;

    /**
     * To efficiently buffer data, use a multiple of 4 KiB as this is likely to match the hardware
     * buffer size of certain storage devices.
     */
    protected static final int BUFFER_SEGMENT_SIZE = 4 * 1024;

    /**
     * The desired minimum amount of chunks in which a {@link Resource} shall be
     * {@link #transfer(Resource, InputStream, OutputStream, int, long) transferred}.
     * This corresponds to the minimum times {@link #fireTransferProgress(TransferEvent, byte[], int)}
     * is executed. 100 notifications is a conservative value that will lead to small chunks for
     * any artifact less that {@link #BUFFER_SEGMENT_SIZE} * {@link #MINIMUM_AMOUNT_OF_TRANSFER_CHUNKS}
     * in size.
     */
    protected static final int MINIMUM_AMOUNT_OF_TRANSFER_CHUNKS = 100;

    protected Repository repository;

    protected SessionEventSupport sessionEventSupport = new SessionEventSupport();

    protected TransferEventSupport transferEventSupport = new TransferEventSupport();

    protected AuthenticationInfo authenticationInfo;

    protected boolean interactive = true;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /**
     * read timeout value
     *
     * @since 2.2
     */
    private int readTimeout =
            Integer.parseInt(System.getProperty("maven.wagon.rto", Integer.toString(Wagon.DEFAULT_READ_TIMEOUT)));

    private ProxyInfoProvider proxyInfoProvider;

    /**
     * @deprecated
     */
    protected ProxyInfo proxyInfo;

    private RepositoryPermissions permissionsOverride;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    @Override
    public Repository getRepository() {
        return repository;
    }

    public ProxyInfo getProxyInfo() {
        return proxyInfoProvider != null ? proxyInfoProvider.getProxyInfo(null) : null;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return authenticationInfo;
    }

    // ----------------------------------------------------------------------
    // Connection
    // ----------------------------------------------------------------------

    @Deprecated
    @Override
    public void openConnection() throws ConnectionException, AuthenticationException {
        try {
            openConnectionInternal();
        } catch (ConnectionException | AuthenticationException e) {
            fireSessionConnectionRefused();

            throw e;
        }
    }

    @Override
    public void connect(Repository repository) throws ConnectionException, AuthenticationException {
        connect(repository, null, (ProxyInfoProvider) null);
    }

    @Override
    public void connect(Repository repository, ProxyInfo proxyInfo)
            throws ConnectionException, AuthenticationException {
        connect(repository, null, proxyInfo);
    }

    @Override
    public void connect(Repository repository, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException {
        connect(repository, null, proxyInfoProvider);
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo)
            throws ConnectionException, AuthenticationException {
        connect(repository, authenticationInfo, (ProxyInfoProvider) null);
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo)
            throws ConnectionException, AuthenticationException {
        final ProxyInfo proxy = proxyInfo;
        connect(repository, authenticationInfo, (ProxyInfoProvider) protocol -> {
            if (protocol == null || proxy == null || protocol.equalsIgnoreCase(proxy.getType())) {
                return proxy;
            } else {
                return null;
            }
        });
    }

    @Override
    public void connect(
            Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException {
        if (repository == null) {
            throw new NullPointerException("repository cannot be null");
        }

        if (permissionsOverride != null) {
            repository.setPermissions(permissionsOverride);
        }

        this.repository = repository;

        if (authenticationInfo == null) {
            authenticationInfo = new AuthenticationInfo();
        }

        if (authenticationInfo.getUserName() == null) {
            // Get user/pass that were encoded in the URL.
            if (repository.getUsername() != null) {
                authenticationInfo.setUserName(repository.getUsername());
                if (repository.getPassword() != null && authenticationInfo.getPassword() == null) {
                    authenticationInfo.setPassword(repository.getPassword());
                }
            }
        }

        this.authenticationInfo = authenticationInfo;

        this.proxyInfoProvider = proxyInfoProvider;

        fireSessionOpening();

        openConnection();

        fireSessionOpened();
    }

    protected abstract void openConnectionInternal() throws ConnectionException, AuthenticationException;

    @Override
    public void disconnect() throws ConnectionException {
        fireSessionDisconnecting();

        try {
            closeConnection();
        } catch (ConnectionException e) {
            fireSessionError(e);
            throw e;
        }

        fireSessionDisconnected();
    }

    protected abstract void closeConnection() throws ConnectionException;

    protected void createParentDirectories(File destination) throws TransferFailedException {
        File destinationDirectory = destination.getParentFile();
        try {
            destinationDirectory = destinationDirectory.getCanonicalFile();
        } catch (IOException e) {
            // not essential to have a canonical file
        }
        if (destinationDirectory != null && !destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
            if (!destinationDirectory.exists()) {
                throw new TransferFailedException(
                        "Specified destination directory cannot be created: " + destinationDirectory);
            }
        }
    }

    @Override
    public void setTimeout(int timeoutValue) {
        connectionTimeout = timeoutValue;
    }

    @Override
    public int getTimeout() {
        return connectionTimeout;
    }

    // ----------------------------------------------------------------------
    // Stream i/o
    // ----------------------------------------------------------------------

    protected void getTransfer(Resource resource, File destination, InputStream input) throws TransferFailedException {
        getTransfer(resource, destination, input, true, Long.MAX_VALUE);
    }

    protected void getTransfer(Resource resource, OutputStream output, InputStream input)
            throws TransferFailedException {
        getTransfer(resource, output, input, true, Long.MAX_VALUE);
    }

    @Deprecated
    protected void getTransfer(Resource resource, File destination, InputStream input, boolean closeInput, int maxSize)
            throws TransferFailedException {
        getTransfer(resource, destination, input, closeInput, (long) maxSize);
    }

    protected void getTransfer(Resource resource, File destination, InputStream input, boolean closeInput, long maxSize)
            throws TransferFailedException {
        // ensure that the destination is created only when we are ready to transfer
        fireTransferDebug("attempting to create parent directories for destination: " + destination.getName());
        createParentDirectories(destination);

        fireGetStarted(resource, destination);

        try (OutputStream output = new LazyFileOutputStream(destination)) {
            getTransfer(resource, output, input, closeInput, maxSize);
        } catch (final IOException e) {
            if (destination.exists()) {
                boolean deleted = destination.delete();

                if (!deleted) {
                    destination.deleteOnExit();
                }
            }

            fireTransferError(resource, e, TransferEvent.REQUEST_GET);

            String msg = "GET request of: " + resource.getName() + " from " + repository.getName() + " failed";

            throw new TransferFailedException(msg, e);
        } catch (TransferFailedException e) {
            if (destination.exists()) {
                boolean deleted = destination.delete();

                if (!deleted) {
                    destination.deleteOnExit();
                }
            }
            throw e;
        }

        fireGetCompleted(resource, destination);
    }

    @Deprecated
    protected void getTransfer(
            Resource resource, OutputStream output, InputStream input, boolean closeInput, int maxSize)
            throws TransferFailedException {
        getTransfer(resource, output, input, closeInput, (long) maxSize);
    }

    protected void getTransfer(
            Resource resource, OutputStream output, InputStream input, boolean closeInput, long maxSize)
            throws TransferFailedException {
        try {
            transfer(resource, input, output, TransferEvent.REQUEST_GET, maxSize);

            finishGetTransfer(resource, input, output);

            if (closeInput) {
                input.close();
                input = null;
            }

        } catch (IOException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_GET);

            String msg = "GET request of: " + resource.getName() + " from " + repository.getName() + " failed";

            throw new TransferFailedException(msg, e);
        } finally {
            if (closeInput) {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            cleanupGetTransfer(resource);
        }
    }

    protected void finishGetTransfer(Resource resource, InputStream input, OutputStream output)
            throws TransferFailedException {}

    protected void cleanupGetTransfer(Resource resource) {}

    protected void putTransfer(Resource resource, File source, OutputStream output, boolean closeOutput)
            throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {
        firePutStarted(resource, source);

        transfer(resource, source, output, closeOutput);

        firePutCompleted(resource, source);
    }

    /**
     * Write from {@link File} to {@link OutputStream}
     *
     * @param resource    resource to transfer
     * @param source      file to read from
     * @param output      output stream
     * @param closeOutput whether the output stream should be closed or not
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     * @throws AuthorizationException
     * @since 1.0-beta-1
     */
    protected void transfer(Resource resource, File source, OutputStream output, boolean closeOutput)
            throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {
        try (InputStream input = new FileInputStream(source)) {
            putTransfer(resource, input, output, closeOutput);
        } catch (FileNotFoundException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
            throw new TransferFailedException("Specified source file does not exist: " + source, e);
        } catch (final IOException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
            throw new TransferFailedException("Failure transferring " + source, e);
        }
    }

    protected void putTransfer(Resource resource, InputStream input, OutputStream output, boolean closeOutput)
            throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {
        try {
            transfer(
                    resource,
                    input,
                    output,
                    TransferEvent.REQUEST_PUT,
                    resource.getContentLength() == WagonConstants.UNKNOWN_LENGTH
                            ? Long.MAX_VALUE
                            : resource.getContentLength());

            finishPutTransfer(resource, input, output);

            if (closeOutput) {
                output.close();
                output = null;
            }
        } catch (IOException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

            String msg = "PUT request to: " + resource.getName() + " in " + repository.getName() + " failed";

            throw new TransferFailedException(msg, e);
        } finally {
            if (closeOutput) {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            cleanupPutTransfer(resource);
        }
    }

    protected void cleanupPutTransfer(Resource resource) {}

    protected void finishPutTransfer(Resource resource, InputStream input, OutputStream output)
            throws TransferFailedException, AuthorizationException, ResourceDoesNotExistException {}

    /**
     * Write from {@link InputStream} to {@link OutputStream}.
     * Equivalent to {@link #transfer(Resource, InputStream, OutputStream, int, int)} with a maxSize equals to
     * {@link Integer#MAX_VALUE}
     *
     * @param resource    resource to transfer
     * @param input       input stream
     * @param output      output stream
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     * @throws IOException
     */
    protected void transfer(Resource resource, InputStream input, OutputStream output, int requestType)
            throws IOException {
        transfer(resource, input, output, requestType, Long.MAX_VALUE);
    }

    /**
     * Write from {@link InputStream} to {@link OutputStream}.
     * Equivalent to {@link #transfer(Resource, InputStream, OutputStream, int, int)} with a maxSize equals to
     * {@link Integer#MAX_VALUE}
     *
     * @param resource    resource to transfer
     * @param input       input stream
     * @param output      output stream
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     * @param maxSize     size of the buffer
     * @throws IOException
     * @deprecated Please use the transfer using long as type of maxSize
     */
    @Deprecated
    protected void transfer(Resource resource, InputStream input, OutputStream output, int requestType, int maxSize)
            throws IOException {
        transfer(resource, input, output, requestType, (long) maxSize);
    }

    /**
     * Write from {@link InputStream} to {@link OutputStream}.
     * Equivalent to {@link #transfer(Resource, InputStream, OutputStream, int, long)} with a maxSize equals to
     * {@link Integer#MAX_VALUE}
     *
     * @param resource    resource to transfer
     * @param input       input stream
     * @param output      output stream
     * @param requestType one of {@link TransferEvent#REQUEST_GET} or {@link TransferEvent#REQUEST_PUT}
     * @param maxSize     size of the buffer
     * @throws IOException
     */
    @SuppressWarnings("RedundantCast")
    protected void transfer(Resource resource, InputStream input, OutputStream output, int requestType, long maxSize)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(getBufferCapacityForTransfer(resource.getContentLength()));
        int halfBufferCapacity = buffer.capacity() / 2;

        TransferEvent transferEvent = new TransferEvent(this, resource, TransferEvent.TRANSFER_PROGRESS, requestType);
        transferEvent.setTimestamp(System.currentTimeMillis());

        ReadableByteChannel in = Channels.newChannel(input);

        long remaining = maxSize;
        while (remaining > 0L) {
            int read = in.read(buffer);

            if (read == -1) {
                // EOF, but some data has not been written yet.
                if (((Buffer) buffer).position() != 0) {
                    ((Buffer) buffer).flip();
                    fireTransferProgress(transferEvent, buffer.array(), ((Buffer) buffer).limit());
                    output.write(buffer.array(), 0, ((Buffer) buffer).limit());
                    ((Buffer) buffer).clear();
                }

                break;
            }

            // Prevent minichunking/fragmentation: when less than half the buffer is utilized,
            // read some more bytes before writing and firing progress.
            if (((Buffer) buffer).position() < halfBufferCapacity) {
                continue;
            }

            ((Buffer) buffer).flip();
            fireTransferProgress(transferEvent, buffer.array(), ((Buffer) buffer).limit());
            output.write(buffer.array(), 0, ((Buffer) buffer).limit());
            remaining -= ((Buffer) buffer).limit();
            ((Buffer) buffer).clear();
        }
        output.flush();
    }

    /**
     * Provides a buffer size for efficiently transferring the given amount of bytes such that
     * it is not fragmented into too many chunks. For larger files larger buffers are provided such that downstream
     * {@link #fireTransferProgress(TransferEvent, byte[], int) listeners} are not notified too frequently.
     * For instance, transferring gigabyte-sized resources would result in millions of notifications when using
     * only a few kibibytes of buffer, drastically slowing down transfer since transfer progress listeners and
     * notifications are synchronous and may block, e.g., when writing download progress status to console.
     *
     * @param numberOfBytes can be 0 or less, in which case a default buffer size is used.
     * @return a byte buffer suitable for transferring the given amount of bytes without too many chunks.
     */
    protected int getBufferCapacityForTransfer(long numberOfBytes) {
        if (numberOfBytes <= 0L) {
            return DEFAULT_BUFFER_SIZE;
        }

        final long numberOfBufferSegments = numberOfBytes / (BUFFER_SEGMENT_SIZE * MINIMUM_AMOUNT_OF_TRANSFER_CHUNKS);
        final long potentialBufferSize = numberOfBufferSegments * BUFFER_SEGMENT_SIZE;
        if (potentialBufferSize > Integer.MAX_VALUE) {
            return MAXIMUM_BUFFER_SIZE;
        }
        return min(MAXIMUM_BUFFER_SIZE, max(DEFAULT_BUFFER_SIZE, (int) potentialBufferSize));
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void fireTransferProgress(TransferEvent transferEvent, byte[] buffer, int n) {
        transferEventSupport.fireTransferProgress(transferEvent, buffer, n);
    }

    protected void fireGetCompleted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferCompleted(transferEvent);
    }

    protected void fireGetStarted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferStarted(transferEvent);
    }

    protected void fireGetInitiated(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferInitiated(transferEvent);
    }

    protected void firePutInitiated(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferInitiated(transferEvent);
    }

    protected void firePutCompleted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferCompleted(transferEvent);
    }

    protected void firePutStarted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        transferEventSupport.fireTransferStarted(transferEvent);
    }

    protected void fireSessionDisconnected() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTED);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionDisconnected(sessionEvent);
    }

    protected void fireSessionDisconnecting() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTING);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionDisconnecting(sessionEvent);
    }

    protected void fireSessionLoggedIn() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_LOGGED_IN);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionLoggedIn(sessionEvent);
    }

    protected void fireSessionLoggedOff() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_LOGGED_OFF);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionLoggedOff(sessionEvent);
    }

    protected void fireSessionOpened() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENED);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionOpened(sessionEvent);
    }

    protected void fireSessionOpening() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENING);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionOpening(sessionEvent);
    }

    protected void fireSessionConnectionRefused() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_CONNECTION_REFUSED);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionConnectionRefused(sessionEvent);
    }

    protected void fireSessionError(Exception exception) {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, exception);

        sessionEvent.setTimestamp(timestamp);

        sessionEventSupport.fireSessionError(sessionEvent);
    }

    protected void fireTransferDebug(String message) {
        transferEventSupport.fireDebug(message);
    }

    protected void fireSessionDebug(String message) {
        sessionEventSupport.fireDebug(message);
    }

    @Override
    public boolean hasTransferListener(TransferListener listener) {
        return transferEventSupport.hasTransferListener(listener);
    }

    @Override
    public void addTransferListener(TransferListener listener) {
        transferEventSupport.addTransferListener(listener);
    }

    @Override
    public void removeTransferListener(TransferListener listener) {
        transferEventSupport.removeTransferListener(listener);
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        sessionEventSupport.addSessionListener(listener);
    }

    @Override
    public boolean hasSessionListener(SessionListener listener) {
        return sessionEventSupport.hasSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        sessionEventSupport.removeSessionListener(listener);
    }

    protected void fireTransferError(Resource resource, Exception e, int requestType) {
        TransferEvent transferEvent = new TransferEvent(this, resource, e, requestType);
        transferEventSupport.fireTransferError(transferEvent);
    }

    public SessionEventSupport getSessionEventSupport() {
        return sessionEventSupport;
    }

    public void setSessionEventSupport(SessionEventSupport sessionEventSupport) {
        this.sessionEventSupport = sessionEventSupport;
    }

    public TransferEventSupport getTransferEventSupport() {
        return transferEventSupport;
    }

    public void setTransferEventSupport(TransferEventSupport transferEventSupport) {
        this.transferEventSupport = transferEventSupport;
    }

    /**
     * This method is used if you are not streaming the transfer, to make sure any listeners dependent on state
     * (eg checksum observers) succeed.
     */
    protected void postProcessListeners(Resource resource, File source, int requestType)
            throws TransferFailedException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        TransferEvent transferEvent = new TransferEvent(this, resource, TransferEvent.TRANSFER_PROGRESS, requestType);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(source);

        try (InputStream input = Files.newInputStream(source.toPath())) {
            while (true) {
                int n = input.read(buffer);

                if (n == -1) {
                    break;
                }

                fireTransferProgress(transferEvent, buffer, n);
            }
        } catch (IOException e) {
            fireTransferError(resource, e, requestType);
            throw new TransferFailedException("Failed to post-process the source file", e);
        }
    }

    @Override
    public void putDirectory(File sourceDirectory, String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throw new UnsupportedOperationException("The wagon you are using has not implemented putDirectory()");
    }

    @Override
    public boolean supportsDirectoryCopy() {
        return false;
    }

    protected static String getPath(String basedir, String dir) {
        String path;
        path = basedir;
        if (!basedir.endsWith("/") && !dir.startsWith("/")) {
            path += "/";
        }
        path += dir;
        return path;
    }

    @Override
    public boolean isInteractive() {
        return interactive;
    }

    @Override
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    @Override
    public List<String> getFileList(String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throw new UnsupportedOperationException("The wagon you are using has not implemented getFileList()");
    }

    @Override
    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        throw new UnsupportedOperationException("The wagon you are using has not implemented resourceExists()");
    }

    protected ProxyInfo getProxyInfo(String protocol, String host) {
        if (proxyInfoProvider != null) {
            ProxyInfo proxyInfo = proxyInfoProvider.getProxyInfo(protocol);
            if (!ProxyUtils.validateNonProxyHosts(proxyInfo, host)) {
                return proxyInfo;
            }
        }
        return null;
    }

    public RepositoryPermissions getPermissionsOverride() {
        return permissionsOverride;
    }

    public void setPermissionsOverride(RepositoryPermissions permissionsOverride) {
        this.permissionsOverride = permissionsOverride;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public int getReadTimeout() {
        return this.readTimeout;
    }
}
