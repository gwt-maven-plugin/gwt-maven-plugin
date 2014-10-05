package org.codehaus.mojo.gwt.webxml;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.shell.AbstractGwtWebMojo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * <p>
 * <b>If you use {@link #scanRemoteServiceRelativePathAnnotation} you must bind this mojo to at least compile phase</b>
 * Because the classpath scanner need to see compile classes
 * 
 * @author cooper
 * @version $Id$
 */
@Mojo(name = "mergewebxml", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class MergeWebXmlMojo
    extends AbstractGwtWebMojo
{

    /**
     * Location on filesystem where merged web.xml will be created. The maven-war-plugin must be configured to use this
     * path as <a href="http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html#webXml"> webXml</a> parameter
     */
    @Parameter(defaultValue = "${project.build.directory}/web.xml")
    private File mergedWebXml;

    /**
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "false")
    private boolean scanRemoteServiceRelativePathAnnotation;

    /**
     * @since 2.1.0-1
     */
    @Parameter
    private Map<String,String> packageNamePerModule;

    /**
     * @since 2.1.0-1
     */
    @Component
    private ServletAnnotationFinder servletAnnotationFinder;

    @Override
    protected boolean isGenerator() {
        return true;
    }

    
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( "pom".equals( getProject().getPackaging() ) )
        {
            getLog().info( "GWT mergewebxml is skipped" );
            return;
        }

        try
        {
            if ( !mergedWebXml.exists() )
            {
                mergedWebXml.getParentFile().mkdirs();
                mergedWebXml.createNewFile();
            }

            FileUtils.copyFile( getWebXml(), mergedWebXml );

            Set<ServletDescriptor> servlets = new LinkedHashSet<ServletDescriptor>();
            

            for ( String module : getModules() )
            {
                GwtModule gwtModule = readModule( module );

                Map<String, String> moduleServlets = isWebXmlServletPathAsIs() ? gwtModule.getServlets( "" )
                                                                              : gwtModule.getServlets();
                getLog().debug( "merge " + moduleServlets.size() + " servlets from module " + module );
                for ( Map.Entry<String, String> servlet : moduleServlets.entrySet() )
                {
                    servlets.add( new ServletDescriptor( servlet.getKey(), servlet.getValue() ) );
                }

                if ( scanRemoteServiceRelativePathAnnotation && packageNamePerModule != null )
                {
                    String packageName = packageNamePerModule.get( gwtModule.getName() );
                    if ( StringUtils.isBlank( packageName ) )
                    {
                        // here with try with the rename-to value
                        packageName = packageNamePerModule.get( gwtModule.getPath() );
                    }
                    if ( StringUtils.isNotBlank( packageName ) )
                    {
                        getLog().debug( "search annotated servlet with package name " + packageName + " in module "
                                            + gwtModule.getName() );
                        Set<ServletDescriptor> annotatedServlets = servletAnnotationFinder
                            .findServlets( packageName, isWebXmlServletPathAsIs() ? null : gwtModule.getPath(), getAnnotationSearchClassLoader() );
                        servlets.addAll( annotatedServlets );
                    } else
                    {
                        getLog().debug( "cannot find package name for module " + gwtModule.getName() + " or path "
                                            + gwtModule.getPath() );
                    }
                }

            }

            new GwtWebInfProcessor().process( mergedWebXml, mergedWebXml, servlets );
            getLog().info( servlets.size() + " servlet(s) merged into " + mergedWebXml );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to merge web.xml", e );
        }
    }
    
    private ClassLoader getAnnotationSearchClassLoader()
        throws MalformedURLException
    {

        return new URLClassLoader( new URL[] { new File( getProject().getBuild().getOutputDirectory() ).toURI().toURL() } );

    }
}
