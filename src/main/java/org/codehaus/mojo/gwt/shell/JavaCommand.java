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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineTimeOutException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class JavaCommand
{
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

    /**
     * Indicates whether to print the full command (as part of exception message) when encountering error.
     */
    private boolean printCommandOnError = true;

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

    public void setPrintCommandOnError( boolean printCommandOnError ) {
        this.printCommandOnError = printCommandOnError;
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

        List<String> command = new ArrayList<String>();
        if (this.jvmArgs != null)
        {
            command.addAll( this.jvmArgs );
        }
        if ( systemProperties != null )
        {
            for ( Map.Entry<?, ?> entry : systemProperties.entrySet() )
            {
                command.add( "-D" + entry.getKey() + "=" + entry.getValue() );
            }
        }
        command.add( mainClass );
        command.addAll( args );

        List<String> path = new ArrayList<String>( classpath.size() );
        for ( File file : classpath ) path.add( file.getAbsolutePath() );
        String classpath = StringUtils.join( path.iterator(), File.pathSeparator );

        try
        {
            String[] arguments = command.toArray( new String[command.size()] );

            Commandline cmd = new Commandline();
            cmd.setExecutable( this.getJavaCommand() );
            cmd.addEnvironment( "CLASSPATH", classpath );
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
            log.debug( "With CLASSPATH :\n" + classpath );
            int status = CommandLineUtils.executeCommandLine( cmd, out, err, timeOut );

            if ( status != 0 )
            {
                throw new JavaCommandException( "Command failed with status "  + status
                        + (printCommandOnError ? ":\n" + cmd : "" ) );
            }
        }
        catch ( CommandLineTimeOutException e )
        {
            throw new JavaCommandException(
                    "Time-out on command line execution" + (printCommandOnError ? ":\n" + command : ""), e );
        }
        catch ( CommandLineException e )
        {
            throw new JavaCommandException(
                    "Failed to execute command line" + (printCommandOnError ? ":\n" + command : ""), e );
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
                + " doesn't exists please check your environment" );
        }
        if ( jvmFile.isDirectory() )
        {
            // it's a directory we construct the path to the java executable
            return jvmFile.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
        }
        log.debug( "use jvm " + jvm );
        return jvm;
    }
}
