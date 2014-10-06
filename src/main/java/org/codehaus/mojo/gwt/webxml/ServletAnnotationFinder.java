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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;



/**
 * The goal is to find classed annotated with {@link RemoteServiceRelativePath}
 * to generated {@link ServletDescriptor}
 * 
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 * @since 2.1.0-1
 */
@Component(role = ServletAnnotationFinder.class)
public class ServletAnnotationFinder
    extends AbstractLogEnabled
{

    public ServletAnnotationFinder()
    {
        // no op
    }

	private MetadataReader getMetadataReader(String className,
											 MetadataReaderFactory factory,
											 PathMatchingResourcePatternResolver resourceResolver)
		throws IOException
	{
		String resourcePath = ClassUtils.convertClassNameToResourcePath(className);
		Resource resource = resourceResolver.getResource(resourcePath + ".class");
		if (resource.exists()) {
			return factory.getMetadataReader(resource);
		}
		return null;
	}

	private boolean extendsRemoteServlet(ClassMetadata classMetadata,
										 MetadataReaderFactory factory,
										 PathMatchingResourcePatternResolver resourceResolver)
		throws IOException
	{
		if (classMetadata.hasSuperClass()) {
			String name = classMetadata.getSuperClassName();
			if (name.equals("com.google.gwt.user.server.rpc.RemoteServiceServlet")) {
				return true;
			}
			MetadataReader r = getMetadataReader(classMetadata.getSuperClassName(), factory, resourceResolver);
			return extendsRemoteServlet(r.getClassMetadata(), factory, resourceResolver);
		}
		return false;
	}

	private AnnotationMetadata getAnnotationMetadataIfServlet(MetadataReader metadataReader,
															  MetadataReaderFactory factory,
															  PathMatchingResourcePatternResolver resourceResolver)
	{
		ClassMetadata classMetadata = metadataReader.getClassMetadata();

		try {
			if (classMetadata.isConcrete() && extendsRemoteServlet(classMetadata, factory, resourceResolver)) {
				for (String i : metadataReader.getClassMetadata().getInterfaceNames()) {
					MetadataReader r = getMetadataReader(i, factory, resourceResolver);
					if (r != null && r.getAnnotationMetadata()
						.hasAnnotation( RemoteServiceRelativePath.class.getName() )) {
						return r.getAnnotationMetadata();
					}
				}
			}
		} catch (IOException e) {
			getLogger().warn("Failed to read class metadata: " + e);
		}
		return null;
	}

    /**
     * @param packageName
     * @return cannot return <code>null</null>
     * @throws IOException
     */
    public Set<ServletDescriptor> findServlets( String packageName, String startPath, ClassLoader classLoader )
        throws IOException
    {
        Set<ServletDescriptor> servlets = new LinkedHashSet<ServletDescriptor>();
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver(
                                                                                                                           classLoader );
        String patternFinder = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + ClassUtils.convertClassNameToResourcePath( packageName ) + "/**/*.class";

        Resource[] resources = pathMatchingResourcePatternResolver.getResources( patternFinder );
        SimpleMetadataReaderFactory simpleMetadataReaderFactory = new SimpleMetadataReaderFactory();
        getLogger().debug( "springresource " + resources.length + " for pattern " + patternFinder );
        for ( Resource resource : resources )
        {
            getLogger().debug( "springresource " + resource.getFilename() );
            MetadataReader metadataReader = simpleMetadataReaderFactory.getMetadataReader( resource );

			AnnotationMetadata annotationMetadata = getAnnotationMetadataIfServlet(metadataReader,
																				   simpleMetadataReaderFactory,
																				   pathMatchingResourcePatternResolver);
            if ( annotationMetadata != null )
            {
                Map<String, Object> annotationAttributes = annotationMetadata
                    .getAnnotationAttributes( RemoteServiceRelativePath.class.getName() );
                getLogger().debug( "found RemoteServiceRelativePath annotation for class "
                                       + metadataReader.getClassMetadata().getClassName() );
                if ( StringUtils.isNotBlank( startPath ) )
                {
                    StringBuilder path = new StringBuilder();
                    if ( !startPath.startsWith( "/" ) )
                    {
                        path.append( '/' );
                    }
                    path.append( startPath );
                    String annotationPathValue = (String) annotationAttributes.get( "value" );
                    if ( !annotationPathValue.startsWith( "/" ) )
                    {
                        path.append( '/' );
                    }
                    path.append( annotationPathValue );
                    ServletDescriptor servletDescriptor = new ServletDescriptor( path.toString(), metadataReader
                        .getClassMetadata().getClassName() );
                    servlets.add( servletDescriptor );
                }
                else
                {
                    StringBuilder path = new StringBuilder();
                    String annotationPathValue = (String) annotationAttributes.get( "value" );
                    if ( !annotationPathValue.startsWith( "/" ) )
                    {
                        path.append( '/' );
                    }
                    path.append( annotationPathValue );
                    ServletDescriptor servletDescriptor = new ServletDescriptor( path.toString(), metadataReader
                        .getClassMetadata().getClassName() );
                    servlets.add( servletDescriptor );
                }
            }
        }
        return servlets;
    }

}
