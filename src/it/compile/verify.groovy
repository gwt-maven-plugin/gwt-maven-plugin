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
assert new File(basedir, 'target/classes').exists();
assert new File(basedir, 'src/main/webapp/hello').exists();
assert new File(basedir, 'src/main/webapp/hello/hello.nocache.js').exists();
assert new File(basedir, 'src/main/webapp/com.google.gwt.sample.hello.Hello').exists();
assert new File(basedir, 'src/main/webapp/com.google.gwt.sample.hello.Hello/com.google.gwt.sample.hello.Hello.nocache.js').exists();
assert new File(basedir, 'target/extra').exists();
assert new File(basedir, 'target/extra/hello/rpcPolicyManifest').exists();
assert new File(basedir, 'target/workDir').exists();
assert new File(basedir, 'target/workDir/com.google.gwt.sample.hello.Hello').exists();
assert new File(basedir, 'target/workDir/org.codehaus.mojo.gwt.test.Hello').exists();
assert new File(basedir, 'target/deploy').exists();
assert new File(basedir, 'target/deploy/hello/symbolMaps').exists();
assert new File(basedir, 'target/persistentunitcache').exists();
// assert new File(basedir, 'target/savedSources').exists();
assert new File(basedir, 'target/missingDeps').exists();

assert !new File(basedir, 'target/.generated').exists();

assert new File(basedir, 'build.log').exists();

content = new File(basedir, 'build.log').text;
assert content.contains( '-draftCompile' );
assert content.contains( '-failOnError' );
assert content.contains( "'-optimize' '1'" ) || content.contains("-optimize 1");
assert content.contains( '-Dgwt.persistentunitcache=true' );
assert content.contains( '-XclosureCompiler' );
assert content.contains( '-XnoaggressiveOptimizations' );
assert content.contains( '-XcompilerMetrics' );
assert content.contains( "'-XfragmentCount' '2'" ) || content.contains("-XfragmentCount 2");
assert content.contains( '-XnoclusterFunctions' );
assert content.contains( '-XenforceStrictResources' );
assert content.contains( '-XnoinlineLiteralParameters' );
assert content.contains( '-XnooptimizeDataflow' );
assert content.contains( '-XnoordinalizeEnums' );
assert content.contains( '-XnoremoveDuplicateFunctions' );
assert content.contains( "'-sourceLevel' 'auto'" ) || content.contains("-sourceLevel auto");
assert content.contains( '-incrementalCompileWarnings' );
assert content.contains( "'-XjsInteropMode' 'JS'" ) || content.contains("-XjsInteropMode JS");
assert content.contains( "'-Xnamespace' 'NONE'" ) || content.contains("-Xnamespace NONE");
assert content.contains( '-overlappingSourceWarnings' );
assert content.contains( "'-XmethodNameDisplayMode' 'FULL'" ) || content.contains("-XmethodNameDisplayMode FULL");
  
return true;
