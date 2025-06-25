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
import java.nio.file.Files;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

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

    public void start(Environment env) throws IOException {
        File tmpFile = File.createTempFile("wagon", "test-sh");
        tmpFile.deleteOnExit();
        int exitValue = 0;
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();
        try {

            // hackish default commandline tools not support ; or && so write a file with the script
            // and "/bin/sh -e " + tmpFile.getPath();
            Files.write(tmpFile.toPath(), commandLine.getBytes());

            Commandline cl = new Commandline();
            cl.setExecutable("/bin/sh");
            // cl.createArg().setValue( "-e" );
            // cl.createArg().setValue( tmpFile.getPath() );
            cl.createArg().setFile(tmpFile);

            exitValue = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            System.out.println("exit value " + exitValue);
            /*
            if ( exitValue == 0 )
            {
                out.write( stdout.getOutput().getBytes() );
                out.write( '\n' );
                out.flush();

            }
            else
            {
                out.write( stderr.getOutput().getBytes() );
                out.write( '\n' );
                out.flush();

            }*/

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
        /*
        out.write( exitValue );
        out.write( '\n' );

        */
        out.flush();
    }

    public void destroy() {}

    private void deleteQuietly(File f) {

        try {
            f.delete();
        } catch (Exception e) {
            // ignore
        }
    }
}
