package org.apache.maven.wagon.tck.http.util;

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class TestUtil
{

    private static final Map<String, File> bases = new HashMap<String, File>();

    private TestUtil()
    {
    }

    public static File getResource( final String path )
        throws URISyntaxException, IOException
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( path );
        if ( resource == null )
        {
            throw new IllegalStateException( "Cannot find classpath resource: " + path );
        }

        if ( resource.getProtocol().startsWith( "jar" ) )
        {
            // File f = new File( path );
            // f = File.createTempFile( f.getName() + ".", ".tmp" );

            String url = resource.toExternalForm();
            int startIdx = url.lastIndexOf( ':' ) + 1;
            int endIdx = url.indexOf( "!" );
            url = url.substring( startIdx, endIdx );

            File base = bases.get( url );
            if ( base == null )
            {
                File urlFile = new File( url );

                base = new File( "target/tck-resources/" + urlFile.getName() );
                base.getParentFile().mkdirs();

                System.out.println( "unpacking test resources in jar: " + url );
                JarFile jf = null;
                try
                {
                    jf = new JarFile( urlFile );

                    InputStream in = null;
                    OutputStream out = null;

                    for ( Enumeration<JarEntry> en = jf.entries(); en.hasMoreElements(); )
                    {
                        JarEntry je = en.nextElement();
                        File target = new File( base, je.getName() ).getAbsoluteFile();
                        if ( je.isDirectory() )
                        {
                            target.mkdirs();
                        }
                        else
                        {
                            target.getParentFile().mkdirs();

                            try
                            {
                                in = jf.getInputStream( je );
                                out = new FileOutputStream( target );

                                IOUtil.copy( in, out );
                            }
                            finally
                            {
                                IOUtil.close( in );
                                IOUtil.close( out );
                            }
                        }
                    }

                    bases.put( url, base );
                }
                finally
                {
                    if ( jf != null )
                    {
                        try
                        {
                            jf.close();
                        }
                        catch ( Exception e )
                        {
                        }
                    }
                }
            }

            return new File( base, path );
        }
        else
        {
            return new File( resource.toURI().normalize() );
        }
    }

}
