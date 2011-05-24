package org.apache.maven.wagon.shared.http;

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

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

public class HttpConfiguration
{
    
    private static final HttpMethodConfiguration DEFAULT_PUT =
        new HttpMethodConfiguration().addParam( HttpClientParams.USE_EXPECT_CONTINUE, "%b,true" );
    
    private HttpMethodConfiguration all;
    
    private HttpMethodConfiguration get;
    
    private HttpMethodConfiguration put;
    
    private HttpMethodConfiguration head;

    public HttpMethodConfiguration getAll()
    {
        return all;
    }

    public HttpConfiguration setAll( HttpMethodConfiguration all )
    {
        this.all = all;
        return this;
    }

    public HttpMethodConfiguration getGet()
    {
        return get;
    }
    
    public HttpConfiguration setGet( HttpMethodConfiguration get )
    {
        this.get = get;
        return this;
    }

    public HttpMethodConfiguration getPut()
    {
        return put;
    }

    public HttpConfiguration setPut( HttpMethodConfiguration put )
    {
        this.put = put;
        return this;
    }

    public HttpMethodConfiguration getHead()
    {
        return head;
    }

    public HttpConfiguration setHead( HttpMethodConfiguration head )
    {
        this.head = head;
        return this;
    }
    
    public HttpMethodConfiguration getMethodConfiguration( HttpMethod method )
    {
        if ( method instanceof GetMethod )
        {
            return HttpMethodConfiguration.merge( all, get );
        }
        else if ( method instanceof PutMethod )
        {
            return HttpMethodConfiguration.merge( DEFAULT_PUT, all, put );
        }
        else if ( method instanceof HeadMethod )
        {
            return HttpMethodConfiguration.merge( all, head );
        }
        
        return all;
    }

}
