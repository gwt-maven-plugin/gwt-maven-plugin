package fr.salvadordiaz.gwt;

import fr.salvadordiaz.gwt.client.FailTestGwt;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Failing GwtTestSuite
 */
public class GwtFailTestSuite
    extends TestCase
{

    public static Test suite()
    {
        GWTTestSuite suite = new GWTTestSuite( "This will fail" );
        suite.addTestSuite( FailTestGwt.class );
        return suite;
    }

}
