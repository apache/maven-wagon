package org.apache.maven.wagon.providers.file;

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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Test driver for LazyLockableFileOutputStreamIntegrationTest
 */
public class LazyLockableFileOutputStreamTestDriver
{
    public static void main( String[] args ) throws Exception
    {
        File file = new File( args[0] );
        long timeout = Long.parseLong( args[1] );
        long startTime = System.currentTimeMillis();
        OutputStream outputStream = new LazyLockableFileOutputStream( file, timeout, TimeUnit.SECONDS );
        try
        {
            outputStream.write( 1 );
        }
        catch ( IOException e )
        {
            if ( e.getMessage().equals( "Can't write file, lock " + file.getAbsolutePath() + ".lck exists" ) )
            {
                System.out.println( "ready" );
                // CHECKSTYLE_OFF: MagicNumber
                System.exit( 126 );
                // CHECKSTYLE_ON: MagicNumber
            }
            else if ( e.getMessage().equals( "Failed to create lockfile " + file.getAbsolutePath()
                    + ".lck after waiting " + timeout + " seconds. File already exists." ) )
            {
                long diff = System.currentTimeMillis() - startTime;
                if ( diff < TimeUnit.SECONDS.toMillis( timeout ) )
                {
                    throw new Exception( "We were supposed to wait for " + timeout
                            + " seconds, but Exception came early at " + diff + " milliseconds." );
                }
                System.out.println( "ready" );
                // CHECKSTYLE_OFF: MagicNumber
                System.exit( 127 );
                // CHECKSTYLE_ON: MagicNumber
            }
            else
            {
                throw e;
            }
        }
        System.out.println( "ready" );
        //noinspection ResultOfMethodCallIgnored
        System.in.read(); //wait for input to allow test to control when to exit.
        outputStream.close();
    }
}
