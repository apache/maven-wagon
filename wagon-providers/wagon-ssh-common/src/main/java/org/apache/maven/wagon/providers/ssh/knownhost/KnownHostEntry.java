package org.apache.maven.wagon.providers.ssh.knownhost;

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

public class KnownHostEntry
{

    private String hostName;

    private String keyType;

    private String keyValue;

    public KnownHostEntry()
    {
    }

    public KnownHostEntry( String hostName, String keyType, String keyValue )
    {
        this.hostName = hostName;
        this.keyType = keyType;
        this.keyValue = keyValue;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }

    public String getKeyType()
    {
        return keyType;
    }

    public void setKeyType( String keyType )
    {
        this.keyType = keyType;
    }

    public String getKeyValue()
    {
        return keyValue;
    }

    public void setKeyValue( String keyValue )
    {
        this.keyValue = keyValue;
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( hostName == null ) ? 0 : hostName.hashCode() );
        result = prime * result + ( ( keyType == null ) ? 0 : keyType.hashCode() );
        result = prime * result + ( ( keyValue == null ) ? 0 : keyValue.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        KnownHostEntry other = (KnownHostEntry) obj;
        if ( hostName == null )
        {
            if ( other.hostName != null )
            {
                return false;
            }
        }
        else if ( !hostName.equals( other.hostName ) )
        {
            return false;
        }

        if ( keyType == null )
        {
            if ( other.keyType != null )
            {
                return false;
            }
        }
        else if ( !keyType.equals( other.keyType ) )
        {
            return false;
        }

        if ( keyValue == null )
        {
            if ( other.keyValue != null )
            {
                return false;
            }
        }
        else if ( !keyValue.equals( other.keyValue ) )
        {
            return false;
        }
        return true;
    }

}
