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

import javax.inject.Named;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.eclipse.sisu.Typed;

/**
 * SCP deployer using "external" scp program.  To allow for
 * ssh-agent type behavior, until we can construct a Java SSH Agent and interface for JSch.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo [BP] add compression flag
 */
@Named("scpexe")
@Typed(Wagon.class)
public class ScpExternalWagon extends AbstractWagon implements CommandExecutor {
    /**
     * The external SCP command to use - default is <code>scp</code>.
     *
     * @component.configuration default="scp"
     */
    private String scpExecutable = "scp";

    /**
     * The external SSH command to use - default is <code>ssh</code>.
     *
     * @component.configuration default="ssh"
     */
    private String sshExecutable = "ssh";

    /**
     * Arguments to pass to the SCP command.
     *
     * @component.configuration
     */
    private String scpArgs;

    /**
     * Arguments to pass to the SSH command.
     *
     * @component.configuration
     */
    private String sshArgs;

    private ScpHelper sshTool = new ScpHelper(this);

    private static final int SSH_FATAL_EXIT_CODE = 255;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void openConnectionInternal() throws AuthenticationException {
        if (authenticationInfo == null) {
            authenticationInfo = new AuthenticationInfo();
        }
    }

    public void closeConnection() {
        // nothing to disconnect
    }

    public boolean getIfNewer(String resourceName, File destination, long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        fireSessionDebug("getIfNewer in SCP wagon is not supported - performing an unconditional get");
        get(resourceName, destination);
        return true;
    }

    /**
     * @return The hostname of the remote server prefixed with the username, which comes either from the repository URL
     *         or from the authenticationInfo.
     */
    private String buildRemoteHost() {
        String username = this.getRepository().getUsername();
        if (username == null) {
            username = authenticationInfo.getUserName();
        }

        if (username == null) {
            return getRepository().getHost();
        } else {
            return username + "@" + getRepository().getHost();
        }
    }

    public void executeCommand(String command) throws CommandExecutionException {
        fireTransferDebug("Executing command: " + command);

        executeCommand(command, false);
    }

    public Streams executeCommand(String command, boolean ignoreFailures) throws CommandExecutionException {
        boolean putty = isPuTTY();

        File privateKey;
        try {
            privateKey = ScpHelper.getPrivateKey(authenticationInfo);
        } catch (FileNotFoundException e) {
            throw new CommandExecutionException(e.getMessage(), e);
        }
        ProcessBuilder cl = createBaseCommandLine(putty, sshExecutable, privateKey);

        int port =
                repository.getPort() == WagonConstants.UNKNOWN_PORT ? ScpHelper.DEFAULT_SSH_PORT : repository.getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            if (putty) {
                addLine(cl, "-P " + port);
            } else {
                addLine(cl, "-p " + port);
            }
        }

        if (sshArgs != null) {
            addLine(cl, sshArgs);
        }

        String remoteHost = this.buildRemoteHost();

        cl.command().add(remoteHost);

        cl.command().add(command);

        fireSessionDebug("Executing command: " + describe(cl));

        try {
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            int exitCode = execute(cl, out, err);
            Streams streams = new Streams();
            streams.setOut(out.toString());
            streams.setErr(err.toString());
            fireSessionDebug(streams.getOut());
            fireSessionDebug(streams.getErr());
            if (exitCode != 0) {
                if (!ignoreFailures || exitCode == SSH_FATAL_EXIT_CODE) {
                    throw new CommandExecutionException("Exit code " + exitCode + " - " + err);
                }
            }
            return streams;
        } catch (IOException e) {
            throw new CommandExecutionException("Error executing command line", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandExecutionException("Error executing command line", e);
        }
    }

    protected boolean isPuTTY() {
        String exe = sshExecutable.toLowerCase(Locale.ENGLISH);
        return exe.contains("plink") || exe.contains("klink");
    }

    private ProcessBuilder createBaseCommandLine(boolean putty, String executable, File privateKey) {
        ProcessBuilder cl = new ProcessBuilder(executable);

        if (privateKey != null) {
            cl.command().add("-i");
            cl.command().add(privateKey.getPath());
        }

        String password = authenticationInfo.getPassword();
        if (putty && password != null) {
            cl.command().add("-pw");
            cl.command().add(password);
        }

        // should check interactive flag, but scpexe never works in interactive mode right now due to i/o streams
        if (putty) {
            cl.command().add("-batch");
        } else {
            cl.command().add("-o");
            cl.command().add("BatchMode yes");
        }
        return cl;
    }

