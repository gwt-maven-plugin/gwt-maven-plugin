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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Extends the gwt goal and runs the project in the GWT Hosted mode with a debugger port hook (optionally suspended).
 *
 * @goal debug
 * @description Runs the project with a debugger port hook (optionally suspended).
 * @author cooper
 * @version $Id$
 */
public class DebugMojo
    extends RunMojo
{

    /**
     * Port to listen for debugger connection on.
     * 
     * @parameter default-value="8000" expression="${gwt.debugPort}"
     */
    private int debugPort;

    /**
     * Whether or not to suspend execution until a debugger connects.
     *
     * @parameter default-value="true" expression="${gwt.debugSuspend}"
     */
    private boolean debugSuspend;

    /**
     * Attach to the debugger application at the specified debugPort.
     * 
     * @parameter default-value="false" expression="${attachDebugger}"
     */
    private boolean attachDebugger;

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.RunMojo#getFileName()
     */
    @Override
    protected String getFileName()
    {
        return "debug";
    }

    /**
     * Override extraJVMArgs to append JVM debugger option
     * <p>
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo#getExtraJvmArgs()
     */
    @Override
    public String getExtraJvmArgs()
    {
        String extras = super.getExtraJvmArgs();
        extras += " -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket";
        extras += ",server=" + ( attachDebugger ? "n" : "y" );
        extras += ",address=" + debugPort;
        extras += ",suspend=" + ( debugSuspend ? "y" : "n" );
        return extras;
    }


    @Override
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( debugSuspend )
        {
            getLog().info( "starting debugger on port " + debugPort + " in suspend mode" );
        }
        else
        {
            getLog().info( "starting debugger on port " + debugPort );
        }

        super.doExecute( );
    }
}
