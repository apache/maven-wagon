package org.apache.maven.wagon;

import org.codehaus.plexus.embed.Embedder;
import org.apache.maven.wagon.manager.WagonManager;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Conductor
{
    private Embedder embedder;

    private WagonManager wagonManager;

    public Conductor()
        throws Exception
    {
        embedder = new Embedder();

        embedder.start();

        wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
    }
}
