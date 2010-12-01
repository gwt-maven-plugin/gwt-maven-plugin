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
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Creates CSS interfaces for css files.
 * Will use the utility tool provided in gwt sdk which create a corresponding Java interface for accessing 
 * the classnames used in the file.
 * @goal css
 * @author Stale Undheim <undheim@corporater.com>
 * @author olamy
 * @since 2.1.0-1
 */
public class CSSMojo
    extends AbstractGwtShellMojo
{
    /**
     * List of resourceBundles that should be used to generate CSS interfaces.
     * 
     * @parameter
     */
    private String[] cssFiles;

    /**
     * Shortcut for a single cssFile
     * 
     * @parameter
     */
    private String cssFile;

    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        setup();
        boolean generated = false;

        // java -cp gwt-dev.jar:gwt-user.jar
        // com.google.gwt.resources.css.InterfaceGenerator -standalone -typeName some.package.MyCssResource -css
        // input.css
        if ( cssFiles != null )
        {
            for ( String file : cssFiles )
            {
                final String typeName = FilenameUtils.separatorsToSystem( file ).
                    substring( 0, file.lastIndexOf( '.' ) ).replace( File.separatorChar, '.' );
                final File javaOutput =
                    new File( getGenerateDirectory(), typeName.replace( '.', File.separatorChar ) + ".java" );
                final StringBuilder content = new StringBuilder();
                out = new StreamConsumer()
                {
                    public void consumeLine( String line )
                    {
                        content.append( line ).append( SystemUtils.LINE_SEPARATOR );
                    }
                };
                for ( Resource resource : (List<Resource>) getProject().getResources() )
                {
                    final File candidate = new File( resource.getDirectory(), file );
                    if ( candidate.exists() )
                    {
                        getLog().info( "Generating " + javaOutput + " with typeName " + typeName );
                        ensureTargetPackageExists( getGenerateDirectory(), typeName );

                        
                        try
                        {
                            new JavaCommand( "com.google.gwt.resources.css.InterfaceGenerator" )
                            .withinScope( Artifact.SCOPE_COMPILE )
                            .arg( "-standalone" )
                            .arg( "-typeName" )
                            .arg( typeName )
                            .arg( "-css" )
                            .arg( candidate.getAbsolutePath() )
                            .withinClasspath( getGwtDevJar() , getGwtUserJar() )
                            .execute();                            
                            final FileWriter outputWriter = new FileWriter( javaOutput );
                            outputWriter.write( content.toString() );
                            outputWriter.close();
                        }
                        catch ( IOException e )
                        {
                            throw new MojoExecutionException( "Failed to write to file: " + javaOutput );
                        }
                        generated = true;
                        break;
                    }
                }
                
                if ( content.length() == 0 )
                {
                    throw new MojoExecutionException( "cannot generate java source from file " + file + "." );
                }
            }
        }

        if ( generated )
        {
            getLog().debug( "add compile source root " + getGenerateDirectory() );
            addCompileSourceRoot( getGenerateDirectory() );
        }

    }

    private void setup()

        throws MojoExecutionException
    {
        if ( cssFiles == null && cssFile != null )
        {
            cssFiles = new String[] { cssFile };
        }

    }

    private void ensureTargetPackageExists( File generateDirectory, String targetName )
    {
        targetName = targetName.contains( "." ) ? targetName.substring( 0, targetName.lastIndexOf( '.' ) ) : targetName;
        String targetPackage = targetName.replace( '.', File.separatorChar );
        getLog().debug( "ensureTargetPackageExists, targetName : " + targetName + ", targetPackage : " + targetPackage );
        File targetPackageDirectory = new File( generateDirectory, targetPackage );
        if ( !targetPackageDirectory.exists() )
        {
            targetPackageDirectory.mkdirs();
        }
    }

}
