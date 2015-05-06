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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Creates CSS interfaces for css files.
 * Will use the utility tool provided in gwt sdk which create a corresponding Java interface for accessing 
 * the classnames used in the file.
 *
 * @author Stale Undheim <undheim@corporater.com>
 * @author olamy
 * @since 2.1.0-1
 */
@Mojo(name = "css", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CSSMojo
    extends AbstractGwtShellMojo
{
    /**
     * List of resourceBundles that should be used to generate CSS interfaces.
     */
    @Parameter
    private String[] cssFiles;

    /**
     * Shortcut for a single cssFile
     */
    @Parameter
    private String cssFile;

    @Parameter(property = "project.build.sourceEncoding")
    private String encoding;

    @Component
    private BuildContext buildContext;

    @Override
    protected boolean isGenerator() {
        return true;
    }

    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        setup();

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
                for ( Resource resource : getProject().getResources() )
                {
                    final File candidate = new File( resource.getDirectory(), file );
                    if ( candidate.exists() )
                    {
                        if ( buildContext.isUptodate( javaOutput, candidate ) )
                        {
                            getLog().debug( javaOutput.getAbsolutePath() + " is up to date. Generation skipped" );
                            break;
                        }

                        getLog().info( "Generating " + javaOutput + " with typeName " + typeName );
                        ensureTargetPackageExists( getGenerateDirectory(), typeName );

                        
                        try
                        {
                            final StringBuilder content = new StringBuilder();
                            createJavaCommand()
                                .setMainClass( "com.google.gwt.resources.css.InterfaceGenerator" )
                                .addToClasspath( getClasspath( Artifact.SCOPE_COMPILE ) )
                                .arg( "-standalone" )
                                .arg( "-typeName" )
                                .arg( typeName )
                                .arg( "-css" )
                                .arg( candidate.getAbsolutePath() )
                                .addToClasspath( getGwtDevJar() )
                                .addToClasspath( getGwtUserJar() )
                                .setOut( new StreamConsumer()
                                    {
                                        public void consumeLine( String line )
                                        {
                                            content.append( line ).append( SystemUtils.LINE_SEPARATOR );
                                        }
                                    } )
                                .execute();
                            if ( content.length() == 0 )
                            {
                                throw new MojoExecutionException( "cannot generate java source from file " + file + "." );
                            }
                            final OutputStreamWriter outputWriter =
                                new OutputStreamWriter( buildContext.newFileOutputStream( javaOutput ) , encoding );
                            try {
                                outputWriter.write( content.toString() );
                            } finally {
                                IOUtil.close( outputWriter );
                            }
                        }
                        catch ( IOException e )
                        {
                            throw new MojoExecutionException( "Failed to write to file: " + javaOutput, e );
                        }
                        catch ( JavaCommandException e )
                        {
                            throw new MojoExecutionException( e.getMessage(), e );
                        }
                        break;
                    }
                }
            }
        }
    }

    private void setup()
    {
        setupGenerateDirectory();

        if ( encoding == null )
        {
            getLog().warn( "Encoding is not set, your build will be platform dependent" );
            encoding = Charset.defaultCharset().name();
        }

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
