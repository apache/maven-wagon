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
package org.apache.maven.wagon.providers.scm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the directory copy that {@link ScmWagon#putDirectory} uses to stage files into the checkout.
 */
class ScmWagonCopyDirectoryStructureTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesFilesBehindSymlinkedDirectory() throws IOException {
        Path source = Files.createDirectories(tempDir.resolve("source"));
        Path real = Files.createDirectories(tempDir.resolve("real"));
        write(real.resolve("nested/data.txt"), "linked content");
        write(source.resolve("plain.txt"), "plain content");

        try {
            Files.createSymbolicLink(source.resolve("link"), real);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            Assumptions.abort("Symbolic links are not supported here: " + e);
        }

        Path target = tempDir.resolve("target");
        ScmWagon.copyDirectoryStructure(source.toFile(), target.toFile());

        assertEquals("plain content", read(target.resolve("plain.txt")));
        assertEquals("linked content", read(target.resolve("link/nested/data.txt")));
        assertFalse(Files.isSymbolicLink(target.resolve("link")));
    }

    @Test
    @Timeout(30)
    void doesNotCopyDestinationInsideSourceIntoItself() throws IOException {
        Path source = Files.createDirectories(tempDir.resolve("source"));
        write(source.resolve("a.txt"), "a");
        write(source.resolve("sub/b.txt"), "b");
        Path target = source.resolve("checkout");

        ScmWagon.copyDirectoryStructure(source.toFile(), target.toFile());

        assertEquals("a", read(target.resolve("a.txt")));
        assertEquals("b", read(target.resolve("sub/b.txt")));
        assertFalse(Files.exists(target.resolve("checkout")), "destination was copied into itself");
    }

    @Test
    void rejectsSameSourceAndDestination() throws IOException {
        File source = Files.createDirectories(tempDir.resolve("source")).toFile();

        IOException e = assertThrows(IOException.class, () -> ScmWagon.copyDirectoryStructure(source, source));
        assertTrue(e.getMessage().contains("same directory"), e.getMessage());
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
