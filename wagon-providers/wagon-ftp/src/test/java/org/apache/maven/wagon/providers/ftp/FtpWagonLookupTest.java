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
package org.apache.maven.wagon.providers.ftp;

import javax.inject.Inject;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * The protocols that no {@code WagonTestCase} covers: each hint finds its wagon, and a new one on every lookup.
 */
@PlexusTest
class FtpWagonLookupTest {

    @Inject
    private PlexusContainer container;

    @Test
    void lookupFtpsReturnsANewFtpsWagon() throws Exception {
        assertNewInstanceOnEachLookup("ftps", FtpsWagon.class);
    }

    @Test
    void lookupFtphReturnsANewFtpHttpWagon() throws Exception {
        assertNewInstanceOnEachLookup("ftph", FtpHttpWagon.class);
    }

    private void assertNewInstanceOnEachLookup(String hint, Class<? extends Wagon> type) throws Exception {
        Wagon wagon = container.lookup(Wagon.class, hint);

        assertEquals(type, wagon.getClass());
        assertNotSame(wagon, container.lookup(Wagon.class, hint));
    }
}
