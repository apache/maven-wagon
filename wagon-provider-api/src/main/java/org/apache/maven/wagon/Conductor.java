package org.apache.maven.wagon;

import org.codehaus.plexus.embed.Embedder;
import org.apache.maven.wagon.manager.WagonManager;
import org.apache.maven.wagon.repository.Repository;


/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class Conductor
    implements WagonManager
{
    private Embedder embedder;

    private WagonManager wagonManager;

    // ----------------------------------------------------------------------
    // 
    // ----------------------------------------------------------------------

    public Conductor()
        throws Exception
    {
        embedder = new Embedder();

        embedder.start();

        wagonManager = (WagonManager) embedder.lookup( WagonManager.ROLE );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public Wagon getWagon( String protocol )
        throws UnsupportedProtocolException
    {
        return wagonManager.getWagon( protocol );
    }
    
    public void releaseWagon( Wagon wagon ) throws Exception
    {
       wagonManager.releaseWagon( wagon );    
    }
    

    public void addRepository( Repository repository )
    {
        wagonManager.addRepository( repository );
    }

    public void removeRepository( Repository repository )
    {
        wagonManager.removeRepository( repository );
    }
}
