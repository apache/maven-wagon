package org.apache.maven.wagon;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import java.io.File;
import java.io.IOException;

import org.apache.maven.wagon.artifact.Artifact;
import org.apache.maven.wagon.artifact.DefaultArtifact;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.repository.Repository;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class WagonTestCase
    extends PlexusTestCase
{
    protected static String POM = "pom.xml";
    
    protected Repository localRepository;

    protected Repository testRepository;

    protected String localRepositoryPath;

    //protected MavenXpp3Reader modelReader;

    protected Artifact artifact;

    protected File sourceFile;

    protected File destFile;

    protected String resource;

    protected File artifactSourceFile;

    protected File artifactDestFile;

    protected ChecksumObserver checksumObserver;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    public WagonTestCase( String testName )
    {
        super( testName );

        checksumObserver = new ChecksumObserver();
    }

    // ----------------------------------------------------------------------
    // Methods that should be provided by subclasses for proper testing
    // ----------------------------------------------------------------------

    protected abstract String getTestRepositoryUrl() throws IOException;

    protected abstract String getProtocol();

    // ----------------------------------------------------------------------
    // 1. Create a local file repository which mimic a users local file
    //    Repository.
    //
    // 2. Create a test repository for the type of wagon we are testing. So,
    //    for example, for testing the file wagon we might have a test
    //    repository url of file://${basedir}/target/file-repository.
    // ----------------------------------------------------------------------

    protected void setupRepositories()
        throws Exception
    {
        resource = "test-resource.txt";

        //modelReader = new MavenXpp3Reader();

        // ----------------------------------------------------------------------
        // Create the test repository for the wagon we are testing.
        // ----------------------------------------------------------------------

        testRepository = new Repository();

        testRepository.setUrl( getTestRepositoryUrl() );

        testRepository.setAuthenticationInfo( getAuthInfo() );

        // ----------------------------------------------------------------------
        // Create a test local repository.
        // ----------------------------------------------------------------------

        localRepositoryPath =  FileTestUtils.createDir( "local-repository" ).getPath();

        localRepository = createFileRepository( "file://" + localRepositoryPath );

        message( "Local repository: " + localRepository );

        File f = new File( localRepositoryPath );

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

    protected void setupWagonTestingFixtures()
        throws Exception
    {
    }

    protected void tearDownWagonTestingFixtures()
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
    //
    // ----------------------------------------------------------------------

    public void testWagon()
        throws Exception
    {
        setupRepositories();

        setupWagonTestingFixtures();

        fileRoundTripTesting();

        artifactRoundTripTesting();

        tearDownWagonTestingFixtures();
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

        wagon.addTransferListener( checksumObserver );

        wagon.connect( testRepository );

        sourceFile = new File( basedir, POM );

        wagon.put( sourceFile, resource );

        wagon.removeTransferListener( checksumObserver );

        wagon.disconnect();
    }

    protected void getFile()
        throws Exception
    {
        message( "Getting test artifact from test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.addTransferListener( checksumObserver );

        wagon.connect( testRepository );

        destFile =  FileTestUtils.createUniqueFile( this );

        destFile.deleteOnExit();

        wagon.get( resource, destFile );

        wagon.removeTransferListener( checksumObserver );

        wagon.disconnect();
    }

    protected void fileRoundTripTesting()
        throws Exception
    {
        message( "File round trip testing ..." );

        putFile();

        getFile();

        System.out.println( "checksumObserver:" + checksumObserver );

        System.out.println( "actual:" + checksumObserver.getActualChecksum() );

        System.out.println( "expected:" + checksumObserver.getExpectedChecksum() );

        assertTrue( checksumObserver.cheksumIsValid() );

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

        artifactSourceFile = new File( basedir, POM );

        wagon.put( artifactSourceFile, getTestArtifact() );

        wagon.disconnect();
    }

    protected void getArtifact()
        throws Exception
    {
        message( "Getting test artifact from test repository " + testRepository );

        Wagon wagon = getWagon();

        wagon.connect( testRepository );

        artifactDestFile =  FileTestUtils.createUniqueFile( this );

        artifactDestFile.deleteOnExit();

        wagon.get( getTestArtifact(), artifactDestFile );

        wagon.disconnect();
    }

    protected void artifactRoundTripTesting()
        throws Exception
    {
        message( "Artifact round trip testing ..." );

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
            //Model model = modelReader.read( new FileReader( new File( basedir, POM ) ) );

            artifact = new DefaultArtifact( "groupId", "artifactId", "1.0", "pom" );
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

}
