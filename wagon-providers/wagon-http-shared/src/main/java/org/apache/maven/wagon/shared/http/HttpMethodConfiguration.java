package org.apache.maven.wagon.shared.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpMethodConfiguration
{
    
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    private static final String COERCE_PATTERN = "\\{%(\\w+ ),([^}]+)\\}";
    
    private Boolean useDefaultHeaders;
    
    private Map headers = new LinkedHashMap();
    
    private Map params = new LinkedHashMap();
    
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    public boolean isUseDefaultHeaders()
    {
        return useDefaultHeaders == null ? true : useDefaultHeaders.booleanValue();
    }

    public void setUseDefaultHeaders( boolean useDefaultHeaders )
    {
        this.useDefaultHeaders = Boolean.valueOf( useDefaultHeaders );
    }
    
    public Boolean getUseDefaultHeaders()
    {
        return useDefaultHeaders;
    }
    
    public void addHeader( String header, String value )
    {
        headers.put( header, value );
    }

    public Map getHeaders()
    {
        return headers;
    }

    public void setHeaders( Map headers )
    {
        this.headers = headers;
    }
    
    public void addParam( String param, String value )
    {
        params.put( param, value );
    }

    public Map getParams()
    {
        return params;
    }

    public void setParams( Map params )
    {
        this.params = params;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout( int connectionTimeout )
    {
        this.connectionTimeout = connectionTimeout;
    }

    public HttpMethodParams asMethodParams( HttpMethodParams defaults )
    {
        if ( !hasParams() )
        {
            return null;
        }
        
        HttpMethodParams p = new HttpMethodParams();
        p.setDefaults( defaults );
        
        fillParams( p );
        
        return p;
    }

    private boolean hasParams()
    {
        if ( connectionTimeout < 1 && params == null )
        {
            return false;
        }
        
        return true;
    }

    private void fillParams( HttpMethodParams p )
    {
        if ( !hasParams() )
        {
            return;
        }
        
        if ( connectionTimeout > 0 )
        {
            p.setSoTimeout( connectionTimeout );
        }
        
        if ( params != null )
        {
            Pattern coercePattern = Pattern.compile( COERCE_PATTERN );
            
            for ( Iterator it = params.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                
                Matcher matcher = coercePattern.matcher( value );
                if ( matcher.matches() )
                {
                    char type = matcher.group( 1 ).charAt( 0 );
                    value = matcher.group( 2 );
                    
                    switch( type )
                    {
                        case 'i':
                        {
                            p.setIntParameter( value, Integer.parseInt( value ) );
                            break;
                        }
                        case 'd':
                        {
                            p.setDoubleParameter( value, Double.parseDouble( value ) );
                            break;
                        }
                        case 'l':
                        {
                            p.setLongParameter( value, Long.parseLong( value ) );
                            break;
                        }
                        case 'b':
                        {
                            p.setBooleanParameter( value, Boolean.valueOf( value ).booleanValue() );
                            break;
                        }
                    }
                }
                else
                {
                    p.setParameter( key, value );
                }
            }
        }
    }

    public Header[] asRequestHeaders()
    {
        if ( headers == null )
        {
            return null;
        }
        
        Header[] result = new Header[headers.size()];
        
        int index = 0;
        for ( Iterator it = headers.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            
            Header header = new Header( key, value );
            result[index++] = header;
        }
        
        return result;
    }
    
    private HttpMethodConfiguration copy()
    {
        HttpMethodConfiguration copy = new HttpMethodConfiguration();
        
        copy.setConnectionTimeout( getConnectionTimeout() );
        if ( getHeaders() != null )
        {
            copy.setHeaders( getHeaders() );
        }
        
        if ( getParams() != null )
        {
            copy.setParams( getParams() );
        }

        copy.setUseDefaultHeaders( isUseDefaultHeaders() );
        
        return copy;
    }

    public static HttpMethodConfiguration merge( HttpMethodConfiguration base, HttpMethodConfiguration local )
    {
        if ( base == null && local == null )
        {
            return null;
        }
        else if ( base == null )
        {
            return local;
        }
        else if ( local == null )
        {
            return base;
        }
        else
        {
            HttpMethodConfiguration result = base.copy();
            
            if ( local.getConnectionTimeout() != DEFAULT_CONNECTION_TIMEOUT )
            {
                result.setConnectionTimeout( local.getConnectionTimeout() );
            }
            
            if ( local.getHeaders() != null )
            {
                result.getHeaders().putAll( local.getHeaders() );
            }
            
            if ( local.getParams() != null )
            {
                result.getParams().putAll( local.getParams() );
            }
            
            if ( local.getUseDefaultHeaders() != null )
            {
                result.setUseDefaultHeaders( local.isUseDefaultHeaders() );
            }
            
            return result;
        }
    }
    
}
