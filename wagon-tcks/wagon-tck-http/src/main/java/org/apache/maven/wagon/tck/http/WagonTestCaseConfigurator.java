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
package org.apache.maven.wagon.tck.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class WagonTestCaseConfigurator implements Contextualizable {
    private static final String UNSUPPORTED_ELEMENT = "unsupported";

    /**
     * Classpath location of the per-use-case configuration, e.g.
     * {@code META-INF/wagon-tck/http-use-cases.xml}. The Plexus shim cannot inject a nested XML tree into a
     * {@link PlexusConfiguration} field, so the configuration lives in its own resource and is parsed on
     * demand by {@link #getUseCaseConfigs()}.
     */
    private String useCaseConfigsResource;

    private PlexusConfiguration useCaseConfigsDom;

    private ComponentConfigurator configurator;

    private ClassRealm realm;

    private String wagonHint;

    private static Logger logger = LoggerFactory.getLogger(WagonTestCaseConfigurator.class);

    public boolean isSupported(final String useCaseId) {
        PlexusConfiguration useCaseConfigs = getUseCaseConfigs();
        if (useCaseConfigs != null) {
            PlexusConfiguration config = useCaseConfigs.getChild(useCaseId, false);

            if (config != null && config.getChild(UNSUPPORTED_ELEMENT, false) != null) {
                logger.info("Test case '" + useCaseId + "' is marked as unsupported by this wagon.");
                return false;
            }
        }

        return true;
    }

    public boolean configureWagonForTest(final Wagon wagon, final String useCaseId)
            throws ComponentConfigurationException {
        PlexusConfiguration useCaseConfigs = getUseCaseConfigs();
        if (useCaseConfigs != null) {
            PlexusConfiguration config = useCaseConfigs.getChild(useCaseId, false);

            if (config != null) {
                if (config.getChild(UNSUPPORTED_ELEMENT, false) != null) {
                    logger.error("Test case '" + useCaseId + "' is marked as unsupported by this wagon.");
                    return false;
                } else {
                    logger.info("Configuring wagon for test case: " + useCaseId + " with:\n\n" + config);
                    configurator.configureComponent(wagon, useCaseConfigs.getChild(useCaseId, false), realm);
                }
            } else {
                logger.info("No wagon configuration found for test case: " + useCaseId);
            }
        } else {
            logger.info("No test case configurations found.");
        }

        return true;
    }

    public void contextualize(final Context context) throws ContextException {
        PlexusContainer container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
        this.realm = container.getContainerRealm();
        // The Plexus shim does not register a default ComponentConfigurator component, and the TCK only ever
        // wants the basic one, so instantiate it directly rather than looking it up.
        configurator = new BasicComponentConfigurator();
    }

    public PlexusConfiguration getUseCaseConfigs() {
        if (useCaseConfigsDom == null && useCaseConfigsResource != null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (InputStream in = loader.getResourceAsStream(useCaseConfigsResource)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "TCK use-case configuration not found on the classpath: " + useCaseConfigsResource);
                }

                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                useCaseConfigsDom = new XmlPlexusConfiguration(Xpp3DomBuilder.build(reader));
            } catch (XmlPullParserException | IOException e) {
                throw new IllegalStateException("Malformed TCK use-case configuration: " + useCaseConfigsResource, e);
            }
        }

        return useCaseConfigsDom;
    }

    public void setUseCaseConfigsResource(final String useCaseConfigsResource) {
        this.useCaseConfigsResource = useCaseConfigsResource;
        this.useCaseConfigsDom = null;
    }

    public String getWagonHint() {
        return wagonHint;
    }

    public void setWagonHint(final String wagonHint) {
        this.wagonHint = wagonHint;
    }
}
