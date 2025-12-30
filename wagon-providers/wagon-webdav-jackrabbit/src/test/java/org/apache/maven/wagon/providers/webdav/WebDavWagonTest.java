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

import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Properties;

import it.could.webdav.DAVServlet;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamingWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.http.HttpWagonTestCase;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/*
 * WebDAV Wagon Test
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 */
public class WebDavWagonTest extends HttpWagonTestCase {

    protected String getTestRepositoryUrl() throws IOException {
        return getProtocol() + "://localhost:" + getTestRepositoryPort() + "/newfolder/folder2/";
    }

    protected String getProtocol() {
        return "dav";
    }

    protected ServletContextHandler createContext(Server server, File repositoryDirectory) throws IOException {
        ServletContextHandler dav = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ServletHolder davServletHolder = new ServletHolder(new DAVServlet());
        davServletHolder.setInitParameter("rootPath", repositoryDirectory.getAbsolutePath());
        davServletHolder.setInitParameter("xmlOnly", "false");
        dav.addServlet(davServletHolder, "/*");
        return dav;
    }

    protected long getExpectedLastModifiedOnGet(Repository repository, Resource resource) {
        File file = new File(getDavRepository(), resource.getName());
        return (file.lastModified() / 1000L) * 1000L;
    }

    private File getDavRepository() {
        return getTestFile("target/test-output/http-repository/newfolder/folder2");
    }

    private void assertURL(String userUrl, String expectedUrl) {
        Repository repo = new Repository("test-geturl", userUrl);
        String actualUrl = (new WebDavWagon()).getURL(repo);
        assertEquals("WebDavWagon.getURL(" + userUrl + ")", expectedUrl, actualUrl);
    }

    /**
     * Tests the maven 2.0.x way to define a webdav URL without SSL.
     */
    public void testGetURLDavHttp() {
        assertURL("dav:http://localhost:9080/dav/", "http://localhost:9080/dav/");
    }

    /**
     * Tests the maven 2.0.x way to define a webdav URL with SSL.
     */
    public void testGetURLDavHttps() {
        assertURL("dav:https://localhost:9443/dav/", "https://localhost:9443/dav/");
    }

    /**
     * Tests the URI spec way of defining a webdav URL without SSL.
     */
    public void testGetURLDavUri() {
        assertURL("dav://localhost:9080/dav/", "http://localhost:9080/dav/");
    }

    /**
     * Tests the URI spec way of defining a webdav URL with SSL.
     */
    public void testGetURLDavUriWithSsl() {
        assertURL("davs://localhost:9443/dav/", "https://localhost:9443/dav/");
    }

    /**
     * Tests the URI spec way of defining a webdav URL without SSL.
     */
    public void testGetURLDavPlusHttp() {
        assertURL(
                "dav+https://localhost:" + getTestRepositoryPort() + "/dav/",
                "https://localhost:" + getTestRepositoryPort() + "/dav/");
    }

    /**
     * Tests the URI spec way of defining a webdav URL with SSL.
     */
    public void testGetURLDavPlusHttps() {
        assertURL("dav+https://localhost:9443/dav/", "https://localhost:9443/dav/");
    }

    public void testMkdirs() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        WebDavWagon wagon = (WebDavWagon) getWagon();
        wagon.connect(testRepository, getAuthInfo());

