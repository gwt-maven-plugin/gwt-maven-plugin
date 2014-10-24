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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * Invokes the GWT Compiler for the project source.
 * See compiler options :
 * http://www.gwtproject.org/doc/latest/DevGuideCompilingAndDebugging.html#DevGuideCompilerOptions
 *
 * @version $Id$
 * @author cooper
 * @author ccollins
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class CompileMojo
    extends AbstractGwtShellMojo
{

    @Parameter(property = "gwt.compiler.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Don't try to detect if GWT compilation is up-to-date and can be skipped.
     * <p>
     * Can be set from command line using '-Dgwt.compiler.force=true'.
     */
    @Parameter(property = "gwt.compiler.force", defaultValue = "false")
    private boolean force;

    /**
     * On GWT 1.6+, number of parallel processes used to compile GWT premutations. Defaults to
     * platform available processors number.
     * 
     * <p>
     * Can be unset from command line using '-Dgwt.compiler.localWorkers=n'.
     * </p>
     */
    @Parameter(property = "gwt.compiler.localWorkers")
    private int localWorkers;

    /**
     * Whether or not to enable assertions in generated scripts (-checkAssertions).
     */
    @Parameter(alias = "enableAssertions", defaultValue = "false")
    private boolean checkAssertions;

    /**
     * EXPERIMENTAL: Disables some java.lang.Class methods (e.g. getName()).
     * <p>
     * Can be set from command line using '-Dgwt.disableClassMetadata=true'.
     * </p>
     */
    @Parameter(defaultValue = "false", property = "gwt.disableClassMetadata")
    private boolean disableClassMetadata;

    /**
     * EXPERIMENTAL: Disables run-time checking of cast operations.
     * <p>
     * Can be set from command line using '-Dgwt.disableCastChecking=true'.
     * </p>
     */
    @Parameter(defaultValue = "false", property = "gwt.disableCastChecking")
    private boolean disableCastChecking;

    /**
     * EXPERIMENTAL: Disables code-splitting.
     * <p>
     * Can be set from command line using '-Dgwt.disableRunAsync=true'.
     * </p>
     */
    @Parameter(defaultValue = "false", property = "gwt.disableRunAsync")
    private boolean disableRunAsync;

    /**
     * Validate all source code, but do not compile.
     * <p>
     * Can be set from command line using '-Dgwt.validateOnly=true'.
     * </p>
     */
    @Parameter(defaultValue = "false", property = "gwt.validateOnly")
    private boolean validateOnly;

    /**
     * Enable faster, but less-optimized, compilations.
     * <p>
     * Can be set from command line using '-Dgwt.draftCompile=true'.
     * </p>
     * <p>
     * This is equivalent to '-Dgwt.compiler.optimizationLevel=0 -Dgwt.compiler.disableAggressiveOptimization=true'.
     * </p>
     */
    @Parameter(defaultValue = "false", property = "gwt.draftCompile")
    private boolean draftCompile;

    /**
     * The directory into which extra, non-deployed files will be written.
     */
    @Parameter(defaultValue = "${project.build.directory}/extra")
    private File extra;

    /**
     * The compiler's working directory for internal use (must be writeable; defaults to a system temp dir)
     */
    @Parameter
    private File workDir;

    /**
     * add -extra parameter to the compiler command line
     * <p>
     * Can be set from command line using '-Dgwt.extraParam=true'.
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "false", property = "gwt.extraParam")
    private boolean extraParam;

    /**
     * Compile a report that tells the "Story of Your Compile".
     * <p>
     * Can be set from command line using '-Dgwt.compiler.compileReport=true'.
     * </p>
     *
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "false", property = "gwt.compiler.compileReport")
    private boolean compileReport;

    /**
     * Sets the optimization level used by the compiler.  0=none 9=maximum.
     * <p>
     * -1 uses the default level of the compiler.
     * </p>
     * <p>
     * Can be set from command line using '-Dgwt.compiler.optimizationLevel=n'.
     * </p>
     * @parameter default-value="-1" expression="${gwt.compiler.optimizationLevel}"
     * @since 2.1.0-1
     */
    @Parameter(defaultValue = "-1", property = "gwt.compiler.optimizationLevel")
    private int optimizationLevel;

    /**
     * EXPERIMENTAL: Emit extra, detailed compile-report information in the "Story Of Your Compile" at the expense of compile time.
     * <p>
     * Can be set from command line using '-Dgwt.compiler.soycDetailed=true'.
     * </p>
     *
     * @since 2.1.0-1
     */
    @Parameter(alias = "soycDetailed", defaultValue = "false", property = "gwt.compiler.soycDetailed")
    private boolean detailedSoyc;

    /**
     * Fail compilation if any input file contains an error.
     * 
     * <p>
     * Can be set from command line using '-Dgwt.compiler.strict=true'.
     * </p>
     *
     * @since 2.1.0-1
     */
    @Parameter(alias = "strict", defaultValue = "false", property = "gwt.compiler.strict")
    private boolean failOnError;

    /**
     * EXPERIMENTAL: Compile output Javascript with the Closure compiler for even further optimizations.
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.enableClosureCompiler=true'
     * </p>
     *
     * @since 2.5.0-rc1
     */
    @Parameter(alias = "enableClosureCompiler", defaultValue = "false", property = "gwt.compiler.enableClosureCompiler")
    private boolean closureCompiler;

    /**
     * EXPERIMENTAL: add -XdisableAggressiveOptimization parameter to the compiler command line
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.disableAggressiveOptimization=true'
     * </p>
     *
     * @since 2.5.0-rc1
     * @deprecated since 2.6.0-rc1
     */
    @Parameter(defaultValue = "false", property = "gwt.compiler.disableAggressiveOptimization")
    private boolean disableAggressiveOptimization;

    /**
     * EXPERIMENTAL: Gather compiler metrics.
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.compilerMetrics=true'
     * </p>
     *
     * @since 2.5.0-rc1
     */
    @Parameter(defaultValue = "false", property = "gwt.compiler.compilerMetrics")
    private boolean compilerMetrics;

    /**
     * EXPERIMENTAL: Limits of number of fragments using a code splitter that merges split points.
     * <p>
     * Can be set from the command line using '-Dgwt.compiler.fragmentCount=n'
     * </p>
     *
     * @since 2.5.0-rc1
     */
    @Parameter(defaultValue = "-1", property = "gwt.compiler.fragmentCount")
    private int fragmentCount;

    /**
     * EXPERIMENTAL: Cluster similar functions in the output to improve compression.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.compiler.clusterFunctions")
    private boolean clusterFunctions;

    /**
     * EXPERIMENTAL: Avoid adding implicit dependencies on "client" and "public" for
     * modules that don't define any dependencies.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "false", property = "gwt.compiler.enforceStrictResources")
    private boolean enforceStrictResources;

    /**
     * EXPERIMENTAL: Inline literal parameters to shrink function declarations and
     * provide more deadcode elimination possibilities.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.compiler.inlineLiteralParameters")
    private boolean inlineLiteralParameters;

    /**
     * EXPERIMENTAL: Analyze and optimize dataflow.
     *
     * since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.compiler.optimizeDataflow")
    private boolean optimizeDataflow;

    /**
     * EXPERIMENTAL: Ordinalize enums to reduce some large strings.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.compiler.ordinalizeEnums")
    private boolean ordinalizeEnums;

    /**
     * EXPERIMENTAL: Removing duplicate functions.
     * <p>
     * Will interfere with stacktrace deobfuscation and so is only honored when compiler.stackMode is set to strip.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "true", property = "gwt.compiler.removeDuplicateFunctions")
    private boolean removeDuplicateFunctions;

    /**
     * Enables saving source code needed by debuggers.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "false", property = "gwt.saveSource")
    private boolean saveSource;

    /**
     * Overrides where source files useful to debuggers will be written.
     * <p>
     * Default: saved with extras.
     *
     * @since 2.6.0-rc2
     */
    @Parameter
    private File saveSourceOutput;

    /**
     * Specifies Java source level.
     *
     * @since 2.6.0-rc1
     */
    @Parameter(defaultValue = "auto", property = "maven.compiler.source")
    private String sourceLevel;

    /**
     * Whether to show warnings during monolithic compiles for issues that will break
     * in incremental compiles (strict compile errors, strict source directory inclusion,
     * missing dependencies).
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "false")
    private boolean incrementalCompileWarnings;

    /**
     * EXPERIMENTAL: Specifies JsInterop mode, either NONE, JS, or CLOSURE.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "NONE")
    private String jsInteropMode;

    /**
     * Specifies a file into which detailed missing dependency information will be written.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter
    private File missingDepsFile;

    /**
     * Puts most JavaScript globals into namespaces.
     * <p>
     * Value is one of PACKAGE or NONE.
     * <p>
     * Default: PACKAGE for -draftCompile, otherwise NONE
     * 
     * @since 2.7.0-rc1
     */
    @Parameter
    private String namespace;

    /**
     * Whether to show warnings during monolithic compiles for overlapping source inclusion.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "false")
    private boolean overlappingSourceWarnings;

    /**
     * EXPERIMENTAL: Emit detailed compile-report information in the "Story Of Your Compile"  in the new json format.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "false")
    private boolean enableJsonSoyc;

    /**
     * Compiles faster by reusing data from the previous compile.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(alias = "compilePerFile", defaultValue = "false", property = "gwt.compiler.incremental")
    private boolean incremental;

    /**
     * EXPERIMENTAL: Emit extra information allow chrome dev tools to display Java identifiers in many places instead of JavaScript functions.
     * <p>
     * Value can be one of NONE, ONLY_METHOD_NAME, ABBREVIATED or FULL.
     * 
     * @since 2.7.0-rc1
     */
    @Parameter(defaultValue = "NONE", property = "gwt.compiler.methodNameDisplayMode")
    private String methodNameDisplayMode;

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

    @Override
    protected String getExtraJvmArgs()
    {
        String jvmArgs = super.getExtraJvmArgs();
        // workaround to GWT issue 4031 with IBM JDK
        // @see https://code.google.com/p/google-web-toolkit/issues/detail?id=4031#c16
        if ( System.getProperty( "java.vendor" ).startsWith( "IBM" ) && StringUtils.isEmpty(getJvm()) && !StringUtils.isEmpty( jvmArgs ))
        {
            return jvmArgs + " -Dgwt.jjs.javaArgs=" + StringUtils.quoteAndEscape( jvmArgs, '"', new char[] { '"', ' ', '\t', '\r', '\n' } );
        }
        return jvmArgs;
    }

    private void compile( String[] modules )
        throws MojoExecutionException
    {
        boolean upToDate = true;

        JavaCommand cmd = createJavaCommand()
            .setMainClass( "com.google.gwt.dev.Compiler" );
        if ( gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
               .addToClasspath( getGwtDevJar() );
        }
        cmd.addToClasspath( getClasspath( Artifact.SCOPE_COMPILE ) );
        if ( !gwtSdkFirstInClasspath )
        {
            cmd.addToClasspath( getGwtUserJar() )
               .addToClasspath( getGwtDevJar() );
        }

        cmd.arg( "-logLevel", getLogLevel() )
            .arg( "-style", getStyle() )
            .arg( "-war", getOutputDirectory().getAbsolutePath() )
            .arg( "-localWorkers", String.valueOf( getLocalWorkers() ) )
            // optional advanced arguments
            .arg( checkAssertions, "-checkAssertions" )
            .arg( draftCompile, "-draftCompile" )
            .arg( validateOnly, "-validateOnly" )
            .arg( disableClassMetadata, "-XnoclassMetadata" )
            .arg( disableCastChecking, "-XnocheckCasts" )
            .arg( disableRunAsync, "-XnocodeSplitting" )
            .arg( failOnError, "-failOnError" )
            .arg( detailedSoyc, "-XdetailedSoyc" )
            .arg( closureCompiler, "-XclosureCompiler" )
            .arg( compileReport, "-compileReport" )
            .arg( compilerMetrics, "-XcompilerMetrics" )
            .arg( disableAggressiveOptimization, "-XnoaggressiveOptimizations" )
            .arg( "-XfragmentCount", String.valueOf( fragmentCount ) )
            .arg( !clusterFunctions, "-XnoclusterFunctions" )
            .arg( enforceStrictResources, "-XenforceStrictResources" )
            .arg( !inlineLiteralParameters, "-XnoinlineLiteralParameters" )
            .arg( !optimizeDataflow, "-XnooptimizeDataflow" )
            .arg( !ordinalizeEnums, "-XnoordinalizeEnums" )
            .arg( !removeDuplicateFunctions, "-XnoremoveDuplicateFunctions" )
            .arg( saveSource, "-saveSource" )
            .arg( "-sourceLevel", sourceLevel )
            .arg( incrementalCompileWarnings, "-incrementalCompileWarnings" )
            .arg( overlappingSourceWarnings, "-overlappingSourceWarnings")
            .arg( enableJsonSoyc, "-XenableJsonSoyc" )
            .arg( incremental, "-incremental" )
        ;

        if ( jsInteropMode != null && jsInteropMode.length() > 0 && !jsInteropMode.equals( "NONE" ) )
        {
            cmd.arg( "-XjsInteropMode", jsInteropMode );
        }
        if ( methodNameDisplayMode != null && methodNameDisplayMode.length() > 0 && !methodNameDisplayMode.equals( "NONE" ))
        {
            cmd.arg( "-XmethodNameDisplayMode", methodNameDisplayMode );
        }

        if ( missingDepsFile != null )
        {
            cmd.arg( "-missingDepsFile", missingDepsFile.getAbsolutePath() );
        }

        if ( namespace != null && namespace.length() > 0 )
        {
            cmd.arg( "-Xnamespace", namespace );
        }

        if ( saveSourceOutput != null )
        {
            cmd.arg( "-saveSourceOutput", saveSourceOutput.getAbsolutePath() );
        }

        if ( optimizationLevel >= 0 )
        {
            cmd.arg( "-optimize" ).arg( Integer.toString( optimizationLevel ) );
        }

        if ( extraParam || compileReport || ( saveSource && saveSourceOutput == null ) )
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
            try
            {
                cmd.execute();
            }
            catch ( JavaCommandException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
    }

    private int getLocalWorkers()
    {
        if ( localWorkers > 0 )
        {
            return localWorkers;
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
            for (String sourceRoot : getProject().getCompileSourceRoots()) {
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
