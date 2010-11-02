package org.codehaus.mojo.gwt.shell;

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
import java.util.Collection;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which run a GWT module in the GWT Hosted mode.
 *
 * @goal run
 * @execute phase=compile goal:war:exploded
 * @requiresDirectInvocation
 * @requiresDependencyResolution test
 * @description Runs the the project in the GWT Hosted mode for development.
 * @author ccollins
 * @author cooper
 * @version $Id$
 */
public class RunMojo
    extends AbstractGwtWebMojo
{
    /**
     * Location of the hosted-mode web application structure.
     *
     * @parameter default-value="${project.build.directory}/${project.build.finalName}"
     */
    // Parameter shared with EclipseMojo
    private File hostedWebapp;

    /**
     * The MavenProject executed by the "compile" phase
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * URL that should be automatically opened in the GWT shell. For example com.myapp.gwt.Module/Module.html.
     * <p>
     * When the host page is outside the module "public" folder (for example, at webapp root), the module MUST be
     * specified (using a single &lt;module&gt; in configuration or by setting <code>-Dgwt.module=..</code>) and the
     * runTarget parameter can only contain the host page URI.
     * <p>
     * When the GWT module host page is part of the module "public" folder, the runTarget MAY define the full GWT module
     * path (<code>com.myapp.gwt.Module/Module.html</code>) that will be automatically converted according to the
     * <code>rename-to</code> directive into <code>renamed/Module.html</code>.
     *
     * @parameter expression="${runTarget}"
     * @required
     */
    private String runTarget;

    /**
     * Forked process execution timeOut (in seconds). Primary used for integration-testing.
     * @parameter
     */
    @SuppressWarnings("unused")
    private int runTimeOut;

    /**
     * Runs the embedded GWT server on the specified port.
     *
     * @parameter default-value="8888"
     */
    private int port;

    /**
     * Specify the location on the filesystem for the generated embedded Tomcat directory.
     *
     * @parameter default-value="${project.build.directory}/tomcat"
     */
    private File tomcat;

    /**
     * Location of the compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readOnly
     */
    private File buildOutputDirectory;


    /**
     * Source Tomcat context.xml for GWT shell - copied to /gwt/localhost/ROOT.xml (used as the context.xml for the
     * SHELL - requires Tomcat 5.0.x format - hence no default).
     *
     * @parameter
     */
    private File contextXml;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     * <p>
     * Can be set from command line using '-Dgwt.noserver=...'
     *
     * @parameter default-value="false" expression="${gwt.noserver}"
     */
    private boolean noServer;

    /**
     * Specifies a different embedded web server to run (must implement ServletContainerLauncher)
     *
     * @parameter expression="${gwt.server}"
     */
    private String server;

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
     * List of System properties to pass when running the hosted mode.
     *
     * @parameter
     * @since 1.2
     */
    private Map<String, String> systemProperties;


    public String getRunTarget()
    {
        return this.runTarget;
    }

    /**
     * @return the GWT module to run (gwt 1.6+) -- expected to be unique
     */
    public String getRunModule()
        throws MojoExecutionException
    {
        String[] modules = getModules();
        if ( noServer )
        {
            if (modules.length != 1)
            {
                getLog().error(
                    "Running in 'noserver' mode you must specify the single module to run using -Dgwt.module=..." );
                throw new MojoExecutionException( "No single module specified" );
            }
            return modules[0];
        }
        if ( modules.length == 1 )
        {
            // A single module is set, no ambiguity
            return modules[0];
        }
        int dash = runTarget.indexOf( '/' );
        if ( dash > 0 )
        {
            return runTarget.substring( 0, dash );
        }
        // The runTarget MUST start with the full GWT module path
        throw new MojoExecutionException(
            "Unable to choose a GWT module to run. Please specify your module(s) in the configuration" );
    }

    /**
     * @return the startup URL to open in hosted browser (gwt 1.6+)
     */
    public String getStartupUrl()
       throws MojoExecutionException
    {
        if ( noServer )
        {
            return runTarget;
        }

        int dash = runTarget.indexOf( '/' );
        if ( dash > 0 )
        {
            String prefix = runTarget.substring( 0, dash );
            // runTarget includes the GWT module full path.
            // Lets retrieve the GWT module and apply the rename-to directive
            String[] modules = getModules();
            for ( String module : modules )
            {
                if ( prefix.equals( module ) )
                {
                    return readModule( module ).getPath() + '/' + runTarget.substring( dash + 1 );
                }
            }
        }
        return runTarget;
    }

    protected String getFileName()
    {
        return "run";
    }

    public void doExecute( )
        throws MojoExecutionException, MojoFailureException
    {
        JavaCommand cmd = new JavaCommand( "com.google.gwt.dev.DevMode" )
            .withinScope( Artifact.SCOPE_RUNTIME )
            .withinClasspath( getGwtUserJar() )
            .withinClasspath( getGwtDevJar() )
            .arg( "-war", hostedWebapp.getAbsolutePath() )
            .arg( "-gen", getGen().getAbsolutePath() )
            .arg( "-logLevel", getLogLevel() )
            .arg( "-port", Integer.toString( getPort() ) )
            .arg( "-startupUrl", getStartupUrl() )
            .arg( noServer, "-noserver" );

        if ( server != null )
        {
            cmd.arg( "-server", server );
        }

        if ( whitelist != null && whitelist.length() > 0 )
        {
            cmd.arg( "-whitelist", whitelist );
        }
        if ( blacklist != null && blacklist.length() > 0 )
        {
            cmd.arg( "-blacklist", blacklist );
        }

        if ( systemProperties != null && !systemProperties.isEmpty() )
        {
            for ( String key : systemProperties.keySet() )
            {
                String value = systemProperties.get( key );
                if ( value != null )
                {
                    getLog().info( " " + key + "=" + value );
                    cmd.systemProperty( key, value );
                }
                else
                {
                    getLog().info( "skip sysProps " + key + " with empty value" );
                }
            }
        }

        if ( !noServer )
        {
            setupExplodedWar();
        }
        else
        {
            getLog().info( "noServer is set! Skipping exploding war file..." );
        }

        for ( String module : getModules() )
        {
            cmd.arg( module );
        }

        cmd.execute();
    }

    private void setupExplodedWar()
        throws MojoExecutionException
    {
        getLog().info( "create exploded Jetty webapp in " + hostedWebapp );

        File classes = new File( hostedWebapp, "WEB-INF/classes" );
        classes.mkdirs();

        if ( !buildOutputDirectory.getAbsolutePath().equals( classes.getAbsolutePath() ) )
        {
            getLog().warn( "Your POM <build><outputdirectory> does not match your "
                                + "hosted webapp WEB-INF/classes folder for GWT Hosted browser to see your classes." );
            try
            {
                FileUtils.copyDirectoryStructure( buildOutputDirectory, classes );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy classes to " + classes , e );
            }
        }

        File lib = new File( hostedWebapp, "WEB-INF/lib" );
        lib.mkdirs();

        Collection<Artifact> artifacts = getProjectArtifacts();
        for ( Artifact artifact : artifacts )
        {
            try
            {
                // Using m2eclipse with "resolve workspace dependencies" the artifact is the buildOutputDirectory
                if ( ! artifact.getFile().isDirectory() )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), lib );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy runtime dependency " + artifact, e );
            }
        }
    }

    public File getContextXml()
    {
        return this.contextXml;
    }

    public int getPort()
    {
        return this.port;
    }

    public File getTomcat()
    {
        return this.tomcat;
    }

    /**
     * @param runTimeOut the runTimeOut to set
     */
    public void setRunTimeOut( int runTimeOut )
    {
        setTimeOut( runTimeOut );
    }

    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }
}
