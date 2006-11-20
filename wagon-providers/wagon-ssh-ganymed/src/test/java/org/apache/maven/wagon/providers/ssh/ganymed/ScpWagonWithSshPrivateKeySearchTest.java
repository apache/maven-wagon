package org.apache.maven.wagon.providers.ssh.ganymed;

import org.apache.maven.wagon.WagonTestCase;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.TestData;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: ScpWagonWithSshPrivateKeySearchTest.java 312572 2005-10-10 07:17:58Z brett $
 */
public class ScpWagonWithSshPrivateKeySearchTest
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

        authInfo.setPassphrase( "" );

        return authInfo;
    }


}
