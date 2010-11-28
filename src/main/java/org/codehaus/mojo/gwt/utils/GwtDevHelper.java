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

import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


/**
 * Helper class to resolve gwt-dev jar artifact 
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
public class GwtDevHelper
{
    private final MavenProject mavenProject;

    private final Map<String, Artifact> pluginArtifacts;

    private final Log log;

    private final String gwtGroupId;

    public GwtDevHelper( Map<String, Artifact> pluginArtifacts, MavenProject mavenProject, Log log, String gwtGroupId )
    {
        this.pluginArtifacts = pluginArtifacts;
        this.mavenProject = mavenProject;
        this.log = log;
        this.gwtGroupId = gwtGroupId;
    }

    public File getGwtDevJar()
        throws IOException
    {
        checkGwtDevAsDependency();
        checkGwtUserVersion();
        return pluginArtifacts.get( "com.google.gwt:gwt-dev" ).getFile();
    }

    public void checkGwtDevAsDependency()
    {
        for ( Iterator iterator = this.mavenProject.getArtifacts().iterator(); iterator.hasNext(); )
        {
            Artifact artifact = (Artifact) iterator.next();
            if ( this.gwtGroupId.equals( artifact.getGroupId() ) && "gwt-dev".equals( artifact.getArtifactId() )
                && !SCOPE_TEST.equals( artifact.getScope() ) )
            {
                log.warn( "Don't declare gwt-dev as a project dependency. This may introduce complex dependency conflicts" );
            }
        }
    }

    private void checkGwtUserVersion()
        throws IOException
    {
        String resource = "org/codehaus/mojo/gwt/mojoGwtVersion.properties";
        InputStream inputStream = getClass().getResourceAsStream( "/" + resource );
        //Thread.currentThread().getContextClassLoader().getResourceAsStream( resource );
        if ( inputStream == null )
        {
            log.info( "skip impossible to load properties file " + resource + " gwt version check will be ignored" );
            return;
        }
        Properties properties = new Properties();
        try
        {
            properties.load( inputStream );

        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
        for ( Iterator iterator = this.mavenProject.getCompileArtifacts().iterator(); iterator.hasNext(); )
        {
            Artifact artifact = (Artifact) iterator.next();
            if ( this.gwtGroupId.equals( artifact.getGroupId() ) && "gwt-user".equals( artifact.getArtifactId() ) )
            {
                String mojoGwtVersion = properties.getProperty( "gwt.version" );
                //ComparableVersion with an up2date maven version
                ArtifactVersion mojoGwtArtifactVersion = new DefaultArtifactVersion( mojoGwtVersion );
                ArtifactVersion userGwtArtifactVersion = new DefaultArtifactVersion( artifact.getVersion() );
                if ( userGwtArtifactVersion.compareTo( mojoGwtArtifactVersion ) < 0 )
                {
                    log.warn( "You're project declares dependency on gwt-user " + artifact.getVersion()
                        + ". This plugin is designed for at least gwt version " + mojoGwtVersion );
                }
                break;
            }
        }
    }
}
