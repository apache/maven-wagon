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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 */
class AbstractWagonTest {

    private static class TestWagon extends AbstractWagon {
        @Override
        protected void closeConnection() throws ConnectionException {}

        @Override
        protected void openConnectionInternal() throws ConnectionException, AuthenticationException {}

        @Override
        public void get(String resourceName, File destination) {}

        @Override
        public boolean getIfNewer(String resourceName, File destination, long timestamp) {
            return false;
        }

        @Override
        public void put(File source, String destination) {}
    }

    private WagonMock wagon = null;

    private File destination;

    private File source;

    private String artifact;

    private SessionListener sessionListener = null;

    private TransferListener transferListener = null;

    @BeforeEach
    void setUp() {
        String basedir = System.getProperty("basedir");

        destination = new File(basedir, "target/folder/subfolder");
        source = new File(basedir, "pom.xml");
        wagon = new WagonMock();
        sessionListener = mock(SessionListener.class);
        wagon.addSessionListener(sessionListener);
        transferListener = mock(TransferListener.class);
        wagon.addTransferListener(transferListener);
    }

    @Test
    void calculationOfTransferBufferSize() {
        // 1 KiB -> Default buffer size (4 KiB)
        assertEquals(4096, wagon.getBufferCapacityForTransfer(1024L));

        // 1 MiB -> Twice the default buffer size (8 KiB)
        assertEquals(4096 * 2, wagon.getBufferCapacityForTransfer(1024L * 1024));

        // 100 MiB -> Maximum buffer size (512 KiB)
        assertEquals(4096 * 128, wagon.getBufferCapacityForTransfer(1024L * 1024 * 100));

        // 1 GiB -> Maximum buffer size (512 KiB)
        assertEquals(4096 * 128, wagon.getBufferCapacityForTransfer(1024L * 1024 * 1024));

        // 100 GiB -> Maximum buffer size (512 KiB)
        assertEquals(4096 * 128, wagon.getBufferCapacityForTransfer(1024L * 1024 * 1024 * 100));
    }

    @Test
    void sessionListenerRegistration() {
        assertTrue(wagon.hasSessionListener(sessionListener));

        wagon.removeSessionListener(sessionListener);

        assertFalse(wagon.hasSessionListener(sessionListener));
    }

    @Test
    void transferListenerRegistration() {
        assertTrue(wagon.hasTransferListener(transferListener));

        wagon.removeTransferListener(transferListener);

        assertFalse(wagon.hasTransferListener(transferListener));
    }

    @Test
    void noProxyConfiguration() throws Exception {
        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository);
        assertNull(wagon.getProxyInfo());
        assertNull(wagon.getProxyInfo("http", "www.example.com"));
        assertNull(wagon.getProxyInfo("dav", "www.example.com"));
        assertNull(wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
        assertNull(wagon.getProxyInfo("http", "localhost"));
    }

