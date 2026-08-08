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
package org.apache.maven.wagon.providers.ssh.jsch;

import java.util.List;

import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * A {@link Prompter} for the tests, wired in through
 * {@code src/test/resources/META-INF/plexus/components.xml}.
 * <p>
 * {@code plexus-interactivity-api} stopped shipping a {@code META-INF/plexus/components.xml} in 1.3, so its
 * {@code DefaultPrompter} is invisible to the {@code plexus-container-default} that {@code PlexusTestCase} runs;
 * without a replacement every {@code lookup( Wagon.ROLE, "scp" )} fails because {@code ConsoleInteractiveUserInfo}
 * and {@code PrompterUIKeyboardInteractive} both require one.
 * <p>
 * The tests are non-interactive, so nothing should ever prompt: every method throws rather than blocking on
 * {@code System.in}.
 */
public class TestPrompter implements Prompter {

    public String prompt(String message) throws PrompterException {
        throw unexpected(message);
    }

    public String prompt(String message, String defaultReply) throws PrompterException {
        throw unexpected(message);
    }

    public String prompt(String message, List<String> possibleValues) throws PrompterException {
        throw unexpected(message);
    }

    public String prompt(String message, List<String> possibleValues, String defaultReply) throws PrompterException {
        throw unexpected(message);
    }

    public String promptForPassword(String message) throws PrompterException {
        throw unexpected(message);
    }

    public void showMessage(String message) throws PrompterException {
        // tests run non-interactively, but a message costs nothing and helps when a test does go interactive
        System.out.println(message);
    }

    private PrompterException unexpected(String message) {
        return new PrompterException("The tests are non-interactive, unexpected prompt: " + message);
    }
}
