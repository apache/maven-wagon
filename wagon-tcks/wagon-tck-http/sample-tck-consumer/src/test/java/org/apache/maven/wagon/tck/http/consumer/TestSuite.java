package org.apache.maven.wagon.tck.http.consumer;

import org.apache.maven.wagon.tck.http.GetWagonTests;
import org.apache.maven.wagon.tck.http.HttpsGetWagonTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith( Suite.class )
@Suite.SuiteClasses( { GetWagonTests.class, HttpsGetWagonTests.class } )
public class TestSuite 
{
}
