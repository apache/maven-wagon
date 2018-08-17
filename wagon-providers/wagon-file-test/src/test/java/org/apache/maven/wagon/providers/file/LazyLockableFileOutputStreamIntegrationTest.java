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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LazyLockableFileOutputStreamIntegrationTest
{

    //This timeout is just to ensure a failing test doesn't hang indefinitely
    private static final long processTimeoutInMilliseconds = 30000;
    //This timeout is "real" and says how long to wait for a file lock
    private static final String lockTimeoutInSeconds = "5";

    @Test
    public void testLockDontWait() throws Exception
    {
        String jarFile = System.getProperty( "jarFile" );
        String testOutputDir = System.getProperty( "testOutputDir" );
        String lockFile = testOutputDir + "lockfile1";
        Process firstProcess = startProcess( jarFile, testOutputDir, lockFile, false );
        waitForReady( firstProcess );
        ensureNotExited( firstProcess, testOutputDir );

        Process secondProcess = startProcess( jarFile, testOutputDir, lockFile, false );
        waitForReady( secondProcess );

        // CHECKSTYLE_OFF: MagicNumber
        assertEquals( 126, secondProcess.waitFor() );
        // CHECKSTYLE_ON: MagicNumber
        firstProcess.getOutputStream().close();
        assertEquals( 0, firstProcess.waitFor() );
    }

    @Test
    public void testLockWait() throws Exception
    {
        String jarFile = System.getProperty( "jarFile" );
        String testOutputDir = System.getProperty( "testOutputDir" );
        String lockFile = testOutputDir + "lockfile2";
        Process firstProcess = startProcess( jarFile, testOutputDir, lockFile, false );
        waitForReady( firstProcess );
        ensureNotExited( firstProcess, testOutputDir );

        Process secondProcess = startProcess( jarFile, testOutputDir, lockFile, true );
        waitForReady( secondProcess );
        
        // CHECKSTYLE_OFF: MagicNumber
        assertEquals( 127, secondProcess.waitFor() );
        // CHECKSTYLE_ON: MagicNumber
        firstProcess.getOutputStream().close();
        assertEquals( 0, firstProcess.waitFor() );
    }

    private void ensureNotExited(Process process, String testOutputDir) {
        try
        {
            int exitValue = process.exitValue();
            fail( "Lock process exited unexpectedly with status " + exitValue + ".\nSee " + testOutputDir
                    + " /externalProcessOutput.txt for more info." );
        }
        catch ( IllegalThreadStateException ignore )
        {
            //We actually want this exception since we don't want the process to have ended
        }
    }

    private void waitForReady( Process process ) throws IOException
    {
        InputStream inputStream = process.getInputStream();
        int length;
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();
        while ( ( length = inputStream.read( buffer ) ) != -1 )
        {
            byteArrayOutputStream.write( buffer, 0, length );
            if ( "ready".equals( byteArrayOutputStream.toString().trim() ) )
            {
                break;
            }
            if ( System.currentTimeMillis() - startTime > processTimeoutInMilliseconds )
            {
                byteArrayOutputStream.close();
                fail( "Spawned process failed to get ready in less than " + processTimeoutInMilliseconds
                        + " milliseconds." );
            }
        }
        byteArrayOutputStream.close();
    }

    private Process startProcess( String jarFile, String testOutputDir, String lockFile, boolean waitForLock )
            throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder( "java", "-cp", jarFile + File.pathSeparatorChar + testOutputDir
                + "test-classpath/*", LazyLockableFileOutputStreamTestDriver.class.getName(), lockFile,
                waitForLock ? lockTimeoutInSeconds : "0" );
        builder.redirectError( ProcessBuilder.Redirect.appendTo(
                new File( testOutputDir + "externalProcessOutput.txt" ) ) );
        return builder.start();
    }
}
