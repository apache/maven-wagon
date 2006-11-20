package org.apache.maven.wagon.providers.ssh.knownhost;

import org.apache.maven.wagon.authentication.AuthenticationException;

/**
 * Exception related to known_host check failures.
 */
public class KnownHostChangedException
    extends AuthenticationException
{
    public KnownHostChangedException( String host, Throwable cause )
    {
        super( "The host key was different to that in the known_hosts configuration for host: " + host, cause );
    }
}
