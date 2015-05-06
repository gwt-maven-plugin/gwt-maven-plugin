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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.Type;

/**
 * Goal which generate Async interface.
 * 
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
@Mojo(name = "generateAsync", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class GenerateAsyncMojo
    extends AbstractGwtMojo
{
    private static final String REMOTE_SERVICE_INTERFACE = "com.google.gwt.user.client.rpc.RemoteService";

    private final static Map<String, String> WRAPPERS = new HashMap<String, String>();
    static
    {
        WRAPPERS.put( "boolean", Boolean.class.getName() );
        WRAPPERS.put( "byte", Byte.class.getName() );
        WRAPPERS.put( "char", Character.class.getName() );
        WRAPPERS.put( "short", Short.class.getName() );
        WRAPPERS.put( "int", Integer.class.getName() );
        WRAPPERS.put( "long", Long.class.getName() );
        WRAPPERS.put( "float", Float.class.getName() );
        WRAPPERS.put( "double", Double.class.getName() );
    }

    /**
     * Pattern for GWT service interface
     */
    @Parameter(defaultValue = "**/*Service.java")
    private String servicePattern;

    /**
     * Return a com.google.gwt.http.client.Request on async interface to allow cancellation.
     */
    @Parameter(defaultValue = "false")
    private boolean returnRequest;

    /**
     * A (MessageFormat) Pattern to get the GWT-RPC servlet URL based on service interface name. For example to
     * "{0}.rpc" if you want to map GWT-RPC calls to "*.rpc" in web.xml, for example when using Spring dispatch servlet
     * to handle RPC requests.
     */
    @Parameter(defaultValue = "{0}", property = "gwt.rpcPattern")
    private String rpcPattern;

    /**
     * Stop the build on error
     */
    @Parameter(defaultValue = "true", property = "maven.gwt.failOnError")
    private boolean failOnError;

    /**
     * Pattern for GWT service interface
     */
    @Parameter(defaultValue = "false", property = "generateAsync.force")
    private boolean force;

    @Parameter(property = "project.build.sourceEncoding")
    private String encoding;

    @Component
    private BuildContext buildContext;

    @Override
    protected boolean isGenerator()
    {
        return true;
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( "pom".equals( getProject().getPackaging() ) )
        {
            getLog().info( "GWT generateAsync is skipped" );
            return;
        }

        setupGenerateDirectory();

        if ( encoding == null )
        {
            getLog().warn( "Encoding is not set, your build will be platform dependent" );
            encoding = Charset.defaultCharset().name();
        }

        JavaDocBuilder builder = createJavaDocBuilder();

        List<String> sourceRoots = getProject().getCompileSourceRoots();
        for ( String sourceRoot : sourceRoots )
        {
            try
            {
                scanAndGenerateAsync( new File( sourceRoot ), builder );
            }
            catch ( Throwable e )
            {
                getLog().error( "Failed to generate Async interface", e );
                if ( failOnError )
                {
                    throw new MojoExecutionException( "Failed to generate Async interface", e );
                }
            }
        }
    }

    /**
     * @param sourceRoot the base directory to scan for RPC services
     * @return true if some file have been generated
     * @throws Exception generation failure
     */
    private boolean scanAndGenerateAsync( File sourceRoot, JavaDocBuilder builder )
        throws Exception
    {
        Scanner scanner = buildContext.newScanner( sourceRoot );
        scanner.setIncludes( new String[] { servicePattern } );
        scanner.scan();
        String[] sources = scanner.getIncludedFiles();
        if ( sources.length == 0 )
        {
            return false;
        }
        boolean fileGenerated = false;
        for ( String source : sources )
        {
            File sourceFile = new File( sourceRoot, source );
            File targetFile = getTargetFile( source );
            if ( !force && buildContext.isUptodate( targetFile, sourceFile ) )
            {
                getLog().debug( targetFile.getAbsolutePath() + " is up to date. Generation skipped" );
                // up to date, but still need to report generated-sources directory as sourceRoot
                fileGenerated = true;
                continue;
            }

            String className = getTopLevelClassName( source );
            JavaClass clazz = builder.getClassByName( className );
            if ( isEligibleForGeneration( clazz ) )
            {
                getLog().debug( "Generating async interface for service " + className );
                targetFile.getParentFile().mkdirs();
                generateAsync( clazz, targetFile );
                fileGenerated = true;
            }
        }
        return fileGenerated;
    }

    private File getTargetFile( String source )
    {
        String targetFileName = source.substring( 0, source.length() - 5 ) + "Async.java";
        File targetFile = new File( getGenerateDirectory(), targetFileName );
        return targetFile;
    }

    /**
     * @param clazz the RPC service java class
     * @param targetFile RemoteAsync file to generate
     * @throws Exception generation failure
     */
    private void generateAsync( JavaClass clazz, File targetFile )
        throws IOException
    {
        PrintWriter writer = new PrintWriter( new BufferedWriter(
            new OutputStreamWriter( buildContext.newFileOutputStream( targetFile ), encoding ) ) );

        boolean hasRemoteServiceRelativePath = hasRemoteServiceRelativePath(clazz);

        String className = clazz.getName();
        if ( clazz.getPackage() != null )
        {
            writer.println( "package " + clazz.getPackageName() + ";" );
            writer.println();
        }
        writer.println( "import com.google.gwt.core.client.GWT;" );
        writer.println( "import com.google.gwt.user.client.rpc.AsyncCallback;" );

        if (!hasRemoteServiceRelativePath)
        {
            writer.println( "import com.google.gwt.user.client.rpc.ServiceDefTarget;" );
        }

        writer.println();
        writer.println( "public interface " + className + "Async" );
        writer.println( "{" );

        JavaMethod[] methods = clazz.getMethods( true );
        for ( JavaMethod method : methods )
        {
            boolean deprecated = isDeprecated( method );

            writer.println( "" );
            writer.println( "    /**" );
            writer.println( "     * GWT-RPC service  asynchronous (client-side) interface" );
            writer.println( "     * @see " + clazz.getFullyQualifiedName() );
            if ( deprecated )
                writer.println( "     * @deprecated" );
            writer.println( "     */" );
            if ( deprecated )
                writer.println( "    @Deprecated" );
            if ( returnRequest )
            {
                writer.print( "    com.google.gwt.http.client.Request " + method.getName() + "( " );
            }
            else
            {
                writer.print( "    void " + method.getName() + "( " );
            }
            JavaParameter[] params = method.getParameters();
            for ( int j = 0; j < params.length; j++ )
            {
                JavaParameter param = params[j];
                if ( j > 0 )
                {
                    writer.print( ", " );
                }

                writer.print( method.getParameterTypes( true )[j].getGenericValue() );
                if ( param.getType().getDimensions() != method.getParameterTypes( true )[j].getDimensions() )
                {
                    for ( int dimensions = 0; dimensions < param.getType().getDimensions(); dimensions++ )
                    {
                        writer.print( "[]" );
                    }
                }
                writer.print( " " + param.getName() );
            }
            if ( params.length > 0 )
            {
                writer.print( ", " );
            }

            if ( method.getReturnType().isVoid() )
            {
                writer.println( "AsyncCallback<Void> callback );" );
            }
            else if ( method.getReturnType().isPrimitive() )
            {
                String primitive = method.getReturnType().getGenericValue();
                writer.println( "AsyncCallback<" + WRAPPERS.get( primitive ) + "> callback );" );
            }
            else
            {
                Type returnType = method.getReturnType( true );
                String type = returnType.getGenericValue();

                if ( method.getReturnType().getDimensions() != method.getReturnType( true ).getDimensions() )
                {
                    for ( int dimensions = 0; dimensions < method.getReturnType().getDimensions(); dimensions++ )
                    {
                        type += "[]";
                    }
                }
                writer.println( "AsyncCallback<" + type + "> callback );" );
            }
            writer.println();
        }

        writer.println();
        writer.println( "    /**" );
        writer.println( "     * Utility class to get the RPC Async interface from client-side code" );
        writer.println( "     */" );
        writer.println( "    public static final class Util " );
        writer.println( "    { " );
        writer.println( "        private static " + className + "Async instance;" );
        writer.println();
        writer.println( "        public static final " + className + "Async getInstance()" );
        writer.println( "        {" );
        writer.println( "            if ( instance == null )" );
        writer.println( "            {" );
        writer.println( "                instance = (" + className + "Async) GWT.create( " + className + ".class );" );
        if ( !hasRemoteServiceRelativePath )
        {
            String uri = MessageFormat.format( rpcPattern, className );
            writer.println( "                ServiceDefTarget target = (ServiceDefTarget) instance;" );
            writer.println( "                target.setServiceEntryPoint( GWT.getModuleBaseURL() + \"" + uri + "\" );" );
        }
        writer.println( "            }" );
        writer.println( "            return instance;" );
        writer.println( "        }" );
        writer.println( "" );
        writer.println( "        private Util()" );
        writer.println( "        {" );
        writer.println( "            // Utility class should not be instantiated" );
        writer.println( "        }" );
        writer.println( "    }" );

        writer.println( "}" );
        writer.close();
    }

    private boolean isEligibleForGeneration( JavaClass javaClass )
    {
        return javaClass.isInterface() && javaClass.isPublic() && javaClass.isA( REMOTE_SERVICE_INTERFACE );
    }

    private JavaDocBuilder createJavaDocBuilder()
        throws MojoExecutionException
    {
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( encoding );
        builder.getClassLibrary().addClassLoader( getProjectClassLoader() );
        for ( String sourceRoot : getProject().getCompileSourceRoots() )
        {
            builder.getClassLibrary().addSourceFolder( new File( sourceRoot ) );
        }
        return builder;
    }

    private String getTopLevelClassName( String sourceFile )
    {
        String className = sourceFile.substring( 0, sourceFile.length() - 5 ); // strip ".java"
        return className.replace( File.separatorChar, '.' );
    }

    /**
     * Determine if a client service method is deprecated.
     * 
     * @see MGWT-352
     */
    private boolean isDeprecated( JavaMethod method )
    {
        if ( method == null )
            return false;

        for ( Annotation annotation : method.getAnnotations() )
        {
            if ( "java.lang.Deprecated".equals( annotation.getType().getFullyQualifiedName() ) )
            {
                return true;
            }
        }

        return method.getTagByName( "deprecated" ) != null;
    }

    private boolean hasRemoteServiceRelativePath(final JavaClass clazz)
    {
        if ( clazz != null && clazz.getAnnotations() != null )
        {
            for ( Annotation annotation : clazz.getAnnotations() )
            {
                getLog().debug( "annotation found on service interface " + annotation );
                if ( annotation.getType().getValue().equals( "com.google.gwt.user.client.rpc.RemoteServiceRelativePath" ) )
                {
                    getLog().debug( "@RemoteServiceRelativePath annotation found on service interface" );
                    return true;
                }
            }
        }

        return false;
    }

    private ClassLoader getProjectClassLoader() throws MojoExecutionException
    {
        Collection<File> classpath = getClasspath( Artifact.SCOPE_COMPILE );
        URL[] urls = new URL[classpath.size()];
        try
        {
            int i = 0;
            for ( File classpathFile : classpath )
            {
                urls[i] = classpathFile.toURI().toURL();
                i++;
            }
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        return new URLClassLoader( urls, ClassLoader.getSystemClassLoader() );
    }
}