    @Test
    void nullProxyConfiguration() throws Exception {
        Repository repository = new Repository("fake", "http://fake");

        wagon.connect(repository, (ProxyInfo) null);
        verify(sessionListener).sessionOpened(any());
        assertNull(wagon.getProxyInfo());
        assertNull(wagon.getProxyInfo("http", "www.example.com"));
        assertNull(wagon.getProxyInfo("dav", "www.example.com"));
        assertNull(wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
        assertNull(wagon.getProxyInfo("http", "localhost"));

        wagon.connect(repository);
        verify(sessionListener, times(2)).sessionOpened(any());
        assertNull(wagon.getProxyInfo());
        assertNull(wagon.getProxyInfo("http", "www.example.com"));
        assertNull(wagon.getProxyInfo("dav", "www.example.com"));
        assertNull(wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
        assertNull(wagon.getProxyInfo("http", "localhost"));

        wagon.connect(repository, new AuthenticationInfo());
        verify(sessionListener, times(3)).sessionOpened(any());
        assertNull(wagon.getProxyInfo());
        assertNull(wagon.getProxyInfo("http", "www.example.com"));
        assertNull(wagon.getProxyInfo("dav", "www.example.com"));
        assertNull(wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
        assertNull(wagon.getProxyInfo("http", "localhost"));
    }

    @Test
    void legacyProxyConfiguration() throws Exception {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType("http");

        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository, proxyInfo);
        assertEquals(proxyInfo, wagon.getProxyInfo());
        assertEquals(proxyInfo, wagon.getProxyInfo("http", "www.example.com"));
        assertNull(wagon.getProxyInfo("dav", "www.example.com"));
        assertNull(wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
    }

    @Test
    void proxyConfiguration() throws Exception {
        final ProxyInfo httpProxyInfo = new ProxyInfo();
        httpProxyInfo.setType("http");

        final ProxyInfo socksProxyInfo = new ProxyInfo();
        socksProxyInfo.setType("http");

        ProxyInfoProvider proxyInfoProvider = protocol -> {
            if ("http".equals(protocol) || "dav".equals(protocol)) {
                return httpProxyInfo;
            } else if ("scp".equals(protocol)) {
                return socksProxyInfo;
            }
            return null;
        };

        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository, proxyInfoProvider);
        assertNull(wagon.getProxyInfo());
        assertEquals(httpProxyInfo, wagon.getProxyInfo("http", "www.example.com"));
        assertEquals(httpProxyInfo, wagon.getProxyInfo("dav", "www.example.com"));
        assertEquals(socksProxyInfo, wagon.getProxyInfo("scp", "www.example.com"));
        assertNull(wagon.getProxyInfo("ftp", "www.example.com"));
    }

    @Test
    void sessionOpenEvents() throws Exception {
        Repository repository = new Repository("fake", "http://fake");

        sessionListener.sessionOpening(any(SessionEvent.class));
        sessionListener.sessionOpened(any(SessionEvent.class));

        wagon.connect(repository);

        verify(sessionListener).sessionOpening(any(SessionEvent.class));
        verify(sessionListener).sessionOpened(any(SessionEvent.class));

        assertEquals(repository, wagon.getRepository());
    }

    @Test
    void sessionConnectionRefusedEventConnectionException() {
        assertThrows(
                ConnectionException.class, () -> runTestSessionConnectionRefusedEvent(new ConnectionException("")));
    }

    @Test
    void sessionConnectionRefusedEventAuthenticationException() {
        assertThrows(
                AuthenticationException.class,
                () -> runTestSessionConnectionRefusedEvent(new AuthenticationException("")));
    }

    private void runTestSessionConnectionRefusedEvent(final WagonException exception)
            throws ConnectionException, AuthenticationException {
        Repository repository = new Repository("fake", "http://fake");

        Wagon wagon = new TestWagon() {
            @Override
            protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
                if (exception instanceof ConnectionException) {
                    throw (ConnectionException) exception;
                }
                if (exception instanceof AuthenticationException) {
                    throw (AuthenticationException) exception;
                }
            }
        };
        wagon.addSessionListener(sessionListener);

        try {
            wagon.connect(repository);
            fail();
        } finally {
            verify(sessionListener).sessionOpening(any(SessionEvent.class));
            verify(sessionListener).sessionConnectionRefused(any(SessionEvent.class));
            assertEquals(repository, wagon.getRepository());
        }
    }

    @Test
    void sessionCloseEvents() throws Exception {
        wagon.disconnect();

        verify(sessionListener).sessionDisconnecting(any(SessionEvent.class));
        verify(sessionListener).sessionDisconnected(any(SessionEvent.class));
    }

    @Test
    void sessionCloseRefusedEventConnectionException() throws Exception {
        Wagon wagon = new TestWagon() {
            @Override
            protected void closeConnection() throws ConnectionException {
                throw new ConnectionException("");
            }
        };
        wagon.addSessionListener(sessionListener);

        assertThrows(ConnectionException.class, wagon::disconnect);

        verify(sessionListener).sessionDisconnecting(any(SessionEvent.class));
        verify(sessionListener).sessionError(any(SessionEvent.class));
        verify(sessionListener, never()).sessionDisconnected(any(SessionEvent.class));
    }

    @Test
    void getTransferEvents() throws Exception {
        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository);

        wagon.fireTransferDebug("fetch debug message");
        wagon.get(artifact, destination);

        verify(transferListener).debug("fetch debug message");
        verify(transferListener).transferInitiated(any(TransferEvent.class));
        verify(transferListener).transferStarted(any(TransferEvent.class));
        verify(transferListener, atLeastOnce()).debug(anyString());
        verify(transferListener, atLeastOnce()).transferProgress(any(TransferEvent.class), any(byte[].class), anyInt());
        verify(transferListener).transferCompleted(any(TransferEvent.class));
    }

