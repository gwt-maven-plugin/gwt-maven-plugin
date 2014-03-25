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
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Abstract Support class for all GWT-related operations.
 * <p>
 * Provide methods to build classpath for GWT SDK tools.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public abstract class AbstractGwtMojo
    extends AbstractMojo
{
    /** GWT artifacts groupId */
    public static final String GWT_GROUP_ID = "com.google.gwt";

    // --- Some Maven tools ----------------------------------------------------

    /**
     * @parameter expression="${plugin.version}"
     * @required
     * @readonly
     */
    private String version;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private Collection<Artifact> pluginArtifacts;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;


    /**
     * @required
     * @readonly
     * @component
     */
    protected ClasspathBuilder classpathBuilder;

    // --- Some MavenSession related structures --------------------------------

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * The maven project descriptor
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    // --- Plugin parameters ---------------------------------------------------

    /**
     * Folder where generated-source will be created (automatically added to compile classpath).
     *
     * @parameter default-value="${project.build.directory}/generated-sources/gwt"
     * @required
     */
    private File generateDirectory;

    /**
     * Location on filesystem where GWT will write output files (-out option to GWTCompiler).
     *
     * @parameter expression="${gwt.war}" default-value="${project.build.directory}/${project.build.finalName}"
     * @alias outputDirectory
     */
    private File webappDirectory;

    /**
     * Location of the web application static resources (same as maven-war-plugin parameter)
     *
     * @parameter default-value="${basedir}/src/main/webapp"
     */
    protected File warSourceDirectory;

    /**
     * Select the place where GWT application is built. In <code>inplace</code> mode, the warSourceDirectory is used to
     * match the same use case of the {@link war:inplace
     * http://maven.apache.org/plugins/maven-war-plugin/inplace-mojo.html} goal.
     *
     * @parameter default-value="false" expression="${gwt.inplace}"
     */
    private boolean inplace;

    /**
     * The forked command line will use gwt sdk jars first in classpath.
     * see issue http://code.google.com/p/google-web-toolkit/issues/detail?id=5290
     *
     * @parameter default-value="false" expression="${gwt.gwtSdkFirstInClasspath}"
     * @since 2.1.0-1
     */
    protected boolean gwtSdkFirstInClasspath;

    /**
     * Additional classpath entries to prepend to the classpath.
     *
     * @parameter
     */
    private File[] additionalSources;

    public File getOutputDirectory()
    {
        return inplace ? warSourceDirectory : webappDirectory;
    }

    /**
     * Add classpath elements to a classpath URL set
     *
     * @param elements the initial URL set
     * @param urls the urls to add
     * @param startPosition the position to insert URLS
     * @return full classpath URL set
     * @throws MojoExecutionException some error occured
     */
    protected int addClasspathElements( Collection<?> elements, URL[] urls, int startPosition )
        throws MojoExecutionException
    {
        for ( Object object : elements )
        {
            try
            {
                if ( object instanceof Artifact )
                {
                    urls[startPosition] = ( (Artifact) object ).getFile().toURI().toURL();
                }
                else if ( object instanceof Resource )
                {
                    urls[startPosition] = new File( ( (Resource) object ).getDirectory() ).toURI().toURL();
                }
                else
                {
                    urls[startPosition] = new File( (String) object ).toURI().toURL();
                }
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException(
                                                  "Failed to convert original classpath element " + object + " to URL.",
                                                  e );
            }
            startPosition++;
        }
        return startPosition;
    }


    /**
     * Build the GWT classpath for the specified scope
     *
     * @param scope Artifact.SCOPE_COMPILE or Artifact.SCOPE_TEST
     * @return a collection of dependencies as Files for the specified scope.
     * @throws MojoExecutionException if classPath building failed
     */
    public Collection<File> getClasspath( String scope )
        throws MojoExecutionException
    {
        try
        {
            Collection<File> baseEntries = classpathBuilder.buildClasspathList( getProject(), scope, getProjectArtifacts(), isGenerator() );

            Collection<File> files;
            if ( additionalSources != null && additionalSources.length > 0 )
            {
                // Prepend additional classpath entries to the final classpath.
                files = new ArrayList<File>();
                files.addAll( Arrays.asList( additionalSources ) );
                files.addAll( baseEntries );
            }
            else
            {
                files = baseEntries;
            }

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "GWT SDK execution classpath :" );
                for ( File f : files )
                {
                    getLog().debug( "   " + f.getAbsolutePath() );
                }
            }
            return files;
        }
        catch ( ClasspathBuilderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }


    /**
     * Whether to use processed resources and compiled classes ({@code false}), or raw resources ({@code true }).
     */
    protected boolean isGenerator() {
        return false;
    }

	// FIXME move to GwtDevHelper stuff to avoid duplicates
    protected File getGwtDevJar()
        throws MojoExecutionException
    {
        checkGwtDevAsDependency();
        checkGwtUserVersion();
        return getArtifact( "com.google.gwt", "gwt-dev" ).getFile();
    }

    protected File getGwtCodeServerJar()
        throws MojoExecutionException
    {
        checkGwtUserVersion();
        return getArtifact( "com.google.gwt", "gwt-codeserver" ).getFile();
    }

    protected Artifact getArtifact( String groupId, String artifactId )
    {
        return getArtifact( groupId, artifactId, null );
    }

    protected File[] getGwtUserJar()
            throws MojoExecutionException
    {
        checkGwtUserVersion();
        Artifact gwtUserArtifact = getArtifact( "com.google.gwt", "gwt-user" );

        Set<Artifact> artifacts = new HashSet<Artifact>();
        ArtifactResolutionResult result = null;
        try
        {
            result = resolver.resolveTransitively( artifacts, gwtUserArtifact,
                    remoteRepositories, localRepository, artifactMetadataSource );
        }
        catch (ArtifactResolutionException e)
        {
            throw new MojoExecutionException( "Failed to resolve artifact", e);
        }
        catch (ArtifactNotFoundException e)
        {
            throw new MojoExecutionException( "Failed to resolve artifact", e);
        }

        Collection<Artifact> resolved = result.getArtifacts();
        int i = 0;
        File[] files = new File[ resolved.size() + 1 ];
        files[i++] = gwtUserArtifact.getFile();
        for ( Artifact artifact : resolved )
        {
            files[i++] = artifact.getFile();
        }

        return files;
    }

    protected Artifact getArtifact( String groupId, String artifactId, String classifier )
    {
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() ) )
            {
                if ( classifier != null && classifier.equals( artifact.getClassifier() ) )
                {
                    return artifact;
                }
                if ( classifier == null && artifact.getClassifier() == null )
                {
                    return artifact;
                }
            }
        }
        getLog().error( "Failed to retrieve " + groupId + ":" + artifactId + ":" + classifier );
        return null;
    }

    /**
     * TODO remove !
     * Check that gwt-dev is not define in dependencies : this can produce version conflicts with other dependencies, as
     * gwt-dev is a "uber-jar" with some commons-* and jetty libs inside.
     */
    private void checkGwtDevAsDependency()
    {
        for ( Iterator iterator = getProject().getArtifacts().iterator(); iterator.hasNext(); )
        {
            Artifact artifact = (Artifact) iterator.next();
            if ( GWT_GROUP_ID.equals( artifact.getGroupId() )
                && "gwt-dev".equals( artifact.getArtifactId() )
                && !SCOPE_TEST.equals(  artifact.getScope() ) )
            {
                getLog().warn( "Don't declare gwt-dev as a project dependency. This may introduce complex dependency conflicts" );
            }
        }
    }

    /**
     * Check gwt-user dependency matches plugin version
     */
    private void checkGwtUserVersion() throws MojoExecutionException
    {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream( "org/codehaus/mojo/gwt/mojoGwtVersion.properties" );
        Properties properties = new Properties();
        try
        {
            properties.load( inputStream );

        }
        catch (IOException e)
        {
            throw new MojoExecutionException( "Failed to load plugin properties", e );
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
        for ( Iterator iterator = getProject().getCompileArtifacts().iterator(); iterator.hasNext(); )
        {
            Artifact artifact = (Artifact) iterator.next();
            if ( GWT_GROUP_ID.equals( artifact.getGroupId() )
                 && "gwt-user".equals( artifact.getArtifactId() ) )
            {
                String mojoGwtVersion = properties.getProperty( "gwt.version" );
                //ComparableVersion with an up2date maven version
                ArtifactVersion mojoGwtArtifactVersion = new DefaultArtifactVersion( mojoGwtVersion );
                ArtifactVersion userGwtArtifactVersion = new DefaultArtifactVersion( artifact.getVersion() );
                if ( userGwtArtifactVersion.compareTo( mojoGwtArtifactVersion ) < 0 )
                {
                    getLog().warn( "Your project declares dependency on gwt-user " + artifact.getVersion()
                                       + ". This plugin is designed for at least gwt version " + mojoGwtVersion );
                }
                break;
            }
        }
    }

    protected Artifact resolve( String groupId, String artifactId, String version, String type, String classifier )
        throws MojoExecutionException
    {
        // return project.getArtifactMap().get( groupId + ":" + artifactId );

        Artifact artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
        try
        {
            resolver.resolve(artifact, remoteRepositories, localRepository);
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "artifact not found - " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "artifact resolver problem - " + e.getMessage(), e );
        }
        return artifact;
    }

    /**
     * @param path file to add to the project compile directories
     */
    protected void addCompileSourceRoot( File path )
    {
        getProject().addCompileSourceRoot( path.getAbsolutePath() );
    }

    /**
     * @return the project
     */
    public MavenProject getProject()
    {
        return project;
    }


    public ArtifactRepository getLocalRepository()
    {
        return this.localRepository;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return this.remoteRepositories;
    }

    public File getGenerateDirectory()
    {
        if ( !generateDirectory.exists() )
        {
            getLog().debug( "Creating target directory " + generateDirectory.getAbsolutePath() );
            generateDirectory.mkdirs();
        }
        return generateDirectory;
    }

    @SuppressWarnings( "unchecked" )
    public Set<Artifact> getProjectArtifacts()
    {
        return project.getArtifacts();
    }

    @SuppressWarnings( "unchecked" )
    public Set<Artifact> getProjectRuntimeArtifacts()
    {
        Set<Artifact> artifacts = new HashSet<Artifact>();
        for (Artifact projectArtifact : (Collection<Artifact>) project.getArtifacts() )
        {
            String scope = projectArtifact.getScope();
            if ( SCOPE_RUNTIME.equals( scope )
              || SCOPE_COMPILE.equals( scope ) )
            {
                artifacts.add( projectArtifact );
            }

        }
        return artifacts;
    }


}
