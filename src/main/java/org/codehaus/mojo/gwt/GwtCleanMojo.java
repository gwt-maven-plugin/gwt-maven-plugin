package org.codehaus.mojo.gwt;

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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Cleanup the webapp directory for GWT module compilation output
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @goal clean
 */
public class GwtCleanMojo
    extends AbstractGwtModuleMojo
{

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            for ( String name : getModules() )
            {
                File output = new File( getOutputDirectory(), readModule( name ).getPath() );
                clean( output );
            }
            clean( new File( getOutputDirectory(), ".gwt-tmp" ) );
        }
        catch ( GwtModuleReaderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void clean( File output )
    {
        try
        {
            FileUtils.deleteDirectory( output );
        }
        catch ( IOException e )
        {
            getLog().warn( "Failed to delete directory " + output );
        }
    }
}