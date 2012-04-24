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
import java.util.ArrayList;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * 
 * @author Robert Scholte
 *
 */
public abstract class AbstractGwtMojoTestCase
    extends AbstractMojoTestCase
{

    /**
     * 
     * @param pDir path to the pomDirectory, relative to the baseDir of this project
     * @return the GwtMojo corresponding with the goal
     * @throws Exception
     */
    protected ArtifactResolver newMojo( String pDir )
        throws Exception
    {
        File testRoot = new File( getBasedir(), pDir );
        ArtifactResolver vm = (ArtifactResolver) lookupMojo( getGoal(), new File( testRoot, "pom.xml" ) );

        MavenProject project = new MavenProjectStub();
        //addCompileSourceRoot will generate singletonList if null, which doesn't support a second add.
        setVariableValueToObject( project, "compileSourceRoots", new ArrayList<String>() );
        project.addCompileSourceRoot( new File( testRoot, "src/main/java" ).getAbsolutePath() );

        //required field of mojo
        setVariableValueToObject( vm, "project", project );

        return vm;
    }

    protected abstract String getGoal();
}
