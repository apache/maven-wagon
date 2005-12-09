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

import org.apache.commons.openpgp.BouncyCastleOpenPgpStreamingSigner;
import org.apache.commons.openpgp.KeyRing;
import org.apache.commons.openpgp.OpenPgpException;
import org.apache.commons.openpgp.OpenPgpStreamingSigner;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;

import java.io.IOException;

/**
 * Listener to the upload process that can sign the artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo handle non-detached
 * @todo does this need to handle reset in transferStarted so it can be reused?
 */
public class WagonOpenPgpSignerObserver
    extends AbstractTransferListener
{
    private final OpenPgpStreamingSigner signer;

    private byte[] actualSignature;

    public WagonOpenPgpSignerObserver( String keyId, KeyRing keyRing, boolean asciiArmored )
        throws OpenPgpException
    {
        signer = new BouncyCastleOpenPgpStreamingSigner( keyId, keyRing, asciiArmored );
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        this.actualSignature = null;
    }

    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        try
        {
            signer.update( buffer, 0, length );
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
            actualSignature = signer.finish();
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

    public byte[] getActualSignature()
    {
        return actualSignature;
    }
}
