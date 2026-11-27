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
package org.apache.maven.wagon.providers.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

/**
 * @author Olivier Lamy
 */
public class ShellCommand implements Command {
    protected static final int OK = 0;

    protected static final int WARNING = 1;

    protected static final int ERROR = 2;

    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    private ExitCallback callback;

    private Thread thread;

    private String commandLine;

    public ShellCommand(String commandLine) {
        this.commandLine = commandLine;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void start(ChannelSession channel, Environment env) throws IOException {
        File tmpFile = File.createTempFile("wagon", "test-sh");
        tmpFile.deleteOnExit();
        int exitValue = 0;
        StringConsumer stderr = new StringConsumer();
        StringConsumer stdout = new StringConsumer();
        try {
            // hackish default commandline tools do not support ; or && so write a file with the script
            // and "/bin/sh -e " + tmpFile.getPath();
            Files.write(tmpFile.toPath(), commandLine.getBytes());

            Process process = new ProcessBuilder("/bin/sh", tmpFile.getPath()).start();
            Thread outPump = stdout.pump(process.getInputStream());
            Thread errPump = stderr.pump(process.getErrorStream());
            exitValue = process.waitFor();
            outPump.join();
            errPump.join();
            System.out.println("exit value " + exitValue);
        } catch (Exception e) {
            exitValue = ERROR;
            e.printStackTrace();
        } finally {
            deleteQuietly(tmpFile);
            if (exitValue != 0) {
                err.write(stderr.getOutput().getBytes());
                err.write('\n');
                err.flush();
                callback.onExit(exitValue, stderr.getOutput());
            } else {
                out.write(stdout.getOutput().getBytes());
                out.write('\n');
                out.flush();
                callback.onExit(exitValue, stdout.getOutput());
            }
        }
    }

    public void destroy(ChannelSession channel) throws Exception {}

    private void deleteQuietly(File f) {

        try {
            f.delete();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Drains a process stream into a string, the way {@code CommandLineUtils.StringStreamConsumer} did.
     * A separate thread per stream avoids filling the pipe buffer and deadlocking the child process.
     */
    private static final class StringConsumer {
        private final StringBuilder output = new StringBuilder();

        Thread pump(final InputStream stream) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (InputStream in = stream) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            output.append(new String(buffer, 0, read, Charset.defaultCharset()));
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
            thread.start();
            return thread;
        }

        String getOutput() {
            return output.toString();
        }
    }
}
