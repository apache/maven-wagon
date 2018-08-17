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

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.maven.wagon.LazyOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


/**
 * Lazy Lockable OutputStream that initializes lazily and places a file lock.
 *
 * @author <a href="mailto:erikhakan@gmail.com">Erik Håkansson</a>
 *
 */
public class LazyLockableFileOutputStream
    extends LazyOutputStream<WriterOutputStream>
{

    private File file;
    private long timeout;
    private TimeUnit timeoutUnit;

    public LazyLockableFileOutputStream( File file, long timeout, TimeUnit timeoutUnit )
    {
        this.file = file;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    /**
     *
     */
    protected void initialize()
        throws IOException
    {
        boolean wait = timeout > 0;
        setDelegee( new WriterOutputStream( new WaitingLockableFileWriter( file, Charset.defaultCharset(), false,
            file.getParent(), timeout, timeoutUnit, wait ), Charset.defaultCharset() ) );
    }
}
