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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.ClasspathBuilder;
import org.codehaus.mojo.gwt.ClasspathBuilderException;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.shell.AbstractGwtWebMojo;

/**
 * Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * <p>
 * <b>If you use {@link #scanRemoteServiceRelativePathAnnotation} you must bind this mojo to at least compile phase</b>
 * Because the classpath scanner need to see compile classes
 * </p>
 * @goal mergewebxml
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @description Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * @author cooper
 * @version $Id$
 */
public class MergeWebXmlMojo
    extends AbstractGwtWebMojo
{

    /**
     * Location on filesystem where merged web.xml will be created. The maven-war-plugin must be configured to use this
     * path as <a href="http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html#webXml"> webXml</a> parameter
     * 
     * @parameter default-value="${project.build.directory}/web.xml"
     */
    private File mergedWebXml;
    
    /**
     * 
     * @parameter default-value="false"
     * @since 2.1.0-1
     */    
    private boolean scanRemoteServiceRelativePathAnnotation;
    
    /**
     * @parameter
     * @since 2.1.0-1
     */
    private Map<String,String> packageNamePerModule;
    
    /**
     * @component
     * @required
     * @readonly
     * @since 2.1.0-1
     */
    private ServletAnnotationFinder servletAnnotationFinder;


    /** Creates a new instance of MergeWebXmlMojo */
    public MergeWebXmlMojo()
    {
        super();
    }

    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {

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
        throws ClasspathBuilderException, MalformedURLException
    {
        Collection<File> classPathFiles = classpathBuilder.buildClasspathList( getProject(), Artifact.SCOPE_COMPILE, Collections.<Artifact>emptySet() );

        List<URL> urls = new ArrayList<URL>( classPathFiles.size() );

        for ( File file : classPathFiles )
        {
            urls.add( file.toURL() );
        }

        URLClassLoader url = new URLClassLoader( urls.toArray( new URL[urls.size()] ) );
        return url;

    }
}
