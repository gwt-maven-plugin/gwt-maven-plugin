package org.codehaus.mojo.gwt;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * 
 * @author Robert Scholte
 *
 */
public class GwtModuleMojoTest
    extends AbstractMojoTestCase
{

    public void testGWT17()
        throws Exception
    {
        AbstractGwtModuleMojo mojo = new AbstractGwtModuleMojo()
        {
            public void execute()
                throws MojoExecutionException, MojoFailureException
            {
                //nothing, won't be tests
            }
        };

        //only modules
        setVariableValueToObject( mojo, "module", null );
        setVariableValueToObject( mojo, "modules", new String[] { "module1", "module2", "module3" } );
        assertEquals( 3, mojo.getModules().length );

        //only a module
        setVariableValueToObject( mojo, "module", "singleModule" );
        setVariableValueToObject( mojo, "modules", null );
        assertEquals( 1, mojo.getModules().length );

        //both
        setVariableValueToObject( mojo, "module", "singleModule" );
        setVariableValueToObject( mojo, "modules", new String[] { "module1", "module2", "module3" } );
        assertEquals( 1, mojo.getModules().length );
    }

}
