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

    public void setAll( HttpMethodConfiguration all )
    {
        this.all = all;
    }

    public HttpMethodConfiguration getGet()
    {
        return get;
    }
    
    public void setGet( HttpMethodConfiguration get )
    {
        this.get = get;
    }

    public HttpMethodConfiguration getPut()
    {
        return put;
    }

    public void setPut( HttpMethodConfiguration put )
    {
        this.put = put;
    }

    public HttpMethodConfiguration getHead()
    {
        return head;
    }

    public void setHead( HttpMethodConfiguration head )
    {
        this.head = head;
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
