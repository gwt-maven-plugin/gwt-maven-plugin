package fr.salvadordiaz.gwt.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Failing GwtTestCase <br/>
 * read : http://mojo.codehaus.org/gwt-maven-plugin/user-guide/testing.html pour plus d'informations
 * 
 * @author salvador
 * @see GwtFailTestSuite
 * 
 */
public class FailTestGwt
    extends GWTTestCase
{

    @Override
    public String getModuleName()
    {
        return "fr.salvadordiaz.gwt.Fail";
    }

    public void testSetTranslation()
        throws Exception
    {
        // you would think this wouldn't fail... you'd be wrong ;)
        assertTrue( true );
    }

}
