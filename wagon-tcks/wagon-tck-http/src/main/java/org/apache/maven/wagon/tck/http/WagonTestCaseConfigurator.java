package org.apache.maven.wagon.tck.http;

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

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

public class WagonTestCaseConfigurator
    implements Contextualizable
{
    private static final String UNSUPPORTED_ELEMENT = "unsupported";

    private PlexusConfiguration useCaseConfigs;

    private ComponentConfigurator configurator;

    private ClassRealm realm;

    private String wagonHint;

    public boolean isSupported( final String useCaseId )
    {
        if ( useCaseConfigs != null )
        {
            PlexusConfiguration config = useCaseConfigs.getChild( useCaseId, false );

            if ( config != null && config.getChild( UNSUPPORTED_ELEMENT, false ) != null )
            {
                System.out.println( "Test case '" + useCaseId + "' is marked as unsupported by this wagon." );
                return false;
            }
        }

        return true;
    }

    public boolean configureWagonForTest( final Wagon wagon, final String useCaseId )
        throws ComponentConfigurationException
    {
        if ( useCaseConfigs != null )
        {
            PlexusConfiguration config = useCaseConfigs.getChild( useCaseId, false );

            if ( config != null )
            {
                if ( config.getChild( UNSUPPORTED_ELEMENT, false ) != null )
                {
                    System.out.println( "Test case '" + useCaseId + "' is marked as unsupported by this wagon." );
                    return false;
                }
                else
                {
                    System.out.println( "Configuring wagon for test case: " + useCaseId + " with:\n\n" + config );
                    configurator.configureComponent( wagon, useCaseConfigs.getChild( useCaseId, false ), realm );
                }
            }
            else
            {
                System.out.println( "No wagon configuration found for test case: " + useCaseId );
            }
        }
        else
        {
            System.out.println( "No test case configurations found." );
        }

        return true;
    }

    public void contextualize( final Context context )
        throws ContextException
    {
        PlexusContainer container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
        this.realm = container.getContainerRealm();
        try
        {
            configurator = (ComponentConfigurator) container.lookup( ComponentConfigurator.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new ContextException( "Failed to lookup component configurator: " + e.getMessage(), e );
        }
    }

    public PlexusConfiguration getUseCaseConfigs()
    {
        return useCaseConfigs;
    }

    public void setUseCaseConfigs( final PlexusConfiguration useCaseConfigs )
    {
        this.useCaseConfigs = useCaseConfigs;
    }

    public String getWagonHint()
    {
        return wagonHint;
    }

    public void setWagonHint( final String wagonHint )
    {
        this.wagonHint = wagonHint;
    }

}
