package org.apache.maven.wagon.tck.http;

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

import static junit.framework.Assert.assertEquals;
import static org.codehaus.plexus.util.FileUtils.fileRead;

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 */
public final class Assertions
{

    public static void assertFileContentsFromResource( final String resourceBase, final String resourceName,
                                                       final File output, final String whyWouldItFail )
        throws IOException
    {
        String content = readResource( resourceBase, resourceName );
        String test = fileRead( output );

        assertEquals( whyWouldItFail, content, test );
    }

    private static String readResource( final String base, final String name )
        throws IOException
    {
        String url = base;
        if ( !url.endsWith( "/" ) && !name.startsWith( "/" ) )
        {
            url += "/";
        }
        url += name;

        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        InputStream stream = cloader.getResourceAsStream( url );

        if ( stream == null )
        {
            return null;
        }

        final String resource = IOUtil.toString( stream );
        stream.close();
        return resource;
    }

}
