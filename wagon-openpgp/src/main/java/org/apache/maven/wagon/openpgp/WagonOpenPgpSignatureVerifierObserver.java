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

import org.apache.commons.openpgp.BouncyCastleOpenPgpStreamingSignatureVerifier;
import org.apache.commons.openpgp.KeyRing;
import org.apache.commons.openpgp.OpenPgpException;
import org.apache.commons.openpgp.OpenPgpStreamingSignatureVerifier;
import org.apache.commons.openpgp.SignatureStatus;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;

import java.io.IOException;
import java.io.InputStream;

/**
 * Listener to the upload process that can sign the artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo handle non-detached
 * @todo does this need to handle reset in transferStarted so it can be reused?
 */
public class WagonOpenPgpSignatureVerifierObserver
    extends AbstractTransferListener
{
    private final OpenPgpStreamingSignatureVerifier verifier;

    private SignatureStatus status;

    public WagonOpenPgpSignatureVerifierObserver( InputStream inputStream, KeyRing keyRing, boolean asciiArmored )
        throws OpenPgpException, IOException
    {
        verifier = new BouncyCastleOpenPgpStreamingSignatureVerifier( inputStream, keyRing, asciiArmored );
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        this.status = null;
    }

    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        try
        {
            verifier.update( buffer, 0, length );
        }
        catch ( OpenPgpException e )
        {
            // TODO: record the error in some way
        }
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        try
        {
            status = verifier.finish();
        }
        catch ( OpenPgpException e )
        {
            // TODO: record the error in some way
        }
        catch ( IOException e )
        {
            // TODO: record the error in some way
        }
    }

    public SignatureStatus getStatus()
    {
        return status;
    }
}
