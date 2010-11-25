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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Collect GWT java source code and module descriptor to be added as resources. Common
 * functionality for different implementations GwtResourcesMojo and GwtSourcesJarMojo
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @author <a href="mailto:vlads@pyx4j.com">Vlad Skarzhevskyy</a>
 * 
 */
abstract class GwtResourcesBaseMojo
    extends AbstractGwtModuleMojo
{

    protected class ResourceFile
    {

        File basedir;

        String fileRelativeName;

        public ResourceFile( File basedir, String fileRelativeName )
        {
            super();
            this.basedir = basedir;
            this.fileRelativeName = fileRelativeName;
        }

    }

    /**
     * Collect GWT java source code and module descriptor to be added as resources.
     */
    protected Collection<ResourceFile> getAllResourceFiles()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            Set<ResourceFile> sourcesAndResources = new HashSet<ResourceFile>();
            Set<String> sourcesAndResourcesPath = new HashSet<String>();
            sourcesAndResourcesPath.addAll( getProject().getCompileSourceRoots() );
            for ( Resource resource : (Collection<Resource>) getProject().getResources() )
            {
                sourcesAndResourcesPath.add( resource.getDirectory() );
            }

            for ( String name : getModules() )
            {
                GwtModule module = readModule( name );

                sourcesAndResources.add( getDescriptor( module, sourcesAndResourcesPath ) );
                int count = 1;

                for ( String source : module.getSources() )
                {
                    getLog().debug( "GWT sources from " + name + '.' + source );
                    Collection<ResourceFile> files = getAsResources( module, source, sourcesAndResourcesPath,
                                                                     "**/*.java" );
                    sourcesAndResources.addAll( files );
                    count += files.size();
                }
                for ( String source : module.getSuperSources() )
                {
                    getLog().debug( "GWT super-sources from " + name + '.' + source );
                    Collection<ResourceFile> files = getAsResources( module, source, sourcesAndResourcesPath,
                                                                     "**/*.java" );
                    sourcesAndResources.addAll( files );
                    count += files.size();
                }
                getLog().info( count + " source files from GWT module " + name );
            }
            return sourcesAndResources;
        }
        catch ( GwtModuleReaderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * @param source
     * @param include TODO
     * @param name
     */
    private Collection<ResourceFile> getAsResources( GwtModule module, String source, Set<String> paths, String include )
        throws MojoExecutionException
    {
        String pattern = module.getPackage().replace( '.', '/' );

        Set<ResourceFile> sourcesAndResources = new HashSet<ResourceFile>();

        for ( String path : paths )
        {
            File basedir = new File( path );
            // the default "src/main/resource" may not phisicaly exist in project
            if ( !basedir.exists() )
            {
                continue;
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( basedir );
            scanner.setIncludes( new String[] { pattern + '/' + source + '/' + include } );
            scanner.scan();
            String[] includedFiles = scanner.getIncludedFiles();
            for ( String included : includedFiles )
            {
                sourcesAndResources.add( new ResourceFile( basedir, included ) );
            }
        }

        return sourcesAndResources;
    }

    private ResourceFile getDescriptor( GwtModule module, Set<String> paths )
        throws MojoExecutionException
    {
        String moduleFilePath = module.getName().replace( '.', '/' ) + GWT_MODULE_EXTENSION;
        for ( String path : paths )
        {
            File basedir = new File( path );
            File descriptor = new File( basedir, moduleFilePath );
            if ( descriptor.exists() )
            {
                return new ResourceFile( basedir, moduleFilePath );
            }
        }
        throw new MojoExecutionException( "Failed to retrieve GWT descriptor in project sources " + moduleFilePath );
    }

}
