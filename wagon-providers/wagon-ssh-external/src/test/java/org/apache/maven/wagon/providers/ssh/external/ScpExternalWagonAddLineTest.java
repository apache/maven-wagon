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
package org.apache.maven.wagon.providers.ssh.external;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScpExternalWagonAddLineTest {

    private static java.util.List<String> split(String line) {
        ProcessBuilder pb = new ProcessBuilder("exe");
        ScpExternalWagon.addLine(pb, line);
        return pb.command().subList(1, pb.command().size());
    }

    @Test
    void splitsOnSpaces() {
        assertEquals(Arrays.asList("-o", "A=b", "-v"), split("-o  A=b -v"));
    }

    @Test
    void quotesGroupArguments() {
        assertEquals(Arrays.asList("-o", "A B", "C D"), split("-o \"A B\" 'C D'"));
    }

    @Test
    void backslashIsOrdinary() {
        assertEquals(Arrays.asList("C:\\dir\\key"), split("C:\\dir\\key"));
    }

    @Test
    void nullAndEmptyAddNothing() {
        assertTrue(split(null).isEmpty());
        assertTrue(split("").isEmpty());
    }
}
