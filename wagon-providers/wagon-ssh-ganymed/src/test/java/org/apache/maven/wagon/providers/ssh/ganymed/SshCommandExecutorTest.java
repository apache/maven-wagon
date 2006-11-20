package org.apache.maven.wagon.providers.ssh.ganymed;

import org.apache.maven.wagon.CommandExecutorTestCase;
import org.apache.maven.wagon.repository.Repository;

/**
 * Test the command executor.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: SshCommandExecutorTest.java 312781 2005-10-11 02:04:28Z brett $
 */
public class SshCommandExecutorTest
    extends CommandExecutorTestCase
{

    protected Repository getTestRepository()
    {
        return new Repository( "test", "scp://localhost/" );
    }
}
