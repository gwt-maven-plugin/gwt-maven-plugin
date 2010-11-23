package org.codehaus.mojo.gwt.shell;

/*
 * CompileMojo.java
 *
 * Created on January 13, 2007, 11:42 AM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 */

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.util.StringUtils;

/**
 * Invokes the GWTCompiler for the project source.
 *
 * @phase prepare-package
 * @goal compile
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author cooper
 * @author ccollins
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
// @phase prepare-package should be even better to avoid unecessary gwt:compile when used with m2eclipse
public class CompileMojo
    extends AbstractGwtShellMojo
{

    /**
     * @parameter expression="${gwt.compiler.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Don't try to detect if GWT compilation is up-to-date and can be skipped.
     *
     * @parameter expression="${gwt.compiler.force}" default-value="false"
     */
    private boolean force;

    /**
     * On GWT 1.6+, number of parallel processes used to compile GWT premutations. Defaults to
     * platform available processors number.
     * @parameter
     */
    private int localWorkers;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

    /**
     * Ask GWT to create the Story of Your Compile (SOYC)
     * <p>
     * Can be unset from command line using '-Dgwt.compiler.soyc=false'.
     * </p>
     * @parameter expression="${gwt.compiler.soyc}" default-value="true"
     */
    private String soyc;

    /**
     * Artifacts to be included as source-jars in GWTCompiler Classpath. Removes the restriction that source code must
     * be bundled inside of the final JAR when dealing with external utility libraries not designed exclusivelly for
     * GWT. The plugin will download the source.jar if necessary.
     * <p>
     * This option is a workaround to avoid packaging sources inside the same JAR when splitting and application into
     * modules. A smaller JAR can then be used on server classpath and distributed without sources (that may not be
     * desirable).
     *
     * @parameter
     */
    private String[] compileSourcesArtifacts;

    /**
     * Logs output in a graphical tree view.
     * <p>
     * Can be set from command line using '-Dgwt.treeLogger=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.treeLogger}"
     */
    private boolean treeLogger;

    /**
     * EXPERIMENTAL: Disables some java.lang.Class methods (e.g. getName()).
     * <p>
     * Can be set from command line using '-Dgwt.disableClassMetadata=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.disableClassMetadata}"
     */
    private boolean disableClassMetadata;

    /**
     * EXPERIMENTAL: Disables run-time checking of cast operations.
     * <p>
     * Can be set from command line using '-Dgwt.disableCastChecking=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.disableCastChecking}"
     */
    private boolean disableCastChecking;

    /**
     * Validate all source code, but do not compile.
     * <p>
     * Can be set from command line using '-Dgwt.validateOnly=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.validateOnly}"
     */
    private boolean validateOnly;

    /**
     * Enable faster, but less-optimized, compilations.
     * <p>
     * Can be set from command line using '-Dgwt.draftCompile=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.draftCompile}"
     */
    private boolean draftCompile;

    /**
     * The directory into which extra, non-deployed files will be written.
     *
     * @parameter default-value="${project.build.directory}/extra"
     */
    private File extra;

    /**
     * The temp directory is used for temporary compiled files (defaults is system temp directory).
     *
     * @parameter
     */
    private File workDir;
    
    /**
     * add -extra parameter to the compiler command line
     * <p>
     * Can be set from command line using '-Dgwt.extraParam=true'.
     * </p>
     * @parameter default-value="false" expression="${gwt.extraParam}"
     * @since 2.1.1
     */
    private boolean extraParam;

    public void doExecute( )
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip || "pom".equals( getProject().getPackaging() ) )
        {
            getLog().info( "GWT compilation is skipped" );
            return;
        }

        if ( !this.getOutputDirectory().exists() )
        {
            this.getOutputDirectory().mkdirs();
        }

        compile( getModules() );
    }

    private void compile( String[] modules )
        throws MojoExecutionException
    {
        boolean upToDate = true;


        
        JavaCommand cmd = new JavaCommand( "com.google.gwt.dev.Compiler" )
            .withinScope( Artifact.SCOPE_COMPILE )
            .withinClasspath( getGwtUserJar() )
            .withinClasspath( getGwtDevJar() )
            .arg( "-gen", getGen().getAbsolutePath() )
            .arg( "-logLevel", getLogLevel() )
            .arg( "-style", getStyle() )
            .arg( "-war", getOutputDirectory().getAbsolutePath() )
            .arg( "-localWorkers", String.valueOf( getLocalWorkers() ) )
            // optional advanced arguments
            .arg( enableAssertions, "-ea" )
            .arg( draftCompile, "-draftCompile" )
            .arg( validateOnly, "-validateOnly" )
            .arg( treeLogger, "-treeLogger" )
            .arg( disableClassMetadata, "-XdisableClassMetadata" )
            .arg( disableCastChecking, "-XdisableCastChecking" );
        
        if ( extraParam )
        {
            if ( !extra.exists() )
            {
                extra.mkdirs();
            }
            cmd.arg( "-extra" ).arg( extra.getAbsolutePath() );
        }
        
        addCompileSourceArtifacts( cmd );

        if ( workDir != null )
        {
            cmd.arg( "-workDir" )
               .arg( String.valueOf( workDir ) );
        }

        addSOYC( cmd );

        for ( String target : modules )
        {
            if ( !compilationRequired( target, getOutputDirectory() ) )
            {
                continue;
            }
            cmd.arg( target );
            upToDate = false;
        }
        if ( !upToDate )
        {
            cmd.execute();
        }
    }

    /**
     * Add sources.jar artifacts for project dependencies listed as compileSourcesArtifacts. This is a GWT hack to avoid
     * packaging java source files into JAR when sharing code between server and client. Typically, some domain model
     * classes or business rules may be packaged as a separate Maven module. With GWT packaging this requires to
     * distribute such classes with code, that may not be desirable.
     * <p>
     * The hack can also be used to include utility code from external librariries that may not have been designed for
     * GWT.
     */
    private void addCompileSourceArtifacts( JavaCommand cmd )
        throws MojoExecutionException
    {
        if ( compileSourcesArtifacts == null )
        {
            return;
        }
        for ( String include : compileSourcesArtifacts )
        {
            List<String> parts = new ArrayList<String>();
            parts.addAll( Arrays.asList( include.split( ":" ) ) );
            if ( parts.size() == 2 )
            {
                // type is optional as it will mostly be "jar"
                parts.add( "jar" );
            }
            String dependencyId = StringUtils.join( parts.iterator(), ":" );
            boolean found = false;

            for ( Artifact artifact : getProjectArtifacts() )
            {
                getLog().debug( "compare " + dependencyId + " with " + artifact.getDependencyConflictId() );
                if ( artifact.getDependencyConflictId().equals( dependencyId ) )
                {
                    getLog().debug( "Add " + dependencyId + " sources.jar artifact to compile classpath" );
                    Artifact sources =
                        resolve( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                            "jar", "sources" );
                    cmd.withinClasspath( sources.getFile() );
                    found = true;
                    break;
                }
            }
            if ( !found )
                getLog().warn(
                    "Declared compileSourcesArtifact was not found in project dependencies " + dependencyId );
        }
    }

    private void addSOYC( JavaCommand cmd )
    {
        if ( soyc != null && Boolean.valueOf( soyc ).booleanValue() == false )
        {
            getLog().debug( "SOYC has been disabled by user" );
        }
        else
        {
            cmd.arg( "-soyc" );
            // we force -extra param to the cli even if not asked if not soyc will failed 
            if ( !extraParam )
            {
                if ( !extra.exists() )
                {
                    extra.mkdirs();
                }
                cmd.arg( "-extra" ).arg( extra.getAbsolutePath() );
            }            
        }
    }

    private int getLocalWorkers()
    {
        if ( localWorkers > 0 )
        {
            return localWorkers;
        }
        // workaround to GWT issue 4031 whith IBM JDK
        // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=4031
        if ( System.getProperty( "java.vendor" ).startsWith( "IBM" ) )
        {
            getLog().info( "Build is using IBM JDK, localWorkers set to 1 as workaround to gwt#4031" );
            return 1;
        }
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Try to find out, if there are stale sources. If aren't some, we don't have to compile... ...this heuristic
     * doesn't take into account, that there could be updated dependencies. But for this case, as 'clean compile' could
     * be executed which would force a compilation.
     *
     * @param module Name of the GWT module to compile
     * @param output Output path
     * @return true if compilation is required (i.e. stale sources are found)
     * @throws MojoExecutionException When sources scanning fails
     * @author Alexander Gordt
     */
    private boolean compilationRequired( String module, File output )
        throws MojoExecutionException
    {
        GwtModule gwtModule = readModule( module );
        if ( gwtModule.getEntryPoints().size() == 0 )
        {
            getLog().debug( gwtModule.getName() + " has no EntryPoint - compilation skipped" );
            // No entry-point, this is an utility module : compiling this one will fail
            // with '[ERROR] Module has no entry points defined'
            return false;
        }

        if ( force )
        {
            return true;
        }

        String modulePath = gwtModule.getPath();
        String outputTarget = modulePath + "/" + modulePath + ".nocache.js";

        // Require compilation if no js file present in target.
        if ( !new File( output, outputTarget ).exists() )
        {
            return true;
        }

        // js file allreay exists, but may not be up-to-date with project source files
        SingleTargetSourceMapping singleTargetMapping = new SingleTargetSourceMapping( ".java", outputTarget );
        StaleSourceScanner scanner = new StaleSourceScanner();
        scanner.addSourceMapping( singleTargetMapping );

        SingleTargetSourceMapping gwtModuleMapping = new SingleTargetSourceMapping( ".gwt.xml", outputTarget );
        scanner.addSourceMapping( gwtModuleMapping );

        Collection<File> compileSourceRoots = new HashSet<File>();
        classpathBuilder.addSourcesWithActiveProjects( getProject(), compileSourceRoots, SCOPE_COMPILE );
        classpathBuilder.addResourcesWithActiveProjects( getProject(), compileSourceRoots, SCOPE_COMPILE );
        for ( File sourceRoot : compileSourceRoots )
        {
            if ( !sourceRoot.isDirectory() )
            {
                continue;
            }
            try
            {
                if ( !scanner.getIncludedSources( sourceRoot, output ).isEmpty() )
                {
                    getLog().debug( "found stale source in " + sourceRoot + " compared with " + output );
                    return true;
                }
            }
            catch ( InclusionScanException e )
            {
                throw new MojoExecutionException( "Error scanning source root: \'" + sourceRoot + "\' "
                    + "for stale files to recompile.", e );
            }
        }
        getLog().info( module + " is up to date. GWT compilation skipped" );
        return false;
    }
}
