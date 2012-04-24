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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.codehaus.mojo.gwt.webxml.GwtWebInfProcessor;
import org.codehaus.mojo.gwt.webxml.ServletDescriptor;

/**
 * @author cooper
 * @version $Id$
 */
public class MakeCatalinaBase
{
    private static final String GWT_DEV_SHELL = "com.google.gwt.dev.shell.GWTShellServlet";

    private File baseDir;

    private File sourceWebXml;

    private String shellServletMappingURL;

    /**
     * @param baseDir
     * @param sourceWebXml
     * @param shellServletMappingURL
     */
    public MakeCatalinaBase( File baseDir, File sourceWebXml, String shellServletMappingURL )
    {
        super();
        this.baseDir = baseDir;
        this.sourceWebXml = sourceWebXml;
        this.shellServletMappingURL = shellServletMappingURL;
    }

    public void setup()
        throws Exception
    {
        baseDir.mkdirs();

        File conf = new File( baseDir, "conf" );
        conf.mkdirs();

        File gwt = new File( conf, "gwt" );
        gwt.mkdirs();

        File localhost = new File( gwt, "localhost" );
        localhost.mkdirs();

        File webapps = new File( baseDir, "webapps" );
        webapps.mkdirs();

        File root = new File( webapps, "ROOT" );
        root.mkdirs();

        File webinf = new File( root, "WEB-INF" );
        webinf.mkdirs();
        new File( baseDir, "work" ).mkdirs();

        FileOutputStream fos = new FileOutputStream( new File( conf, "web.xml" ) );
        InputStream baseWebXml = getClass().getResourceAsStream( "baseWeb.xml" );
        if ( baseWebXml != null )
        {
            IOUtils.copy( baseWebXml, fos );
        }
        File mergeWebXml = new File( webinf, "web.xml" );
        if ( sourceWebXml.exists() )
        {
            ServletDescriptor d = new ServletDescriptor( shellServletMappingURL, GWT_DEV_SHELL );
            d.setName( "shell" );
            Collection<ServletDescriptor> servlets = Collections.singleton( d );

            new GwtWebInfProcessor().process( sourceWebXml, mergeWebXml, servlets );
        }
        else
        {
            fos = new FileOutputStream( mergeWebXml );
            IOUtils.copy( getClass().getResourceAsStream( "emptyWeb.xml" ), fos );
        }

    }
}