        try {
            String repositoryUrl = testRepository.getUrl();

            // test leading /
            String fooUrl = repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/") + "foo";
            assertFalse("Collection should not exist before creation", wagon.collectionExists(fooUrl));
            wagon.mkdirs("/foo");
            assertTrue("Collection should exist after creation", wagon.collectionExists(fooUrl));

            // test trailing /
            String barUrl = repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/") + "bar";
            assertFalse("Collection should not exist before creation", wagon.collectionExists(barUrl));
            wagon.mkdirs("bar/");
            assertTrue("Collection should exist after creation", wagon.collectionExists(barUrl));

            // test when already exists (should not fail)
            wagon.mkdirs("bar");
            assertTrue("Collection should still exist", wagon.collectionExists(barUrl));

            // test several parts
            String deepUrl = repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/") + "1/2/3/4";
            assertFalse("Deep collection should not exist before creation", wagon.collectionExists(deepUrl));
            wagon.mkdirs("1/2/3/4");
            assertTrue("Deep collection should exist after creation", wagon.collectionExists(deepUrl));

            // test additional part and trailing /
            String deeperUrl = repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/") + "1/2/3/4/5";
            assertFalse("Deeper collection should not exist before creation", wagon.collectionExists(deeperUrl));
            wagon.mkdirs("1/2/3/4/5/");
            assertTrue("Deeper collection should exist after creation", wagon.collectionExists(deeperUrl));
        } finally {
            wagon.disconnect();

            tearDownWagonTestingFixtures();
        }
    }

    public void testMkdirsWithNoBasedir() throws Exception {
        // WAGON-244
        setupWagonTestingFixtures();

        setupRepositories();

        // reconstruct with no basedir
        testRepository.setUrl(
                testRepository.getProtocol() + "://" + testRepository.getHost() + ":" + testRepository.getPort());

        WebDavWagon wagon = (WebDavWagon) getWagon();
        wagon.connect(testRepository, getAuthInfo());

        try {
            String repositoryUrl = testRepository.getUrl();

            // test leading /
            String fooUrl = repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/") + "foo";
            assertFalse("Collection should not exist before creation", wagon.collectionExists(fooUrl));
            wagon.mkdirs("/foo");
            assertTrue("Collection should exist after creation", wagon.collectionExists(fooUrl));
        } finally {
            wagon.disconnect();

            tearDownWagonTestingFixtures();
        }
    }

    protected void setHttpConfiguration(StreamingWagon wagon, Properties headers, Properties params) {
        HttpConfiguration config = new HttpConfiguration();

        HttpMethodConfiguration methodConfiguration = new HttpMethodConfiguration();
        methodConfiguration.setHeaders(headers);
        methodConfiguration.setParams(params);
        config.setAll(methodConfiguration);
        ((WebDavWagon) wagon).setHttpConfiguration(config);
    }

    /**
     * Make sure Wagon WebDAV can detect remote directory
     *
     * @throws Exception
     */
    public void testWagonWebDavGetFileList() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        String dirName = "file-list";

        String filenames[] =
                new String[] {"test-resource.txt", "test-resource.pom", "test-resource b.txt", "more-resources.dat"};

        for (int i = 0; i < filenames.length; i++) {
            putFile(dirName + "/" + filenames[i], dirName + "/" + filenames[i], filenames[i] + "\n");
        }

        String dirnames[] = new String[] {"test-dir1", "test-dir2"};

        for (int i = 0; i < dirnames.length; i++) {
            new File(getDavRepository(), dirName + "/" + dirnames[i]).mkdirs();
        }

        Wagon wagon = getWagon();

        wagon.connect(testRepository, getAuthInfo());

        List<String> list = wagon.getFileList(dirName);

        assertNotNull("file list should not be null.", list);
        assertEquals("file list should contain 6 items", 6, list.size());

        for (int i = 0; i < filenames.length; i++) {
            assertTrue("Filename '" + filenames[i] + "' should be in list.", list.contains(filenames[i]));
        }

        for (int i = 0; i < dirnames.length; i++) {
            assertTrue("Directory '" + dirnames[i] + "' should be in list.", list.contains(dirnames[i] + "/"));
        }

        ///////////////////////////////////////////////////////////////////////////
        list = wagon.getFileList("");
        assertNotNull("file list should not be null.", list);
        assertEquals("file list should contain 1 items", 1, list.size());

        ///////////////////////////////////////////////////////////////////////////
        list = wagon.getFileList(dirName + "/test-dir1");
        assertNotNull("file list should not be null.", list);
        assertEquals("file list should contain 0 items", 0, list.size());

        /////////////////////////////////////////////////////////////////////////////
        try {
            list = wagon.getFileList(dirName + "/test-dir-bogus");
            fail("Exception expected");
        } catch (ResourceDoesNotExistException e) {

        }

        wagon.disconnect();

        tearDownWagonTestingFixtures();
    }

    public void testWagonFailsOnPutFailureByDefault() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        File testFile = getTempFile();

        System.clearProperty(WebDavWagon.CONTINUE_ON_FAILURE_PROPERTY);

        WebDavWagon wagon = new TimeoutSimulatingWagon();
        wagon.connect(testRepository, getAuthInfo());

        try {
            String filename = TimeoutSimulatingWagon.TIMEOUT_TRIGGER + ".txt";

            try {
                wagon.put(testFile, filename);
                fail("Exception expected");
            } catch (TransferFailedException e) {

            }
        } finally {
            wagon.disconnect();

            tearDownWagonTestingFixtures();
        }
    }

    private File getTempFile() throws IOException {
        File inputFile = File.createTempFile("test-resource", ".txt");
        inputFile.deleteOnExit();
        return inputFile;
    }

    private static class TimeoutSimulatingWagon extends WebDavWagon {
        private static final String TIMEOUT_TRIGGER = "timeout";

        protected CloseableHttpResponse execute(HttpUriRequest httpRequestBase) throws HttpException, IOException {
            if (httpRequestBase.getURI().getPath().contains(TIMEOUT_TRIGGER)) {
                throw new SocketTimeoutException("Timeout triggered by request for '"
                        + httpRequestBase.getURI().getPath() + "'");
            } else {
                return super.execute(httpRequestBase);
            }
        }
    }

    public void testWagonContinuesOnPutFailureIfPropertySet() throws Exception {
        setupWagonTestingFixtures();

        setupRepositories();

        File testFile = getTempFile();

        String continueOnFailureProperty = WebDavWagon.CONTINUE_ON_FAILURE_PROPERTY;
        System.setProperty(continueOnFailureProperty, "true");

        WebDavWagon wagon = new TimeoutSimulatingWagon();
        wagon.connect(testRepository, getAuthInfo());

        try {
            String filename = TimeoutSimulatingWagon.TIMEOUT_TRIGGER + ".txt";

            wagon.put(testFile, filename);
        } finally {
            wagon.disconnect();

            System.clearProperty(continueOnFailureProperty);

            tearDownWagonTestingFixtures();
        }
    }

    @Override
    protected boolean supportPreemptiveAuthenticationPut() {
        return false;
    }

    @Override
    protected boolean supportPreemptiveAuthenticationGet() {
        return false;
    }

    @Override
    protected boolean supportProxyPreemptiveAuthentication() {
        return true;
    }

    protected void testPreemptiveAuthenticationGet(TestSecurityHandler sh, boolean preemptive) {
        if (preemptive) {
            assertEquals(
                    "testPreemptiveAuthenticationGet preemptive=true: expected 1 request, got "
                            + sh.handlerRequestResponses,
                    1,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(0).responseCode);
        } else {
            assertEquals(
                    "testPreemptiveAuthenticationGet preemptive=false: expected 2 requests (401,200), got "
                            + sh.handlerRequestResponses,
                    2,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, sh.handlerRequestResponses.get(0).responseCode);
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(1).responseCode);
        }
    }

    protected void testPreemptiveAuthenticationPut(TestSecurityHandler sh, boolean preemptive) {
        if (preemptive) {
            assertEquals(
                    "testPreemptiveAuthenticationPut preemptive=true: expected 2 requests (200,201), got "
                            + sh.handlerRequestResponses,
                    2,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(0).responseCode);
            assertEquals(HttpServletResponse.SC_CREATED, sh.handlerRequestResponses.get(1).responseCode);
        } else {
            assertEquals(
                    "testPreemptiveAuthenticationPut preemptive=false: expected 3 requests (401,200,201), got "
                            + sh.handlerRequestResponses,
                    3,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, sh.handlerRequestResponses.get(0).responseCode);
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(1).responseCode);
            assertEquals(HttpServletResponse.SC_CREATED, sh.handlerRequestResponses.get(2).responseCode);
        }
    }

    /* This method cannot be reasonable used to represend GET and PUT for WebDAV, it would contain too much
     * duplicate code. Leave as-is, but don't use it.
     */
    protected void testPreemptiveAuthentication(TestSecurityHandler sh, boolean preemptive) {
        if (preemptive) {
            assertEquals(
                    "testPreemptiveAuthentication preemptive=false: expected 2 requests (200,.), got "
                            + sh.handlerRequestResponses,
                    2,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(0).responseCode);
        } else {
            assertEquals(
                    "testPreemptiveAuthentication preemptive=false: expected 3 requests (401,200,200), got "
                            + sh.handlerRequestResponses,
                    3,
                    sh.handlerRequestResponses.size());
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, sh.handlerRequestResponses.get(0).responseCode);
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(1).responseCode);
            assertEquals(HttpServletResponse.SC_OK, sh.handlerRequestResponses.get(2).responseCode);
        }
    }

    @Override
    protected void checkRequestResponseForRedirectPutWithFullUrl(
            RedirectHandler redirectHandler, PutHandler putHandler) {
        assertEquals("found:" + putHandler.handlerRequestResponses, 1, putHandler.handlerRequestResponses.size());
        assertEquals(
                "found:" + putHandler.handlerRequestResponses,
                HttpServletResponse.SC_CREATED,
                putHandler.handlerRequestResponses.get(0).responseCode);
        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses, 2, redirectHandler.handlerRequestResponses.size());
        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses,
                HttpServletResponse.SC_SEE_OTHER,
                redirectHandler.handlerRequestResponses.get(0).responseCode);
    }

    @Override
    protected void checkRequestResponseForRedirectPutWithRelativeUrl(
            RedirectHandler redirectHandler, PutHandler putHandler) {
        assertEquals("found:" + putHandler.handlerRequestResponses, 0, putHandler.handlerRequestResponses.size());

        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses, 4, redirectHandler.handlerRequestResponses.size());
        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses,
                HttpServletResponse.SC_SEE_OTHER,
                redirectHandler.handlerRequestResponses.get(0).responseCode);
        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses,
                HttpServletResponse.SC_OK,
                redirectHandler.handlerRequestResponses.get(1).responseCode);
        assertEquals(
                "found:" + redirectHandler.handlerRequestResponses,
                HttpServletResponse.SC_SEE_OTHER,
                redirectHandler.handlerRequestResponses.get(2).responseCode);
    }

    @Override
    protected void verifyWagonExceptionMessage(Exception e, int forStatusCode, String forUrl, String forReasonPhrase) {
        Repository repo = new Repository("test-geturl", forUrl);
        String expectedMessageUrl = (new WebDavWagon()).getURL(repo);
        super.verifyWagonExceptionMessage(e, forStatusCode, expectedMessageUrl, forReasonPhrase);
    }
}
