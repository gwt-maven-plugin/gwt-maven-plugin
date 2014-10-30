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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Runs the project in the GWT (Classic or Super) Dev Mode for development.
 *
 * @author ccollins
 * @author cooper
 * @version $Id$
 */
@Mojo(name = "run", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES, goal = "war:exploded")
public class RunMojo
    extends AbstractGwtWebMojo
{
    /**
     * Location of the hosted-mode web application structure.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    // Parameter shared with EclipseMojo
    private File hostedWebapp;

    /**
     * The MavenProject executed by the "compile" phase
     */
    @Parameter(defaultValue = "${executedProject}")
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
     */
    @Parameter(property = "runTarget", required = true)
    private String runTarget;

    /**
     * Forked process execution timeOut (in seconds). Primary used for integration-testing.
     */
    @Parameter
    @SuppressWarnings("unused")
    private int runTimeOut;

    /**
     * Runs the embedded GWT server on the specified port.
     */
    @Parameter(defaultValue = "8888", property = "gwt.port")
    private int port;

    /**
     * Runs the code server on the specified port.
     */
    @Parameter(defaultValue = "9997", property = "gwt.codeServerPort")
    private int codeServerPort;

    /**
     * Location of the compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File buildOutputDirectory;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     * <p>
     * Can be set from command line using '-Dgwt.noserver=...'
     */
    @Parameter(defaultValue = "false", property = "gwt.noserver")
    private boolean noServer;

    /**
     * Specifies a different embedded web server to run (must implement ServletContainerLauncher)
     */
    @Parameter(property = "gwt.server")
    private String server;

    /**
     * List of System properties to pass when running the hosted mode.
     *
     * @since 1.2
     */
    @Parameter
    private Map<String, String> systemProperties;
    
    /**
     * Copies the contents of warSourceDirectory to hostedWebapp.
     * <p>
     * Can be set from command line using '-Dgwt.copyWebapp=...'
     * </p>
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "false", property = "gwt.copyWebapp")
    private boolean copyWebapp;

    /**
     * set the appengine sdk to use
     * <p>
     * Artifact will be downloaded with groupId : {@link #appEngineGroupId} 
     * and artifactId {@link #appEngineArtifactId}
     * <p>
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "1.3.8", property = "gwt.appEngineVersion")
    private String appEngineVersion;

    /**
     * <p>
     * List of {@link Pattern} jars to exclude from the classPath when running
     * dev mode
     * </p>
     * 
     * @since 2.1.0-1
     */
    @Parameter
    private List<String> runClasspathExcludes;

    /**
     * <p>
     * Location to find appengine sdk or to unzip downloaded one see {@link #appEngineVersion}
     * </p>
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "${project.build.directory}/appengine-sdk/", property = "gwt.appEngineHome")
    private File appEngineHome;

    /**
     * groupId to download appengine sdk from maven repo
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "com.google.appengine", property = "gwt.appEngineGroupId")
    private String appEngineGroupId;

    /**
     * groupId to download appengine sdk from maven repo
     * 
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "appengine-java-sdk", property = "gwt.appEngineArtifactId")
    private String appEngineArtifactId;

    /**
     * To look up Archiver/UnArchiver implementations
     * @since 2.1.0-1
     */
    @Component
    protected ArchiverManager archiverManager;

     /**
     * Set GWT shell bindAddress.
     * <p>
     * Can be set from command line using '-Dgwt.bindAddress=...'
     * @since 2.1.0-1
     */
    @Parameter(property = "gwt.bindAddress")
    private String bindAddress;

    /**
     * EXPERIMENTAL: Cache results of generators with stable output.
     * 
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.cacheGeneratorResults")
    private boolean cacheGeneratorResults;

    /**
     * The compiler's working directory for internal use (must be writeable; defaults to a system temp dir)
     *
     * @since 2.6.0-rc1
     */
    @Parameter
    private File workDir;

    /**
     * Logs to a file in the given directory, as well as graphically
     * 
     * @since 2.6.0-rc1
     */
    @Parameter
    private File logDir;

    /**
     * Specifies Java source level.
     * <p>
     * The default value depends on the JVM used to launch Maven.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "auto", property = "maven.compiler.source")
    private String sourceLevel;

    /**
     * Runs Super Dev Mode instead of classic Development Mode.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.superDevMode")
    private boolean superDevMode;

    /**
     * Compiles faster by reusing data from the previous compile.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(alias = "compilePerFile", defaultValue = "true", property = "gwt.compiler.incremental")
    private boolean incremental;

    /**
     * EXPERIMENTAL: Specifies JsInterop mode, either NONE, JS, or CLOSURE.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "NONE")
    private String jsInteropMode;

    /**
     * EXPERIMENTAL: Emit extra information allow chrome dev tools to display Java identifiers in many places instead of JavaScript functions.
     * <p>
     * Value can be one of NONE, ONLY_METHOD_NAME, ABBREVIATED or FULL.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "NONE", property = "gwt.compiler.methodNameDisplayMode")
    private String methodNameDisplayMode;

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

    public void doExecute( )
        throws MojoExecutionException, MojoFailureException
    {
        JavaCommand cmd = createJavaCommand()
            .setMainClass( "com.google.gwt.dev.DevMode" );

        if ( gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
                .addToClasspath( getGwtDevJar() );
        }

        cmd.addToClasspath( getClasspath( Artifact.SCOPE_RUNTIME ) );
        addCompileSourceArtifacts( cmd );
        addArgumentDeploy(cmd);
        addArgumentGen( cmd );
        addPersistentUnitCache(cmd);

        if ( !gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
                .addToClasspath( getGwtDevJar() );
        }

        cmd.arg( "-war", hostedWebapp.getAbsolutePath() )
            .arg( "-logLevel", getLogLevel() )
            .arg( "-port", Integer.toString( getPort() ) )
            .arg( "-codeServerPort" , Integer.toString( codeServerPort ))
            .arg( "-startupUrl", getStartupUrl() )
            .arg( noServer, "-nostartServer" )
            .arg( !cacheGeneratorResults, "-XnocacheGeneratorResults" )
            .arg( !superDevMode, "-nosuperDevMode" )
            .arg( !incremental, "-noincremental" )
            .arg( "-sourceLevel", sourceLevel );

        if ( jsInteropMode != null && jsInteropMode.length() > 0 && !jsInteropMode.equals( "NONE" ) )
        {
            cmd.arg( "-XjsInteropMode", jsInteropMode );
        }
        if ( methodNameDisplayMode != null && methodNameDisplayMode.length() > 0 && !methodNameDisplayMode.equals( "NONE" ))
        {
            cmd.arg( "-XmethodNameDisplayMode", methodNameDisplayMode );
        }

        if ( workDir != null )
        {
            cmd.arg( "-workDir", workDir.getAbsolutePath() );
        }
        if ( logDir != null )
        {
            cmd.arg( "-logdir", logDir.getAbsolutePath() );
        }

        if ( server != null )
        {
            cmd.arg( "-server", server );
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

        if ( modulePathPrefix != null && !modulePathPrefix.isEmpty() )
        {
            cmd.arg( "-modulePathPrefix" ).arg( modulePathPrefix );
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

        try
        {
            cmd.execute();
        }
        catch ( JavaCommandException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    @Override
    protected void postProcessClassPath( Collection<File> classPath )
    {
        boolean isAppEngine = "com.google.appengine.tools.development.gwt.AppEngineLauncher".equals( server );
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

    /**
     * Copied a directory structure with deafault exclusions (.svn, CVS, etc)
     *
     * @param sourceDir The source directory to copy, must not be <code>null</code>.
     * @param destDir The target directory to copy to, must not be <code>null</code>.
     * @throws java.io.IOException If the directory structure could not be copied.
     */
    private void copyDirectoryStructureIfModified(File sourceDir, File destDir)
            throws IOException {
        
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        scanner.addDefaultExcludes();
        scanner.scan();

        /*
         * NOTE: Make sure the destination directory is always there (even if empty) to support POM-less ITs.
         */
        destDir.mkdirs();
        String[] includedDirs = scanner.getIncludedDirectories();
        for ( int i = 0; i < includedDirs.length; ++i ) {
            File clonedDir = new File( destDir, includedDirs[i] );
            clonedDir.mkdirs();
        }

        String[] includedFiles = scanner.getIncludedFiles();
        for ( int i = 0; i < includedFiles.length; ++i ) {
            File sourceFile = new File(sourceDir, includedFiles[i]);
            File destFile = new File(destDir, includedFiles[i]);
            FileUtils.copyFileIfModified(sourceFile, destFile);
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
                // can't use FileUtils.copyDirectoryStructureIfModified because it does not 
                // excludes the DEFAULTEXCLUDES
                copyDirectoryStructureIfModified(warSourceDirectory, hostedWebapp);
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

        Collection<Artifact> artifacts = getProjectRuntimeArtifacts();
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

    public int getPort()
    {
        return this.port;
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
