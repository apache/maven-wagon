package org.apache.maven.wagon.manager;

import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class DefaultWagonManagerTest
    extends PlexusTestCase
{
    public void testDefaltWagonManager()
        throws Exception
    {
        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );

        Wagon wagon = null;

        try
        {
            wagon = (Wagon) wagonManager.getWagon( "a" );

            assertNotNull( wagon );

            wagon = (Wagon) wagonManager.getWagon( "b1" );

            assertNotNull( wagon );

            wagon = (Wagon) wagonManager.getWagon( "b2" );

            assertNotNull( wagon );

            wagon = (Wagon) wagonManager.getWagon( "c" );

            assertNotNull( wagon );
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            fail( e.getMessage() );
        }

        try
        {
            wagon = (Wagon) wagonManager.getWagon( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            //ok
        }
    }
}
