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

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Util to consolidate classpath manipulation stuff in one place.
 * 
 * @author ccollins
 * @version $Id$
 * @plexus.component role="org.codehaus.mojo.gwt.ClasspathBuilder"
 */
public class ClasspathBuilder
    extends AbstractLogEnabled
{

    /**
     * Build classpath list using either gwtHome (if present) or using *project* dependencies. Note that this is ONLY
     * used for the script/cmd writers (so the scopes are not for the compiler, or war plugins, etc). This is required
     * so that the script writers can get the dependencies they need regardless of the Maven scopes (still want to use
     * the Maven scopes for everything else Maven, but for GWT-Maven we need to access deps differently - directly at
     * times).
     *
     * @param project The maven project the Mojo is running for
     * @param artifacts the project artifacts (all scopes)
     * @param scope artifact scope to use
     * @return file collection for classpath
     * @throws MojoExecutionException 
     */
    @SuppressWarnings( "unchecked" )
    public Collection<File> buildClasspathList( final MavenProject project, final String scope,
                                                Set<Artifact> artifacts )
        throws ClasspathBuilderException
    {
        getLogger().debug( "establishing classpath list (scope = " + scope + ")" );

        Set<File> items = new LinkedHashSet<File>();

        // Note : Don't call addSourceWithActiveProject as a GWT dependency MUST be a valid GWT library module :
        // * include java sources in the JAR as resources
        // * define a gwt.xml module file to declare the required inherits
        // addSourceWithActiveProject would make some java sources available to GWT compiler that should not be accessible in
        // a non-reactor build, making the build less deterministic and encouraging bad design.

        addSources( items, project.getCompileSourceRoots() );
        addResources( items, project.getResources() );
        items.add( new File( project.getBuild().getOutputDirectory() ) );

        // Use our own ClasspathElements fitering, as for RUNTIME we need to include PROVIDED artifacts,
        // that is not the default Maven policy, as RUNTIME is used here to build the GWTShell execution classpath

        if ( scope.equals( SCOPE_TEST ) )
        {
            addSources( items, project.getTestCompileSourceRoots() );
            addResources( items, project.getTestResources() );
            items.add( new File( project.getBuild().getTestOutputDirectory() ) );

            // Add all project dependencies in classpath
            for ( Artifact artifact : artifacts )
            {
                items.add( artifact.getFile() );
            }
        }
        else if ( scope.equals( SCOPE_COMPILE ) )
        {
            // Add all project dependencies in classpath
            getLogger().debug( "candidate artifacts : " + artifacts.size() );
            for ( Artifact artifact : artifacts )
            {
                String artifactScope = artifact.getScope();
                if ( SCOPE_COMPILE.equals( artifactScope ) || SCOPE_PROVIDED.equals( artifactScope )
                    || SCOPE_SYSTEM.equals( artifactScope ) )
                {
                    items.add( artifact.getFile() );
                }
            }
        }
        else if ( scope.equals( SCOPE_RUNTIME ) )
        {
            // Add all dependencies BUT "TEST" as we need PROVIDED ones to setup the execution
            // GWTShell that is NOT a full JEE server
            for ( Artifact artifact : artifacts )
            {
                getLogger().debug( "candidate artifact : " + artifact );
                if ( !artifact.getScope().equals( SCOPE_TEST ) && artifact.getArtifactHandler().isAddedToClasspath() )
                {
                    items.add( artifact.getFile() );
                }
            }
        }
        else
        {
            throw new ClasspathBuilderException( "unsupported scope " + scope );
        }
        return items;
    }

    /**
     * Add all sources and resources also with active (maven reactor active) referenced project sources and resources.
     *
     * @param project
     * @param items
     * @param scope
     */
    public void addSourcesWithActiveProjects( final MavenProject project, final Collection<File> items,
                                              final String scope )
    {
        final List<Artifact> scopeArtifacts = getScopeArtifacts( project, scope );

        addSources( items, getSourceRoots( project, scope ) );

        for ( Artifact artifact : scopeArtifacts )
        {
            String projectReferenceId =
                getProjectReferenceId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
            MavenProject refProject = (MavenProject) project.getProjectReferences().get( projectReferenceId );
            if ( refProject != null )
            {
                addSources( items, getSourceRoots( refProject, scope ) );
            }
        }
    }

    /**
     * Add all sources and resources also with active (maven reactor active) referenced project sources and resources.
     *
     * @param project
     * @param items
     * @param scope
     */
    public void addResourcesWithActiveProjects( final MavenProject project, final Collection<File> items,
                                                final String scope )
    {
        final List<Artifact> scopeArtifacts = getScopeArtifacts( project, scope );

        addResources( items, getResources( project, scope ) );

        for ( Artifact artifact : scopeArtifacts )
        {
            String projectReferenceId =
                getProjectReferenceId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
            MavenProject refProject = (MavenProject) project.getProjectReferences().get( projectReferenceId );
            if ( refProject != null )
            {
                addResources( items, getResources( refProject, scope ) );
            }
        }
    }

    /**
     * Get artifacts for specific scope.
     *
     * @param project
     * @param scope
     * @return
     */
    @SuppressWarnings( "unchecked" )
    private List<Artifact> getScopeArtifacts( final MavenProject project, final String scope )
    {
        if ( SCOPE_COMPILE.equals( scope ) )
        {
            return project.getCompileArtifacts();
        }
        if ( SCOPE_RUNTIME.equals( scope ) )
        {
            return project.getRuntimeArtifacts();
        }
        else if ( SCOPE_TEST.equals( scope ) )
        {
            return project.getTestArtifacts();
        }
        else
        {
            throw new RuntimeException( "Not allowed scope " + scope );
        }
    }

    /**
     * Get source roots for specific scope.
     *
     * @param project
     * @param scope
     * @return
     */
    @SuppressWarnings( "unchecked" )
    private List<String> getSourceRoots( final MavenProject project, final String scope )
    {
        if ( SCOPE_COMPILE.equals( scope ) || SCOPE_RUNTIME.equals( scope ) )
        {
            return project.getCompileSourceRoots();
        }
        else if ( SCOPE_TEST.equals( scope ) )
        {
            List<String> sourceRoots = new ArrayList<String>();
            sourceRoots.addAll( project.getTestCompileSourceRoots() );
            sourceRoots.addAll( project.getCompileSourceRoots() );
            return sourceRoots;
        }
        else
        {
            throw new RuntimeException( "Not allowed scope " + scope );
        }
    }

    /**
     * Get resources for specific scope.
     *
     * @param project
     * @param scope
     * @return
     */
    @SuppressWarnings( "unchecked" )
    private List<Resource> getResources( final MavenProject project, final String scope )
    {
        if ( SCOPE_COMPILE.equals( scope ) || SCOPE_RUNTIME.equals( scope ) )
        {
            return project.getResources();
        }
        else if ( SCOPE_TEST.equals( scope ) )
        {
            List<Resource> resources = new ArrayList<Resource>();
            resources.addAll( project.getTestResources() );
            resources.addAll( project.getResources() );
            return resources;
        }
        else
        {
            throw new RuntimeException( "Not allowed scope " + scope );
        }
    }

    /**
     * Add source path and resource paths of the project to the list of classpath items.
     *
     * @param items Classpath items.
     * @param sourceRoots
     */
    private void addSources( final Collection<File> items, final Collection<String> sourceRoots )
    {
        for ( String path : sourceRoots )
        {
            items.add( new File( path ) );
        }
    }

    /**
     * Add source path and resource paths of the project to the list of classpath items.
     *
     * @param items Classpath items.
     * @param resources
     */
    private void addResources( final Collection<File> items, final Collection<Resource> resources )
    {
        for ( Resource resource : resources )
        {
            items.add( new File( resource.getDirectory() ) );
        }
    }

    /**
     * Cut from MavenProject.java
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    private String getProjectReferenceId( final String groupId, final String artifactId, final String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }
}
