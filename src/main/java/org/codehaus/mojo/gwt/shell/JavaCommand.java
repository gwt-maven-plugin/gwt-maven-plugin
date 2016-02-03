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

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.*;

import java.io.*;
import java.util.*;

/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class JavaCommand
{
    private static final int MAX_INLINE_CLASSPATH_LENGTH = 4096;

    private String mainClass;

    private List<File> classpath = new ArrayList<File>();

    private List<String> args = new ArrayList<String>();

    private Properties systemProperties = new Properties();

    private Properties env = new Properties();

    private List<String> jvmArgs;

    private String jvm;

    private Log log;

    private int timeOut;

    private List<ClassPathProcessor> classPathProcessors = new ArrayList<ClassPathProcessor>();

    /**
     * A plexus-util StreamConsumer to redirect messages to plugin log
     */
    private StreamConsumer out = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            log.info( line );
        }
    };

    /**
     * A plexus-util StreamConsumer to redirect errors to plugin log
     */
    private StreamConsumer err = new StreamConsumer()
    {
        public void consumeLine( String line )
        {
            log.error( line );
        }
    };
    private ArtifactFactory artifactFactory;
    private ArtifactRepository localRepository;

    public String getVersion() {
        return version;
    }

    public JavaCommand setVersion(String version) {
        this.version = version;
        return this;
    }

    private String version;

    public String getMainClass()
    {
        return mainClass;
    }

    public JavaCommand setMainClass( String mainClass )
    {
        this.mainClass = mainClass;
        return this;
    }

    public List<File> getClasspath()
    {
        return classpath;
    }

    public JavaCommand setClasspath( List<File> classpath )
    {
        this.classpath = classpath;
        return this;
    }

    public List<String> getArgs()
    {
        return args;
    }

    public JavaCommand setArgs( List<String> args )
    {
        this.args = args;
        return this;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    public JavaCommand setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
        return this;
    }

    public Properties getEnv()
    {
        return env;
    }

    public JavaCommand setEnv( Properties env )
    {
        this.env = env;
        return this;
    }

    public List<String> getJvmArgs()
    {
        if (this.jvmArgs == null)
        {
            this.jvmArgs = new ArrayList<String>();
        }
        return jvmArgs;
    }

    public JavaCommand setJvmArgs( List<String> jvmArgs )
    {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public String getJvm()
    {
        return jvm;
    }

    public JavaCommand setJvm( String jvm )
    {
        this.jvm = jvm;
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public JavaCommand setLog( Log log )
    {
        this.log = log;
        return this;
    }

    public int getTimeOut()
    {
        return timeOut;
    }

    public JavaCommand setTimeOut( int timeOut )
    {
        this.timeOut = timeOut;
        return this;
    }

    public List<ClassPathProcessor> getClassPathProcessors()
    {
        return classPathProcessors;
    }

    public JavaCommand addClassPathProcessors( ClassPathProcessor classPathProcessor )
    {
        classPathProcessors.add( classPathProcessor );
        return this;
    }

    public JavaCommand setClassPathProcessors( List<ClassPathProcessor> classPathProcessors )
    {
        this.classPathProcessors = classPathProcessors;
        return this;
    }

    public JavaCommand setOut( StreamConsumer out )
    {
        this.out = out;
        return this;
    }

    public JavaCommand addToClasspath( File file )
    {
        return addToClasspath( Collections.singleton( file ) );
    }

    public JavaCommand addToClasspath( Collection<File> elements )
    {
        classpath.addAll( elements );
        return this;
    }

    public JavaCommand prependToClasspath( Collection<File> elements )
    {
        classpath.addAll( 0, elements );
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
        throws JavaCommandException
    {
        for (ClassPathProcessor classPathProcessor : classPathProcessors )
        {
            classPathProcessor.postProcessClassPath( classpath );
        }

        List<String> command = null;
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) && getClassPathLength() > MAX_INLINE_CLASSPATH_LENGTH )
        {
            getLog().debug(
                    "Microsoft Windows and long classpath detected. Using org.codehaus.mojo.gwt.shell.WindowsCommandLineLauncher" );
            try {
                command = prepareCommandForWindows();
            } catch (MojoExecutionException e) {
                throw new JavaCommandException("Error creating long-path command for Windows: ", e);
            }
        }
        else
        {
            command = prepareCommand();
        }

        try
        {
            String[] arguments = command.toArray( new String[command.size()] );

            // On windows, the default Shell will fall into command line length limitation issue
            // On Unixes, not using a Shell breaks the classpath (NoClassDefFoundError:
            // com/google/gwt/dev/Compiler).
            Commandline cmd =
                Os.isFamily( Os.FAMILY_WINDOWS ) ? new Commandline( new JavaShell() ) : new Commandline();

            cmd.setExecutable( this.getJavaCommand() );
            cmd.addArguments( arguments );
            if ( env != null )
            {
                for ( Map.Entry<?, ?> entry : env.entrySet() )
                {
                    log.debug( "add env " + (String) entry.getKey() + " with value " + (String) entry.getValue() );
                    cmd.addEnvironment( (String) entry.getKey(), (String) entry.getValue() );
                }
            }
            log.debug( "Execute command :\n" + cmd.toString() );
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
                throw new JavaCommandException( "Command [[\n" + cmd.toString()
                    + "\n]] failed with status " + status );
            }
        }
        catch ( CommandLineTimeOutException e )
        {
            if ( timeOut > 0 )
            {
                log.warn( "Forked JVM has been killed on time-out after " + timeOut + " seconds" );
                return;
            }
            throw new JavaCommandException( "Time-out on command line execution :\n" + command, e );
        }
        catch ( CommandLineException e )
        {
            throw new JavaCommandException( "Failed to execute command line :\n" + command, e );
        }
    }

    private String getJavaCommand()
        throws JavaCommandException
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
            throw new JavaCommandException( "the configured jvm " + jvm
                + " doesn't exists please check your environnement" );
        }
        if ( jvmFile.isDirectory() )
        {
            // it's a directory we construct the path to the java executable
            return jvmFile.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
        }
        log.debug( "use jvm " + jvm );
        return jvm;
    }

    private List<String> prepareCommandForWindows()
            throws MojoExecutionException
    {
        //Write classpath to temporary file
        final File classPathFile = FileUtils.createTempFile( "gwt-maven-plugin", "classpath", null );
        //classPathFile.deleteOnExit();

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
        if (getJvmArgs() != null) {
            command.addAll( getJvmArgs() );
        }
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
        command.add( mainClass );
        command.addAll( args );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new FileInputStream(classPathFile), baos);
            log.info("Windows class path written to file is: " + baos.toString());
        } catch (IOException e) {
            log.error("Unable to open Windows class path file.");
        }
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
        command.add( mainClass );
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

    public JavaCommand setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
        return this;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public JavaCommand setLocalRepository(ArtifactRepository artifactRepository) {
        this.localRepository = artifactRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }
}
