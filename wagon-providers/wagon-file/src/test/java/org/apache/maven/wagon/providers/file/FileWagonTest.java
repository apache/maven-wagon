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
package org.apache.maven.wagon.providers.file;

import java.io.File;
import java.io.IOException;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.getName;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class FileWagonTest extends StreamingWagonTestCase {
    protected String getProtocol() {
        return "file";
    }

    protected String getTestRepositoryUrl() throws IOException {
        File file = FileTestUtils.createUniqueDir(getName() + ".file-repository.");

        return file.toPath().toUri().toASCIIString();
    }

    /**
     * This test is introduced to allow for null file wagons
     * which are used heavily in the maven component ITs.
     *
     * @throws ConnectionException
     * @throws AuthenticationException
     */
    @Test
    public void testNullFileWagon() throws ConnectionException, AuthenticationException {
        Wagon wagon = new FileWagon();
        Repository repository = new Repository();
        wagon.connect(repository);
        wagon.disconnect();
    }

    protected long getExpectedLastModifiedOnGet(Repository repository, Resource resource) {
        return new File(repository.getBasedir(), resource.getName()).lastModified();
    }

    @Test
    public void testResourceExists() throws Exception {
        String url = new File(getBasedir()).toPath().toUri().toASCIIString();

        Wagon wagon = new FileWagon();
        Repository repository = new Repository("someID", url);
        wagon.connect(repository);

        assertTrue(wagon.resourceExists("target"));
        assertTrue(wagon.resourceExists("target/"));
        assertTrue(wagon.resourceExists("pom.xml"));

        assertFalse(wagon.resourceExists("pom.xml/"));

        wagon.disconnect();
    }
}
