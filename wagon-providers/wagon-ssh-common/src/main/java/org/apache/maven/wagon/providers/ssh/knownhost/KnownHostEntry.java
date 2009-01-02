package org.apache.maven.wagon.providers.ssh.knownhost;

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
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        KnownHostEntry other = (KnownHostEntry) obj;
        if ( hostName == null )
        {
            if ( other.hostName != null )
                return false;
        }
        else if ( !hostName.equals( other.hostName ) )
            return false;
        if ( keyType == null )
        {
            if ( other.keyType != null )
                return false;
        }
        else if ( !keyType.equals( other.keyType ) )
            return false;
        if ( keyValue == null )
        {
            if ( other.keyValue != null )
                return false;
        }
        else if ( !keyValue.equals( other.keyValue ) )
            return false;
        return true;
    }

}
