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

import java.util.Set;

import org.codehaus.mojo.gwt.servlets.HelloRemoteServlet;
import org.codehaus.mojo.gwt.webxml.ServletAnnotationFinder;
import org.codehaus.mojo.gwt.webxml.ServletDescriptor;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 */
public class ServletAnnotationFinderTest
    extends PlexusTestCase
{
    public void testFindServletEmptyPath()
        throws Exception
    {
        ServletAnnotationFinder servletAnnotationFinder = (ServletAnnotationFinder) lookup( ServletAnnotationFinder.class
            .getName() );
        Set<ServletDescriptor> servletDescriptors = servletAnnotationFinder.findServlets( "org.codehaus.mojo.gwt",
                                                                                          null, Thread.currentThread()
                                                                                              .getContextClassLoader() );

        assertEquals( 1, servletDescriptors.size() );
        ServletDescriptor desc = servletDescriptors.iterator().next();
        assertEquals( HelloRemoteServlet.class.getName(), desc.getClassName() );
        assertEquals( "/HelloService", desc.getPath() );
    }

    public void testFindServletWithPath()
        throws Exception
    {
        ServletAnnotationFinder servletAnnotationFinder = (ServletAnnotationFinder) lookup( ServletAnnotationFinder.class
            .getName() );
        Set<ServletDescriptor> servletDescriptors = servletAnnotationFinder.findServlets( "org.codehaus.mojo.gwt",
                                                                                          "foo", Thread.currentThread()
                                                                                              .getContextClassLoader() );

        assertEquals( 1, servletDescriptors.size() );
        ServletDescriptor desc = servletDescriptors.iterator().next();
        assertEquals( HelloRemoteServlet.class.getName(), desc.getClassName() );
        assertEquals( "/foo/HelloService", desc.getPath() );
    }

    public void testFindServletWithPrependPath()
        throws Exception
    {
        ServletAnnotationFinder servletAnnotationFinder = (ServletAnnotationFinder) lookup( ServletAnnotationFinder.class
            .getName() );
        Set<ServletDescriptor> servletDescriptors = servletAnnotationFinder.findServlets( "org.codehaus.mojo.gwt",
                                                                                          "/foo", Thread
                                                                                              .currentThread()
                                                                                              .getContextClassLoader() );

        assertEquals( 1, servletDescriptors.size() );
        ServletDescriptor desc = servletDescriptors.iterator().next();
        assertEquals( HelloRemoteServlet.class.getName(), desc.getClassName() );
        assertEquals( "/foo/HelloService", desc.getPath() );
    }  

}
