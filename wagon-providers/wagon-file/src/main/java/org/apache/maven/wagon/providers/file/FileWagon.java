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

import javax.inject.Named;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;

/**
 * Wagon Provider for Local File System
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
@Named("file")
public class FileWagon extends StreamWagon {
    public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException {
        if (getRepository().getBasedir() == null) {
            throw new TransferFailedException("Unable to operate with a null basedir.");
        }

        Resource resource = inputData.getResource();

        File file = new File(getRepository().getBasedir(), resource.getName());

        if (!file.exists()) {
            throw new ResourceDoesNotExistException("File: " + file + " does not exist");
        }

        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file));

            inputData.setInputStream(in);

            resource.setContentLength(file.length());

            resource.setLastModified(file.lastModified());
        } catch (FileNotFoundException e) {
            throw new TransferFailedException("Could not read from file: " + file.getAbsolutePath(), e);
        }
    }

    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        if (getRepository().getBasedir() == null) {
            throw new TransferFailedException("Unable to operate with a null basedir.");
        }

        Resource resource = outputData.getResource();

        File file = new File(getRepository().getBasedir(), resource.getName());

        createParentDirectories(file);

        OutputStream outputStream = new BufferedOutputStream(new LazyFileOutputStream(file));

        outputData.setOutputStream(outputStream);
    }

    protected void openConnectionInternal() throws ConnectionException {
        if (getRepository() == null) {
            throw new ConnectionException("Unable to operate with a null repository.");
        }

        if (getRepository().getBasedir() == null) {
            // This condition is possible when using wagon-file under integration testing conditions.
            fireSessionDebug("Using a null basedir.");
            return;
        }

        // Check the File repository exists
        File basedir = new File(getRepository().getBasedir());
        if (!basedir.exists()) {
            if (!basedir.mkdirs()) {
                throw new ConnectionException(
                        "Repository path " + basedir + " does not exist," + " and cannot be created.");
            }
        }

        if (!basedir.canRead()) {
            throw new ConnectionException("Repository path " + basedir + " cannot be read");
        }
    }

    public void closeConnection() {}

    public boolean supportsDirectoryCopy() {
        // TODO: should we test for null basedir here?
        return true;
    }

    public void putDirectory(File sourceDirectory, String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (getRepository().getBasedir() == null) {
            throw new TransferFailedException("Unable to putDirectory() with a null basedir.");
        }

        File path = resolveDestinationPath(destinationDirectory);

        try {
            /*
             * Done to address issue found in HP-UX with regards to "." directory references. Details found in ..
             * WAGON-30 - wagon-file failed when used by maven-site-plugin WAGON-33 - FileWagon#putDirectory() fails in
             * HP-UX if destinationDirectory is "."
             * http://www.nabble.com/With-maven-2.0.2-site%3Adeploy-doesn%27t-work-t934716.html for details. Using
             * path.getCanonicalFile() ensures that the path is fully resolved before an attempt to create it. TODO:
             * consider moving this to a reusable directory-creation helper.
             */
            File realFile = path.getCanonicalFile();
            realFile.mkdirs();
        } catch (IOException e) {
            // Fall back to standard way if getCanonicalFile() fails.
            path.mkdirs();
        }

        if (!path.exists() || !path.isDirectory()) {
            String emsg = "Could not make directory '" + path.getAbsolutePath() + "'.";

            // Add assistive message in case of failure.
            File basedir = new File(getRepository().getBasedir());
            if (!basedir.canWrite()) {
                emsg += "  The base directory " + basedir + " is read-only.";
            }

            throw new TransferFailedException(emsg);
        }

        try {
            copyDirectoryStructure(sourceDirectory, path, path);
        } catch (IOException e) {
            throw new TransferFailedException("Error copying directory structure", e);
        }
    }

    /**
     * Recursively copies the contents of {@code sourceDirectory} into {@code destinationDirectory}. Empty directories
     * are included, existing files are overwritten and the destination directory itself is never copied into. This
     * is a JDK-only implementation that replaces the former dependency-based copy.
     *
     * @param sourceDirectory the directory to copy from
     * @param destinationDirectory the directory to copy into
     * @param rootDestinationDirectory the root destination, skipped if it appears below the source
     * @throws IOException if the source cannot be read or the destination cannot be written
     */
    private static void copyDirectoryStructure(
            File sourceDirectory, File destinationDirectory, File rootDestinationDirectory) throws IOException {
        if (sourceDirectory == null) {
            throw new IOException("source directory can't be null.");
        }
        if (destinationDirectory == null) {
            throw new IOException("destination directory can't be null.");
        }
        if (sourceDirectory.equals(destinationDirectory)) {
            throw new IOException("source and destination are the same directory.");
        }
        if (!sourceDirectory.exists()) {
            throw new IOException("Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ").");
        }

        File[] files = sourceDirectory.listFiles();
        if (files == null) {
            throw new IOException("Could not list directory " + sourceDirectory.getAbsolutePath());
        }

        String sourcePath = sourceDirectory.getAbsolutePath();

        for (File file : files) {
            if (file.equals(rootDestinationDirectory)) {
                // We don't copy the destination directory into itself
                continue;
            }

            String dest = file.getAbsolutePath().substring(sourcePath.length() + 1);
            File destination = new File(destinationDirectory, dest);

            if (file.isFile()) {
                if (file.getCanonicalFile().equals(destination.getCanonicalFile())) {
                    continue;
                }
                Files.createDirectories(destination.getParentFile().toPath());
                Files.copy(
                        file.toPath(),
                        destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            } else if (file.isDirectory()) {
                if (!destination.exists() && !destination.mkdirs()) {
                    throw new IOException(
                            "Could not create destination directory '" + destination.getAbsolutePath() + "'.");
                }
                copyDirectoryStructure(file, destination, rootDestinationDirectory);
            } else {
                throw new IOException("Unknown file type: " + file.getAbsolutePath());
            }
        }
    }

    private File resolveDestinationPath(String destinationPath) {
        String basedir = getRepository().getBasedir();

        destinationPath = destinationPath.replace("\\", "/");

        File path;

        if (destinationPath.equals(".")) {
            path = new File(basedir);
        } else {
            path = new File(basedir, destinationPath);
        }

        return path;
    }

    public List<String> getFileList(String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (getRepository().getBasedir() == null) {
            throw new TransferFailedException("Unable to getFileList() with a null basedir.");
        }

        File path = resolveDestinationPath(destinationDirectory);

        if (!path.exists()) {
            throw new ResourceDoesNotExistException("Directory does not exist: " + destinationDirectory);
        }

        if (!path.isDirectory()) {
            throw new ResourceDoesNotExistException("Path is not a directory: " + destinationDirectory);
        }

        File[] files = path.listFiles();

        List<String> list = new ArrayList<>(files.length);
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory() && !name.endsWith("/")) {
                name += "/";
            }
            list.add(name);
        }
        return list;
    }

    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        if (getRepository().getBasedir() == null) {
            throw new TransferFailedException("Unable to getFileList() with a null basedir.");
        }

        File file = resolveDestinationPath(resourceName);

        if (resourceName.endsWith("/")) {
            return file.isDirectory();
        }

        return file.exists();
    }
}
