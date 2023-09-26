package org.codehaus.mojo.gwt.utils;

import org.apache.commons.logging.Log;
import org.codehaus.plexus.util.AbstractScanner;

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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import cern.colt.Arrays;

/**
 * Scans jar files.
 *
 * @author fabien.cortina at gmail.com
 */
public final class JarFileScanner extends AbstractScanner
{
    private final File jarFile;
    private final Set<String> matchingEntries;
    private final Log log;

    public JarFileScanner( File jarFile, Log log )
    {
        this.jarFile = jarFile;
        this.matchingEntries = new HashSet<String>();
        this.log = log;
    }

    public String[] getIncludedFiles()
    {
        return matchingEntries.toArray( new String[ matchingEntries.size() ] );
    }

    public String[] getIncludedDirectories()
    {
        return new String[0];
    }

    public File getBasedir()
    {
        return null;
    }

    public void scan()
    {
        matchingEntries.clear();

        try
        {
            setupDefaultFilters();
            setupMatchPatterns();

            scanJarFileEntries();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Error scanning file " + jarFile, e );
        }
    }

    private void scanJarFileEntries()
        throws IOException
    {
        InputStream fileInputStream = new FileInputStream( jarFile );
        InputStream bufferedInputStream = new BufferedInputStream( fileInputStream );
        JarInputStream jarInputStream = new JarInputStream( bufferedInputStream );

        JarEntry jarEntry;
        while ( ( jarEntry = jarInputStream.getNextJarEntry() ) != null )
        {
            String entryName = jarEntry.getName();
            if ( isIncluded( entryName ) )
            {
                if ( !isExcluded( entryName ) )
                {
                    matchingEntries.add(entryName);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("entry " + entryName + " rejected; includes: " + Arrays.toString(
                            includes) + "; excludes: " + Arrays.toString(excludes));
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("entry " + entryName + " rejected; includes: " + Arrays.toString(
                        includes) + "; excludes: " + Arrays.toString(excludes));
                }
            }
            jarInputStream.closeEntry();
        }

        jarInputStream.close();
    }
}
