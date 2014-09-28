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
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Runs GWT modules with Super Dev Mode.
 *
 * @goal run-codeserver
 * @execute phase=process-classes
 * @requiresDirectInvocation
 * @requiresDependencyResolution compile
 * @description Runs the project in GWT SuperDevMode for development.
 * @author t.broyer
 * @since 2.5.0-rc1
 */
public class SuperDevModeMojo extends AbstractGwtShellMojo
{

    /**
     * Set SuperDevMode's bindAddress.
     * <p>
     * Can be set from command line using '-Dgwt.bindAddress=...'
     *
     * @parameter expression="${gwt.bindAddress}"
     */
    private String bindAddress;

    /**
     * The port where the code server will run.
     *
     * @parameter expression="${gwt.codeServerPort}"
     */
    private Integer codeServerPort;

    /**
     * The root of the directory tree where the code server will write compiler output.
     * If not supplied, a temporary directory will be used.
     *
     * @parameter
     */
    private File codeServerWorkDir;

    /**
     * Precompile modules.
     * 
     * @parameter default-value="true" expression="${gwt.codeServer.precompile}"
     * @since 2.6.0-rc1
     */
    private boolean precompile;

    /**
     * EXPERIMENTAL: Avoid adding implicit dependencies on "client" and "public"
     * for modules that don't define any dependencies.
     * 
     * @parameter default-value="false" expression="${gwt.compiler.enforceStrictResources}"
     * @since 2.6.0-rc1
     */
    private boolean enforceStrictResources;

    /**
     * Specifies Java source level.
     *
     * @parameter expression="${maven.compiler.source}" default-value="auto"
     * @since 2.6.0-rc1
     */
    private String sourceLevel;

    /**
     * Stop compiling if a module has a Java file with a compile error, even if unused.
     * <p>
     * Can be set from command line using '-Dgwt.compiler.strict=true'.
     * 
     * @parameter alias="strict" default-value="false" expression="${gwt.compiler.strict}"
     * @since 2.7.0-rc1
     */
    private boolean failOnError;

    /**
     * Compiles faster by reusing data from the previous compile.
     * 
     * @parameter alias="compilePerFile" default-value="true" expression="${gwt.compiler.incremental}"
     * @since 2.7.0-rc1
     */
    private boolean incremental;

    /**
     * EXPERIMENTAL: Specifies JsInterop mode, either NONE, JS, or CLOSURE.
     * 
     * @parameter default-value="NONE
     * @since 2.7.0-rc1
     */
    private String jsInteropMode;

    /**
     * The MavenProject executed by the "process-classes" phase.
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    @Override
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        JavaCommand cmd = new JavaCommand( "com.google.gwt.dev.codeserver.CodeServer" );

        if ( gwtSdkFirstInClasspath )
        {
            cmd.withinClasspath( getGwtUserJar() )
                .withinClasspath( getGwtDevJar() )
                .withinClasspath( getGwtCodeServerJar() );
        }

        cmd.withinScope( Artifact.SCOPE_COMPILE );
        addCompileSourceArtifacts( cmd );
        addPersistentUnitCache(cmd);

        if ( !gwtSdkFirstInClasspath )
        {
            cmd.withinClasspath( getGwtUserJar() )
                .withinClasspath( getGwtDevJar() )
                .withinClasspath( getGwtCodeServerJar() );
        }

        cmd.arg( !precompile, "-noprecompile" );
        cmd.arg( enforceStrictResources, "-XenforceStrictResources" );
        cmd.arg( "-sourceLevel", sourceLevel );
        cmd.arg( failOnError, "-failOnError" );
        cmd.arg( !incremental, "-noincremental" );

        if ( jsInteropMode != null && jsInteropMode.length() > 0 )
        {
            cmd.arg( "-XjsInteropMode", jsInteropMode );
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

        for ( String module : getModules() )
        {
            cmd.arg( module );
        }

        cmd.execute();
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

