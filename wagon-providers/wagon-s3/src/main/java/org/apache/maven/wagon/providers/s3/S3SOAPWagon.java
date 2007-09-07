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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.soap.axis.SoapS3Service;
import org.jets3t.service.security.AWSCredentials;

/**
 * A SOAP impl of S3 Wagon.
 * 
 * @author Eric Redmond
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon" 
 *   role-hint="s3soap"
 *   instantiation-strategy="per-lookup"
 */
public class S3SOAPWagon
    extends AbstractS3Wagon
{
    public String getProtocol()
    {
        return "s3soap";
    }

    protected S3Service getService( AWSCredentials awsCredentials )
        throws S3ServiceException
    {
        return new SoapS3Service( awsCredentials );
    }
}
