package org.codehaus.mojo.gwt.utils;

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

import java.io.*;
import java.util.*;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Scans a project and/or artifacts.
 *
 * @author fabien.cortina at gmail.com
 */
public final class ProjectScanner extends AbstractScanner
{
    private final Log log;
    private final SortedSet<String> matchingFiles;

    private MavenProject project;
    private Collection<File> artifacts;

    public ProjectScanner( Log log )
    {
        this.log = log;
        this.matchingFiles = new TreeSet<String>();
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public Collection<File> getArtifacts()
    {
        return artifacts == null ? Collections.<File>emptyList() : artifacts;
    }

    public void setArtifacts( Collection<File> artifacts )
    {
        this.artifacts = artifacts;
    }

    public String[] getIncludedFiles()
    {
        return matchingFiles.toArray( new String[ matchingFiles.size() ] );
    }

    public String[] getIncludedDirectories()
    {
        return new String[0];
    }

    public File getBasedir()
    {
        return project.getBasedir();
    }

    public void scan()
    {
        matchingFiles.clear();

        for ( File root : getSourceRoots() )
        {
            scan( root );
        }

        for ( File artifact : getArtifacts() )
        {
            scan( artifact );
        }
    }

    private Collection<File> getSourceRoots()
    {
        Collection<File> directories = new LinkedList<File>();
        if ( project != null )
        {
            Collection<String> roots = (Collection<String>) project.getCompileSourceRoots();
            for ( String root : roots )
            {
                File directory = new File( root );
                directories.add( directory );
            }
        }
        return directories;
    }

    private void scan( File fileOrDirectory )
    {
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Scanning: " + fileOrDirectory.getPath() );
            }

            AbstractScanner scanner = scannerFor( fileOrDirectory );
            scanner.setIncludes( includes );
            scanner.setExcludes( excludes );
            scanner.scan();

            Collections.addAll( matchingFiles, scanner.getIncludedFiles() );
        }
        catch ( FileNotFoundException e )
        {
            log.warn( e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            log.warn( e.getMessage() );
        }
        catch ( Throwable t )
        {
            log.warn( "Error while scanning '" + fileOrDirectory.getPath() + "'", t );
        }
    }

    private AbstractScanner scannerFor( File fileOrDirectory )
            throws FileNotFoundException, IllegalArgumentException {
        if ( !fileOrDirectory.exists() )
        {
            throw new FileNotFoundException( "File does not exist: " + fileOrDirectory );
        }
        else if ( fileOrDirectory.isDirectory() )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( fileOrDirectory );
            return scanner;
        }
        else if ( fileOrDirectory.getName().endsWith( ".jar") )
        {
            return new JarFileScanner( fileOrDirectory );
        }
        else
        {
            throw new IllegalArgumentException( "Unexpected file: " + fileOrDirectory );
        }
    }
}
