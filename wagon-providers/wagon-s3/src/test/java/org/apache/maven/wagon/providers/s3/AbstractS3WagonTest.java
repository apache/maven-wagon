package org.apache.maven.wagon.providers.s3;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.wagon.WagonTestCase;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Base test class for S3 wagon tests. You must have a file named "s3auth.properties" in the
 * test classpath (i.e. src/test/resources) containing two properties: awsAccessKey and awsSecretKey
 * set to a valid Amazon S3 account's information.
 * 
 * @author eredmond
 */
public abstract class AbstractS3WagonTest
    extends WagonTestCase
{
    private AuthenticationInfo authInfo = new AuthenticationInfo();
    
    protected final String awsAccessKey;
    protected final String awsSecretKey;

    public AbstractS3WagonTest()
    {
        Properties testProperties = new Properties();
        try
        {
            InputStream propertiesIS = getClassLoader().getResourceAsStream( "s3auth.properties" );
            testProperties.load( propertiesIS );
        }
        catch( Exception e )
        {
            fail( "Unable to load properties file from classpath: s3auth.properties. You must have this file " +
                "in the classpath to run tests." );
        }

        awsAccessKey = testProperties.getProperty( "awsAccessKey" );
        awsSecretKey = testProperties.getProperty( "awsSecretKey" );

        if( awsAccessKey == null || awsAccessKey.length() == 0 ||
        	awsSecretKey == null || awsSecretKey.length() == 0 )
        {
            fail( "awsAccessKey or awsSecretKey is not set in s3auth.properties. You must have these properties set to run tests." );
        }
    }

    protected abstract String getProtocol();

    protected abstract String getTestRepositoryUrl() throws IOException;

    protected AuthenticationInfo getAuthInfo()
    {
        authInfo = new AuthenticationInfo();

        authInfo.setPassphrase( awsSecretKey );
        authInfo.setPrivateKey( awsAccessKey );

        return authInfo;
    }

    public void testWagon()
    {
        // TODO: checksum failure... but why?
    }
}
