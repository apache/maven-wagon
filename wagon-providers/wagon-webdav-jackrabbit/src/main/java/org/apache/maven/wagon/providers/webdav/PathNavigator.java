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

import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Arrays;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class PathNavigator
{
    private final List<String> list;

    private int currentPosition;

    public PathNavigator( String path )
    {
        list = Arrays.asList( StringUtils.split( path, "/" ) );
        currentPosition = list.size();
    }

    public String getPath()
    {
        List<String> currentPathList = list.subList( 0, currentPosition );
        StringBuilder sb = new StringBuilder();
        for ( String path : currentPathList )
        {
            sb.append(path);
            sb.append('/');
        }
        return sb.toString();
    }

    public boolean backward()
    {
        if ( currentPosition == 0 )
        {
            return false;
        }
        currentPosition--;
        return true;
    }

    public boolean forward()
    {
        if ( currentPosition + 1 > list.size() )
        {
            return false;
        }
        currentPosition++;
        return true;
    }
}
