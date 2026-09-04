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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the MKCOL status handling of {@link WebDavWagon#mkColPath} with scripted responses, which the
 * server-backed tests in {@link WebDavWagonTest} cannot do: a real server never answers 403 on demand, and never
 * loses a race that turns a forward MKCOL into a 405.
 */
class WebDavWagonMkColTest {

    private static final String BASE = "http://host";

    @Test
    void createsEveryMissingCollectionFromTheTopDown() throws IOException {
        // nothing exists, so the walk back reports a missing ancestor until the root, then creates on the way down
        ScriptedWagon wagon = new ScriptedWagon(409, 409, 409, 201, 201, 201, 201);

        wagon.mkColPath(BASE, "/a/b/c/");

        assertEquals(
                Arrays.asList(
                        BASE + "/a/b/c/",
                        BASE + "/a/b/",
                        BASE + "/a/",
                        BASE + "/",
                        BASE + "/a/",
                        BASE + "/a/b/",
                        BASE + "/a/b/c/"),
                wagon.requested);
    }

    @Test
    void stopsWalkingBackAtACollectionThatAlreadyExists() throws IOException {
        // 405 means "already a collection here", so the walk back stops and only the rest is created
        ScriptedWagon wagon = new ScriptedWagon(409, 409, 405, 201, 201);

        wagon.mkColPath(BASE, "/a/b/c/");

        assertEquals(5, wagon.requested.size());
        assertEquals(BASE + "/a/", wagon.requested.get(2));
    }

    @Test
    void toleratesACollectionAppearingUnderneathUsOnTheWayDown() throws IOException {
        // someone else created a/b between our walk back and our walk down; 405 there is not a failure
        ScriptedWagon wagon = new ScriptedWagon(409, 409, 405, 405, 201);

        wagon.mkColPath(BASE, "/a/b/c/");

        assertEquals(5, wagon.requested.size());
    }

    @Test
    void reportsARefusalWhereItHappensRatherThanWalkingPastIt() {
        // 403 is not "the parent is missing", so it must stop at once instead of walking to the root
        ScriptedWagon wagon = new ScriptedWagon(403);

        IOException e = assertThrows(IOException.class, () -> wagon.mkColPath(BASE, "/a/b/c/"));

        assertEquals(1, wagon.requested.size(), "walked past a refusal instead of reporting it");
        assertTrue(e.getMessage().contains(BASE + "/a/b/c/"), e.getMessage());
        assertTrue(e.getMessage().contains("403"), e.getMessage());
    }

    @Test
    void failsWhenTheWholePathIsExhaustedAndStillConflicting() {
        // the server claims a missing ancestor all the way to the root; there is nowhere left to go
        ScriptedWagon wagon = new ScriptedWagon(409, 409, 409, 409);

        IOException e = assertThrows(IOException.class, () -> wagon.mkColPath(BASE, "/a/b/c/"));

        assertEquals(4, wagon.requested.size());
        assertTrue(e.getMessage().contains("409"), e.getMessage());
    }

    private static final class ScriptedWagon extends WebDavWagon {

        private final Deque<Integer> statuses = new ArrayDeque<>();
        private final List<String> requested = new ArrayList<>();

        ScriptedWagon(int... statuses) {
            for (int status : statuses) {
                this.statuses.add(status);
            }
        }

        @Override
        int doMkCol(String url) {
            requested.add(url);
            if (statuses.isEmpty()) {
                throw new IllegalStateException("unexpected MKCOL of " + url + "; the script ran out");
            }
            return statuses.removeFirst();
        }
    }
}
