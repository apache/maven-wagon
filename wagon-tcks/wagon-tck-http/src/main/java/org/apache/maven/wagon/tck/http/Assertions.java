package org.apache.maven.wagon.tck.http;

import static junit.framework.Assert.assertEquals;
import static org.codehaus.plexus.util.FileUtils.fileRead;

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

        return IOUtil.toString( stream );
    }

}
