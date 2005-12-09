package org.apache.maven.wagon.openpgp;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.commons.openpgp.BouncyCastleKeyRing;
import org.apache.commons.openpgp.BouncyCastleOpenPgpSignatureVerifier;
import org.apache.commons.openpgp.KeyRing;
import org.apache.commons.openpgp.OpenPgpSignatureVerifier;
import org.apache.commons.openpgp.SignatureStatus;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Test the wagon observer for open pgp signatures.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class WagonOpenPgpObserverTest
    extends PlexusTestCase
{
    private String keyId = "A7D16BD4";

    private static final String PASSWORD = "cop";

    private KeyRing keyRing;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        keyRing = new BouncyCastleKeyRing( getClass().getResourceAsStream( "/secring.gpg" ),
                                           getClass().getResourceAsStream( "/pubring.gpg" ), PASSWORD.toCharArray() );
    }

    public void testSign()
        throws Exception
    {
        WagonOpenPgpSignerObserver observer = new WagonOpenPgpSignerObserver( keyId, keyRing, false );

        Wagon wagon = (Wagon) lookup( Wagon.ROLE, "file" );

        wagon.addTransferListener( observer );

        File tempDir = new File( FileTestUtils.getTestOutputDir(), "openpgp-repo" );
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Repository repository = new Repository( "test", tempDir.toURL().toString() );

        wagon.connect( repository );

        // TODO: use streams when available
        wagon.put( getTestFile( "src/test/resources/test-input.txt" ), "test-input.txt" );

        byte[] signature = observer.getActualSignature();

        wagon.removeTransferListener( observer );

        wagon.disconnect();

        // check signature
        OpenPgpSignatureVerifier verifier = new BouncyCastleOpenPgpSignatureVerifier();
        SignatureStatus status = verifier.verifyDetachedSignature( getClass().getResourceAsStream( "/test-input.txt" ),
                                                                   new ByteArrayInputStream( signature ), keyRing,
                                                                   false );

        assertNotNull( "check we got a status", status );
        assertTrue( "check it was successful", status.isValid() );
    }

    public void testVerify()
        throws Exception
    {
        WagonOpenPgpSignatureVerifierObserver observer = new WagonOpenPgpSignatureVerifierObserver(
            getClass().getResourceAsStream( "/test-signature.bpg" ), keyRing, false );

        Wagon wagon = (Wagon) lookup( Wagon.ROLE, "file" );

        wagon.addTransferListener( observer );

        File tempDir = new File( FileTestUtils.getTestOutputDir(), "openpgp-repo" );
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Repository repository = new Repository( "test", getTestFile( "src/test/resources" ).toURL().toString() );

        wagon.connect( repository );

        // TODO: use streams when available
        wagon.get( "test-input.txt", new File( tempDir, "test-input.txt" ) );

        SignatureStatus status = observer.getStatus();

        assertNotNull( "check we got a status", status );
        assertTrue( "check it was successful", status.isValid() );

        wagon.removeTransferListener( observer );

        wagon.disconnect();
    }

}
