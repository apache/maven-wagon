package org.apache.maven.wagon.artifact.verify;

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

import junit.framework.TestCase;

public class SHA1VerifierTest
    extends TestCase
{
    private SHA1Verifier s;

    protected void setUp()
    {
        s = new SHA1Verifier();
    }

    public void testSHA1WithABC()
    {
        // input: "abc"
        // checksum: A9993E36 4706816A BA3E2571 7850C26C 9CD0D89D

        s.init();

        s.update( (byte) 'a' );

        s.update( (byte) 'b' );

        s.update( (byte) 'c' );

        s.finish();

        assertEquals( "A9993E364706816ABA3E25717850C26C9CD0D89D".toLowerCase(), s.digout() );
    }

    public void testSHA1WithLongerString()
    {
        // input: "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        // checksum: 84983E44 1C3BD26E BAAE4AA1 F95129E5 E54670F1

        s.init();

        s.updateASCII( "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq" );

        s.finish();

        assertEquals( "84983E441C3BD26EBAAE4AA1F95129E5E54670F1".toLowerCase(), s.digout() );

    }

    public void testSHA1WithAMillionAs()
    {
        // input: A million repetitions of "a"
        // checksum: 34AA973C D4C4DAA4 F61EEB2B DBAD2731 6534016F

        s.init();

        for ( int i = 0; i < 1000000; i++ )
        {
            s.update( (byte) 'a' );
        }

        s.finish();

        assertEquals( "34AA973CD4C4DAA4F61EEB2BDBAD27316534016F".toLowerCase(), s.digout() );
    }
}
