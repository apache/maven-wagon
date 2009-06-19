package org.apache.maven.wagon.shared.http;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;

public class HttpConfiguration
{
    
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
            return HttpMethodConfiguration.merge( all, put );
        }
        else if ( method instanceof HeadMethod )
        {
            return HttpMethodConfiguration.merge( all, head );
        }
        
        return all;
    }

}
