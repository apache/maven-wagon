package org.apache.maven.wagon;

import org.apache.maven.wagon.artifact.Artifact;
import org.apache.maven.wagon.artifact.DefaultArtifact;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.events.Debug;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.FileReader;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class WagonTestCase
    extends PlexusTestCase
{
    protected Repository localRepository;

    protected Repository testRepository;

    protected String localRepositoryPath;

    protected MavenXpp3Reader modelReader;

    protected Artifact artifact;

    protected File sourceFile;

    protected File destFile;

    public WagonTestCase()
    {
    }

    public WagonTestCase( String name )
    {
        super( name );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        modelReader = new MavenXpp3Reader();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        testRepository = new Repository();

        testRepository.setUrl( getTestRepositoryUrl() );

        testRepository.setAuthenticationInfo( getAuthInfo() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        localRepositoryPath = new File( basedir, "/target/local-repository" ).getPath();

        localRepository = createFileRepository( "file://" + localRepositoryPath );

        File f = new File( localRepositoryPath, "/maven/jars" );

        if ( !f.exists() )
        {
            f.mkdirs();
        }

        f = new File( localRepositoryPath, "/maven/poms" );

        if ( !f.exists() )
        {
            f.mkdirs();
        }
    }

    protected abstract String getTestRepositoryUrl();

    protected AuthenticationInfo getAuthInfo()
    {
        return new AuthenticationInfo();
    }

    protected abstract String getProtocol();

    protected Wagon getWagon()
        throws Exception
    {
        Wagon wagon = (Wagon) lookup( Wagon.ROLE, getProtocol() );

        Debug debug = new Debug();

        wagon.addSessionListener( debug );

        wagon.addTransferListener( debug );

        return wagon;
    }

    private void message( String message )
    {
        System.out.println( "---------------------------------------------------------------------------------------------------------" );
        System.out.println( message );
        System.out.println( "---------------------------------------------------------------------------------------------------------" );
    }

    protected  void put()
        throws Exception
    {
        message( "Putting test artifact into test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        sourceFile = new File( basedir, "project.xml" );

        wagon.put( sourceFile, getTestArtifact() );

        wagon.disconnect();
    }

    protected void get()
        throws Exception
    {
        message( "Getting test artifact from test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        destFile = File.createTempFile( "wagon", ".tmp" );

        wagon.get( getTestArtifact(), destFile );

        wagon.disconnect();
    }

    public void testRoundTrip()
        throws Exception
    {
        put();

        get();

        // Now compare the conents of the artifact that was placed in
        // the repository with the contents of the artifact that was
        // retrieved from the repository.

        System.out.println( "---------------------------------------------------------------------------------------------------------" );

        System.out.print( "Evaluating and comparing ... " );

        String sourceContent = FileUtils.fileRead( sourceFile );

        String destContent = FileUtils.fileRead( destFile );

        assertEquals( sourceContent, destContent );

        System.out.println( "OK" );

        System.out.println( "---------------------------------------------------------------------------------------------------------" );

    }

    protected Artifact getTestArtifact()
        throws Exception
    {
        if ( artifact == null )
        {
            Model model = modelReader.read( new FileReader( new File( basedir, "project.xml" ) ) );

            artifact = new DefaultArtifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), "pom" );
        }

        return artifact;
    }

    protected void customizeContext()
        throws Exception
    {
        getContainer().addContextValue( "test.repository", localRepositoryPath );
    }


    protected Repository createFileRepository( String url )
    {
        File path = new File( url.substring( 7 ) );

        path.mkdirs();

        Repository repository = new Repository();

        repository.setUrl( url );

        return repository;
    }

    protected File generateFile( String file, String content )
        throws IOException
    {
        File f = new File( file );

        f.getParentFile().mkdirs();

        Writer writer = new FileWriter( f );

        writer.write( content );

        writer.close();

        return f;
    }

    protected void generateRepositoryArtifact( Artifact artifact, Repository repository, String content )
        throws IOException
    {
        String fileName = repository.getBasedir() + "/" + repository.artifactPath( artifact );

        generateFile( fileName, content );
    }

    protected String loadString( File file )
        throws Exception
    {
        return FileUtils.fileRead( file );
    }
}
