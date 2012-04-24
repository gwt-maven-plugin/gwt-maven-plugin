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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.mojo.gwt.ClasspathBuilder;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.GwtModuleReader;
import org.codehaus.mojo.gwt.utils.DefaultGwtModuleReader;
import org.codehaus.mojo.gwt.utils.GwtModuleReaderException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * see http://code.google.com/webtoolkit/doc/latest/DevGuideCompileReport.html#Usage
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @goal compile-report
 * @since 2.1.0-1
 */
public class CompileReport
    extends AbstractMavenReport
{

    /**
     * The output directory of the gwt compiler reports.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/gwtCompileReports"
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
     * @component
     * @since 2.1.0-1
     */
    protected ClasspathBuilder classpathBuilder;    
    
    /**
     * @parameter default-value="false" expression="${gwt.compilerReport.skip}"
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
        // TODO check the compiler has created the raw xml soyc file
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
        return "GWT Compiler Report";
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "GWT Compiler Report";
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "gwt-compiler-reports";
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
   
    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        
        if ( skip )
        {
            getLog().info( "Compiler Report is skipped" );
            return;
        }        
        
        if ( !reportingOutputDirectory.exists() )
        {
            reportingOutputDirectory.mkdirs();
        }
        boolean compileReports = true;

        //compile-report
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( extra );
        scanner.setIncludes( new String[] { "**/soycReport/compile-report/index.html" } );
        
        if (extra.exists())
        {
            scanner.scan();
        } else
        {
            compileReports = false;
        }
        
        if (!compileReports || scanner.getIncludedFiles().length == 0 )
        {
            getLog().warn( "No compile reports found, did you compile with compileReport option set ?" );
            compileReports = false;
        }
        
        String[] includeFiles = compileReports ? scanner.getIncludedFiles() : new String[0];
        
        for ( String path : includeFiles )
        {
            String module = path.substring( 0, path.indexOf( File.separatorChar ) );
            File dirTarget = new File( reportingOutputDirectory.getAbsolutePath() + File.separatorChar + module );
            getLog().debug( "file in path " + path + " to target " + dirTarget.getAbsolutePath());
            try
            {
                FileUtils.copyDirectoryStructure( new File(extra, path).getParentFile(), dirTarget );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( e.getMessage(), e );
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
                                                                                                 compileReports,
                                                                                                 "gwtCompileReports",
                                                                                                 true, i18n, locale );
            compilationReportRenderer.render();
        }
        catch ( GwtModuleReaderException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

    }



}
