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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

import static org.easymock.EasyMock.*;

public class StreamWagonTest extends TestCase {
    private static class TestWagon extends StreamWagon {
        public void closeConnection() throws ConnectionException {}

        public void fillInputData(InputData inputData)
                throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {}

        public void fillOutputData(OutputData outputData) throws TransferFailedException {}

        protected void openConnectionInternal() throws ConnectionException, AuthenticationException {}
    }

    private Repository repository = new Repository("id", "url");

    public void testNullInputStream() throws Exception {
        StreamingWagon wagon = new TestWagon() {
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(null);
            }
        };

        TransferListener listener = createMock(TransferListener.class);
        listener.transferInitiated(anyObject(TransferEvent.class));
        TransferEvent transferEvent = new TransferEvent(
                wagon, new Resource("resource"), new TransferFailedException(""), TransferEvent.REQUEST_GET);
        listener.transferError(transferEvent);
        replay(listener);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try {
            wagon.getToStream("resource", new ByteArrayOutputStream());
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e.getMessage());
        } finally {
            wagon.disconnect();
        }

        verify(listener);
    }

    public void testNullOutputStream() throws Exception {
        StreamingWagon wagon = new TestWagon() {
            public void fillOutputData(OutputData inputData) {
                inputData.setOutputStream(null);
            }
        };

        TransferListener listener = createMock(TransferListener.class);
        listener.transferInitiated(anyObject(TransferEvent.class));
        TransferEvent transferEvent = new TransferEvent(
                wagon, new Resource("resource"), new TransferFailedException(""), TransferEvent.REQUEST_PUT);
        listener.transferError(transferEvent);
        replay(listener);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try {
            wagon.putFromStream(new ByteArrayInputStream(new byte[0]), "resource");
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e.getMessage());
        } finally {
            wagon.disconnect();
        }

        verify(listener);
    }

    public void testTransferFailedExceptionOnInput() throws Exception {
        try {
            runTestTransferError(new TransferFailedException(""));
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testTransferFailedExceptionOnOutput() throws Exception {
        StreamingWagon wagon = new TestWagon() {
            public void fillOutputData(OutputData inputData) throws TransferFailedException {
                throw (TransferFailedException) new TransferFailedException("");
            }
        };

        TransferListener listener = createMock(TransferListener.class);
        listener.transferInitiated(anyObject(TransferEvent.class));
        TransferEvent transferEvent = new TransferEvent(
                wagon, new Resource("resource"), new TransferFailedException(""), TransferEvent.REQUEST_PUT);
        listener.transferError(transferEvent);
        replay(listener);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try {
            wagon.putFromStream(new ByteArrayInputStream(new byte[0]), "resource");
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e.getMessage());
        } finally {
            wagon.disconnect();
            verify(listener);
        }
    }

    public void testResourceDoesNotExistException() throws Exception {
        try {
            runTestTransferError(new ResourceDoesNotExistException(""));
            fail();
        } catch (ResourceDoesNotExistException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testAuthorizationException() throws Exception {
        try {
            runTestTransferError(new AuthorizationException(""));
            fail();
        } catch (AuthorizationException e) {
            assertNotNull(e.getMessage());
        }
    }

    private void runTestTransferError(final WagonException exception)
            throws ConnectionException, AuthenticationException, ResourceDoesNotExistException, AuthorizationException,
                    TransferFailedException {
        StreamingWagon wagon = new TestWagon() {
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

        TransferListener listener = createMock(TransferListener.class);
        listener.transferInitiated(anyObject(TransferEvent.class));
        TransferEvent transferEvent =
                new TransferEvent(wagon, new Resource("resource"), exception, TransferEvent.REQUEST_GET);
        listener.transferError(transferEvent);
        replay(listener);

        wagon.connect(repository);
        wagon.addTransferListener(listener);
        try {
            wagon.getToStream("resource", new ByteArrayOutputStream());
            fail();
        } finally {
            wagon.disconnect();
            verify(listener);
        }
    }

    public void testGetIfNewerWithNewerResource() throws Exception {
        long resourceTime = System.currentTimeMillis();
        long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertTrue(runTestGetIfNewer(resourceTime, comparisonTime));
    }

    public void testGetIfNewerWithOlderResource() throws Exception {
        long comparisonTime = System.currentTimeMillis();
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewer(resourceTime, comparisonTime));
    }

    public void testGetIfNewerWithSameTimeResource() throws Exception {
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewer(resourceTime, resourceTime));
    }

    private boolean runTestGetIfNewer(final long resourceTime, long comparisonTime)
            throws IOException, ConnectionException, AuthenticationException, TransferFailedException,
                    ResourceDoesNotExistException, AuthorizationException {
        StreamingWagon wagon = new TestWagon() {
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(new byte[0]));
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

    public void testGetToStream() throws Exception {
        final String content = "the content to return";
        final long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        StreamingWagon wagon = new TestWagon() {
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
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

    public void testGet() throws Exception {
        final String content = "the content to return";
        final long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        StreamingWagon wagon = new TestWagon() {
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                inputData.getResource().setLastModified(comparisonTime);
            }
        };

        File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.deleteOnExit();

        wagon.connect(repository);
        try {
            wagon.get("resource", tempFile);
            assertEquals(content, FileUtils.fileRead(tempFile));
        } finally {
            wagon.disconnect();
            tempFile.delete();
        }
    }

    public void testGetIfNewerToStreamWithNewerResource() throws Exception {
        long resourceTime = System.currentTimeMillis();
        long comparisonTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertTrue(runTestGetIfNewerToStream(resourceTime, comparisonTime));
    }

    public void testGetIfNewerToStreamWithOlderResource() throws Exception {
        long comparisonTime = System.currentTimeMillis();
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewerToStream(resourceTime, comparisonTime));
    }

    public void testGetIfNewerToStreamWithSameTimeResource() throws Exception {
        long resourceTime =
                new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01").getTime();
        assertFalse(runTestGetIfNewerToStream(resourceTime, resourceTime));
    }

    private boolean runTestGetIfNewerToStream(final long resourceTime, long comparisonTime)
            throws IOException, ConnectionException, AuthenticationException, TransferFailedException,
                    ResourceDoesNotExistException, AuthorizationException {
        StreamingWagon wagon = new TestWagon() {
            public void fillInputData(InputData inputData) {
                inputData.setInputStream(new ByteArrayInputStream(new byte[0]));
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

    public void testPutFromStream() throws Exception {
        final String content = "the content to return";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamingWagon wagon = new TestWagon() {
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

    public void testPutFromStreamWithResourceInformation() throws Exception {
        final String content = "the content to return";
        final long lastModified = System.currentTimeMillis();

        OutputStream out = new ByteArrayOutputStream();
        StreamingWagon wagon = new TestWagon() {
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
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    "resource",
                    content.length(),
                    lastModified);
            assertEquals(content, out.toString());
        } finally {
            wagon.disconnect();
        }
    }

    public void testPut() throws Exception {
        final String content = "the content to return";

        final File tempFile = File.createTempFile("wagon", "tmp");
        FileUtils.fileWrite(tempFile.getAbsolutePath(), content);
        tempFile.deleteOnExit();

        OutputStream out = new ByteArrayOutputStream();
        Wagon wagon = new TestWagon() {
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

    public void testPutFileDoesntExist() throws Exception {
        final File tempFile = File.createTempFile("wagon", "tmp");
        tempFile.delete();
        assertFalse(tempFile.exists());

        Wagon wagon = new TestWagon();

        wagon.connect(repository);
        try {
            wagon.put(tempFile, "resource");
            fail();
        } catch (TransferFailedException e) {
            assertNotNull(e.getMessage());
        } finally {
            wagon.disconnect();
        }
    }
}
