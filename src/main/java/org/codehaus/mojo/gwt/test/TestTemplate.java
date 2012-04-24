package org.codehaus.mojo.gwt.test;

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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public class TestTemplate
{
    /**
     * Callback to do something with a test
     */
    public interface CallBack
    {
        /**
         * @param sourceDir where the test was found
         * @param test the test name
         * @throws MojoExecutionException some error occured
         */
        void doWithTest( File sourceDir, String test )
            throws MojoExecutionException;
    }

    /**
     * @param project the maven project
     * @param includes inclusion patterns
     * @param excludes exclusion patterns
     * @param callBack what to do with thoses tests
     * @throws MojoExecutionException some error occured
     */
    public TestTemplate( MavenProject project, String includes, String excludes, CallBack callBack )
        throws MojoExecutionException
    {
        for ( String root : ( List < String > ) project.getTestCompileSourceRoots() )
        {
            File sourceDir = new File( root );
            if ( !sourceDir.exists() )
            {
                continue;
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( sourceDir );
            if ( includes != null && !"".equals( includes ) )
            {
                scanner.setIncludes( includes.split( "," ) );
            }
            if ( excludes != null && !"".equals( excludes ) )
            {
                scanner.setExcludes( excludes.split( "," ) );
            }
            scanner.scan();
            String[] files = scanner.getIncludedFiles();
            for ( String file : files )
            {
                callBack.doWithTest( sourceDir, file );
            }
        }
    }
}
