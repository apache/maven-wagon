package org.apache.maven.wagon.events;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.Wagon;

import java.io.File;

/***
 * TransferEvent is used to notify TransferListeners about progress
 * in transfer of resources form/to the respository
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class TransferEvent extends WagonEvent
{

    /** A transfer was started. */
    public final static int TRANSFER_STARTED = 1;

    /** A transfer is completed. */
    public final static int TRANSFER_COMPLETED = 2;

    /** A transfer is in progress. */
    public final static int TRANSFER_PROGRESS = 3;

    /** An error occured during transfer */
    public final static int TRANSFER_ERROR = 4;

    /** Indicates GET transfer  (from the repository) */
    public final static int REQUEST_GET = 5;

    /** Indicates PUT transfer (to the repository)*/
    public final static int REQUEST_PUT = 6;

    private String resource;

    private int eventType;
    
    private int requestType;

    private byte[] data;
    
    private int dataLength;

    private Exception exception;

    private long progress;
    
    private File localFile;
    

    public TransferEvent(
            final Wagon wagon,
            final String resource,
            final int eventType,
            final int requestType )
    {
        super( wagon );
     
        this.resource = resource;
        
        setEventType( eventType );
        
        setRequestType( requestType );

    }

    public TransferEvent(
            final Wagon wagon,
            final String resource,
            final Exception exception )
    {
        super( wagon );
        
        this.resource = resource;
        
        setEventType( TRANSFER_ERROR );
        
        this.exception = exception;

    }



    /**
     * @return Returns the resource.
     */
    public String getResource()
    {
        return resource;
    }

    /**
     * @return Returns the exception.
     */
    public Exception getException()
    {
        return exception;
    }

    /**
     * Returns the request type.
     * @return Returns the request type. The Request type is one of
     * <code>TransferEvent.REQUEST_GET<code> or <code>TransferEvent.REQUEST_PUT<code>
     */
    public int getRequestType()
    {
        return requestType;
    }

    /**
     * Sets the request type
     * @param requestType The requestType to set.
     *  The Request type value should be either
     * <code>TransferEvent.REQUEST_GET<code> or <code>TransferEvent.REQUEST_PUT<code>.
     * @throws IllegalArgumentException when
     */
    public void setRequestType( final int requestType )
    {
        switch ( requestType )
        {

            case REQUEST_PUT:
                break;
            case REQUEST_GET:
                break;

            default :
                throw new IllegalArgumentException(
                        "Illegal request type: " + requestType );
        }

        this.requestType = requestType;
    }

    /**
     * @return Returns the eventType.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * @param eventType The eventType to set.
     */
    public void setEventType( final int eventType )
    {
        switch ( eventType )
        {

            case TRANSFER_STARTED:
                break;
            case TRANSFER_COMPLETED:
                break;
            case TRANSFER_PROGRESS:
                break;
            case TRANSFER_ERROR:
                break;
            default :
                throw new IllegalArgumentException(
                        "Illegal event type: " + eventType );
        }

        this.eventType = eventType;
    }

    /**
     * The
     * @param exception The exception to set.
     */
    public void setException( final Exception exception )
    {
        this.exception = exception;
    }


    /**
     * @param resource The resource to set.
     */
    public void setResource( final String resource )
    {
        this.resource = resource;
    }

    



    /**
     *
     * @param data
     * @param dataLength
     */
    public void setData( final byte[] data, final int dataLength )
    {
        this.data = data;
        
        this.dataLength = dataLength;
    }

    /**
     *
     * @return
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     *
     * @return
     */
    public int getDataLength()
    {
        return dataLength;
    }
    
    

    /**
     * @return Returns the local file.
     */
    public File getLocalFile()
    {
        return localFile;
    }
    /**
     * @param localFile The local file to set.
     */
    public void setLocalFile( File localFile )
    {
        this.localFile = localFile;
    }
}
