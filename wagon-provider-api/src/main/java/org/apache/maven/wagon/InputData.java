package org.apache.maven.wagon;

import org.apache.maven.wagon.resource.Resource;

import java.io.InputStream;

/**
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class InputData
{
   private InputStream inputStream;

   private Resource  resource;

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream( InputStream inputStream )
    {
        this.inputStream = inputStream;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource( Resource resource )
    {
        this.resource = resource;
    }

}
