package org.apache.maven.wagon.manager.stats;

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

import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * WagonStatistics 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.wagon.manager.stats.WagonStatistics"
 *   instantiation-strategy="per-lookup"
 */
public class WagonStatistics
    extends AbstractLogEnabled
    implements TransferListener, SessionListener
{
    private TransferStatistics totals = new TransferStatistics();

    private Map transfersByProtocol = new HashMap();

    private Map transfersByRepository = new HashMap();

    /**
     * Get the top level transfer statistics.
     * 
     * @return the top level transfer statistics.
     */
    public TransferStatistics getTotalTransferStatistics()
    {
        return totals;
    }

    /**
     * Get a map of transfer statistics by protocol.
     * 
     * @return Map of {@link TransferStatistics} object values with {@link String} protocol keys.
     */
    public Map /*<String, TransferStatistics>*/getTransfersByProtocol()
    {
        return transfersByProtocol;
    }

    /**
     * Get a map of transfer statistics by repository.
     * 
     * @return Map of {@link TransferStatistics} object values with {@link Repository}  keys.
     */
    public Map /*<Repository, TransferStatistics>*/getTransfersByRepository()
    {
        return transfersByRepository;
    }

    public void dump()
    {
        Iterator it;

        getLogger().info( "-------------------------------------------" );
        getLogger().info( "  Wagon Statistics " );
        dumpTransferStats( totals, "Totals:" );

        it = transfersByRepository.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String repoid = (String) entry.getKey();
            TransferStatistics repostats = (TransferStatistics) entry.getValue();
            dumpTransferStats( repostats, "Repository [" + repoid + "]:" );
        }

        getLogger().info( "-------------------------------------------" );
    }

    private void dumpTransferStats( TransferStatistics stats, String header )
    {
        DecimalFormat fmt = new DecimalFormat( "#,##0" );

        getLogger().info( "   " + header );
        getLogger().info( "      bytes transferred: " + fmt.format( stats.getBytesTransferred() ) );
        getLogger().info(
                          "      bytes sent: " + fmt.format( stats.getBytesSent() ) + " - fetched: "
                              + fmt.format( stats.getBytesFetched() ) );
        getLogger().info(
                          "      resources sent: " + fmt.format( stats.getCountResourcesSent() ) + " - failures: "
                              + fmt.format( stats.getCountSendFailures() ) );
        getLogger().info(
                          "      resources fetched: " + fmt.format( stats.getCountResourcesFetched() )
                              + " - failures: " + fmt.format( stats.getCountFetchFailures() ) );
    }

    private TransferStatistics getTransferStatsByRepository( Repository repo )
    {
        String key = repo.getId();

        TransferStatistics xferStats;

        if ( !transfersByRepository.containsKey( key ) )
        {
            xferStats = new TransferStatistics();
            transfersByRepository.put( key, xferStats );
        }
        else
        {
            xferStats = (TransferStatistics) transfersByRepository.get( key );
        }

        return xferStats;
    }

    public void debug( String message )
    {
        getLogger().debug( message );
    }

    public void transferCompleted( TransferEvent transferEvent )
    {
        getLogger().debug( ".transferCompleted(" + transferEvent + ")" );
        TransferStatistics repoStats = getTransferStatsByRepository( transferEvent.getRepository() );

        switch ( transferEvent.getRequestType() )
        {
            case TransferEvent.REQUEST_GET:
                repoStats.increaseCountResourcesFetched( 1 );
                totals.increaseCountResourcesFetched( 1 );
                break;
            case TransferEvent.REQUEST_PUT:
                repoStats.increaseCountResourcesSent( 1 );
                totals.increaseCountResourcesSent( 1 );
                break;
            default:
                getLogger().error(
                                   "Unknown Request Type [" + transferEvent.getRequestType()
                                       + "], unable to process statistics." );
        }
    }

    public void transferError( TransferEvent transferEvent )
    {
        getLogger().debug( ".transferError(" + transferEvent + ")" );

        TransferStatistics repoStats = getTransferStatsByRepository( transferEvent.getRepository() );

        switch ( transferEvent.getRequestType() )
        {
            case TransferEvent.REQUEST_GET:
                repoStats.increaseCountFetchFailures( 1 );
                totals.increaseCountFetchFailures( 1 );
                break;
            case TransferEvent.REQUEST_PUT:
                repoStats.increaseCountSendFailures( 1 );
                totals.increaseCountSendFailures( 1 );
                break;
            default:
                getLogger().error(
                                   "Unknown Request Type [" + transferEvent.getRequestType()
                                       + "], unable to process statistics." );
        }
    }

    public void transferInitiated( TransferEvent transferEvent )
    {
        getLogger().debug( ".transferInitiated(" + transferEvent + ")" );
    }

    public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
    {
        getLogger().debug( ".transferProgress(" + transferEvent + ", (byte[]) buffer, " + length + ")" );

        TransferStatistics repoStats = getTransferStatsByRepository( transferEvent.getRepository() );

        switch ( transferEvent.getRequestType() )
        {
            case TransferEvent.REQUEST_GET:
                repoStats.increaseBytesFetched( length );
                totals.increaseBytesFetched( length );
                break;
            case TransferEvent.REQUEST_PUT:
                repoStats.increaseBytesSent( length );
                totals.increaseBytesSent( length );
                break;
            default:
                getLogger().error(
                                   "Unknown Request Type [" + transferEvent.getRequestType()
                                       + "], unable to process statistics." );
        }
    }

    public void transferStarted( TransferEvent transferEvent )
    {
        getLogger().debug( ".transferStarted(" + transferEvent + ")" );
    }

    public void sessionConnectionRefused( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionConnectionRefused(" + sessionEvent + ")" );
    }

    public void sessionDisconnected( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionDisconnected(" + sessionEvent + ")" );
    }

    public void sessionDisconnecting( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionDisconnecting(" + sessionEvent + ")" );
    }

    public void sessionError( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionError(" + sessionEvent + ")" );
    }

    public void sessionLoggedIn( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionLoggedIn(" + sessionEvent + ")" );
    }

    public void sessionLoggedOff( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionLoggedOff(" + sessionEvent + ")" );
    }

    public void sessionOpened( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionOpened(" + sessionEvent + ")" );
    }

    public void sessionOpening( SessionEvent sessionEvent )
    {
        getLogger().debug( ".sessionOpening(" + sessionEvent + ")" );
    }
}
