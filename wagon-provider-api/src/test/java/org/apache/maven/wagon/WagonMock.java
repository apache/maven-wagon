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
package org.apache.maven.wagon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 */
public class WagonMock extends StreamWagon {
    private boolean errorInputStream;
    private int timeout = 0;

    public WagonMock() {}

    public WagonMock(boolean errorInputStream) {
        this.errorInputStream = errorInputStream;
    }

    @Override
    public void fillInputData(InputData inputData) {

        InputStream is;

        if (errorInputStream) {
            InputStreamMock mockInputStream = new InputStreamMock();

            mockInputStream.setForcedError(true);

            is = mockInputStream;

        } else {
            byte[] buffer = new byte[1024 * 4 * 5];

            is = new ByteArrayInputStream(buffer);
        }

        inputData.setInputStream(is);
    }

    @Override
    public void fillOutputData(OutputData outputData) {

        OutputStream os;

        if (errorInputStream) {
            OutputStreamMock mockOutputStream = new OutputStreamMock();

            mockOutputStream.setForcedError(true);

            os = mockOutputStream;
        } else {
            os = new ByteArrayOutputStream();
        }

        outputData.setOutputStream(os);
    }

    @Override
    public void openConnectionInternal() {}

    @Override
    public void closeConnection() {}

    @Override
    public void setTimeout(int timeoutValue) {
        timeout = timeoutValue;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public List<String> getFileList(String destinationDirectory) {
        return Collections.emptyList();
    }

    @Override
    public boolean resourceExists(String resourceName) {
        return false;
    }
}
