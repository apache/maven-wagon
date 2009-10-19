package org.apache.maven.wagon.tck.http.util;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public final class TestUtil
{

    private TestUtil()
    {
    }

    public static File getResource( final String path )
        throws URISyntaxException
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( path );
        if ( resource == null )
        {
            throw new IllegalStateException( "Cannot find classpath resource: " + path );
        }

        return new File( resource.toURI().normalize() );
    }

}
