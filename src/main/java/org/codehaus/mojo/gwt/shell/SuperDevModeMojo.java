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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Runs GWT modules with Super Dev Mode.
 *
 * @author t.broyer
 * @since 2.5.0-rc1
 */
@Mojo(name = "run-codeserver", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class SuperDevModeMojo extends AbstractGwtShellMojo
{

    /**
     * Set SuperDevMode's bindAddress.
     * <p>
     * Can be set from command line using '-Dgwt.bindAddress=...'
     */
    @Parameter(property = "gwt.bindAddress")
    private String bindAddress;

    /**
     * The port where the code server will run.
     */
    @Parameter(property = "gwt.codeServerPort")
    private Integer codeServerPort;

    /**
     * The root of the directory tree where the code server will write compiler output.
     * If not supplied, a temporary directory will be used.
     */
    @Parameter
    private File codeServerWorkDir;

    /**
     * Precompile modules.
     * 
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.codeServer.precompile")
    private boolean precompile;

    /**
     * EXPERIMENTAL: Avoid adding implicit dependencies on "client" and "public"
     * for modules that don't define any dependencies.
     * 
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "false", property = "gwt.compiler.enforceStrictResources")
    private boolean enforceStrictResources;

    /**
     * Specifies Java source level.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "auto", property = "maven.compiler.source")
    private String sourceLevel;

    /**
     * Stop compiling if a module has a Java file with a compile error, even if unused.
     * <p>
     * Can be set from command line using '-Dgwt.compiler.strict=true'.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(alias = "struct", defaultValue = "false", property = "gwt.compiler.strict")
    private boolean failOnError;

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
     * An output directory where files for launching Super Dev Mode will be written. (Optional.)
     * 
     * @since 2.7.0
     */
    @Parameter(property = "gwt.codeServer.launcherDir")
    private File launcherDir;

    /**
     * The MavenProject executed by the "process-classes" phase.
     */
    @Parameter(defaultValue = "${executedProject}")
    private MavenProject executedProject;

    @Override
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        JavaCommand cmd = createJavaCommand()
            .setMainClass( "com.google.gwt.dev.codeserver.CodeServer" );

        if ( gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
                .addToClasspath( getGwtDevJar() );
        }

        cmd.addToClasspath( getClasspath( Artifact.SCOPE_COMPILE ) );
        addCompileSourceArtifacts( cmd );
        addPersistentUnitCache(cmd);

        if ( !gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
                .addToClasspath( getGwtDevJar() );
        }

        cmd.arg( "-logLevel", getLogLevel() );
        cmd.arg( !precompile, "-noprecompile" );
        cmd.arg( enforceStrictResources, "-XenforceStrictResources" );
        cmd.arg( "-sourceLevel", sourceLevel );
        cmd.arg( failOnError, "-failOnError" );
        cmd.arg( !incremental, "-noincremental" );

        if ( jsInteropMode != null && jsInteropMode.length() > 0 && !jsInteropMode.equals( "NONE" ) )
        {
            cmd.arg( "-XjsInteropMode", jsInteropMode );
        }
        if ( methodNameDisplayMode != null && methodNameDisplayMode.length() > 0 && !methodNameDisplayMode.equals( "NONE" ))
        {
            cmd.arg( "-XmethodNameDisplayMode", methodNameDisplayMode );
        }
        if ( bindAddress != null && bindAddress.length() > 0 )
        {
            cmd.arg( "-bindAddress", bindAddress );
        }
        if ( codeServerPort != null )
        {
            cmd.arg( "-port", String.valueOf( codeServerPort ) );
        }
        if ( codeServerWorkDir != null )
        {
            codeServerWorkDir.mkdirs();
            cmd.arg( "-workDir", codeServerWorkDir.getAbsolutePath() );
        }

        if ( launcherDir != null )
        {
            cmd.arg( "-launcherDir", launcherDir.getAbsolutePath() );
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

