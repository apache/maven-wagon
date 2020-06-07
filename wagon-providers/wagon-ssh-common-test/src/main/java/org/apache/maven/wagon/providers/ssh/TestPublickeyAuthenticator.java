package org.apache.maven.wagon.providers.ssh;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.mina.util.Base64;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.codehaus.plexus.util.IOUtil;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Olivier Lamy
 */
public class TestPublickeyAuthenticator
    implements PublickeyAuthenticator
{
    private boolean keyAuthz;

    public TestPublickeyAuthenticator( boolean keyAuthz )
    {
        this.keyAuthz = keyAuthz;
    }

    public boolean authenticate( String username, PublicKey key, ServerSession session )
    {
        if ( !keyAuthz )
        {
            return false;
        }
        InputStream in = null;
        try
        {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream( "ssh-keys/id_rsa.pub" );
            PublicKey publicKey = decodePublicKey( IOUtil.toString( in ) );
            in.close();
            in = null;

            return ( (RSAPublicKey) publicKey ).getModulus().equals( ( (RSAPublicKey) publicKey ).getModulus() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( in );
        }
    }

    public static byte[] decrypt( byte[] text, PrivateKey key )
        throws Exception
    {
        byte[] dectyptedText = null;
        Cipher cipher = Cipher.getInstance( "RSA/ECB/PKCS1Padding" );
        cipher.init( Cipher.DECRYPT_MODE, key );
        dectyptedText = cipher.doFinal( text );
        return dectyptedText;
    }

    private byte[] bytes;

    private int pos;

    public PublicKey decodePublicKey( String keyLine )
        throws Exception
    {
        bytes = null;
        pos = 0;

        for ( String part : keyLine.split( " " ) )
        {
            if ( part.startsWith( "AAAA" ) )
            {
                bytes = Base64.decodeBase64( part.getBytes() );
                break;
            }
        }
        if ( bytes == null )
        {
            throw new IllegalArgumentException( "no Base64 part to decode" );
        }

        String type = decodeType();
        if ( type.equals( "ssh-rsa" ) )
        {
            BigInteger e = decodeBigInt();
            BigInteger m = decodeBigInt();
            RSAPublicKeySpec spec = new RSAPublicKeySpec( m, e );
            return KeyFactory.getInstance( "RSA" ).generatePublic( spec );
        }
        else if ( type.equals( "ssh-dss" ) )
        {
            BigInteger p = decodeBigInt();
            BigInteger q = decodeBigInt();
            BigInteger g = decodeBigInt();
            BigInteger y = decodeBigInt();
            DSAPublicKeySpec spec = new DSAPublicKeySpec( y, p, q, g );
            return KeyFactory.getInstance( "DSA" ).generatePublic( spec );
        }
        else
        {
            throw new IllegalArgumentException( "unknown type " + type );
        }
    }

    private String decodeType()
    {
        int len = decodeInt();
        String type = new String( bytes, pos, len );
        pos += len;
        return type;
    }

    private int decodeInt()
    {
        // CHECKSTYLE_OFF: MagicNumber
        return ( ( bytes[pos++] & 0xFF ) << 24 ) | ( ( bytes[pos++] & 0xFF ) << 16 ) | ( ( bytes[pos++] & 0xFF ) << 8 )
            | ( bytes[pos++] & 0xFF );
        // CHECKSTYLE_ON: MagicNumber
    }

    private BigInteger decodeBigInt()
    {
        int len = decodeInt();
        byte[] bigIntBytes = new byte[len];
        System.arraycopy( bytes, pos, bigIntBytes, 0, len );
        pos += len;
        return new BigInteger( bigIntBytes );
    }

}
