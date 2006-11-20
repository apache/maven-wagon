package org.apache.maven.wagon.providers.ssh.knownhost;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.wagon.providers.ssh.TestData;

/**
 * Unit test for <code>SingleKnownHostProvider</code>
 *
 * @author Juan F. Codagnone
 * @see SingleKnownHostProvider
 * @since Sep 12, 2005
 */
public class SingleKnownHostsProviderTest
    extends AbstractKnownHostsProviderTest
{
    private final String CORRECT_KEY = TestData.getHostKey();

    private static final String CHANGED_KEY =
        "AAAAB3NzaC1yc2EAAAABIwAAAQEA8VLKkfHl2CNqW+m0603z07dyweWzzdVGQlMPUX4z1264E7M/h+6lPKiOo+u49CL7eQVA+FtWTZoJ3oBAMABcKnHx41TnSpQUkbdR6rzyC6IG1lXiVtEjG2w7DUnxpCtVo5PaQuJobwoXv5NNL3vx03THPgcDJquLPWvGnDWhnXoEh3/6c7rprwT+PrjZ6LIT35ZCUGajoehhF151oNbFMQHllfR6EAiZIP0z0nIVI+Jiv6g+XZapumVPVYjdOfxvLKQope1H9HJamT3bDIm8mkebUB10DzQJYxFt4/0wiNH3L4jsIFn+CiW1/IQm5yyff1CUO87OqVbtp9BlaXZNmw==";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        this.okHostsProvider = new SingleKnownHostProvider( TestData.getHostname(), CORRECT_KEY );
        this.failHostsProvider = new SingleKnownHostProvider( "beaver.codehaus.org", CORRECT_KEY );
        this.changedHostsProvider = new SingleKnownHostProvider( TestData.getHostname(), CHANGED_KEY );
    }
}
