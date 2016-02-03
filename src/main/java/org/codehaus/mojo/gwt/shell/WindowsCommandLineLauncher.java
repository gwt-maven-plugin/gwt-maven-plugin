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

import com.google.gwt.dev.PermutationWorkerFactory;
import com.google.gwt.dev.ThreadedPermutationWorkerFactory;
import com.google.gwt.dev.WindowsExternalPermutationWorkerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class with "main" entry point invoked from JavaCommand that sets-up a URLClassLoader Context Class Loader with
 * classpath entries provided in a file rather than the command-line. This is used to workaround a limitation in
 * Microsoft Windows where the command line maximum length is often exceeded when compiling large GWT applications
 * on Microsoft Windows.
 * args[0] = Name of file containing classpath entries
 * args[1] = Name of real "main" class to be invoked
 * args[2..n] = Additional arguments required by the real "main" class
 */
public class WindowsCommandLineLauncher
{

    public static void main( String[] args )
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException, NoSuchFieldException {

        //Extract arguments
        final String classPathFileName = args[0];
        final String className = args[1];
        final String[] additionalArguments = new String[args.length - 2];
        for ( int i = 2; i < args.length; i++ )
        {
            additionalArguments[i - 2] = args[i];
        }

        final File classPathFile = new File( classPathFileName );
        final List<URL> urls = new ArrayList<URL>();

        //Copy in SystemClassLoader entries
        final String[] systemClassLoaderClassPath =
            ManagementFactory.getRuntimeMXBean().getClassPath().split( File.pathSeparator );
        for ( String classPathEntry : systemClassLoaderClassPath )
        {
            urls.add( new File( classPathEntry ).toURI().toURL() );
        }

        //Read classpath from file
        final BufferedReader input = new BufferedReader( new FileReader( classPathFile ) );
        try
        {
            String classPathEntry = null;
            while ( ( classPathEntry = input.readLine() ) != null )
            {
                urls.add( new File( classPathEntry ).toURI().toURL() );
            }
        }
        finally
        {
            input.close();
        }

        //Set-up URLClassLoader using classpath extracted from the file. Null parent class-loader is important!
        final URL[] a = urls.toArray( new URL[urls.size()] );
        final ClassLoader loader = new URLClassLoader( a, null );

        //Setup new WorkerFactory
        System.setProperty( PermutationWorkerFactory.FACTORY_IMPL_PROPERTY,
                            ThreadedPermutationWorkerFactory.class.getName() + ","
                                + WindowsExternalPermutationWorkerFactory.class.getName() );

        //Record classpath file needed by com.google.gwt.dev.WindowsExternalPermutationWorkerFactory
        System.setProperty( WindowsExternalPermutationWorkerFactory.CLASS_PATH_FILE_PROPERTY,
                            classPathFile.getAbsolutePath() );

        //Load real class
        final Class mainClass = loader.loadClass( className );

        //Invoke real class's main method
        Thread.currentThread().setContextClassLoader( loader );

        final Class mainMethodArgumentsType = String[].class;
        final Method mainMethod = mainClass.getMethod( "main", new Class[]{ mainMethodArgumentsType } );

        // this is SO UGLY...
        Field scl = ClassLoader.class.getDeclaredField("scl");
        scl.setAccessible(true);
        scl.set(null, loader); // needed due to the possible Xerces conflicts as GWT uses system classloader to load these

        mainMethod.invoke( null, new Object[]{ additionalArguments } );
    }

}

