package org.apache.maven.wagon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MockWagon
    extends StreamWagon
{
    private boolean errorInputStream;

    public MockWagon()
    {
    }

    public MockWagon( boolean errorInputStream )
    {
        this.errorInputStream = errorInputStream;
    }

    public String[] getSupportedProtocols()
    {
        return new String[]{"mock"};
    }

    public InputStream getInputStream( String resource )
        throws TransferFailedException
    {
        if ( errorInputStream )
        {
            MockInputStream is = new MockInputStream();

            is.setForcedError( true );

            return is;
        }

        byte[] buffer = new byte[1024 * 4 * 5];

        ByteArrayInputStream is = new ByteArrayInputStream( buffer );

        return is;
    }

    public OutputStream getOutputStream( String resource )
        throws TransferFailedException
    {
        if ( errorInputStream )
        {
            MockOutputStream os = new MockOutputStream();

            os.setForcedError( true );

            return os;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        return os;
    }

    public void openConnection()
    {
    }

    public void closeConnection()
    {
    }
}
