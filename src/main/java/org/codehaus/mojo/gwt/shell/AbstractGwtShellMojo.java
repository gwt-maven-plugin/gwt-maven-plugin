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
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineTimeOutException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Support running GWT SDK Tools as forked JVM with classpath set according to project source/resource directories and
 * dependencies.
 *
 * @author ccollins
 * @author cooper
 * @author willpugh
 * @version $Id$
 */
public abstract class AbstractGwtShellMojo
    extends AbstractGwtModuleMojo
{
    /**
     * Location on filesystem where GWT will write generated content for review (-gen option to GWT Compiler).
     * <p>
     * Can be set from command line using '-Dgwt.gen=...'
     * </p>
     *
     * @parameter default-value="${project.build.directory}/.generated" expression="${gwt.gen}"
     */
    private File gen;

    /**
     * Whether to add -gen parameter to the compiler command line
     * <p>
     * Can be set from command line using '-Dgwt.genParam=false'. Defaults to 'true' for backwards compatibility.
     * </p>
     *
     * @parameter default-value="true" expression="${gwt.genParam}"
     * @since 2.5.0-rc1
     */
    private boolean genParam;

    /**
     * GWT logging level (-logLevel ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL).
     * <p>
     * Can be set from command line using '-Dgwt.logLevel=...'
     * </p>
     *
     * @parameter default-value="INFO" expression="${gwt.logLevel}"
     */
    private String logLevel;

    /**
     * GWT JavaScript compiler output style (-style OBF[USCATED], PRETTY, or DETAILED).
     * <p>
     * Can be set from command line using '-Dgwt.style=...'
     * </p>
     *
     * @parameter default-value="OBF" expression="${gwt.style}"
     */
    private String style;

    /**
     * The directory into which deployable but not servable output files will be written (defaults to 'WEB-INF/deploy' under the webappDirectory directory/jar, and may be the same as the extra directory/jar)
     *
     * @parameter
     * @since 2.3.0-1
     */
    private File deploy;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     * <p>
     * Can be set from command line using '-Dgwt.extraJvmArgs=...', defaults to setting max Heap size to be large enough
     * for most GWT use cases.
     * </p>
     *
     * @parameter expression="${gwt.extraJvmArgs}" default-value="-Xmx512m"
     */
    private String extraJvmArgs;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking scripts. For the default, the
     * jvm will be the same as the one used to run Maven.
     *
     * @parameter expression="${gwt.jvm}"
     * @since 1.1
     */
    private String jvm;

    /**
     * Forked process execution timeOut. Usefull to avoid maven to hang in continuous integration server.
     *
     * @parameter
     */
    private int timeOut;

    /**
     * Artifacts to be included as source-jars in GWTCompiler Classpath. Removes the restriction that source code must
     * be bundled inside of the final JAR when dealing with external utility libraries not designed exclusivelly for
     * GWT. The plugin will download the source.jar if necessary.
     * <p/>
     * This option is a workaround to avoid packaging sources inside the same JAR when splitting and application into
     * modules. A smaller JAR can then be used on server classpath and distributed without sources (that may not be
     * desirable).
     *
     * @parameter
     */
    private String[] compileSourcesArtifacts;

    /**
     * Whether to use the persistent unit cache or not.
     * <p/>
     * Can be set from command line using '-Dgwt.persistentunitcache=...'
     *
     * @parameter expression="${gwt.persistentunitcache}"
     * @since 2.5.0-rc1
     */
    private Boolean persistentunitcache;

    /**
     * The directory where the persistent unit cache will be created if enabled.
     * <p/>
     * Can be set from command line using '-Dgwt.persistentunitcachedir=...'
     *
     * @parameter expression="${gwt.persistentunitcachedir}"
     * @since 2.5.0-rc1
     */
    private File persistentunitcachedir;

    // methods

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        doExecute();
    }

    public abstract void doExecute()
        throws MojoExecutionException, MojoFailureException;

    protected String getExtraJvmArgs()
    {
        return extraJvmArgs;
    }

    protected String getLogLevel()
    {
        return this.logLevel;
    }

    protected String getStyle()
    {
        return this.style;
    }


    protected String getJvm()
    {
        return jvm;
    }

    /**
     * hook to post-process the dependency-based classpath
     */
    protected void postProcessClassPath( Collection<File> classpath )
        throws MojoExecutionException
    {
        // Nothing to do in most case
    }

    private List<String> getJvmArgs()
    {
        List<String> extra = new ArrayList<String>();
        String userExtraJvmArgs = getExtraJvmArgs();
        if ( userExtraJvmArgs != null )
        {
            for ( String extraArg : userExtraJvmArgs.split( " " ) )
            {
                extra.add( extraArg );
            }
        }
        return extra;
    }

    private String getJavaCommand()
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( jvm ) )
        {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            jvm = System.getProperty( "java.home" );
        }

        // does-it exists ? is-it a directory or a path to a java executable ?
        File jvmFile = new File( jvm );
        if ( !jvmFile.exists() )
        {
            throw new MojoExecutionException(
                "the configured jvm " + jvm + " doesn't exists please check your environnement" );
        }
        if ( jvmFile.isDirectory() )
        {
            // it's a directory we construct the path to the java executable
            return jvmFile.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
        }
        getLog().debug( "use jvm " + jvm );
        return jvm;
    }


    /**
     * @param timeOut the timeOut to set
     */
    public void setTimeOut( int timeOut )
    {
        this.timeOut = timeOut;
    }

    /**
     * Add sources.jar artifacts for project dependencies listed as compileSourcesArtifacts. This is a GWT hack to avoid
     * packaging java source files into JAR when sharing code between server and client. Typically, some domain model
     * classes or business rules may be packaged as a separate Maven module. With GWT packaging this requires to
     * distribute such classes with code, that may not be desirable.
     * <p/>
     * The hack can also be used to include utility code from external librariries that may not have been designed for
     * GWT.
     */
    protected void addCompileSourceArtifacts( JavaCommand cmd )
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
                        resolve( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "jar",
                                 "sources" );
                    cmd.withinClasspath( sources.getFile() );
                    found = true;
                    break;
                }
            }
            if ( !found )
            {
                getLog().warn(
                    "Declared compileSourcesArtifact was not found in project dependencies " + dependencyId );
            }
        }
    }

    protected void addArgumentDeploy( JavaCommand cmd )
    {
        if ( deploy != null )
        {
            cmd.arg( "-deploy" ).arg( String.valueOf( deploy ) );
        }
    }

    protected void addArgumentGen( JavaCommand cmd )
    {
        if ( this.genParam )
        {
            if ( !this.gen.exists() )
            {
                this.gen.mkdirs();
            }
            cmd.arg( "-gen", this.gen.getAbsolutePath() );
        }
    }

    protected void addPersistentUnitCache( JavaCommand cmd )
    {
        if ( persistentunitcache != null )
        {
            cmd.systemProperty( "gwt.persistentunitcache", String.valueOf( persistentunitcache.booleanValue() ) );
        }
        if ( persistentunitcachedir != null )
        {
            cmd.systemProperty( "gwt.persistentunitcachedir", persistentunitcachedir.getAbsolutePath() );
        }
    }

    /**
     * A plexus-util StreamConsumer to redirect messages to plugin log
     */
    protected StreamConsumer out = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            getLog().info( line );
        }
    };

    /**
     * A plexus-util StreamConsumer to redirect errors to plugin log
     */
    private StreamConsumer err = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            getLog().error( line );
        }
    };

    /**
     * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
     * @deprecated use the new {@link JavaCommand}
     *             Create a command to execute using builder pattern
     */
    public class JavaCommand
    {
        private String className;

        private List<File> classpath = new LinkedList<File>();

        private List<String> args = new ArrayList<String>();

        private Properties systemProperties = new Properties();

        private Properties env = new Properties();

        //Reasonable maximum length based on http://support.microsoft.com/kb/830473 to accommodate additional parameters
        private static final int MAX_INLINE_CLASSPATH_LENGTH = 4096;

        public JavaCommand( String className )
        {
            this.className = className;
        }

        public JavaCommand withinScope( String scope )
            throws MojoExecutionException
        {
            classpath.addAll( getClasspath( scope ) );
            postProcessClassPath( classpath );
            return this;
        }

        public JavaCommand withinClasspath( File... path )
        {
            for ( File file : path )
            {
                classpath.add( file );
            }
            return this;
        }

        public JavaCommand arg( String arg )
        {
            args.add( arg );
            return this;
        }

        public JavaCommand arg( String arg, String value )
        {
            args.add( arg );
            args.add( value );
            return this;
        }

        public JavaCommand arg( boolean condition, String arg )
        {
            if ( condition )
            {
                args.add( arg );
            }
            return this;
        }

        public JavaCommand systemProperty( String name, String value )
        {
            systemProperties.setProperty( name, value );
            return this;
        }

        public JavaCommand environment( String name, String value )
        {
            env.setProperty( name, value );
            return this;
        }

        public void execute()
            throws MojoExecutionException
        {
            List<String> command = null;
            if ( Os.isFamily( Os.FAMILY_WINDOWS ) && getClassPathLength() > MAX_INLINE_CLASSPATH_LENGTH )
            {
                getLog().debug(
                    "Microsoft Windows and long classpath detected. Using org.codehaus.mojo.gwt.shell.WindowsCommandLineLauncher" );
                command = prepareCommandForWindows();
            }
            else
            {
                command = prepareCommand();
            }

            if ( command == null )
            {
                throw new MojoExecutionException( "Failed to build command" );
            }

            try
            {
                String[] arguments = (String[]) command.toArray( new String[command.size()] );

                // On windows, the default Shell will fall into command line length limitation issue
                // On Unixes, not using a Shell breaks the classpath (NoClassDefFoundError:
                // com/google/gwt/dev/Compiler).
                Commandline cmd =
                    Os.isFamily( Os.FAMILY_WINDOWS ) ? new Commandline( new JavaShell() ) : new Commandline();

                cmd.setExecutable( getJavaCommand() );
                cmd.addArguments( arguments );
                if ( env != null )
                {
                    for ( Map.Entry entry : env.entrySet() )
                    {
                        getLog().debug(
                            "add env " + (String) entry.getKey() + " with value " + (String) entry.getValue() );
                        cmd.addEnvironment( (String) entry.getKey(), (String) entry.getValue() );
                    }
                }
                getLog().debug( "Execute command :\n" + cmd.toString() );
                int status;
                if ( timeOut > 0 )
                {
                    status = CommandLineUtils.executeCommandLine( cmd, out, err, timeOut );
                }
                else
                {
                    status = CommandLineUtils.executeCommandLine( cmd, out, err );
                }

                if ( status != 0 )
                {
                    throw new ForkedProcessExecutionException(
                        "Command [[\n" + cmd.toString() + "\n]] failed with status " + status );
                }
            }
            catch ( CommandLineTimeOutException e )
            {
                if ( timeOut > 0 )
                {
                    getLog().warn( "Forked JVM has been killed on time-out after " + timeOut + " seconds" );
                    return;
                }
                throw new MojoExecutionException( "Time-out on command line execution :\n" + command, e );
            }
            catch ( CommandLineException e )
            {
                throw new MojoExecutionException( "Failed to execute command line :\n" + command, e );
            }

        }

        private List<String> prepareCommandForWindows()
            throws MojoExecutionException
        {
            //Write classpath to temporary file
            final File classPathFile = FileUtils.createTempFile( "gwt-maven-plugin", "classpath", null );
            classPathFile.deleteOnExit();

            PrintWriter writer = null;
            try
            {
                writer = new PrintWriter( classPathFile );
                for ( File file : classpath )
                {
                    writer.println( file.getAbsolutePath() );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to write classpath to temporary file", e );

            }
            finally
            {
                if ( writer != null )
                {
                    writer.close();
                }
            }

            //Redirect Shell execution via proxy that can handle classpath defined in an external file
            List<String> command = new ArrayList<String>();
            command.addAll( getJvmArgs() );
            command.add( "-classpath" );

            //Setup the minimial System ClassLoader classpath needed for:-
            // 1) org.codehaus.mojo:gwt-maven-plugin for org.codehaus.mojo.gwt.shell.WindowsCommandLineLauncher
            // 2) org.codehaus.mojo:gwt-maven-plugin for overriding com.google.gwt.dev.WindowsExternalPermutationWorkerFactory
            // 3) com.google.gwt:gwt-dev for com.google.gwt.dev.ThreadedPermutationWorkerFactory
            // 4) com.google.gwt:gwt-dev for com.google.gwt.dev.PermutationWorkerFactory
            final List<String> minimalClassPath = new ArrayList<String>( 2 );
            Artifact plugin1 = artifactFactory.createArtifact( "org.codehaus.mojo", "gwt-maven-plugin", version,
                                                               Artifact.SCOPE_COMPILE, "maven-plugin" );
            Artifact plugin2 =
                artifactFactory.createArtifact( "com.google.gwt", "gwt-dev", version, Artifact.SCOPE_COMPILE, "jar" );
            minimalClassPath.add( localRepository.getBasedir() + "/" + localRepository.pathOf( plugin1 ) );
            minimalClassPath.add( localRepository.getBasedir() + "/" + localRepository.pathOf( plugin2 ) );
            command.add( StringUtils.join( minimalClassPath.iterator(), File.pathSeparator ) );

            if ( systemProperties != null )
            {
                for ( Map.Entry entry : systemProperties.entrySet() )
                {
                    command.add( "-D" + entry.getKey() + "=" + entry.getValue() );
                }
            }
            command.add( WindowsCommandLineLauncher.class.getName() );
            command.add( classPathFile.getAbsolutePath() );
            command.add( className );
            command.addAll( args );
            return command;
        }

        private List<String> prepareCommand()
        {
            List<String> command = new ArrayList<String>();
            command.addAll( getJvmArgs() );
            command.add( "-classpath" );

            List<String> path = new ArrayList<String>( classpath.size() );
            for ( File file : classpath )
            {
                path.add( file.getAbsolutePath() );
            }
            command.add( StringUtils.join( path.iterator(), File.pathSeparator ) );
            if ( systemProperties != null )
            {
                for ( Map.Entry entry : systemProperties.entrySet() )
                {
                    command.add( "-D" + entry.getKey() + "=" + entry.getValue() );
                }
            }
            command.add( className );
            command.addAll( args );
            return command;
        }

        private int getClassPathLength()
        {
            List<String> path = new ArrayList<String>( classpath.size() );
            for ( File file : classpath )
            {
                path.add( file.getAbsolutePath() );
            }
            return StringUtils.join( path.iterator(), File.pathSeparator ).length();
        }

        public void withinClasspathFirst( File oophmJar )
        {
            classpath.add( 0, oophmJar );
        }
    }

}
