package org.apache.maven.wagon.providers.http;

import junit.framework.Assert;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringOutputStream;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;

/**
 * @author Olivier Lamy
 */
public class HugeFileDownloadTest
    extends PlexusTestCase
{

    private static long HUGE_FILE_SIZE =
        Integer.valueOf( Integer.MAX_VALUE ).longValue() + Integer.valueOf( Integer.MAX_VALUE ).longValue();

    private Server server;

    public void testDownloadHugeFileWithContentLength()
        throws Exception
    {
        File hugeFile = new File( getBasedir(), "/target/hugefile.txt" );
        if ( !hugeFile.exists() || hugeFile.length() < HUGE_FILE_SIZE )
        {
            makeHugeFile( hugeFile );
        }

        server = new Server( 0 );

        Context root = new Context( server, "/", Context.SESSIONS );
        root.setResourceBase( new File( getBasedir(), "/target" ).getAbsolutePath() );
        ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        root.addServlet( servletHolder, "/*" );

        server.start();

        try
        {
            Wagon wagon = getWagon();
            wagon.connect( new Repository( "id", "http://localhost:" + server.getConnectors()[0].getLocalPort() ) );

            File dest = File.createTempFile( "huge", "txt" );

            wagon.get( "hugefile.txt", dest );

            Assert.assertTrue( dest.length() >= HUGE_FILE_SIZE );

            wagon.disconnect();
        }
        finally
        {
            server.start();
        }


    }

    public void testDownloadHugeFileWithChunked()
        throws Exception
    {
        final File hugeFile = new File( getBasedir(), "/target/hugefile.txt" );
        if ( !hugeFile.exists() || hugeFile.length() < HUGE_FILE_SIZE )
        {
            makeHugeFile( hugeFile );
        }

        server = new Server( 0 );

        Context root = new Context( server, "/", Context.SESSIONS );
        root.setResourceBase( new File( getBasedir(), "/target" ).getAbsolutePath() );
        ServletHolder servletHolder = new ServletHolder( new HttpServlet()
        {
            @Override
            protected void doGet( HttpServletRequest req, HttpServletResponse resp )
                throws ServletException, IOException
            {
                FileInputStream fis = new FileInputStream( hugeFile );

                byte[] buffer = new byte[8192];
                int len = 0;
                while ( ( len = fis.read( buffer ) ) != -1 )
                {
                    resp.getOutputStream().write( buffer, 0, len );
                }
                fis.close();
            }
        } );
        root.addServlet( servletHolder, "/*" );

        server.start();

        try
        {
            Wagon wagon = getWagon();
            wagon.connect( new Repository( "id", "http://localhost:" + server.getConnectors()[0].getLocalPort() ) );

            File dest = File.createTempFile( "huge", "txt" );

            wagon.get( "hugefile.txt", dest );

            Assert.assertTrue( dest.length() >= HUGE_FILE_SIZE );

            wagon.disconnect();
        }
        finally
        {
            server.start();
        }


    }


    protected Wagon getWagon()
        throws Exception
    {
        Wagon wagon = (Wagon) lookup( Wagon.ROLE, "http" );

        Debug debug = new Debug();

        wagon.addSessionListener( debug );

        return wagon;
    }

    private void makeHugeFile( File hugeFile )
        throws Exception
    {
        RandomAccessFile ra = new RandomAccessFile( hugeFile.getPath(), "rw" );
        ra.setLength( HUGE_FILE_SIZE + 1 );
        ra.seek( HUGE_FILE_SIZE );
        ra.write( 1 );

    }

}
