package org.apache.maven.wagon;

import org.apache.maven.wagon.resource.Resource;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class OutputData 
{
   private OutputStream outputStream;
    
   private Resource  resource;

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream( OutputStream outputStream )
    {
        this.outputStream = outputStream;
    }

    public Resource getResource()
    {
        return resource;
    }

    public void setResource( Resource resource )
    {
        this.resource = resource;
    }
}
