package org.codehaus.mojo.gwt;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.mojo.gwt.utils.DefaultGwtModuleReader;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;

/**
 * Add support for GWT Modules.
 * <p>
 * Search and read the gwt.xml module files to detect project structure.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public abstract class AbstractGwtModuleMojo
    extends AbstractGwtMojo
    implements GwtModuleReader
{
    /**
     * The project GWT modules. If not set, the plugin will scan the project for <code>.gwt.xml</code> files.
     *
     * @parameter
     * @alias compileTargets
     */
    private String[] modules;

    /**
     * A single GWT module. Shortcut for &lt;modules&gt; or option to specify a single module from command line
     *
     * @parameter expression="${gwt.module}"
     */
    private String module;

    @Override
    public List<String> getGwtModules()
    {
        return convertToList( getModules());
    }

    /**
     * Return the configured modules or scan the project source/resources folder to find them
     *
     * @return the modules
     */
    @SuppressWarnings( "unchecked" )
    public String[] getModules()
    {
        // module has higher priority if set by expression
        if ( module != null )
        {
            return new String[] { module };
        }
        if ( modules == null )
        {
            modules = convertToArray( new DefaultGwtModuleReader(getProject(), getLog(), classpathBuilder).getGwtModules());
        }
        return modules;
    }


    @Override
    public GwtModule readModule( String name )
        throws GwtModuleReaderException
    {
        return new DefaultGwtModuleReader(getProject(), getLog(), classpathBuilder).readModule(name);
    }

    /**
     * @param path file to add to the project compile directories
     */
    @Override
    protected void addCompileSourceRoot( File path )
    {
        getProject().addCompileSourceRoot( path.getAbsolutePath() );
    }

    private static <T> List<T> convertToList( T[] array )
    {
        return ArrayUtils.isEmpty( array )? Collections.<T>emptyList() : Arrays.asList( array );
    }
    private static String[] convertToArray( List<String> list )
    {
        return list.toArray( new String[list.size()] );
    }
}