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
package org.apache.maven.wagon.providers.webdav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for parsing {@code 207 Multi-Status} PROPFIND responses.
 */
public class MultiStatusTest {

    private static List<MultiStatus.Response> parse(String xml) throws IOException {
        return MultiStatus.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getResponses();
    }

    @Test
    public void testCollectionIsRecognised() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response>"
                + "    <D:href>/repo/dir/</D:href>"
                + "    <D:propstat>"
                + "      <D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop>"
                + "      <D:status>HTTP/1.1 200 OK</D:status>"
                + "    </D:propstat>"
                + "  </D:response>"
                + "</D:multistatus>");

        assertEquals(1, responses.size());
        assertEquals("/repo/dir/", responses.get(0).getHref());
        assertTrue(responses.get(0).isCollection());
    }

    @Test
    public void testPlainResourceIsNotACollection() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response>"
                + "    <D:href>/repo/artifact.jar</D:href>"
                + "    <D:propstat>"
                + "      <D:prop><D:resourcetype/></D:prop>"
                + "      <D:status>HTTP/1.1 200 OK</D:status>"
                + "    </D:propstat>"
                + "  </D:response>"
                + "</D:multistatus>");

        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isCollection());
    }

    /**
     * A resourcetype reported under a non-200 propstat says nothing about the resource.
     */
    @Test
    public void testResourceTypeUnderNonOkStatusIsIgnored() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response>"
                + "    <D:href>/repo/thing</D:href>"
                + "    <D:propstat>"
                + "      <D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop>"
                + "      <D:status>HTTP/1.1 404 Not Found</D:status>"
                + "    </D:propstat>"
                + "  </D:response>"
                + "</D:multistatus>");

        assertFalse(responses.get(0).isCollection());
    }

    /**
     * {@code getFileList} relies on the requested collection arriving first, per RFC 4918 9.1.
     */
    @Test
    public void testDocumentOrderIsPreserved() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response><D:href>/repo/dir/</D:href></D:response>"
                + "  <D:response><D:href>/repo/dir/a.jar</D:href></D:response>"
                + "  <D:response><D:href>/repo/dir/b.jar</D:href></D:response>"
                + "</D:multistatus>");

        assertEquals(3, responses.size());
        assertEquals("/repo/dir/", responses.get(0).getHref());
        assertEquals("/repo/dir/a.jar", responses.get(1).getHref());
        assertEquals("/repo/dir/b.jar", responses.get(2).getHref());
    }

    /**
     * An href must occur only once; should a server repeat one, the last wins and keeps the
     * position of the first, as the Jackrabbit-backed code did.
     */
    @Test
    public void testDuplicateHrefsCollapse() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response><D:href>/repo/dir/a</D:href></D:response>"
                + "  <D:response><D:href>/repo/dir/b</D:href></D:response>"
                + "  <D:response>"
                + "    <D:href>/repo/dir/a</D:href>"
                + "    <D:propstat>"
                + "      <D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop>"
                + "      <D:status>HTTP/1.1 200 OK</D:status>"
                + "    </D:propstat>"
                + "  </D:response>"
                + "</D:multistatus>");

        assertEquals(2, responses.size());
        assertEquals("/repo/dir/a", responses.get(0).getHref());
        assertTrue("the later duplicate wins", responses.get(0).isCollection());
        assertEquals("/repo/dir/b", responses.get(1).getHref());
    }

    /**
     * RFC 4918 requires a status, but a server omitting one should still get its properties read.
     */
    @Test
    public void testPropstatWithoutStatusIsRead() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<D:multistatus xmlns:D=\"DAV:\">"
                + "  <D:response>"
                + "    <D:href>/repo/dir/</D:href>"
                + "    <D:propstat>"
                + "      <D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop>"
                + "    </D:propstat>"
                + "  </D:response>"
                + "</D:multistatus>");

        assertTrue(responses.get(0).isCollection());
    }

    /**
     * Some servers put the WebDAV elements in a default namespace rather than a prefixed one.
     */
    @Test
    public void testDefaultNamespaceIsAccepted() throws Exception {
        List<MultiStatus.Response> responses = parse("<?xml version=\"1.0\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "  <response>"
                + "    <href>/repo/dir/</href>"
                + "    <propstat>"
                + "      <prop><resourcetype><collection/></resourcetype></prop>"
                + "      <status>HTTP/1.1 200 OK</status>"
                + "    </propstat>"
                + "  </response>"
                + "</multistatus>");

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isCollection());
    }

    @Test
    public void testNonMultiStatusBodyIsRejected() {
        try {
            parse("<?xml version=\"1.0\"?><html><body>nope</body></html>");
            fail("expected an IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("multistatus"));
        }
    }

    @Test
    public void testMalformedBodyIsRejected() {
        try {
            parse("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\">");
            fail("expected an IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    /**
     * The body is remote input, so a DOCTYPE must not be honoured.
     */
    @Test
    public void testDoctypeIsRejected() {
        try {
            parse("<?xml version=\"1.0\"?>"
                    + "<!DOCTYPE multistatus [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                    + "<D:multistatus xmlns:D=\"DAV:\">"
                    + "  <D:response><D:href>&xxe;</D:href></D:response>"
                    + "</D:multistatus>");
            fail("expected an IOException");
        } catch (IOException expected) {
            // expected
        }
    }
}
