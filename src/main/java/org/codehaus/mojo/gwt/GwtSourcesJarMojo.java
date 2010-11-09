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
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.jar.JarArchiver;


/**
 * Add GWT java source code and module descriptor as resources to project jar. Alternative
 * to gwt:resources for better Eclipse projects synchronization.
 * 
 * @author <a href="mailto:vlads@pyx4j.com">Vlad Skarzhevskyy</a>
 * @goal source-jar
 * @phase package
 */
public class GwtSourcesJarMojo
    extends GwtResourcesBaseMojo
{

    /**
     * Name of the generated JAR.
     * 
     * @parameter alias="jarName" expression="${jar.finalName}"
     *            default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The Jar archiver.
     * 
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * The archive configuration to use. See <a
     * href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     * 
     * @parameter
     */
    private final MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File jarFile = new File( outputDirectory, finalName + ".jar" );
        File origJarFile = new File( outputDirectory, finalName + "-b4gwt.jar" );
        if ( origJarFile.exists() )
        {
            if ( !origJarFile.delete() )
            {
                throw new MojoExecutionException( "Error removing " + origJarFile );
            }
        }
        if ( !jarFile.renameTo( origJarFile ) )
        {
            throw new MojoExecutionException( "Error renaming " + jarFile + " to " + origJarFile );
        }

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );
        archiver.setOutputFile( jarFile );
        archive.setForced( false );
        // It is already created by maven-jar-plugin
        archive.setAddMavenDescriptor( false );

        Collection<ResourceFile> files = getAllResourceFiles();
        try
        {

            // Avoid annoying messages in log "com/package/Ccc.java already added, skipping"
            List<String> jarExcludes = new Vector<String>();

            // Add Sources first since they may already be present in jar from previous run and changed.
            for ( ResourceFile file : files )
            {
                jarArchiver.addFile( new File( file.basedir, file.fileRelativeName ), file.fileRelativeName );
                jarExcludes.add( file.fileRelativeName );
            }

            // Add the context of original jar excluding resources that we just added base on GWT descriptors
            jarArchiver.addArchivedFileSet( origJarFile, null, jarExcludes.toArray( new String[jarExcludes.size()] ) );

            archiver.createArchive( getProject(), archive );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling JAR", e );
        }

    }

}
