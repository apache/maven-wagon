package org.apache.maven.wagon.providers.scm;

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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.scm.manager.plexus.DefaultScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.wagon.FileTestUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.WagonTestCase;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test for {@link ScmWagon}. You need a subclass for each SCM provider you want to test.
 *
 * @author <a href="carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractScmWagonTest
    extends WagonTestCase
{

    private ScmWagon wagon;

    private String providerClassName;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( getCheckoutDirectory() );

        if ( wagon == null )
        {
            wagon = (ScmWagon) super.getWagon();

            DefaultScmManager scmManager = (DefaultScmManager) wagon.getScmManager();

            if ( getScmProvider() != null )
            {
                scmManager.setScmProvider( getScmId(), getScmProvider() );

                providerClassName = getScmProvider().getClass().getName();
            }
            else
            {
                providerClassName = scmManager.getProviderByType( getScmId() ).getClass().getName();
            }

            wagon.setCheckoutDirectory( getCheckoutDirectory() );
        }
    }

    /**
     * Allows overriding the {@link ScmProvider} injected by default in the {@link ScmWagon}.
     * Useful to force the implementation to use for a particular SCM type.
     * If this method returns <code>null</code> {@link ScmWagon} will use the default {@link ScmProvider}.
     *
     * @return the {@link ScmProvider} to use in the {@link ScmWagon}
     */
    protected ScmProvider getScmProvider()
    {
        return null;
    }

    protected Wagon getWagon()
        throws Exception
    {
        return wagon;
    }

    private File getCheckoutDirectory()
    {
        return new File( FileTestUtils.getTestOutputDir(), "/checkout-" + providerClassName );
    }

    protected int getExpectedContentLengthOnGet( int expectedSize )
    {
        return WagonConstants.UNKNOWN_LENGTH;
    }
    
    protected long getExpectedLastModifiedOnGet()
    {
        return 0;
    }

    /**
     * The SCM id, eg. <code>svn</code>, <code>cvs</code>
     *
     * @return the SCM id
     */
    protected abstract String getScmId();

    protected String getProtocol()
    {
        return "scm";
    }

    protected void createDirectory( Wagon wagon, String resourceToCreate, String dirName )
        throws Exception
    {
        super.createDirectory( wagon, resourceToCreate, dirName );
        FileUtils.deleteDirectory( getCheckoutDirectory() );
    }

    protected void assertResourcesAreInRemoteSide( Wagon wagon, List resourceNames )
        throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        FileUtils.deleteDirectory( getCheckoutDirectory() );
        super.assertResourcesAreInRemoteSide( wagon, resourceNames );
    }
}
