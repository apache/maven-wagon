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
package org.apache.maven.wagon;

import java.io.File;

import junit.framework.TestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 *
 */
public class LazyFileOutputStreamTest extends TestCase {

    public void testFileCreation() throws Exception {
        File file = File.createTempFile(getName(), null);

        file.delete();

        assertFalse(file.exists());

        LazyFileOutputStream stream = new LazyFileOutputStream(file);

        assertFalse(file.exists());

        String expected = "michal";

        stream.write(expected.getBytes());

        stream.close();

        assertTrue(file.exists());

        String actual = FileUtils.fileRead(file);

        assertEquals(expected, actual);
    }
}
