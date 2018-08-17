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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * CHECKSTYLE_OFF: LineLength
 * Adapted from commons-io LockableFileWriter
 * https://github.com/apache/commons-io/blob/58b0f795b31482daa6bb5473a8b2c398e029f5fb/src/main/java/org/apache/commons/io/output/LockableFileWriter.java
 * licenced under apache-2.0
 *
 * Aside from some clean-up of not used parts, the main difference is that we can optionally wait for a file lock.
 *
 * @author <a href="mailto:erikhakan@gmail.com">Erik Håkansson</a>
 * CHECKSTYLE_ON: LineLength
 */
public class WaitingLockableFileWriter extends Writer
{
    // Cannot extend ProxyWriter, as requires writer to be
    // known when super() is called

    /**
     * The extension for the lock file.
     */
    private static final String LCK = ".lck";

    /**
     * The writer to decorate.
     */
    private final Writer out;
    /**
     * The lock file.
     */
    private final File lockFile;

    /**
     * Constructs a WaitingLockableFileWriter with a file encoding.
     *
     * @param file        the file to write to, not null
     * @param encoding    the encoding to use, null means platform default
     * @param append      true if content should be appended, false to overwrite
     * @param lockDir     the directory in which the lock file should be held
     * @param timeout     how long to wait for a lock if file is already locked
     * @param timeoutUnit unit of timeout
     * @param wait        should we wait while trying to obtain lock
     * @throws NullPointerException if the file is null
     * @throws IOException          in case of an I/O error
     * @since 2.3
     */
    public WaitingLockableFileWriter( File file, final Charset encoding, final boolean append, String lockDir,
                                      long timeout, TimeUnit timeoutUnit, boolean wait ) throws IOException
    {
        super();
        // init file to create/append
        file = file.getAbsoluteFile();
        if ( file.getParentFile() != null )
        {
            FileUtils.forceMkdir( file.getParentFile() );
        }
        if ( file.isDirectory() )
        {
            throw new IOException( "File specified is a directory" );
        }

        // init lock file
        if ( lockDir == null )
        {
            lockDir = System.getProperty( "java.io.tmpdir" );
        }
        final File lockDirFile = new File( lockDir );
        FileUtils.forceMkdir( lockDirFile );
        testLockDir( lockDirFile );
        lockFile = new File( lockDirFile, file.getName() + LCK );

        // check if locked
        createLock( wait, timeout, timeoutUnit );

        // init wrapped writer
        out = initWriter( file, encoding, append );
    }

    //-----------------------------------------------------------------------

    /**
     * Tests that we can write to the lock directory.
     *
     * @param lockDir the File representing the lock directory
     * @throws IOException if we cannot write to the lock directory
     * @throws IOException if we cannot find the lock file
     */
    private void testLockDir( final File lockDir ) throws IOException
    {
        if ( !lockDir.exists() )
        {
            throw new IOException(
                    "Could not find lockDir: " + lockDir.getAbsolutePath() );
        }
        if ( !lockDir.canWrite() )
        {
            throw new IOException(
                    "Could not write to lockDir: " + lockDir.getAbsolutePath() );
        }
    }

    /**
     * Creates the lock file.
     *
     * @param timeout     how long to wait for a lock if file is already locked
     * @param timeoutUnit unit of timeout
     * @param wait
     * @throws IOException if we cannot create the file
     */
    private void createLock( boolean wait, long timeout, TimeUnit timeoutUnit ) throws IOException
    {
        synchronized ( WaitingLockableFileWriter.class )
        {
            if ( wait && lockFile.exists() )
            {
                long startTime = System.currentTimeMillis();
                long timeoutInMilliseconds = timeoutUnit.toMillis( timeout );
                while ( lockFile.exists() )
                {
                    if ( System.currentTimeMillis() - startTime >= timeoutInMilliseconds )
                    {
                        throw new IOException( "Failed to create lockfile " + lockFile + " after waiting "
                                + timeoutUnit.toSeconds( timeout ) + " seconds. File already exists." );
                    }
                    try
                    {
                        Thread.sleep( 512 );
                    }
                    catch ( InterruptedException e )
                    {
                        throw new IOException( e );
                    }
                }
            }
            if ( !lockFile.createNewFile() )
            {
                //Shouldn't happen if we waited above, but if not, this is possible.
                throw new IOException( "Can't write file, lock " + lockFile.getAbsolutePath() + " exists" );
            }
            lockFile.deleteOnExit();
        }
    }

    /**
     * Initialise the wrapped file writer.
     * Ensure that a cleanup occurs if the writer creation fails.
     *
     * @param file     the file to be accessed
     * @param encoding the encoding to use
     * @param append   true to append
     * @return The initialised writer
     * @throws IOException if an error occurs
     */
    private Writer initWriter( final File file, final Charset encoding, final boolean append ) throws IOException
    {
        final boolean fileExistedAlready = file.exists();
        try
        {
            return new OutputStreamWriter( new FileOutputStream( file.getAbsolutePath(), append ),
                    Charsets.toCharset( encoding ) );

        }
        catch ( final IOException | RuntimeException ex )
        {
            FileUtils.deleteQuietly( lockFile );
            if ( !fileExistedAlready )
            {
                FileUtils.deleteQuietly( file );
            }
            throw ex;
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Closes the file writer and deletes the lockfile (if possible).
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException
    {
        try
        {
            out.close();
        }
        finally
        {
            lockFile.delete();
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Write a character.
     *
     * @param idx the character to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write( final int idx ) throws IOException
    {
        out.write( idx );
    }

    /**
     * Write the characters from an array.
     *
     * @param chr the characters to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write( final char[] chr ) throws IOException
    {
        out.write( chr );
    }

    /**
     * Write the specified characters from an array.
     *
     * @param chr the characters to write
     * @param st  The start offset
     * @param end The number of characters to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write( final char[] chr, final int st, final int end ) throws IOException
    {
        out.write( chr, st, end );
    }

    /**
     * Write the characters from a string.
     *
     * @param str the string to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write( final String str ) throws IOException
    {
        out.write( str );
    }

    /**
     * Write the specified characters from a string.
     *
     * @param str the string to write
     * @param st  The start offset
     * @param end The number of characters to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write( final String str, final int st, final int end ) throws IOException
    {
        out.write( str, st, end );
    }

    /**
     * Flush the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

}
