package org.apache.maven.wagon.providers.ssh.ganymed;

import org.apache.maven.wagon.WagonTestCase;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.TestData;

import java.io.File;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: ScpWagonTest.java 312572 2005-10-10 07:17:58Z brett $
 */
public class ScpWagonTest
    extends WagonTestCase
{
    protected String getProtocol()
    {
        return "scp";
    }

    public String getTestRepositoryUrl()
    {
        return TestData.getTestRepositoryUrl();
    }

    protected AuthenticationInfo getAuthInfo()
    {
        AuthenticationInfo authInfo = super.getAuthInfo();

        authInfo.setUserName( TestData.getUserName() );

        File privateKey = TestData.getPrivateKey();

        if ( privateKey.exists() )
        {
            authInfo.setPrivateKey( privateKey.getAbsolutePath() );

            authInfo.setPassphrase( "" );
        }

        return authInfo;
    }

}
