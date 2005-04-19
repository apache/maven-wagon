package org.apache.maven.wagon;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author <a href="michal@apache.org>Michal Maczka</a>
 * @version $Id$
 */
public class FileTestUtils
{

    public static final File createUniqueFile( final String dirname, final String name )
        throws IOException
    {

        final File dir = createDir( dirname );

        final File retValue = new File( dir, name );

        return retValue;

    }


    public static final File createUniqueDir( final String name )
        throws IOException
    {

        String filename = name + System.currentTimeMillis();

        return createDir( filename );

    }


    public static final File createDir( final String name )
        throws IOException
    {

        final File baseDirectory = getTestOutputDir();

        final File retValue = new File( baseDirectory, name );

        retValue.delete();

        retValue.mkdirs();

        if ( !retValue.exists() )
        {
            throw new IOException( "Unable to create the directory for testdata" );
        }

        return retValue;
    }

    public static final File getTestOutputDir()
    {
        final String tempDir = System.getProperty( "java.io.tmpdir" );

        final String baseDir = System.getProperty( "basedir", tempDir );

        final File base = new File( baseDir ).getAbsoluteFile();

        final String pathname = base + File.separator + "target" + File.separator + "test-output";

        final File retValue = new File( pathname );

        retValue.mkdirs();

        return retValue;
    }

    public static File generateFile( String file, String content )
        throws IOException
    {
        File f = new File( file );

        f.getParentFile().mkdirs();

        Writer writer = new FileWriter( f );

        writer.write( content );

        writer.close();

        return f;
    }
}
