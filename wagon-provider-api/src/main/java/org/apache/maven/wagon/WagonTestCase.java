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

    protected String resource;

    protected File artifactSourceFile;

    protected File artifactDestFile;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    public WagonTestCase()
    {
    }

    public WagonTestCase( String name )
    {
        super( name );
    }

    // ----------------------------------------------------------------------
    // Methods that should be provided by subclasses for proper testing
    // ----------------------------------------------------------------------

    protected abstract String getTestRepositoryUrl();

    protected abstract String getProtocol();

    // ----------------------------------------------------------------------
    // 1. Create a local file repository which mimic a users local file
    //    Repository.
    //
    // 2. Create a test repository for the type of wagon we are testing. So,
    //    for example, for testing the file wagon we might have a test
    //    repository url of file://${basedir}/target/file-repository.
    // ----------------------------------------------------------------------

    protected void setUp()
        throws Exception
    {
        super.setUp();

        resource = "test-resource.txt";

        modelReader = new MavenXpp3Reader();

        // ----------------------------------------------------------------------
        // Create the test repository for the wagon we are testing.
        // ----------------------------------------------------------------------

        testRepository = new Repository();

        testRepository.setUrl( getTestRepositoryUrl() );

        testRepository.setAuthenticationInfo( getAuthInfo() );

        // ----------------------------------------------------------------------
        // Create a test local repository.
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

    protected void customizeContext()
        throws Exception
    {
        getContainer().addContextValue( "test.repository", localRepositoryPath );
    }

    protected void setupFileRoundTripTesting()
        throws Exception
    {
    }

    protected void setupArtifactRoundTripTesting()
        throws Exception
    {
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected AuthenticationInfo getAuthInfo()
    {
        return new AuthenticationInfo();
    }

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

    // ----------------------------------------------------------------------
    // File <--> File round trip testing
    // ----------------------------------------------------------------------
    // We are testing taking a file, our sourcefile, and placing it into the
    // test repository that we have setup.
    // ----------------------------------------------------------------------

    protected void putFile()
        throws Exception
    {
        message( "Putting test artifact into test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        sourceFile = new File( basedir, "project.xml" );

        wagon.put( sourceFile, resource );

        wagon.disconnect();
    }

    protected void getFile()
        throws Exception
    {
        message( "Getting test artifact from test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        destFile = File.createTempFile( "wagon", ".tmp" );

        wagon.get( resource, destFile );

        wagon.disconnect();
    }

    public void testFileRoundTrip()
        throws Exception
    {
        message( "File round trip testing ..." );

        setupFileRoundTripTesting();

        putFile();

        getFile();

        compareContents( sourceFile, destFile );
    }

    // ----------------------------------------------------------------------
    // File <--> Artifact/Repository round trip testing
    // ----------------------------------------------------------------------
    // 1. Place an artifact in the test repository.
    // 2. Get the same artifact that was just placed in the test repository.
    // 3. Compare the contents of the file that was place in the test
    //    repository with the value of the artifact retrieved from the
    //    test repository, they should be the same.
    // ----------------------------------------------------------------------

    protected void putArtifact()
        throws Exception
    {
        message( "Putting file into test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        artifactSourceFile = new File( basedir, "project.xml" );

        wagon.put( artifactSourceFile, getTestArtifact() );

        wagon.disconnect();
    }

    protected void getArtifact()
        throws Exception
    {
        message( "Getting test artifact from test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        artifactDestFile = File.createTempFile( "wagon", ".tmp" );

        wagon.get( getTestArtifact(), artifactDestFile );

        wagon.disconnect();
    }

    public void testArtifactRoundTrip()
        throws Exception
    {
        message( "Artifact round trip testing ..." );

        setupArtifactRoundTripTesting();

        putArtifact();                

        getArtifact();

        compareContents( artifactSourceFile, artifactDestFile );
    }

    protected void compareContents( File sourceFile, File destFile )
        throws Exception
    {
        // Now compare the conents of the artifact that was placed in
        // the repository with the contents of the artifact that was
        // retrieved from the repository.

        System.out.println( "sourceFile = " + sourceFile );

        System.out.println( "destFile = " + destFile );

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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

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
}
