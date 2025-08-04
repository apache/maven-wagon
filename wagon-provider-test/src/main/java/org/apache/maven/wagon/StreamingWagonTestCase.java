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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class StreamingWagonTestCase extends WagonTestCase {
    @Test
    public void testStreamingWagon() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();

            setupRepositories();

            streamRoundTripTesting();

            tearDownWagonTestingFixtures();
        }
    }

    @Test
    public void testFailedGetToStream() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        message("Getting test artifact from test repository " + testRepository);

        StreamingWagon wagon = (StreamingWagon) getWagon();

        wagon.addTransferListener(checksumObserver);

        wagon.connect(testRepository, getAuthInfo());

        destFile = FileTestUtils.createUniqueFile(getName(), getName());

        destFile.deleteOnExit();

        try (OutputStream stream = Files.newOutputStream(destFile.toPath())) {
            assertThrows(ResourceDoesNotExistException.class, () -> wagon.getToStream("fubar.txt", stream));

        } finally {
            wagon.removeTransferListener(checksumObserver);

            wagon.disconnect();

            tearDownWagonTestingFixtures();
        }
    }

    @Test
    public void testWagonGetIfNewerToStreamIsNewer() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();
            setupRepositories();

            int expectedSize = putFile();
            // CHECKSTYLE_OFF: MagicNumber
            getIfNewerToStream(
                    getExpectedLastModifiedOnGet(testRepository, new Resource(resource)) + 30000, false, expectedSize);
            // CHECKSTYLE_ON: MagicNumber
        }
    }

    @Test
    public void testWagonGetIfNewerToStreamIsOlder() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();
            setupRepositories();
            int expectedSize = putFile();
            getIfNewerToStream(
                    new SimpleDateFormat("yyyy-MM-dd").parse("2006-01-01").getTime(), true, expectedSize);
        }
    }

    @Test
    public void testWagonGetIfNewerToStreamIsSame() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();
            setupRepositories();
            int expectedSize = putFile();
            getIfNewerToStream(
                    getExpectedLastModifiedOnGet(testRepository, new Resource(resource)), false, expectedSize);
        }
    }

    private void getIfNewerToStream(long timestamp, boolean expectedResult, int expectedSize) throws Exception {
        StreamingWagon wagon = (StreamingWagon) getWagon();

        ProgressAnswer progressAnswer = setupGetIfNewerTest(wagon, expectedResult, expectedSize);

        connectWagon(wagon);

        try (OutputStream stream = new LazyFileOutputStream(destFile)) {
            boolean result = wagon.getIfNewerToStream(this.resource, stream, timestamp);
            assertEquals(expectedResult, result);
        }

        disconnectWagon(wagon);

        assertGetIfNewerTest(progressAnswer, expectedResult, expectedSize);

        tearDownWagonTestingFixtures();
    }

    @Test
    public void testFailedGetIfNewerToStream() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();
            setupRepositories();
            message("Getting test artifact from test repository " + testRepository);
            StreamingWagon wagon = (StreamingWagon) getWagon();
            wagon.addTransferListener(checksumObserver);
            wagon.connect(testRepository, getAuthInfo());
            destFile = FileTestUtils.createUniqueFile(getName(), getName());
            destFile.deleteOnExit();

            try (OutputStream stream = Files.newOutputStream(destFile.toPath())) {
                assertThrows(
                        ResourceDoesNotExistException.class, () -> wagon.getIfNewerToStream("fubar.txt", stream, 0));
            } finally {
                wagon.removeTransferListener(checksumObserver);

                wagon.disconnect();

                tearDownWagonTestingFixtures();
            }
        }
    }

    protected void streamRoundTripTesting() throws Exception {
        message("Stream round trip testing ...");

        int expectedSize = putStream();

        assertNotNull(checksumObserver.getActualChecksum(), "check checksum is not null");

        assertEquals("6b144b7285ffd6b0bc8300da162120b9", checksumObserver.getActualChecksum(), "compare checksums");

        checksumObserver = new ChecksumObserver();

        getStream(expectedSize);

        assertNotNull(checksumObserver.getActualChecksum(), "check checksum is not null");

        assertEquals("6b144b7285ffd6b0bc8300da162120b9", checksumObserver.getActualChecksum(), "compare checksums");

        // Now compare the contents of the artifact that was placed in
        // the repository with the contents of the artifact that was
        // retrieved from the repository.

        byte[] sourceContent = Files.readAllBytes(sourceFile.toPath());

        byte[] destContent = Files.readAllBytes(destFile.toPath());

        assertArrayEquals(sourceContent, destContent);
    }

    private int putStream() throws Exception {
        String content = "test-resource.txt\n";
        sourceFile = new File(FileTestUtils.getTestOutputDir(), "test-resource");
        sourceFile.getParentFile().mkdirs();
        Files.write(sourceFile.toPath().toAbsolutePath(), content.getBytes(StandardCharsets.UTF_8));

        StreamingWagon wagon = (StreamingWagon) getWagon();

        ProgressAnswer progressAnswer = replayMockForPut(resource, content, wagon);

        message("Putting test artifact: " + resource + " into test repository " + testRepository);

        connectWagon(wagon);

        try (InputStream stream = Files.newInputStream(sourceFile.toPath())) {
            wagon.putFromStream(stream, resource, sourceFile.length(), sourceFile.lastModified());
        } catch (Exception e) {
            logger.error("error while putting resources to the FTP Server", e);
        }

        disconnectWagon(wagon);

        verifyMock(progressAnswer, content.length());
        return content.length();
    }

    private void getStream(int expectedSize) throws Exception {
        destFile = FileTestUtils.createUniqueFile(getName(), getName());
        destFile.deleteOnExit();

        StreamingWagon wagon = (StreamingWagon) getWagon();

        ProgressAnswer progressAnswer = replaceMockForGet(wagon, expectedSize);

        message("Getting test artifact from test repository " + testRepository);

        connectWagon(wagon);

        try (OutputStream stream = Files.newOutputStream(destFile.toPath())) {
            wagon.getToStream(this.resource, stream);
        } catch (Exception e) {
            logger.error("error while reading resources from the FTP Server", e);
        }

        disconnectWagon(wagon);

        verifyMock(progressAnswer, expectedSize);
    }
}
