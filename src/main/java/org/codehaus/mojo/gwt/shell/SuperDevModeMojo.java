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

import java.io.File;

/**
 * EXPERIMENTAL: Runs GWT modules with Super Dev Mode.
 *
 * @goal run-codeserver
 * @execute phase=process-classes
 * @requiresDirectInvocation
 * @requiresDependencyResolution compile
 * @description EXPERIMENTAL: Runs the project in GWT SuperDevMode for development.
 * @author t.broyer
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
     * @parameter
     */
    private Integer port;

    /**
     * The root of the directory tree where the code server will write compiler output.
     * If not supplied, a temporary directory will be used.
     *
     * @parameter
     */
    private File codeServerWorkDir;

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

        // FIXME: add it when CodeServer has it
        // cmd.arg( "-XdisableUpdateCheck" )

        if ( bindAddress != null && bindAddress.length() > 0 )
        {
            cmd.arg( "-bindAddress" ).arg( bindAddress );
        }
        if ( port != null )
        {
            cmd.arg( "-port", String.valueOf( port ) );
        }
        if ( codeServerWorkDir != null )
        {
            cmd.arg( "-workDir", codeServerWorkDir.getAbsolutePath() );
        }

        for ( String module : getModules() )
        {
            cmd.arg( module );
        }

        cmd.execute();
    }
}

