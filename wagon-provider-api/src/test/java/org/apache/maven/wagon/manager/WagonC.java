package org.apache.maven.wagon.manager;

import org.apache.maven.wagon.providers.file.FileWagon;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class WagonC
    extends FileWagon
{
    public String[] getSupportedProtocols()
    {
        return new String[]{ "c" };
    }
}
