package org.apache.maven.wagon.tck.http;


public class HttpsGetWagonTests
    extends GetWagonTests
{
    @Override
    protected boolean isSsl()
    {
        return true;
    }

    @Override
    protected int getDefaultPort()
    {
        return 9443;
    }

}
