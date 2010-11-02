package org.codehaus.mojo.gwt.eclipse;

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

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author ndeloof
 * @version $Id$
 * @plexus.component role="org.codehaus.mojo.gwt.eclipse.EclipseUtil"
 */
public class EclipseUtil
    extends AbstractLogEnabled
{
    /**
     * Read the Eclipse project name for .project file. Fall back to artifactId on error
     *
     * @return project name in eclipse workspace
     */
    public String getProjectName( MavenProject project )
    {
        File dotProject = new File( project.getBasedir(), ".project" );
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( dotProject ) );
            return dom.getChild( "name" ).getValue();
        }
        catch ( Exception e )
        {
            getLogger().warn( "Failed to read the .project file" );
            return project.getArtifactId();
        }
    }
}
