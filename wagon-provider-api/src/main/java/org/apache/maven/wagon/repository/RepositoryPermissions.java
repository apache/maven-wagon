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
package org.apache.maven.wagon.repository;

import java.io.Serializable;

/**
 * Describes the permissions to set on files uploaded to the repository.
 *
 * @author Brett Porter
 *
 */
public class RepositoryPermissions implements Serializable {
    /**
     * Repository group name.
     */
    private String group;

    /**
     * Repository directory mode. Modes can be in either textual (ugo+rx) or octal (755) form.
     */
    private String directoryMode;

    /**
     * Repository file mode. Modes can be in either textual (ugo+rx) or octal (644) form.
     */
    private String fileMode;

    /**
     * Get the repository directory mode to which an artifact will belong to after
     * deployment. Not all protocols permit the changing of the mode.
     *
     * @return mode
     */
    public String getDirectoryMode() {
        return directoryMode;
    }

    /**
     * Set the repository directory mode for the deployed artifact.
     *
     * @param directoryMode repository directory mode for deployed artifacts
     */
    public void setDirectoryMode(final String directoryMode) {
        this.directoryMode = directoryMode;
    }

    /**
     * Get the repository file mode to which an artifact will belong to after
     * deployment. Not all protocols permit the changing of the artifact mode.
     *
     * @return repository group name
     */
    public String getFileMode() {
        return fileMode;
    }

    /**
     * Set the repository file mode for the deployed artifact.
     *
     * @param fileMode repository file mode for deployed artifacts
     */
    public void setFileMode(final String fileMode) {
        this.fileMode = fileMode;
    }

    /**
     * Get the repository group name to which an artifact will belong to after
     * deployment. Not all protocols permit the changing of the artifact
     * group.
     *
     * @return repository group name
     */
    public String getGroup() {
        return group;
    }

    /**
     * Set the repository group name for the deployed artifact.
     *
     * @param group repository group for deployed artifacts
     */
    public void setGroup(final String group) {
        this.group = group;
    }
}
