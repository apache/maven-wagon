package org.apache.maven.wagon.providers.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferEventSupport;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * Put Input Stream is borrowed from wagon-http, and is used to properly
 * notify the listeners of transfer events on a put request.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @todo copy to wagon-providers-api
 */
public class PutInputStream
    extends FileInputStream
{
    private TransferEventSupport eventSupport;

    private TransferEvent event;

    public PutInputStream( File file, Repository repo, Resource resource, Wagon wagon, TransferEventSupport eventSupport )
        throws FileNotFoundException
    {
        super( file );

        this.eventSupport = eventSupport;

        event = new TransferEvent( wagon, repo, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT );

        event.setLocalFile( file );
    }

    public int read( byte buffer[] )
        throws IOException
    {
        return read( buffer, 0, buffer.length );
    }

    public int read()
        throws IOException
    {
        byte buffer[] = new byte[1];

        return read( buffer );
    }

    public int read( byte buffer[], int offset, int length )
        throws IOException
    {
        int retValue = super.read( buffer, offset, length );

        if ( retValue > 0 )
        {
            event.setTimestamp( System.currentTimeMillis() );

            eventSupport.fireTransferProgress( event, buffer, retValue );
        }
        return retValue;
    }
}
