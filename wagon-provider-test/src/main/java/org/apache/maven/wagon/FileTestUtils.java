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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author <a href="michal@apache.org>Michal Maczka</a>
 *
 */
public class FileTestUtils {

    public static File createUniqueFile(final String dirname, final String name) throws IOException {

        final File dir = createDir(dirname);

        final File retValue = new File(dir, name);

        return retValue;
    }

    public static File createUniqueDir(final String name) throws IOException {

        String filename = name + System.currentTimeMillis();

        return createDir(filename);
    }

    public static File createDir(final String name) throws IOException {

        final File baseDirectory = getTestOutputDir();

        final File retValue = new File(baseDirectory, name);

        if (retValue.exists()) {
            cleanDirectory(retValue);
            return retValue;
        }

        retValue.mkdirs();

        if (!retValue.exists()) {
            throw new IOException("Unable to create the directory for testdata " + retValue.getPath());
        }

        return retValue;
    }

    public static File getTestOutputDir() {
        final String tempDir = System.getProperty("java.io.tmpdir");

        final String baseDir = System.getProperty("basedir", tempDir);

        final File base = new File(baseDir).getAbsoluteFile();

        final String pathname = base + File.separator + "target" + File.separator + "test-output";

        final File retValue = new File(pathname);

        retValue.mkdirs();

        return retValue;
    }

    public static File generateFile(String file, String content) throws IOException {
        File f = new File(file);

        f.getParentFile().mkdirs();

        Writer writer = new FileWriter(f);

        try {
            writer.write(content);
        } finally {
            writer.close();
        }

        return f;
    }

    /**
     * Removes all files and sub-directories inside {@code directory}, leaving the directory itself in place.
     */
    private static void cleanDirectory(File directory) throws IOException {
        File[] contents = directory.listFiles();
        if (contents == null) {
            throw new IOException("Unable to list directory: " + directory);
        }
        for (File file : contents) {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
            if (!file.delete() && file.exists()) {
                throw new IOException("Unable to delete: " + file);
            }
        }
    }

    /**
     * Recursively deletes {@code directory}, including the directory itself. Does nothing if it does not exist.
     *
     * @param directory the directory to delete
     * @throws IOException if the directory cannot be listed or a file cannot be deleted
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void delete(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (java.nio.file.AccessDeniedException e) {
            // Git stores object files read-only on Windows; clear the flag and retry once.
            path.toFile().setWritable(true);
            Files.delete(path);
        }
    }

    /**
     * Copies the contents of {@code source} into {@code target}, recreating its directory structure and preserving
     * file attributes. Existing files are overwritten.
     *
     * @param source the directory to copy
     * @param target the destination directory
     * @throws IOException if the source cannot be read or the target cannot be written
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(
                        file,
                        target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates {@code targetDirectory} if necessary and copies {@code source} into it, preserving file attributes.
     * An existing file with the same name is overwritten.
     *
     * @param source the file to copy
     * @param targetDirectory the directory to copy the file into
     * @throws IOException if the file cannot be copied
     */
    public static void copyFileToDirectory(Path source, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        Files.copy(
                source,
                targetDirectory.resolve(source.getFileName()),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
    }
}
