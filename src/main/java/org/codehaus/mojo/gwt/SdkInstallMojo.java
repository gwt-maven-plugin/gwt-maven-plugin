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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Install a GWT (home built) SDK in local repository
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @goal sdkInstall
 */
public class SdkInstallMojo
    extends AbstractGwtModuleMojo
{

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter expression="${sdk}"
     * @required
     * @readonly
     */
    protected File sdk;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String groupId = "com.google.gwt";
        String artifactId = "gwt-user";
        String version = "?";

        Artifact artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, "jar", null );

        try
        {
            installer.install( null, artifact, localRepository );
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Error installing artifact '" + artifact.getDependencyConflictId()
                + "': " + e.getMessage(), e );
        }
    }

}