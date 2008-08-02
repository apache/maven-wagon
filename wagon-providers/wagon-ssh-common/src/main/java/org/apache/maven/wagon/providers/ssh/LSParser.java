package org.apache.maven.wagon.providers.ssh;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.wagon.TransferFailedException;

/**
 * LSParser 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LSParser
{
    private final static Pattern PATTERN = Pattern.compile( ".+\\s+[0-9]+\\s+.+\\s+.+\\s+[0-9]+\\s+"
                                                   + "([0-9]{4}-[0-9]{2}-[0-9]{2}|.+\\s+[0-9]+)\\s+[0-9:]+\\s+(.+?)" );
    
    /**
     * Parse a raw "ls -la", and obtain the list of files.
     * 
     * @todo use ls -1a and do away with the method all together
     * 
     * @param rawLS the raw LS to parse.
     * @return the list of files found.
     * @throws TransferFailedException
     */
    public List parseFiles( String rawLS )
        throws TransferFailedException
    {
        List ret = new ArrayList();
        try
        {
            BufferedReader br = new BufferedReader( new StringReader( rawLS ) );

            String line = br.readLine();

            while ( line != null )
            {
                line = line.trim();
                
                Matcher m = PATTERN.matcher( line );
                if ( m.matches() )
                {
                    ret.add( m.group( 2 ) );
                }
                line = br.readLine();
            }
        }
        catch ( IOException e )
        {
            throw new TransferFailedException( "Error parsing file listing.", e );
        }

        return ret;
    }
}
