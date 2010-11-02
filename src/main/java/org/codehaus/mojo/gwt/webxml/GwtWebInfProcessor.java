package org.codehaus.mojo.gwt.webxml;

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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.WriterFactory;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * @version $Id$
 */
public class GwtWebInfProcessor
{
    private static final String[] BEFORE_SERVLETS =
        { "icon", "display-name", "description", "distributable", "context-param", "filter", "filter-mapping",
            "listener", "servlet" };

    private static final String[] AFTER_SERVLETS =
        { "servlet-mapping", "session-config", "mime-mapping", "welcome-file-list", "error-page", "taglib",
            "resource-env-ref", "resource-ref", "security-constraint", "login-config", "security-role", "env-entry",
            "ejb-ref", "ejb-local-ref" };

    private static final String[] BEFORE_MAPPINGS =
        { "icon", "display-name", "description", "distributable", "context-param", "filter", "filter-mapping",
            "listener", "servlet", "servlet-mapping" };

    private static final String[] AFTER_MAPPINGS =
        { "session-config", "mime-mapping", "welcome-file-list", "error-page", "taglib", "resource-env-ref",
            "resource-ref", "security-constraint", "login-config", "security-role", "env-entry", "ejb-ref",
            "ejb-local-ref" };

    public void process( File webXml, Collection<ServletDescriptor> servletDescriptors )
        throws Exception
    {
        process( webXml, webXml, servletDescriptors );
    }

    public void process( File sourceWebXml, File mergeWebXml, Collection<ServletDescriptor> servletDescriptors )
        throws Exception
    {
        Document dom = insertServlets( sourceWebXml, servletDescriptors );
        XMLOutputter xmlOut = new XMLOutputter( Format.getPrettyFormat() );
        Writer writer = WriterFactory.newXmlWriter( mergeWebXml );
        xmlOut.output( dom, writer );
        writer.flush();
        writer.close();
    }

    private Document insertServlets( File webXml, Collection<ServletDescriptor> servletDescriptors )
        throws JDOMException, IOException
    {
        /*
         * <!ELEMENT web-app (icon?, display-name?, description?, distributable?, context-param*, filter*,
         * filter-mapping*, listener*, servlet*, servlet-mapping*, session-config?, mime-mapping*, welcome-file-list?,
         * error-page*, taglib*, resource-env-ref*, resource-ref*, security-constraint*, login-config?, security-role*,
         * env-entry*, ejb-ref*, ejb-local-ref*)>
         */
        Document dom = getWebXmlAsDocument( webXml );
        Element webapp = dom.getRootElement();
        Namespace ns = webapp.getNamespace();

        int insertAfter = getInsertPosition( webapp, BEFORE_SERVLETS, AFTER_SERVLETS );
        for ( Iterator<ServletDescriptor> it = servletDescriptors.iterator(); it.hasNext(); )
        {
            ServletDescriptor d = it.next();
            XPath path = XPath.newInstance( "/web-app/servlet/servlet-name[text() = '" + d.getName() + "']" );
            if ( path.selectNodes( dom ).size() > 0 )
            {
                // Allready declared in target web.xml
                it.remove();
                continue;
            }

            insertAfter++;
            Element servlet = new Element( "servlet", ns );
            Element servletName = new Element( "servlet-name", ns );
            servletName.setText( d.getName() );
            servlet.addContent( servletName );
            Element servletClass = new Element( "servlet-class", ns );
            servletClass.setText( d.getClassName() );
            servlet.addContent( servletClass );
            webapp.addContent( insertAfter, servlet );
        }
        insertAfter = getInsertPosition( webapp, BEFORE_MAPPINGS, AFTER_MAPPINGS );
        for ( ServletDescriptor d : servletDescriptors )
        {
            insertAfter++;
            Element servletMapping = new Element( "servlet-mapping", ns );
            Element servletName = new Element( "servlet-name", ns );
            servletName.setText( d.getName() );
            servletMapping.addContent( servletName );
            Element urlPattern = new Element( "url-pattern", ns );
            String path = d.getPath();
            if ( path.charAt( 0 ) != '/' )
            {
                path = '/' + path;
            }
            urlPattern.setText( path );
            servletMapping.addContent( urlPattern );
            webapp.addContent( insertAfter, servletMapping );
        }
        return dom;
    }

    private int getInsertPosition( Element webapp, String[] startAfter, String[] stopBefore )
        throws JDOMException, IOException
    {
        List children = webapp.getContent();
        Content insertAfter = new Comment( "inserted by gwt-maven-plugin" );

        ArrayList<String> namesBefore = new ArrayList<String>();
        ArrayList<String> namesAfter = new ArrayList<String>();

        for ( int i = 0; i < startAfter.length; i++ )
        {
            namesBefore.add( startAfter[i] );
        }

        for ( int i = 0; i < stopBefore.length; i++ )
        {
            namesAfter.add( stopBefore[i] );
        }

        if ( ( children == null ) || ( children.size() == 0 ) )
        {
            webapp.addContent( insertAfter );
        }
        else
        {
            boolean foundPoint = false;
            for ( int i = 0; !foundPoint && i < children.size(); i++ )
            {
                Object o = children.get( i );
                if ( !( o instanceof Element ) )
                {
                    continue;
                }

                Element child = (Element) o;

                if ( namesAfter.contains( child.getName() ) )
                {
                    webapp.addContent( i, insertAfter );
                    foundPoint = true;
                    break;
                }

                if ( !namesBefore.contains( child.getName() ) )
                {
                    webapp.addContent( i + 1, insertAfter );
                    foundPoint = true;
                    break;
                }
            }
            if ( !foundPoint )
            {
                webapp.addContent( insertAfter );
            }
        }

        return webapp.indexOf( insertAfter );
    }

    private Document getWebXmlAsDocument( File webXml )
        throws JDOMException, IOException
    {
        SAXBuilder builder = new SAXBuilder( false );
        builder.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
        return builder.build( webXml.toURI().toURL() );
    }
}
