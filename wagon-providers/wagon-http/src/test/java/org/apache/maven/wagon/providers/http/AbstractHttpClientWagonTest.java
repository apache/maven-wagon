package org.apache.maven.wagon.providers.http;

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

import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.junit.Test;

public class AbstractHttpClientWagonTest
{
    @Test
    public void test()
        throws Exception
    {
        AbstractHttpClientWagon wagon = new AbstractHttpClientWagon()
        {
        };

        Repository repository = new Repository( "central", "http://repo.maven.apache.org/maven2" );

        wagon.connect( repository );

        Resource resource = new Resource();

        resource.setName( "junit/junit/maven-metadata.xml" );

        InputData inputData = new InputData();

        inputData.setResource( resource );

        wagon.fillInputData( inputData );

        wagon.disconnect();
    }
}
