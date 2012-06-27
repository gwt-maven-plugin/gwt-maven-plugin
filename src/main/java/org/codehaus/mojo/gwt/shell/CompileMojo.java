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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.utils.DefaultGwtModuleReader;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.util.StringUtils;

/**
 * Invokes the GWTCompiler for the project source.
 * See compiler options :
 * http://code.google.com/intl/fr-FR/webtoolkit/doc/latest/DevGuideCompilingAndDebugging.html#DevGuideCompilerOptions
 *
 * @phase prepare-package
 * @goal compile
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author cooper
 * @author ccollins
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 */
public class CompileMojo
    extends AbstractGwtShellMojo
{

    /**
     * @parameter expression="${gwt.compiler.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Don't try to detect if GWT compilation is up-to-date and can be skipped.
     * <p>
     * Can be set from command line using '-Dgwt.compiler.force=true'.
     * </p>
     * @parameter expression="${gwt.compiler.force}" default-value="false"
     */
    private boolean force;

    /**
     * On GWT 1.6+, number of parallel processes used to compile GWT premutations. Defaults to
     * platform available processors number.
     * 
     * <p>
     * Can be unset from command line using '-Dgwt.compiler.localWorkers=n'.
     * </p>
     * 
     * @parameter expression="${gwt.compiler.localWorkers}"
     */
    private int localWorkers;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

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
     * EXPERIMENTAL: Disables code-splitting.
     * <p>
     * Can be set from command line using '-Dgwt.disableRunAsync=true'.
     * </p>
     *
     * @parameter default-value="false" expression="${gwt.disableRunAsync}"
     */
    private boolean disableRunAsync;

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
     * <p>
     * This is equivalent to '-Dgwt.compiler.optimizationLevel=0 -Dgwt.compiler.disableAggressiveOptimization=true'.
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
     * @since 2.1.0-1
     */
    private boolean extraParam;
    
    /**
     * add -compileReport parameter to the compiler command line
     * <p>
     * Can be set from command line using '-Dgwt.compiler.compileReport=true'.
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.compileReport}"
     * @since 2.1.0-1
     */    
    private boolean compileReport;
    
    /**
     * add -optimize parameter to the compiler command line the value must be between 0 and 9
     * by default -1 so no arg to the compiler
     * <p>
     * Can be set from command line using '-Dgwt.compiler.optimizationLevel=n'.
     * </p>
     * @parameter default-value="-1" expression="${gwt.compiler.optimizationLevel}"
     * @since 2.1.0-1
     */    
    private int optimizationLevel;    
    
    /**
     * add -XsoycDetailed parameter to the compiler command line
     * <p>
     * Can be set from command line using '-Dgwt.compiler.soycDetailed=true'.
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.soycDetailed}"
     * @since 2.1.0-1
     */    
    private boolean soycDetailed;    
    
    
    /**
     * add -strict parameter to the compiler command line
     * 
     * <p>
     * Can be set from command line using '-Dgwt.compiler.strict=true'.
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.strict}"
     * @since 2.1.0-1
     */    
    private boolean strict;     
    
    /**
     * EXPERIMENTAL: add -XenableClosureCompiler parameter to the compiler command line
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.enableClosureCompiler=true'
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.enableClosureCompiler}"
     * @since 2.5.0
     */
    private boolean enableClosureCompiler;

    /**
     * EXPERIMENTAL: add -XdisableAggressiveOptimization parameter to the compiler command line
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.disableAggressiveOptimization=true'
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.disableAggressiveOptimization}"
     * @since 2.5.0
     */
    private boolean disableAggressiveOptimization;

    /**
     * EXPERIMENTAL: add -XcompilerMetrics parameter to the compiler command line
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.compilerMetrics=true'
     * </p>
     * @parameter default-value="false" expression="${gwt.compiler.compilerMetrics}"
     * @since 2.5.0
     */
    private boolean compilerMetrics;

    /**
     * EXPERIMENTAL: add -XfragmentCount parameter to the compiler command line
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.fragmentCount=n'
     * </p>
     * @parameter default-value="-1" expression="${gwt.compiler.fragmentCount}"
     * @since 2.5.0
     */
    private int fragmentCount;

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

        JavaCommand cmd = new JavaCommand( "com.google.gwt.dev.Compiler" );
        if ( gwtSdkFirstInClasspath )
        {
            cmd.withinClasspath( getGwtUserJar() )
               .withinClasspath( getGwtDevJar() );
        }
        cmd.withinScope( Artifact.SCOPE_COMPILE );

        if ( !gwtSdkFirstInClasspath )
        {
            cmd.withinClasspath( getGwtUserJar() )
               .withinClasspath( getGwtDevJar() );
        }

        cmd.arg( "-XdisableUpdateCheck" )
            .arg( "-logLevel", getLogLevel() )
            .arg( "-style", getStyle() )
            .arg( "-war", getOutputDirectory().getAbsolutePath() )
            .arg( "-localWorkers", String.valueOf( getLocalWorkers() ) )
            // optional advanced arguments
            .arg( enableAssertions, "-ea" ).arg( draftCompile, "-draftCompile" )
            .arg( validateOnly, "-validateOnly" ).arg( treeLogger, "-treeLogger" )
            .arg( disableClassMetadata, "-XdisableClassMetadata" )
            .arg( disableCastChecking, "-XdisableCastChecking" )
            .arg( disableRunAsync, "-XdisableRunAsync" )
            .arg( strict, "-strict" )
            .arg( soycDetailed, "-XsoycDetailed" )
            .arg( enableClosureCompiler, "-XenableClosureCompiler" )
            .arg( compilerMetrics, "-XcompilerMetrics" )
            .arg( disableAggressiveOptimization, "-XdisableAggressiveOptimization" )
            .arg( "-XfragmentCount", String.valueOf( fragmentCount ) );


        if ( optimizationLevel >= 0 )
        {
            cmd.arg( "-optimize" ).arg( Integer.toString( optimizationLevel ) );
        }

        if ( extraParam || compileReport )
        {
            getLog().debug( "create extra directory " );
            if ( !extra.exists() )
            {
                extra.mkdirs();
            }
            cmd.arg( "-extra" ).arg( extra.getAbsolutePath() );
        }
        else
        {
            getLog().debug( "NOT create extra directory " );
        }

        if ( compileReport )
        {
            cmd.arg( "-compileReport" );
        }

        addCompileSourceArtifacts( cmd );
        addArgumentDeploy(cmd);
        addArgumentGen( cmd );
        addPersistentUnitCache(cmd);

        if ( workDir != null )
        {
            cmd.arg( "-workDir" ).arg( String.valueOf( workDir ) );
        }

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

    private int getLocalWorkers()
    {
        if ( localWorkers > 0 )
        {
            return localWorkers;
        }
        // workaround to GWT issue 4031 whith IBM JDK
        // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=4031
        if ( System.getProperty( "java.vendor" ).startsWith( "IBM" ) && StringUtils.isEmpty(getJvm()))
        {
            StringBuilder sb = new StringBuilder( "Build is using IBM JDK, and no explicit JVM property has been set." );
            sb.append( SystemUtils.LINE_SEPARATOR );
            sb.append("localWorkers set to 1 as a workaround");
            sb.append( SystemUtils.LINE_SEPARATOR );
            sb.append( "see http://code.google.com/p/google-web-toolkit/issues/detail?id=4031" );
            getLog().info( sb.toString() );
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
        getLog().debug( "**Checking if compilation is required for " + module );
        try
        {

        	GwtModule gwtModule = readModule( module );
            if ( gwtModule.getEntryPoints().size() == 0 )
            {
                getLog().info( gwtModule.getName() + " has no EntryPoint - compilation skipped" );
                // No entry-point, this is an utility module : compiling this one will fail
                // with '[ERROR] Module has no entry points defined'
                return false;
            }
            getLog().debug( "Module has an entrypoint" );

            if ( force )
            {
                return true;
            }
            getLog().debug( "Compilation not forced");
            
            String modulePath = gwtModule.getPath();

            String outputTarget = modulePath + "/" + modulePath + ".nocache.js";
            File outputTargetFile = new File( output, outputTarget );
            // Require compilation if no js file present in target.
            if ( !outputTargetFile.exists() )
            {
                return true;
            }
            getLog().debug( "Output file exists");
            
            File moduleFile = gwtModule.getSourceFile();
            if(moduleFile == null) {
            	return true; //the module was read from something like an InputStream; always recompile this because we can't make any other choice
            }
            getLog().debug( "There is a module source file (not an input stream");
            
            //If input is newer than target, recompile
            if(moduleFile.lastModified() > outputTargetFile.lastModified()) 
            {
                getLog().debug( "Module file has been modified since the output file was created; recompiling" );
            	return true;
            }
            getLog().debug( "The module XML hasn't been updated");

            // js file already exists, but may not be up-to-date with project source files
            SingleTargetSourceMapping singleTargetMapping = new SingleTargetSourceMapping( ".java", outputTarget );
            StaleSourceScanner scanner = new StaleSourceScanner();
            scanner.addSourceMapping( singleTargetMapping );

            SingleTargetSourceMapping uiBinderMapping = new SingleTargetSourceMapping( ".ui.xml", outputTarget );
            scanner.addSourceMapping( uiBinderMapping );

            Collection<File> compileSourceRoots = new HashSet<File>();
           	for (Iterator iterator = getProject().getCompileSourceRoots().iterator(); iterator.hasNext();) {	
				String sourceRoot = (String) iterator.next();
           		for (String sourcePackage : gwtModule.getSources()) {
			        String packagePath = gwtModule.getPackage().replace( '.', File.separatorChar );
		            File sourceDirectory = new File (sourceRoot + File.separatorChar + packagePath + File.separator + sourcePackage);
		            if(sourceDirectory.exists()) {
		            	getLog().debug(" Looking in a source directory "+sourceDirectory.getAbsolutePath() + " for possible changes");
			            compileSourceRoots.add(sourceDirectory);					
		            }
				}
			}

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
        catch ( GwtModuleReaderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