    @Test
    void getError() {
        assertThrows(TransferFailedException.class, () -> {
            Repository repository = new Repository("fake", "http://fake");
            WagonMock wagon = new WagonMock(true);
            wagon.addTransferListener(transferListener);

            wagon.connect(repository);

            wagon.get(artifact, destination);
        });

        verify(transferListener).transferInitiated(any(TransferEvent.class));
        verify(transferListener).transferStarted(any(TransferEvent.class));
        verify(transferListener).debug(anyString());
        verify(transferListener).transferError(any(TransferEvent.class));
    }

    @Test
    void putTransferEvents() throws Exception {
        wagon.fireTransferDebug("deploy debug message");

        Repository repository = new Repository("fake", "http://fake");
        wagon.connect(repository);
        wagon.put(source, artifact);

        verify(transferListener).transferInitiated(any(TransferEvent.class));
        verify(transferListener).transferStarted(any(TransferEvent.class));
        verify(transferListener).transferProgress(any(TransferEvent.class), any(byte[].class), anyInt());
        verify(transferListener).transferCompleted(any(TransferEvent.class));
        verify(transferListener).debug("deploy debug message");
    }

    @Test
    void streamShutdown() {
        InputStreamMock inputStream = new InputStreamMock();
        OutputStreamMock outputStream = new OutputStreamMock();

        try (InputStreamMock in = inputStream) {
            assertFalse(in.isClosed());
        }
        assertTrue(inputStream.isClosed());

        try (OutputStreamMock out = outputStream) {
            assertFalse(out.isClosed());
        }
        assertTrue(outputStream.isClosed());
    }

    @Test
    void repositoryPermissionsOverride() throws Exception {
        Repository repository = new Repository("fake", "http://fake");

        RepositoryPermissions original = new RepositoryPermissions();
        original.setFileMode("664");
        repository.setPermissions(original);

        RepositoryPermissions override = new RepositoryPermissions();
        override.setFileMode("644");
        wagon.setPermissionsOverride(override);

        wagon.connect(repository);

        assertEquals(override, repository.getPermissions());
        assertEquals("644", repository.getPermissions().getFileMode());
    }

    @Test
    void repositoryUserName() throws Exception {
        Repository repository = new Repository("id", "http://bporter:password@www.example.com/path/to/resource");

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName("brett");
        authenticationInfo.setPassword("pass");
        wagon.connect(repository, authenticationInfo);

        assertEquals(authenticationInfo, wagon.getAuthenticationInfo());
        assertEquals("brett", authenticationInfo.getUserName());
        assertEquals("pass", authenticationInfo.getPassword());

        verify(sessionListener, never()).sessionError(any(SessionEvent.class));
    }

    @Test
    void repositoryUserNameNotGivenInCredentials() throws Exception {
        Repository repository = new Repository("id", "http://bporter:password@www.example.com/path/to/resource");

        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        wagon.connect(repository, authenticationInfo);

        assertEquals(authenticationInfo, wagon.getAuthenticationInfo());
        assertEquals("bporter", authenticationInfo.getUserName());
        assertEquals("password", authenticationInfo.getPassword());
    }

    @Test
    void connectNullRepository() {
        assertThrows(NullPointerException.class, () -> wagon.connect(null));
    }

    @Test
    void postProcessListeners() throws Exception {
        File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.deleteOnExit();
        String content = "content";
        Files.write(Paths.get(tempFile.getAbsolutePath()), content.getBytes());

        Resource resource = new Resource("resource");
        wagon.postProcessListeners(resource, tempFile, TransferEvent.REQUEST_PUT);

        // verify(transferListener).transferInitiated(any(TransferEvent.class));
        // verify(transferListener).transferStarted(any(TransferEvent.class));
        TransferEvent event =
                new TransferEvent(wagon, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT);
        event.setLocalFile(tempFile);
        verify(transferListener).transferProgress(eq(event), any(byte[].class), eq(content.length()));
        // verify(transferListener).transferCompleted(any(TransferEvent.class));

        tempFile.delete();
    }
}
