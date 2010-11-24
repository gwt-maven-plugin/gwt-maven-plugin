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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @see http://code.google.com/intl/fr/webtoolkit/doc/latest/DevGuideTestingRemoteTesting.html#Remote_Web
 * @goal browser
 * @requiresDirectInvocation
 * @requiresDependencyResolution test
 * @description Start a BrowserManagerServer for remote tesing.
 */
public class BrowserManagerServerMojo
    extends AbstractGwtWebMojo
{

    /**
     * Name of the BrowserManagerServer to lauch (typically, "ie8")
     * @parameter default-value="ie"
     */
    private String server;

    /**
     * Path to the browser executable.
     * @parameter default-value="C:\\Program Files\\Internet Explorer\\iexplore.exe"
     */
    private File browser;

    public void doExecute( )
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            JavaCommand cmd = new JavaCommand( "com.google.gwt.junit.remote.BrowserManagerServer" )
                .withinClasspath( getGwtUserJar(), getGwtDevJar() ).arg( server ).arg( browser.getAbsolutePath() );

            cmd.execute();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
