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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.wagon.Streams;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * CommandExecutorStreamProcessor
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class CommandExecutorStreamProcessor
{
    private CommandExecutorStreamProcessor()
    {
        // shoo!
    }

    public static Streams processStreams( BufferedReader stderrReader, BufferedReader stdoutReader )
        throws IOException
    {
        Streams streams = new Streams();
        while ( true )
        {
            String line = stdoutReader.readLine();

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

        // drain stderr next, if stream size is more than the allowed buffer size
        // ( ie jsch has a hardcoded 32K size), the remote shell may be blocked. See WAGON-431
        while ( true )
        {
            String line = stderrReader.readLine();

            if ( line == null )
            {
                break;
            }

            // TODO: I think we need to deal with exit codes instead, but IIRC there are some cases of errors that
            // don't have exit codes ignore this error. TODO: output a warning
            if ( !line.startsWith( "Could not chdir to home directory" )
                 && !line.endsWith( "ttyname: Operation not supported" ) )
            {
                streams.setErr( streams.getErr() + line + "\n" );
            }
        }

        return streams;
    }

    public static void processStreams( final BufferedReader stderrReader, final BufferedReader stdoutReader,
                                       final OutputStream errStream, final OutputStream outStream )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while ( true )
                    {
                        String line = stdoutReader.readLine();
                        if ( line == null )
                        {
                            break;
                        }
                        if ( outStream == null )
                        {
                            continue;
                        }
                        outStream.write( ( line + "\n" ).getBytes() );
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
                finally
                {
                    IOUtil.close( stdoutReader );
                    IOUtil.close( outStream );
                }
            }
        } ).start();

        new Thread ( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while ( true )
                    {
                        String line = stderrReader.readLine();
                        if ( line == null )
                        {
                            break;
                        }
                        // TODO: I think we need to deal with exit codes instead, but IIRC there are some cases of errors that
                        // don't have exit codes ignore this error. TODO: output a warning
                        if ( !line.startsWith( "Could not chdir to home directory" )
                                && !line.endsWith( "ttyname: Operation not supported" ) )
                        {
                            if ( errStream == null )
                            {
                                continue;
                            }
                            errStream.write( ( line + "\n" ).getBytes() );
                        }
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
                finally
                {
                    IOUtil.close( stderrReader );
                    IOUtil.close( errStream );
                }
            }
        } ).start();
    }
}
