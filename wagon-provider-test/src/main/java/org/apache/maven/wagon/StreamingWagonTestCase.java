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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class StreamingWagonTestCase extends WagonTestCase {
    public void testStreamingWagon() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();

            setupRepositories();

            streamRoundTripTesting();

            tearDownWagonTestingFixtures();
        }
    }

    public void testFailedGetToStream() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        message("Getting test artifact from test repository " + testRepository);

        StreamingWagon wagon = (StreamingWagon) getWagon();

        wagon.addTransferListener(checksumObserver);

        wagon.connect(testRepository, getAuthInfo());

        destFile = FileTestUtils.createUniqueFile(getName(), getName());

        destFile.deleteOnExit();

        OutputStream stream = null;

        try {
            stream = new FileOutputStream(destFile);
            wagon.getToStream("fubar.txt", stream);
            fail("File was found when it shouldn't have been");
            stream.close();
            stream = null;
        } catch (ResourceDoesNotExistException e) {
            // expected
            assertTrue(true);
        } finally {
            wagon.removeTransferListener(checksumObserver);

            wagon.disconnect();

            IOUtil.close(stream);

            tearDownWagonTestingFixtures();
        }
    }

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

    public void testWagonGetIfNewerToStreamIsOlder() throws Exception {
        if (supportsGetIfNewer()) {
            setupWagonTestingFixtures();
            setupRepositories();
            int expectedSize = putFile();
            getIfNewerToStream(
                    new SimpleDateFormat("yyyy-MM-dd").parse("2006-01-01").getTime(), true, expectedSize);
        }
    }

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

        OutputStream stream = new LazyFileOutputStream(destFile);

        try {
            boolean result = wagon.getIfNewerToStream(this.resource, stream, timestamp);
            assertEquals(expectedResult, result);
        } finally {
            stream.close();
        }

        disconnectWagon(wagon);

        assertGetIfNewerTest(progressAnswer, expectedResult, expectedSize);

        tearDownWagonTestingFixtures();
    }

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
            OutputStream stream = null;
            try {
                stream = new FileOutputStream(destFile);
                wagon.getIfNewerToStream("fubar.txt", stream, 0);
                fail("File was found when it shouldn't have been");
                stream.close();
                stream = null;
            } catch (ResourceDoesNotExistException e) {
                // expected
                assertTrue(true);
            } finally {
                wagon.removeTransferListener(checksumObserver);

                wagon.disconnect();

                IOUtil.close(stream);

                tearDownWagonTestingFixtures();
            }
        }
    }

    protected void streamRoundTripTesting() throws Exception {
        message("Stream round trip testing ...");

        int expectedSize = putStream();

        assertNotNull("check checksum is not null", checksumObserver.getActualChecksum());

        assertEquals("compare checksums", "6b144b7285ffd6b0bc8300da162120b9", checksumObserver.getActualChecksum());

        checksumObserver = new ChecksumObserver();

        getStream(expectedSize);

        assertNotNull("check checksum is not null", checksumObserver.getActualChecksum());

        assertEquals("compare checksums", "6b144b7285ffd6b0bc8300da162120b9", checksumObserver.getActualChecksum());

        // Now compare the conents of the artifact that was placed in
        // the repository with the contents of the artifact that was
        // retrieved from the repository.

        String sourceContent = FileUtils.fileRead(sourceFile);

        String destContent = FileUtils.fileRead(destFile);

        assertEquals(sourceContent, destContent);
    }

    private int putStream() throws Exception {
        String content = "test-resource.txt\n";
        sourceFile = new File(FileTestUtils.getTestOutputDir(), "test-resource");
        sourceFile.getParentFile().mkdirs();
        FileUtils.fileWrite(sourceFile.getAbsolutePath(), content);

        StreamingWagon wagon = (StreamingWagon) getWagon();

        ProgressAnswer progressAnswer = replayMockForPut(resource, content, wagon);

        message("Putting test artifact: " + resource + " into test repository " + testRepository);

        connectWagon(wagon);

        InputStream stream = null;

        try {
            stream = new FileInputStream(sourceFile);
            wagon.putFromStream(stream, resource, sourceFile.length(), sourceFile.lastModified());
            stream.close();
            stream = null;
        } catch (Exception e) {
            logger.error("error while putting resources to the FTP Server", e);
        } finally {
            IOUtil.close(stream);
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

        OutputStream stream = null;

        try {
            stream = new FileOutputStream(destFile);
            wagon.getToStream(this.resource, stream);
            stream.close();
            stream = null;
        } catch (Exception e) {
            logger.error("error while reading resources from the FTP Server", e);
        } finally {
            IOUtil.close(stream);
        }

        disconnectWagon(wagon);

        verifyMock(progressAnswer, expectedSize);
    }
}
