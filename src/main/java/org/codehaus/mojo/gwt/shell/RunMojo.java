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

import static org.codehaus.plexus.util.AbstractScanner.DEFAULTEXCLUDES;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which run a GWT module in the GWT Hosted mode.
 *
 * @goal run
 * @execute phase=process-classes goal:war:exploded
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
    
    /**
     * Copies the contents of warSourceDirectory to hostedWebapp.
     * <p>
     * Can be set from command line using '-Dgwt.copyWebapp=...'
     * </p>
     * @parameter default-value="false" expression="${gwt.copyWebapp}"
     * @since 2.1.0-1
     */
    private boolean copyWebapp;    

    /**
     * set the appengine sdk to use
     * <p>
     * Artifact will be downloaded with groupId : {@link #appEngineGroupId} 
     * and artifactId {@link #appEngineArtifactId}
     * <p>
     * @parameter default-value="1.3.8" expression="${gwt.appEngineVersion}"
     * @since 2.1.0-1
     */
    private String appEngineVersion;
    
    /**
     * <p>
     * List of {@link Pattern} jars to exclude from the classPath when running
     * dev mode
     * </p>
     * @parameter 
     * @since 2.1.0-1
     */
    private List<String> runClasspathExcludes;
    
    /**
     * <p>
     * Location to find appengine sdk or to unzip downloaded one see {@link #appEngineVersion}
     * </p>
     * @parameter default-value="${project.build.directory}/appengine-sdk/" expression="${gwt.appEngineHome}"
     * @since 2.1.0-1
     */    
    private File appEngineHome;
    
    /**
     * <p>
     * groupId to download appengine sdk from maven repo
     * </p>
     * @parameter default-value="com.google.appengine" expression="${gwt.appEngineGroupId}"
     * @since 2.1.0-1
     */    
    private String appEngineGroupId;
    
    /**
     * <p>
     * groupId to download appengine sdk from maven repo
     * </p>
     * @parameter default-value="appengine-java-sdk" expression="${gwt.appEngineArtifactId}"
     * @since 2.1.0-1
     */    
    private String appEngineArtifactId;    
    
    
    /**
     * To look up Archiver/UnArchiver implementations
     * @since 2.1.0-1
     * @component
     */
    protected ArchiverManager archiverManager;
    
    
    
     /**
     * Set GWT shell bindAddress.
     * <p>
     * Can be set from command line using '-Dgwt.bindAddress=...'
     * @since 2.1.0-1
     * @parameter expression="${gwt.bindAddress}"
     */
    private String bindAddress;    

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
                    try
                    {
                        return readModule( module ).getPath() + '/' + runTarget.substring( dash + 1 );
                    }
                    catch ( GwtModuleReaderException e )
                    {
                        throw new MojoExecutionException( e.getMessage(), e );
                    }
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
        try
        {
            JavaCommand cmd = new JavaCommand( "com.google.gwt.dev.DevMode" );

            if ( gwtSdkFirstInClasspath )
            {
                cmd.withinClasspath( getGwtUserJar() ).withinClasspath( getGwtDevJar() );
            }

            cmd.withinScope( Artifact.SCOPE_RUNTIME );
            addCompileSourceArtifacts( cmd );

            if ( !gwtSdkFirstInClasspath )
            {
                cmd.withinClasspath( getGwtUserJar() ).withinClasspath( getGwtDevJar() );
            }

            cmd.arg( "-war", hostedWebapp.getAbsolutePath() )
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
                        getLog().debug( " " + key + "=" + value );
                        cmd.systemProperty( key, value );
                    }
                    else
                    {
                        getLog().debug( "skip sysProps " + key + " with empty value" );
                    }
                }
            }

            if ( bindAddress != null && bindAddress.length() > 0 )
            {
                cmd.arg( "-bindAddress" ).arg( bindAddress );
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
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }        
    }

    @Override
    protected void postProcessClassPath( Collection<File> classPath )
    {
        boolean isAppEngine = "com.google.appengine.tools.development.gwt.AppEngineLauncher".equals( server );
        if ( !isAppEngine )
        {
            return;
        }
        List<Pattern> patternsToExclude = new ArrayList<Pattern>();
        if ( runClasspathExcludes != null && !runClasspathExcludes.isEmpty() )
        {
            for ( String runClasspathExclude : runClasspathExcludes )
            {
                patternsToExclude.add( Pattern.compile( runClasspathExclude ) );
            }
        }
        Iterator<File> it = classPath.iterator();
        while ( it.hasNext() )
        {
            String name = it.next().getName();
            if ( !patternsToExclude.isEmpty() )
            {
                for ( Pattern pattern : patternsToExclude )
                {
                    if ( pattern.matcher( name ).find() )
                    {
                        getLog().info( "remove jar " + name + " from system classpath" );
                        it.remove();
                        continue;
                    }
                }
            }

        }
        // TODO refactor this a little 
        if ( isAppEngine )
        {
            File appEngineToolsApi = new File( appEngineHome, "/lib/appengine-tools-api.jar" );
            File appEngineLocalRuntime = new File( appEngineHome, "/lib/impl/appengine-local-runtime.jar" );
            File appEngineAgent = new File( appEngineHome, "/lib/agent/appengine-agent.jar" );
            if ( appEngineHome.exists() && appEngineToolsApi.exists() && appEngineLocalRuntime.exists()
                && appEngineAgent.exists() )
            {
                classPath.add( appEngineToolsApi );
                classPath.add( appEngineLocalRuntime );
                classPath.add( appEngineAgent );
            }
            else
            {
                try
                {
                    if ( !appEngineHome.exists() )
                    {
                        appEngineHome.mkdirs();
                        // force addition of appengine SDK in a exploded SDK repository location
                        Artifact appEngineSdk =
                            resolve( appEngineGroupId, appEngineArtifactId, appEngineVersion, "zip", "" );
                        // sdk extraction
                        UnArchiver unArchiver = archiverManager.getUnArchiver( appEngineSdk.getFile() );
                        unArchiver.setSourceFile( appEngineSdk.getFile() );
                        unArchiver.setDestDirectory( appEngineHome );
                        getLog().info( "extract appengine " + appEngineVersion + " sdk to " + appEngineHome.getPath() );
                        unArchiver.extract();
                    }
                    else
                    {
                        getLog().info( "use existing appengine sdk from " + appEngineHome.getPath() );
                    }
                    appEngineToolsApi =
                        new File( appEngineHome, "appengine-java-sdk-" + appEngineVersion
                            + "/lib/appengine-tools-api.jar" );
                    if ( !appEngineToolsApi.exists() )
                    {
                        throw new RuntimeException( appEngineToolsApi.getPath() + " not exists" );
                    }
                    classPath.add( appEngineToolsApi );
                    getLog().debug( "add " + appEngineToolsApi.getPath() + " to the classpath" );

                    appEngineLocalRuntime =
                        new File( appEngineHome, "appengine-java-sdk-" + appEngineVersion
                            + "/lib/impl/appengine-local-runtime.jar" );
                    if ( !appEngineLocalRuntime.exists() )
                    {
                        throw new RuntimeException( appEngineLocalRuntime.getPath() + " not exists" );
                    }
                    classPath.add( appEngineLocalRuntime );
                    getLog().debug( "add " + appEngineLocalRuntime.getPath() + " to the classpath" );

                    appEngineAgent =
                        new File( appEngineHome, "appengine-java-sdk-" + appEngineVersion
                            + "/lib/agent/appengine-agent.jar" );
                    classPath.add( appEngineAgent );
                    getLog().debug( "add " + appEngineAgent.getPath() + " to the classpath" );
                }
                catch ( MojoExecutionException e )
                {
                    // FIXME add throws MojoExecutionException in postProcessClassPath
                    throw new RuntimeException( e.getMessage(), e );
                }
                catch ( ArchiverException e )
                {
                    // FIXME add throws MojoExecutionException in postProcessClassPath
                    throw new RuntimeException( e.getMessage(), e );
                }
                catch ( NoSuchArchiverException e )
                {
                    // FIXME add throws MojoExecutionException in postProcessClassPath
                    throw new RuntimeException( e.getMessage(), e );
                }
            }
        }
    }  

    private void setupExplodedWar()
        throws MojoExecutionException
    {
        getLog().info( "create exploded Jetty webapp in " + hostedWebapp );

        if ( copyWebapp && !warSourceDirectory.getAbsolutePath().equals( hostedWebapp.getAbsolutePath() ) )
        {
            try
            {
                String excludes = StringUtils.join( DEFAULTEXCLUDES, "," );
                FileUtils.copyDirectory( warSourceDirectory, hostedWebapp, "**", excludes );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy warSourceDirectory to " + hostedWebapp, e );
            }
        }        
        
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