    ProcessBuilder buildCommandLine(Resource resource, File localFile, boolean put)
            throws TransferFailedException, AuthorizationException {
        boolean putty = isPuTTYSCP();

        File privateKey;
        try {
            privateKey = ScpHelper.getPrivateKey(authenticationInfo);
        } catch (FileNotFoundException e) {
            fireSessionConnectionRefused();

            throw new AuthorizationException(e.getMessage());
        }
        ProcessBuilder cl = createBaseCommandLine(putty, scpExecutable, privateKey);

        File parentFile = localFile.getParentFile();
        if (parentFile == null) {
            try {
                File abs = localFile.getAbsoluteFile();
                parentFile = abs.getParentFile();
            } catch (SecurityException e) {
                fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

                throw new TransferFailedException("Error accessing absolute path of " + localFile, e);
            }
        }
        if (parentFile != null) {
            cl.directory(parentFile);
        }

        int port =
                repository.getPort() == WagonConstants.UNKNOWN_PORT ? ScpHelper.DEFAULT_SSH_PORT : repository.getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            addLine(cl, "-P " + port);
        }

        if (scpArgs != null) {
            addLine(cl, scpArgs);
        }

        String resourceName = normalizeResource(resource);
        String remoteFile = getRepository().getBasedir() + "/" + resourceName;

        remoteFile = remoteFile.replace(" ", "\\ ");

        String qualifiedRemoteFile = this.buildRemoteHost() + ":" + remoteFile;
        if (put) {
            cl.command().add(localFile.getName());
            cl.command().add(qualifiedRemoteFile);
        } else {
            cl.command().add(qualifiedRemoteFile);
            cl.command().add(localFile.getName());
        }

