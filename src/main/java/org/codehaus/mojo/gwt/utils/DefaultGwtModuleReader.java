package org.codehaus.mojo.gwt.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.gwt.AbstractGwtModuleMojo;
import org.codehaus.mojo.gwt.ClasspathBuilder;
import org.codehaus.mojo.gwt.ClasspathBuilderException;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.GwtModuleReader;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class DefaultGwtModuleReader
    implements GwtModuleReader
{
    public static final String GWT_MODULE_EXTENSION = ".gwt.xml";

    private MavenProject mavenProject;

    private ClasspathBuilder classpathBuilder;

    private Log log;

    public DefaultGwtModuleReader( MavenProject mavenProject, Log log, ClasspathBuilder classpathBuilder )
    {
        this.mavenProject = mavenProject;
        this.log = log;
        this.classpathBuilder = classpathBuilder;
    }

    @SuppressWarnings("unchecked")
    public List<String> getGwtModules()
    {
        //Use a Set to avoid duplicate when user set src/main/java as <resource>
        Set<String> mods = new HashSet<String>();

        Collection<String> sourcePaths = (Collection<String>) mavenProject.getCompileSourceRoots();
        for ( String sourcePath : sourcePaths )
        {
            File sourceDirectory = new File( sourcePath );
            if ( sourceDirectory.exists() )
            {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( sourceDirectory.getAbsolutePath() );
                scanner.setIncludes( new String[] { "**/*" + AbstractGwtModuleMojo.GWT_MODULE_EXTENSION } );
                scanner.scan();

                mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );
            }
        }

        Collection<Resource> resources = (Collection<Resource>) mavenProject.getResources();
        for ( Resource resource : resources )
        {
            File resourceDirectoryFile = new File( resource.getDirectory() );
            if ( !resourceDirectoryFile.exists() )
            {
                continue;
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( resource.getDirectory() );
            scanner.setIncludes( new String[] { "**/*" + AbstractGwtModuleMojo.GWT_MODULE_EXTENSION } );
            scanner.scan();
            mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );
        }

        if ( mods.isEmpty() )
        {
            log.warn( "GWT plugin is configured to detect modules, but none were found." );
        }

        List<String> modules = new ArrayList<String>( mods.size() );
        for ( String fileName : mods )
        {
            String path = fileName.substring( 0,
                                              fileName.length() - AbstractGwtModuleMojo.GWT_MODULE_EXTENSION.length() );
            modules.add( path.replace( File.separatorChar, '.' ) );
        }
        if ( modules.size() > 0 )
        {
            log.info( "auto discovered modules " + modules );
        }
        return modules;
    }

    public GwtModule readModule( String name )
        throws GwtModuleReaderException
    {
        String modulePath = name.replace( '.', '/' ) + GWT_MODULE_EXTENSION;
        Collection<String> sourceRoots = mavenProject.getCompileSourceRoots();
        for ( String sourceRoot : sourceRoots )
        {
            File root = new File( sourceRoot );
            File xml = new File( root, modulePath );
            if ( xml.exists() )
            {
                log.debug( "GWT module " + name + " found in " + root );
                return readModule( name, xml );
            }
        }
        Collection<Resource> resources = (Collection<Resource>) mavenProject.getResources();
        for ( Resource resource : resources )
        {
            File root = new File( resource.getDirectory() );
            File xml = new File( root, modulePath );
            if ( xml.exists() )
            {
                log.debug( "GWT module " + name + " found in " + root );
                return readModule( name, xml );
            }
        }

        try
        {
            Collection<File> classpath = getClasspath( Artifact.SCOPE_COMPILE );
            URL[] urls = new URL[classpath.size()];
            int i = 0;
            for ( File file : classpath )
            {
                urls[i++] = file.toURI().toURL();
            }
            InputStream stream = new URLClassLoader( urls ).getResourceAsStream( modulePath );
            if ( stream != null )
            {
                return readModule( name, stream );
            }
        }
        catch ( MalformedURLException e )
        {
            // ignored;
        }
        catch ( ClasspathBuilderException e )
        {
            throw new GwtModuleReaderException( e.getMessage(), e );
        }

        throw new GwtModuleReaderException( "GWT Module " + name + " not found in project sources or resources." );
    }

    private GwtModule readModule( String name, File file )
        throws GwtModuleReaderException

    {
        try
        {
            return readModule( name, new FileInputStream( file ) );
        }
        catch ( FileNotFoundException e )
        {
            throw new GwtModuleReaderException( "Failed to read module file " + file );
        }
    }

    private GwtModule readModule( String name, InputStream xml )
        throws GwtModuleReaderException
    {
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( xml ) );
            return new GwtModule( name, dom, this );
        }
        catch ( Exception e )
        {
            String error = "Failed to read module XML file " + xml;
            log.error( error );
            throw new GwtModuleReaderException( error, e );
        }
    }

    public Collection<File> getClasspath( String scope )
        throws ClasspathBuilderException
    {
        Collection<File> files = classpathBuilder.buildClasspathList( mavenProject, scope, mavenProject.getArtifacts() );

        if ( log.isDebugEnabled() )
        {
            log.debug( "GWT SDK execution classpath :" );
            for ( File f : files )
            {
                log.debug( "   " + f.getAbsolutePath() );
            }
        }
        return files;
    }
}
