package org.codehaus.mojo.gwt.reports;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.mojo.gwt.AbstractGwtMojo;
import org.codehaus.mojo.gwt.ClasspathBuilder;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.GwtModuleReader;
import org.codehaus.mojo.gwt.shell.JavaCommand;
import org.codehaus.mojo.gwt.shell.JavaCommandRequest;
import org.codehaus.mojo.gwt.utils.DefaultGwtModuleReader;
import org.codehaus.mojo.gwt.utils.GwtDevHelper;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * @see http://code.google.com/p/google-web-toolkit/wiki/CodeSplitting#The_Story_of_Your_Compile_(SOYC)
 * @goal soyc
 * @requiresDependencyResolution runtime
 * @deprecated You must now use the CompileReport, SoycDashboard is not anymore supported will be removed in 2.1.2
 */
public class SoycReport
    extends AbstractMavenReport
{

    /**
     * The output directory of the jsdoc report.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/soyc"
     * @required
     * @readonly
     */
    protected File reportingOutputDirectory;

    /**
     * The directory into which extra, non-deployed files will be written.
     *
     * @parameter default-value="${project.build.directory}/extra"
     */
    private File extra;
    
    /**
     * Doxia Site Renderer component.
     *
     * @component
     * @since 2.1.0-1
     */
    protected Renderer siteRenderer;  
    
    /**
     * The output directory for the report. Note that this parameter is only evaluated if the goal is run directly from
     * the command line. If the goal is run indirectly as part of a site generation, the output directory configured in
     * the Maven Site Plugin is used instead.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     * @since 2.1.0-1
     */    
    protected File outputDirectory;
    
    /**
     * The Maven Project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     * @since 2.1.0-1
     */
    protected MavenProject project;    
    
    /**
     * @parameter default-value="${plugin.artifactMap}"
     * @required
     * @readonly
     * @since 2.1.0-1
     */
    private Map<String, Artifact> pluginArtifacts;    
    
    /**
     * @component
     * @since 2.1.0-1
     */
    protected ClasspathBuilder classpathBuilder;    
    
    /**
     * @parameter default-value="false" expression="${gwt.soycReport.skip}"
     * @since 2.1.0-1
     */
    private boolean skip;
    
    /**
     * Internationalization component.
     *
     * @component
     * @since 2.1.0-1
     */
    protected I18N i18n;    

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getI18nString( locale, "soyc.report.description" );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getI18nString( locale, "soyc.report.name" );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "soyc";
    }


    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#isExternalReport()
     */
    public boolean isExternalReport()
    {
        return false;
    }


    @Override
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    @Override
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    @Override
    protected MavenProject getProject()
    {
        return project;
    }

    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        StringBuilder message = new StringBuilder();
        message.append( "--------------------------------------------------------------------------" );
        message.append( SystemUtils.LINE_SEPARATOR );
        message.append( getI18nString( locale, "soyc.report.warning" ) );
        message.append( SystemUtils.LINE_SEPARATOR );
        message.append( "--------------------------------------------------------------------------" );
        getLog().warn( message.toString() );
        if ( skip )
        {
            getLog().info( "Soyc Report is skipped" );
            return;
        }
        
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( extra );
        scanner.setIncludes( new String[] { "**/soycReport/stories0.xml.gz" } );
        
        boolean soycRawReport = true;

        if ( extra.exists() )
        {
            scanner.scan();
        }
        else
        {
            soycRawReport = false;
        }

        if (!soycRawReport || scanner.getIncludedFiles().length == 0 )
        {
            getLog().warn( "No SOYC raw report found, did you compile with soyc option set ?" );
            soycRawReport = false;
        }
        
        GwtDevHelper gwtDevHelper = new GwtDevHelper( pluginArtifacts, project, getLog(), AbstractGwtMojo.GWT_GROUP_ID );
        String[] includeFiles = soycRawReport ? scanner.getIncludedFiles() : new String[0];

        for ( String path : includeFiles )
        {
            try
            {
                //Usage: java com.google.gwt.soyc.SoycDashboard -resources dir -soycDir dir -symbolMaps dir [-out dir]
                String module = path.substring( 0, path.indexOf( File.separatorChar ) );
                JavaCommandRequest javaCommandRequest = new JavaCommandRequest()
                    .setClassName( "com.google.gwt.soyc.SoycDashboard" )
                    .setLog( getLog() );
                JavaCommand cmd = new JavaCommand( javaCommandRequest ).withinClasspath( gwtDevHelper.getGwtDevJar() )
                    .arg( "-out" ).arg( reportingOutputDirectory.getAbsolutePath() + File.separatorChar + module );

                cmd.arg( new File( extra, path ).getAbsolutePath() );
                cmd.arg( new File( extra, path ).getAbsolutePath().replace( "stories", "dependencies" ) );
                cmd.arg( new File( extra, path ).getAbsolutePath().replace( "stories", "splitPoints" ) );
                cmd.execute();
            }
            catch ( Exception e )
            {
                getLog().warn( e.getMessage(), e );
                new CompilationReportRenderer( getSink(), new ArrayList<GwtModule>( 0 ), getLog(), soycRawReport,
                                               "soyc", false, i18n, locale ).render();
            }
        }
        
        try
        {

            GwtModuleReader gwtModuleReader = new DefaultGwtModuleReader( this.project, getLog(), classpathBuilder );

            List<GwtModule> gwtModules = new ArrayList<GwtModule>();
            List<String> moduleNames = gwtModuleReader.getGwtModules();
            for ( String name : moduleNames )
            {
                gwtModules.add( gwtModuleReader.readModule( name ) );
            }
            // add link in the page to all module reports
            CompilationReportRenderer compilationReportRenderer = new CompilationReportRenderer( getSink(), gwtModules,
                                                                                                 getLog(),
                                                                                                 soycRawReport, "soyc",
                                                                                                 false, i18n, locale );
            compilationReportRenderer.render();
        }
        catch ( GwtModuleReaderException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    protected String getI18nString( Locale locale, String key )
    {
        return i18n.getString( "compile-report", locale, key );
    }    
    
}
