package org.apache.maven.wagon.providers.ssh;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.Streams;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * CommandExecutorStreamProcessor 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CommandExecutorStreamProcessor
{
    private CommandExecutorStreamProcessor()
    {
        // shoo!
    }

    public static Streams processStreams( BufferedReader stderrReader, BufferedReader stdoutReader )
        throws IOException, CommandExecutionException
    {
        Streams streams = new Streams();

        while ( true )
        {
            String line = stderrReader.readLine();

            System.out.println( "std err line = " + line );

            if ( line == null )
            {
                break;
            }

            // TODO: I think we need to deal with exit codes instead, but IIRC there are some cases of errors that don't have exit codes
            // ignore this error. TODO: output a warning
            if ( !line.startsWith( "Could not chdir to home directory" ) &&
                !line.endsWith( "ttyname: Operation not supported" ) )
            {
                streams.setErr( streams.getErr() + line + "\n" );
            }
        }

        while ( true )
        {
            String line = stdoutReader.readLine();

            System.out.println( "std out line = " + line );

            if ( line == null )
            {
                break;
            }

            streams.setOut( streams.getOut() + line + "\n" );
        }

        // drain the output stream.
        // TODO: we'll save this for the 1.0-alpha-8 line, so we can test it more. the -q arg in the
        // unzip command should keep us until then...
//            int avail = in.available();
//            byte[] trashcan = new byte[1024];
//
//            while( ( avail = in.available() ) > 0 )
//            {
//                in.read( trashcan, 0, avail );
//            }

        return streams;
    }
}
