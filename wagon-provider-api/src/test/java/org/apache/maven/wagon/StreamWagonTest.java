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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StreamWagonTest {

    private static class TestWagon extends StreamWagon {
        @Override
        public void closeConnection() {}

        @Override
        public void fillInputData(InputData inputData)
                throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {}

        @Override
        public void fillOutputData(OutputData outputData) throws TransferFailedException {}

        @Override
        protected void openConnectionInternal() {}
    }

    private final Repository repository = new Repository("id", "url");

    @Test
    void nullInputStream() throws Exception {

        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(null);
            }
        };

        TransferListener listener = mock(TransferListener.class);
        listener.transferInitiated(any(TransferEvent.class));

        TransferEvent transferEvent = new TransferEvent(
                wagon, new Resource("resource"), new TransferFailedException(""), TransferEvent.REQUEST_GET);
        listener.transferError(transferEvent);

        wagon.connect(repository);
        wagon.addTransferListener(listener);

        try (OutputStream os = new ByteArrayOutputStream()) {
            wagon.getToStream("resource", os);
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e);
            assertEquals("url - Could not open input stream for resource: 'resource'", e.getMessage());
        } finally {
            wagon.disconnect();
        }
        verify(listener, times(1)).transferInitiated(any(TransferEvent.class));
        verify(listener, never()).transferStarted(any(TransferEvent.class));
        verify(listener, never()).transferCompleted(any(TransferEvent.class));
    }

    @Test
    void nullOutputStream() throws Exception {

        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillOutputData(OutputData inputData) {
                inputData.setOutputStream(null);
            }
        };

        TransferListener listener = mock(TransferListener.class);
        listener.transferInitiated(any(TransferEvent.class));
        TransferEvent transferEvent = new TransferEvent(
                wagon, new Resource("resource"), new TransferFailedException(""), TransferEvent.REQUEST_PUT);
        listener.transferError(transferEvent);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try (InputStream is = new ByteArrayInputStream("".getBytes())) {
            wagon.putFromStream(is, "resource");
            fail();
        } catch (TransferFailedException e) {
            assertEquals("url - Could not open output stream for resource: 'resource'", e.getMessage());
        } finally {
            wagon.disconnect();
        }
        verify(listener, times(1)).transferInitiated(any(TransferEvent.class));
        verify(listener, never()).transferStarted(any(TransferEvent.class));
        verify(listener, never()).transferCompleted(any(TransferEvent.class));
    }

    @Test
    void transferFailedExceptionOnInput() {
        assertThrows(TransferFailedException.class, () -> runTestTransferError(new TransferFailedException("")));
    }

    @Test
    void transferFailedExceptionOnOutput() throws Exception {
        TransferFailedException ex = new TransferFailedException("");

        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillOutputData(OutputData inputData) throws TransferFailedException {
                throw ex;
            }
        };

        TransferListener listener = mock(TransferListener.class);
        listener.transferInitiated(any(TransferEvent.class));
        TransferEvent transferEvent = new TransferEvent(wagon, new Resource("resource"), ex, TransferEvent.REQUEST_PUT);
        listener.transferError(transferEvent);

        wagon.connect(repository);
        wagon.addTransferListener(listener);

        try (InputStream is = new ByteArrayInputStream("".getBytes())) {
            wagon.putFromStream(is, "resource");
            fail();
        } catch (TransferFailedException e) {
            assertEquals(ex, e);
        } finally {
            wagon.disconnect();
        }
        verify(listener, times(2)).transferError(any(TransferEvent.class));
    }

    @Test
    void resourceDoesNotExistException() {
        assertThrows(
                ResourceDoesNotExistException.class, () -> runTestTransferError(new ResourceDoesNotExistException("")));
    }

    @Test
    void authorizationException() {
        assertThrows(AuthorizationException.class, () -> runTestTransferError(new AuthorizationException("")));
    }

    private void runTestTransferError(WagonException exception)
            throws ConnectionException, AuthenticationException, ResourceDoesNotExistException, AuthorizationException,
                    TransferFailedException {

        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData)
                    throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
                if (exception instanceof TransferFailedException) {
                    throw (TransferFailedException) exception;
                }
                if (exception instanceof ResourceDoesNotExistException) {
                    throw (ResourceDoesNotExistException) exception;
                }
                if (exception instanceof AuthorizationException) {
                    throw (AuthorizationException) exception;
                }
            }
        };

        TransferListener listener = mock(TransferListener.class);
        listener.transferInitiated(any(TransferEvent.class));
        TransferEvent transferEvent =
                new TransferEvent(wagon, new Resource("resource"), exception, TransferEvent.REQUEST_GET);
        listener.transferError(transferEvent);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try {
            wagon.getToStream("resource", new ByteArrayOutputStream());
            fail();
        } finally {
            wagon.disconnect();
        }
        verify(listener, times(1)).transferInitiated(any(TransferEvent.class));
    }

    @Test
    void getIfNewerWithNewerResource() throws Exception {
        long resourceTime = System.currentTimeMillis();
        long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertTrue(runTestGetIfNewer(resourceTime, comparisonTime));
    }

    @Test
    void getIfNewerWithOlderResource() throws Exception {
        long comparisonTime = System.currentTimeMillis();
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewer(resourceTime, comparisonTime));
    }

    @Test
    void getIfNewerWithSameTimeResource() throws Exception {
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewer(resourceTime, resourceTime));
    }

    private boolean runTestGetIfNewer(final long resourceTime, long comparisonTime)
            throws IOException, ConnectionException, AuthenticationException, TransferFailedException,
                    ResourceDoesNotExistException, AuthorizationException {
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream("".getBytes()));
                inputData.getResource().setLastModified(resourceTime);
            }
        };

        File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.deleteOnExit();

        wagon.connect(repository);
        try {
            return wagon.getIfNewer("resource", tempFile, comparisonTime);
        } finally {
            wagon.disconnect();
            tempFile.delete();
        }
    }

    @Test
    void getToStream() throws Exception {
        final String content = "the content to return";
        final long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(content.getBytes()));
                inputData.getResource().setLastModified(comparisonTime);
            }
        };

        wagon.connect(repository);
        try {
            OutputStream out = new ByteArrayOutputStream();
            wagon.getToStream("resource", out);
            assertEquals(content, out.toString());
        } finally {
            wagon.disconnect();
        }
    }

    @Test
    void get() throws Exception {
        final String content = "the content to return";
        final long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(content.getBytes()));
                inputData.getResource().setLastModified(comparisonTime);
            }
        };

        File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.deleteOnExit();

        wagon.connect(repository);
        try {
            wagon.get("resource", tempFile);
            assertEquals(content, new String(Files.readAllBytes(tempFile.toPath())));
        } finally {
            wagon.disconnect();
            tempFile.delete();
        }
    }

    @Test
    void getIfNewerToStreamWithNewerResource() throws Exception {
        long resourceTime = System.currentTimeMillis();
        long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertTrue(runTestGetIfNewerToStream(resourceTime, comparisonTime));
    }

    @Test
    void getIfNewerToStreamWithOlderResource() throws Exception {
        long comparisonTime = System.currentTimeMillis();
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewerToStream(resourceTime, comparisonTime));
    }

    @Test
    void getIfNewerToStreamWithSameTimeResource() throws Exception {
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewerToStream(resourceTime, resourceTime));
    }

    private boolean runTestGetIfNewerToStream(final long resourceTime, long comparisonTime)
            throws IOException, ConnectionException, AuthenticationException, TransferFailedException,
                    ResourceDoesNotExistException, AuthorizationException {
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream("".getBytes()));
                inputData.getResource().setLastModified(resourceTime);
            }
        };

        wagon.connect(repository);
        try {
            return wagon.getIfNewerToStream("resource", new ByteArrayOutputStream(), comparisonTime);
        } finally {
            wagon.disconnect();
        }
    }

    @Test
    void putFromStream() throws Exception {
        final String content = "the content to return";

        OutputStream out = new ByteArrayOutputStream();
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillOutputData(OutputData outputData) {
                assertEquals("resource", outputData.getResource().getName());
                assertEquals(-1, outputData.getResource().getContentLength());
                assertEquals(0, outputData.getResource().getLastModified());
                outputData.setOutputStream(out);
            }
        };

        wagon.connect(repository);
        try {
            wagon.putFromStream(new ByteArrayInputStream(content.getBytes()), "resource");
            assertEquals(content, out.toString());
        } finally {
            wagon.disconnect();
        }
    }

    @Test
    void putFromStreamWithResourceInformation() throws Exception {
        final String content = "the content to return";
        final long lastModified = System.currentTimeMillis();

        OutputStream out = new ByteArrayOutputStream();
        StreamingWagon wagon = new TestWagon() {
            @Override
            public void fillOutputData(OutputData outputData) {
                assertEquals("resource", outputData.getResource().getName());
                assertEquals(content.length(), outputData.getResource().getContentLength());
                assertEquals(lastModified, outputData.getResource().getLastModified());
                outputData.setOutputStream(out);
            }
        };

        wagon.connect(repository);
        try {
            wagon.putFromStream(
                    new ByteArrayInputStream(content.getBytes()), "resource", content.length(), lastModified);
            assertEquals(content, out.toString());
        } finally {
            wagon.disconnect();
        }
    }

    @Test
    void put() throws Exception {
        String content = "the content to return";

        File tempFile = File.createTempFile("wagon", "tmp");
        Files.write(Paths.get(tempFile.getAbsolutePath()), content.getBytes());
        tempFile.deleteOnExit();

        OutputStream out = new ByteArrayOutputStream();
        Wagon wagon = new TestWagon() {
            @Override
            public void fillOutputData(OutputData outputData) {
                assertEquals("resource", outputData.getResource().getName());
                assertEquals(content.length(), outputData.getResource().getContentLength());
                assertEquals(tempFile.lastModified(), outputData.getResource().getLastModified());
                outputData.setOutputStream(out);
            }
        };

        wagon.connect(repository);
        try {
            wagon.put(tempFile, "resource");
            assertEquals(content, out.toString());
        } finally {
            wagon.disconnect();
            tempFile.delete();
        }
    }

    @Test
    void putFileDoesntExist() throws Exception {
        final File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.delete();
        assertFalse(tempFile.exists());

        Wagon wagon = new TestWagon();

        wagon.connect(repository);
        assertThrows(TransferFailedException.class, () -> wagon.put(tempFile, "resource"));

        wagon.disconnect();
    }
}
