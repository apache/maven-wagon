package org.apache.maven.wagon.providers.ssh;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.TransferFailedException;

/**
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 *
 * @version $Id$
 *
 */
public interface SshCommandExecutor extends Wagon
{
    void executeCommand( String command ) throws TransferFailedException;
}