        return cl;
    }

    private void executeScpCommand(Resource resource, File localFile, boolean put)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        ProcessBuilder cl = buildCommandLine(resource, localFile, put);

        fireSessionDebug("Executing command: " + describe(cl));

        try {
            StringBuilder err = new StringBuilder();
            int exitCode = execute(cl, null, err);
            if (exitCode != 0) {
                String errOutput = err.toString();
                if (!put && errOutput.trim().toLowerCase(Locale.ENGLISH).contains("no such file or directory")) {
                    throw new ResourceDoesNotExistException(errOutput);
                } else {
                    TransferFailedException e =
                            new TransferFailedException("Exit code: " + exitCode + " - " + errOutput);

                    fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

                    throw e;
                }
            }
        } catch (IOException e) {
            fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

            throw new TransferFailedException("Error executing command line", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

            throw new TransferFailedException("Error executing command line", e);
        }
    }

    /** Renders the command line for logging, masking the argument that follows {@code -pw}. */
    private static String describe(ProcessBuilder commandLine) {
        List<String> command = commandLine.command();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(i > 0 && "-pw".equals(command.get(i - 1)) ? "*****" : command.get(i));
        }
        return sb.toString();
    }

    /**
     * Splits a config string exactly as {@code CommandLineUtils.translateCommandline} did for
     * {@code Commandline.Argument.setLine}: spaces separate arguments, quotes group them, and backslashes
     * are ordinary characters. The resulting arguments are appended to the command line.
     */
    static void addLine(ProcessBuilder commandLine, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int state = 0; // 0 = normal, 1 = inside single quotes, 2 = inside double quotes
        StringTokenizer tokenizer = new StringTokenizer(line, "'\" ", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            switch (state) {
                case 1:
                    if ("'".equals(token)) {
                        state = 0;
                    } else {
                        current.append(token);
                    }
                    break;
                case 2:
                    if ("\"".equals(token)) {
                        state = 0;
                    } else {
                        current.append(token);
                    }
                    break;
                default:
                    if ("'".equals(token)) {
                        state = 1;
                    } else if ("\"".equals(token)) {
                        state = 2;
                    } else if (" ".equals(token)) {
                        if (current.length() > 0) {
                            tokens.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(token);
                    }
                    break;
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        commandLine.command().addAll(tokens);
    }

    /**
     * Replaces {@code CommandLineUtils.executeCommandLine}. Both streams are always drained on separate
     * threads so that a chatty child process cannot block on a full pipe; a {@code null} target discards it.
     */
    private static int execute(ProcessBuilder commandLine, StringBuilder out, StringBuilder err)
            throws IOException, InterruptedException {
        Process process = commandLine.start();
        Thread outThread = new StreamConsumer(process.getInputStream(), out).startPump();
        Thread errThread = new StreamConsumer(process.getErrorStream(), err).startPump();
        int exitCode = process.waitFor();
        outThread.join();
        errThread.join();
        return exitCode;
    }

    private static final class StreamConsumer implements Runnable {
        private final InputStream stream;

        private final StringBuilder target;

        StreamConsumer(InputStream stream, StringBuilder target) {
            this.stream = stream;
            this.target = target;
        }

        Thread startPump() {
            Thread thread = new Thread(this);
            thread.start();
            return thread;
        }

        @Override
        public void run() {
            try (InputStream in = stream) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (target != null) {
                        target.append(new String(buffer, 0, read, Charset.defaultCharset()));
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    boolean isPuTTYSCP() {
        String exe = scpExecutable.toLowerCase(Locale.ENGLISH);
        return exe.contains("pscp") || exe.contains("kscp");
    }

    private String normalizeResource(Resource resource) {
        return resource.getName().replace('\\', '/');
    }

    public void put(File source, String destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(destination);

        firePutInitiated(resource, source);

        if (!source.exists()) {
            throw new ResourceDoesNotExistException("Specified source file does not exist: " + source);
        }

        String basedir = getRepository().getBasedir();

        String resourceName = destination.replace('\\', '/');

        String dir = PathUtils.dirname(resourceName);

        dir = dir.replace('\\', '/');

        String umaskCmd = null;
        if (getRepository().getPermissions() != null) {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if (dirPerms != null) {
                umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor(dirPerms);
            }
        }

        String mkdirCmd = "mkdir -p " + basedir + "/" + dir + "\n";

        if (umaskCmd != null) {
            mkdirCmd = umaskCmd + "; " + mkdirCmd;
        }

        try {
            executeCommand(mkdirCmd);
        } catch (CommandExecutionException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

            throw new TransferFailedException("Error executing command for transfer", e);
        }

        resource.setContentLength(source.length());

        resource.setLastModified(source.lastModified());

        firePutStarted(resource, source);

        executeScpCommand(resource, source, true);

        postProcessListeners(resource, source, TransferEvent.REQUEST_PUT);

        try {
            RepositoryPermissions permissions = getRepository().getPermissions();

            if (permissions != null && permissions.getGroup() != null) {
                executeCommand("chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n", true);
            }

            if (permissions != null && permissions.getFileMode() != null) {
                executeCommand(
                        "chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n", true);
            }
        } catch (CommandExecutionException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

            throw new TransferFailedException("Error executing command for transfer", e);
        }
        firePutCompleted(resource, source);
    }

    public void get(String resourceName, File destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        String path = resourceName.replace('\\', '/');

        Resource resource = new Resource(path);

        fireGetInitiated(resource, destination);

        createParentDirectories(destination);

        fireGetStarted(resource, destination);

        executeScpCommand(resource, destination, false);

        postProcessListeners(resource, destination, TransferEvent.REQUEST_GET);

        fireGetCompleted(resource, destination);
    }

    //
    // these parameters are user specific, so should not be read from the repository itself.
    // They can be configured by plexus, or directly on the instantiated object.
    // Alternatively, we may later accept a generic parameters argument to connect, or some other configure(Properties)
    // method on a Wagon.
    //

    public List<String> getFileList(String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return sshTool.getFileList(destinationDirectory, repository);
    }

    public void putDirectory(File sourceDirectory, String destinationDirectory)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        sshTool.putDirectory(this, sourceDirectory, destinationDirectory);
    }

    public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        return sshTool.resourceExists(resourceName, repository);
    }

    public boolean supportsDirectoryCopy() {
        return true;
    }

    public String getScpExecutable() {
        return scpExecutable;
    }

    public void setScpExecutable(String scpExecutable) {
        this.scpExecutable = scpExecutable;
    }

    public String getSshExecutable() {
        return sshExecutable;
    }

    public void setSshExecutable(String sshExecutable) {
        this.sshExecutable = sshExecutable;
    }

    public String getScpArgs() {
        return scpArgs;
    }

    public void setScpArgs(String scpArgs) {
        this.scpArgs = scpArgs;
    }

    public String getSshArgs() {
        return sshArgs;
    }

    public void setSshArgs(String sshArgs) {
        this.sshArgs = sshArgs;
    }
}
