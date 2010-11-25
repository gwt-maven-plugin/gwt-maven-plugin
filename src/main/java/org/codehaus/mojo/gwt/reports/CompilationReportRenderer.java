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


import java.util.List;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.mojo.gwt.GwtModule;

/**
 * project compilation report renderer to display links to 
 * all modules report
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.1
 */
public class CompilationReportRenderer
    extends AbstractMavenReportRenderer
{
    private final List<GwtModule> gwtModules;
    
    private final Log log;
    
    private boolean soycRawReport;
    
    public CompilationReportRenderer( final Sink sink, final List<GwtModule> gwtModules, Log log, boolean soycRawReport)
    {
        super( sink );
        this.gwtModules = gwtModules;
        this.log = log;
        this.soycRawReport = soycRawReport;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReportRenderer#getTitle()
     */
    @Override
    public String getTitle()
    {
        // TODO i18n
        return "GWT Compilation Reports";
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
     */
    @Override
    protected void renderBody()
    {
        // TODO i18n and message for none
        log.debug( "start renderBody" );
        startSection( "GWT Compilation Reports" );
        if ( !this.soycRawReport )
        {
            sink.paragraph();
            sink.bold();
            sink.text( "No SOYC raw report found, did you compile with soyc option set ?"  );
            sink.bold_();
            sink.paragraph_();
        }
        else
        {
            sink.list();
            for ( GwtModule gwtModule : this.gwtModules )
            {

                sink.listItem();
                sink.link( "./" + gwtModule.getPath() + "/index.html" );
                sink.text( gwtModule.getName() );
                sink.link_();
                sink.listItem_();
            }
            sink.list_();
        }
        endSection();
        log.debug( "end renderBody" );
    }

}
