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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.util.xml.Xpp3Dom;




/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class GwtModule
{
    private Xpp3Dom xml;

    private String name;

    private Set<GwtModule> inherits;

    private GwtModuleReader reader;

    public GwtModule( String name, Xpp3Dom xml, GwtModuleReader reader )
    {
        this.name = name;
        this.xml = xml;
        this.reader = reader;
    }

    private String getRenameTo()
    {
        return xml.getAttribute( "rename-to" );
    }

    public String getPublic()
    {
        Xpp3Dom node = xml.getChild( "public" );
        return ( node == null ? "public" : node.getAttribute( "path" ) );
    }

    public String[] getSuperSources()
    {
        Xpp3Dom nodes[] = xml.getChildren( "super-source" );
        if ( nodes == null )
        {
            return new String[0];
        }
        String[] superSources = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            String path = node.getAttribute( "path" );
            if ( path == null )
            {
                path = "";
            }
            superSources[i++] = path;
        }
        return superSources;
    }

    public String[] getSources()
    {
        Xpp3Dom nodes[] = xml.getChildren( "source" );
        if ( nodes == null )
        {
            return new String[] { "client" };
        }
        String[] sources = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            sources[i++] = node.getAttribute( "path" );
        }
        return sources;
    }

    public List<String> getEntryPoints()
        throws GwtModuleReaderException
    {
        List<String> entryPoints = new ArrayList<String>();
        entryPoints.addAll( getLocalEntryPoints() );
        for ( GwtModule module : getInherits() )
        {
            entryPoints.addAll( module.getLocalEntryPoints() );
        }
        return entryPoints;
    }

    private List<String> getLocalEntryPoints()
    {
        Xpp3Dom nodes[] = xml.getChildren( "entry-point" );
        if ( nodes == null )
        {
            return Collections.emptyList();
        }
        List<String> entryPoints = new ArrayList<String>( nodes.length );
        for ( Xpp3Dom node : nodes )
        {
            entryPoints.add( node.getAttribute( "class" ) );
        }
        return entryPoints;
    }

    /**
     * Build the set of inhertied modules. Due to xml inheritence mecanism, there may be cicles in the inheritence
     * graph, so we build a set of inherited modules
     */
    public Set<GwtModule> getInherits()
		throws GwtModuleReaderException
    {
        if ( inherits != null )
        {
            return inherits;
        }

        inherits = new HashSet<GwtModule>();
        addInheritedModules( inherits, getLocalInherits() );

        return inherits;
    }

    /**
     * 
     * @param set
     * @param modules
     * @throws MojoExecutionException
     */
    private void addInheritedModules( Set<GwtModule> set, Set<GwtModule> modules )
        throws GwtModuleReaderException
    {
        for ( GwtModule module : modules )
        {
            if ( set.add( module ) )
            {
                // if module is allready in the set, don't re-parse it's inherits
                addInheritedModules( set, module.getLocalInherits() );
            }
        }

    }

    private Set<GwtModule> getLocalInherits()
        throws GwtModuleReaderException
    {
        Xpp3Dom nodes[] = xml.getChildren( "inherits" );
        if ( nodes == null )
        {
            return Collections.emptySet();
        }
        Set<GwtModule> modules = new HashSet<GwtModule>();
        for ( Xpp3Dom node : nodes )
        {
            String moduleName = node.getAttribute( "name" );
            // exclude modules from gwt-dev/gwt-user
            if ( !moduleName.startsWith( "com.google.gwt." ) )
            {
                modules.add( reader.readModule( moduleName ) );
            }
        }
        return modules;
    }

    public Map<String, String> getServlets()
        throws GwtModuleReaderException
    {
        return getServlets( getPath() );
    }

    public Map<String, String> getServlets( String path )
        throws GwtModuleReaderException
    {
        Map<String, String> servlets = getLocalServlets( path );
        for ( GwtModule module : getInherits() )
        {
            servlets.putAll( module.getLocalServlets( path ) );
        }
        return servlets;
    }

    private Map<String, String> getLocalServlets( String path )
    {
        Map<String, String> servlets = new HashMap<String, String>();
        Xpp3Dom nodes[] = xml.getChildren( "servlet" );
        if ( nodes != null )
        {
            for ( Xpp3Dom node : nodes )
            {
                servlets.put( StringUtils.isBlank( path ) ? node.getAttribute( "path" ) : path + node.getAttribute( "path" ),
                              node.getAttribute( "class" ) );
            }
        }
        return servlets;
    }

    public String getName()
    {
        return name;
    }

    public String getPackage()
    {
        return name.substring( 0, name.lastIndexOf( '.' ) );
    }

    public String getPath()
    {
        if ( getRenameTo() != null )
        {
            return getRenameTo();
        }
        return name;
    }

    @Override
    public boolean equals( Object obj )
    {
        return name.equals( ( (GwtModule) obj ).name );
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
