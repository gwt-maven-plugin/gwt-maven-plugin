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
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;




/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class JavaCommandRequest
{
    private String className;

    private List<File> classpath = new LinkedList<File>();

    private List<String> args = new ArrayList<String>();

    private Properties systemProperties = new Properties();

    private Properties env = new Properties();

    private Collection<File> classPathFiles;
    
    private List<String> jvmArgs;
    
    private String jvm;
    
    private Log log;
    
    private int timeOut;
    
    private List<ClassPathProcessor> classPathProcessors;
    
    public JavaCommandRequest()
    {
        // no op
    }

    public String getClassName()
    {
        return className;
    }

    public JavaCommandRequest setClassName( String className )
    {
        this.className = className;
        return this;
    }

    public List<File> getClasspath()
    {
        return classpath;
    }

    public JavaCommandRequest setClasspath( List<File> classpath )
    {
        this.classpath = classpath;
        return this;
    }

    public List<String> getArgs()
    {
        return args;
    }

    public JavaCommandRequest setArgs( List<String> args )
    {
        this.args = args;
        return this;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    public JavaCommandRequest setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
        return this;
    }

    public Properties getEnv()
    {
        return env;
    }

    public JavaCommandRequest setEnv( Properties env )
    {
        this.env = env;
        return this;
    }

    public Collection<File> getClassPathFiles()
    {
        return classPathFiles;
    }

    public JavaCommandRequest setClassPathFiles( Collection<File> classPathFiles )
    {
        this.classPathFiles = classPathFiles;
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

    public JavaCommandRequest setJvmArgs( List<String> jvmArgs )
    {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public String getJvm()
    {
        return jvm;
    }

    public JavaCommandRequest setJvm( String jvm )
    {
        this.jvm = jvm;
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public JavaCommandRequest setLog( Log log )
    {
        this.log = log;
        return this;
    }

    public int getTimeOut()
    {
        return timeOut;
    }

    public JavaCommandRequest setTimeOut( int timeOut )
    {
        this.timeOut = timeOut;
        return this;
    }

    public List<ClassPathProcessor> getClassPathProcessors()
    {
        if (classPathProcessors == null)
        {
            classPathProcessors = new ArrayList<ClassPathProcessor>();
        }
        return classPathProcessors;
    }

    public JavaCommandRequest setClassPathProcessors( List<ClassPathProcessor> classPathProcessors )
    {
        this.classPathProcessors = classPathProcessors;
        return this;
    }
}
