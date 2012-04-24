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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineTimeOutException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class JavaCommand
{
    private String className;

    private List<File> classpath = new LinkedList<File>();

    private final List<String> args = new ArrayList<String>();

    private Properties systemProperties = new Properties();

    private Properties env = new Properties();

    private Collection<File> classPathFiles;
    
    private List<String> jvmArgs;
    
    private String jvm;
    
    private Log log;
    
    private int timeOut;
    
    private List<ClassPathProcessor> classPathProcessors;
    
    /**
     * A plexus-util StreamConsumer to redirect messages to plugin log
     */
    protected StreamConsumer out = new StreamConsumer()
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
    
    /**
     * 
     * 
     */
    public JavaCommand( JavaCommandRequest javaCommandRequest)
    {
        this.className = javaCommandRequest.getClassName();
        this.classPathFiles = javaCommandRequest.getClassPathFiles();
        this.jvmArgs = javaCommandRequest.getJvmArgs();
        this.jvm = javaCommandRequest.getJvm();
        this.log = javaCommandRequest.getLog();
        this.timeOut = javaCommandRequest.getTimeOut();
        this.classPathProcessors = javaCommandRequest.getClassPathProcessors();
    }

    public JavaCommand withinScope( String scope )
        throws MojoExecutionException
    {
        if ( this.classPathFiles != null )
        {
            classpath.addAll( this.classPathFiles );
        }
        if ( this.classPathProcessors != null )
        {
            for ( ClassPathProcessor classPathProcessor : this.classPathProcessors )
            {
                classPathProcessor.postProcessClassPath( classpath );
            }
        }
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
        throws JavaCommandException
    {
        List<String> command = new ArrayList<String>();
        if (this.jvmArgs != null)
        {
            command.addAll( this.jvmArgs );
        }
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

        try
        {
            String[] arguments = (String[]) command.toArray( new String[command.size()] );

            // On windows, the default Shell will fall into command line length limitation issue
            // On Unixes, not using a Shell breaks the classpath (NoClassDefFoundError:
            // com/google/gwt/dev/Compiler).
            Commandline cmd =
                Os.isFamily( Os.FAMILY_WINDOWS ) ? new Commandline( new JavaShell() ) : new Commandline();

            cmd.setExecutable( this.getJavaCommand() );
            cmd.addArguments( arguments );
            if ( env != null )
            {
                for ( Map.Entry entry : env.entrySet() )
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
    
    public void withinClasspathFirst( File oophmJar )
    {
        classpath.add( 0, oophmJar );
    }
}
