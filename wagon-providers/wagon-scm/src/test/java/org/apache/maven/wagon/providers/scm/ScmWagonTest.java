package org.apache.maven.wagon.providers.scm;

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


import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ScmWagonTest
    extends WagonTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( getCheckoutDirectory() );
    }

    protected Wagon getWagon()
        throws Exception
    {
        ScmWagon wagon = (ScmWagon) super.getWagon();

        wagon.setCheckoutDirectory( getCheckoutDirectory() );

        return wagon;
    }

    private File getCheckoutDirectory()
    {
        return getTestFile( "target/test-output/checkout" );
    }

    protected String getProtocol()
    {
        return "scm";
    }

    protected String getTestRepositoryUrl()
        throws IOException
    {
        String repository = getTestFile( "target/test-classes/test-repo" ).getAbsolutePath();

        // TODO: this is a hack for windows
        if ( repository.indexOf( ":" ) >= 0 )
        {
            repository = repository.substring( repository.indexOf( ":" ) + 1 );
        }
        repository = repository.replace( '\\', '/' );

        return "scm:cvs:local:" + repository + ":repository";
    }
}
