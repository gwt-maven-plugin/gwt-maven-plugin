package org.codehaus.mojo.gwt.eclipse;

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
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Goal which creates Eclipse lauch configurations for GWT modules.
 *
 * @goal eclipse
 * @execute phase=generate-resources
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class EclipseMojo
    extends AbstractGwtModuleMojo
{
    /**
     * @component
     */
    private EclipseUtil eclipseUtil;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * <p>
     * Can be set from command line using '-Dgwt.extraJvmArgs=...', defaults to setting max Heap size to be large enough
     * for most GWT use cases.
     *
     * @parameter expression="${gwt.extraJvmArgs}" default-value="-Xmx512m"
     */
    private String extraJvmArgs;

    /**
     * The currently executed project (phase=generate-resources).
     *
     * @parameter expression="${executedProject}"
     * @readonly
     */
    private MavenProject executedProject;

    /**
     * Location of the compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readOnly
     */
    private File buildOutputDirectory;

    /**
     * Location of the hosted-mode web application structure.
     * 
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    private File hostedWebapp;

    /**
     * Additional parameters to append to the module URL. For example, gwt-log users will set "log_level=DEBUG"
     *
     * @parameter
     */
    private String additionalPageParameters;

    /**
     * Run without hosted mode server
     *
     * @parameter default-value="false" expression="${gwt.noserver}"
     */
    private boolean noserver;

    /**
     * Port of the HTTP server used when noserver is set
     *
     * @parameter default-value="8080" expression="${gwt.port}"
     */
    private int port;

    /**
     * Set GWT shell protocol/host whitelist.
     * <p>
     * Can be set from command line using '-Dgwt.whitelist=...'
     * 
     * @parameter expression="${gwt.whitelist}"
     */
    private String whitelist;

    /**
     * Set GWT shell protocol/host blacklist.
     * <p>
     * Can be set from command line using '-Dgwt.blacklist=...'
     * 
     * @parameter expression="${gwt.blacklist}"
     */
    private String blacklist;

    /**
     * Set GWT shell bindAddress.
     * <p>
     * Can be set from command line using '-Dgwt.bindAddress=...'
     * 
     * @parameter expression="${gwt.bindAddress}"
     */
    private String bindAddress;

    /**
     * Setup a launch configuration for using the Google Eclipse Plugin. This is the recommended setup, as the home-made
     * launch configuration has many limitations. This parameter is only for backward compatibility, the standard lauch
     * configuration template will be removed in a future release.
     * 
     * @parameter default-value="true" expression="${use.google.eclipse.plugin}"
     */
    private boolean useGoogleEclispePlugin;

    /**
     * @param parameters additional parameter for module URL
     */
    public void setAdditionalPageParameters( String parameters )
    {
        // escape the '&' char used for multiple parameters as the result must be XML compliant
        this.additionalPageParameters = StringUtils.replace( parameters, "&", "&amp;" );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !noserver )
        {
            // Jetty requires an exploded webapp
            setupExplodedWar();
        }
        else
        {
            getLog().info( "noServer is set! Skipping exploding war file..." );
        }

        for ( String module : getModules() )
        {
            createLaunchConfigurationForHostedModeBrowser( module );
        }
    }

    protected void setupExplodedWar()
        throws MojoExecutionException
    {
        try
        {
            File classes = new File( hostedWebapp, "WEB-INF/classes" );
            if ( !buildOutputDirectory.getAbsolutePath().equals( classes.getAbsolutePath() ) )
            {
                getLog().warn( "Your POM <build><outputdirectory> must match your "
                    + "hosted webapp WEB-INF/classes folder for GWT Hosted browser to see your classes." );
            }

            File lib = new File( hostedWebapp, "WEB-INF/lib" );
            getLog().info( "create exploded Jetty webapp in " + hostedWebapp );
            lib.mkdirs();

            Collection<Artifact> artifacts = getProject().getRuntimeArtifacts();
            for ( Artifact artifact : artifacts )
            {
                if ( !artifact.getFile().isDirectory() )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), lib );
                }
                else
                {
                    // TODO automatically add this one to GWT warnings exlusions
                }
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Failed to create Jetty exploded webapp", ioe );
        }
    }

    /**
     * create an Eclipse launch configuration file to Eclipse to run the module in hosted browser
     *
     * @param module the GWT module
     * @throws MojoExecutionException some error occured
     */
    private void createLaunchConfigurationForHostedModeBrowser( String module )
        throws MojoExecutionException
    {
        try
        {
            File launchFile = new File( getProject().getBasedir(), readModule( module ).getPath() + ".launch" );
            if ( launchFile.exists() )
            {
                getLog().info( "launch file exists " + launchFile.getName() + " skip generation " );
                return;
            }

            Configuration cfg = new Configuration();
            cfg.setClassForTemplateLoading( EclipseMojo.class, "" );

            Map<String, Object> context = new HashMap<String, Object>();
            // Read compileSourceRoots from executedProject to retrieve generated source directories
            Collection<String> sources = new LinkedList<String>( executedProject.getCompileSourceRoots() );
            List<Resource> resources = executedProject.getResources();
            for ( Resource resource : resources )
            {
                sources.add( resource.getDirectory() );
            }
            context.put( "sources", sources );
            context.put( "module", module );
            context.put( "localRepository", localRepository.getBasedir() );
            int idx = module.lastIndexOf( '.' );
            String page = module.substring( idx + 1 ) + ".html";
            if ( additionalPageParameters != null )
            {
                page += "?" + additionalPageParameters;
            }

            context.put( "modulePath", readModule( module ).getPath() );
            context.put( "page", page );
            int basedir = getProject().getBasedir().getAbsolutePath().length();
            context.put( "out", getOutputDirectory().getAbsolutePath().substring( basedir + 1 ) );
            context.put( "war", hostedWebapp.getAbsolutePath().substring( basedir + 1 ) );
            String args = noserver ? "-noserver -port " + port : "";
            if ( blacklist != null )
            {
                args += " -blacklist " + blacklist;
            }
            if ( whitelist != null )
            {
                args += " -whitelist " + whitelist;
            }
            if ( bindAddress != null )
            {
                args += " -bindAddress " + bindAddress;
            }
            context.put( "additionalArguments", args );
            context.put( "extraJvmArgs", extraJvmArgs );
            context.put( "project", eclipseUtil.getProjectName( getProject() ) );

            context.put( "gwtDevJarPath", getGwtDevJar().getAbsolutePath().replace( '\\', '/' ) );
            Writer configWriter = WriterFactory.newXmlWriter( launchFile );
            String templateName = useGoogleEclispePlugin ? "google.fm" : "launch.fm";
            Template template = cfg.getTemplate( templateName, "UTF-8" );
            template.process( context, configWriter );
            configWriter.flush();
            configWriter.close();
            getLog().info( "Write launch configuration for GWT module : " + launchFile.getAbsolutePath() );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to write launch configuration", ioe );
        }
        catch ( TemplateException te )
        {
            throw new MojoExecutionException( "Unable to merge freemarker template", te );
        }
        catch ( GwtModuleReaderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        
    }

}
