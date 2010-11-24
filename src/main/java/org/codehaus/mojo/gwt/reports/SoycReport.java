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
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo;

/**
 * @see http://code.google.com/p/google-web-toolkit/wiki/CodeSplitting#The_Story_of_Your_Compile_(SOYC)
 * @goal soyc
 * @phase site
 */
public class SoycReport
    extends AbstractGwtShellMojo
    implements MavenReport
{

    /**
     * The output directory of the jsdoc report.
     *
     * @parameter expression="${project.reporting.outputDirectory}/soyc"
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
     * {@inheritDoc}
     *
     * @see org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo#doExecute()
     */
    @Override
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( extra );
        scanner.setIncludes( new String[] { "**/soycReport/stories0.xml.gz" } );
        scanner.scan();

        if ( scanner.getIncludedFiles().length == 0 )
        {
            getLog().warn( "No SOYC raw report found, did you compile with soyc option set ?" );
            return;
        }

        for ( String path : scanner.getIncludedFiles() )
        {
            try
            {
                String module = path.substring( 0, path.indexOf( File.separatorChar ) );
                JavaCommand cmd = new JavaCommand( "com.google.gwt.soyc.SoycDashboard" )
                    .withinClasspath( getGwtDevJar() )
                    //  FIXME
                    // .withinClasspath( runtime.getSoycJar() )
                    //  .arg( "-resources" ).arg( runtime.getSoycJar().getAbsolutePath() )
                    .arg( "-out" ).arg( reportingOutputDirectory.getAbsolutePath() + File.separatorChar + module );

                cmd.arg( new File( extra, path ).getAbsolutePath() );
                cmd.arg( new File( extra, path ).getAbsolutePath().replace( "stories", "dependencies" ) );
                cmd.arg( new File( extra, path ).getAbsolutePath().replace( "stories", "splitPoints" ) );
                cmd.execute();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }            
        }
    }

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
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        try
        {
            doExecute( );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Failed to execute SoycDashboard", e);
        }
        catch ( MojoFailureException e )
        {
            throw new MavenReportException( "Failed to execute SoycDashboard", e );
        }
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
        return "GWT Story Of Your Compiler";
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "GWT Story Of Your Compiler";
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
     * @see org.apache.maven.reporting.MavenReport#getReportOutputDirectory()
     */
    public File getReportOutputDirectory()
    {
        return reportingOutputDirectory;
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

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.reporting.MavenReport#setReportOutputDirectory(java.io.File)
     */
    public void setReportOutputDirectory( File outputDirectory )
    {
        reportingOutputDirectory = outputDirectory;
    }

}
