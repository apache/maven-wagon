package org.apache.maven.wagon.providers.data;

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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * Wagon Provider for Data URI Scheme
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="data"
 *                   instantiation-strategy="per-lookup"
 */
public class DataWagon extends StreamWagon {
    public void fillInputData(InputData inputData)
	    throws TransferFailedException, ResourceDoesNotExistException {
	final Resource resource = inputData.getResource();
	final DataURI dataUri;
	try {
	    final Repository r = this.getRepository();
	    String uriStr = r.getProtocol() + ":" + r.getHost() + "/"
		    + resource.getName();

	    dataUri = DataURI.parse(uriStr);
	} catch (ParseException e) {
	    throw new TransferFailedException("Could not read data uri: "
		    + resource.getName(), e);
	} catch (UnsupportedEncodingException e) {
	    throw new TransferFailedException("Could not read data uri: "
		    + resource.getName(), e);
	}
	final byte[] content = dataUri.getContents().getBytes();
	inputData.setInputStream(new ByteArrayInputStream(content));
	resource.setContentLength(content.length);
    }

    @Override
    public void fillOutputData(OutputData outputData)
	    throws TransferFailedException {
	throw new TransferFailedException(
		"Write content througt data uri scheme is not supported");
    }

    @Override
    public void closeConnection() throws ConnectionException {
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException,
	    AuthenticationException {
    }
}