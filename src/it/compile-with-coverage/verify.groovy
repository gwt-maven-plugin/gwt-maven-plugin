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
assert new File(basedir, 'target/gwt-coverage.in').exists();

content = new File(basedir, 'target/gwt-coverage.in').text;

// From the project
assert content.contains( 'org' + File.separator + 'codehaus' + File.separator + 'mojo' + File.separator + 'gwt' + File.separator + 'test' + File.separator + 'client' + File.separator + 'Hello.java' );
assert content.contains( 'org' + File.separator + 'codehaus' + File.separator + 'mojo' + File.separator + 'gwt' + File.separator + 'test' + File.separator + 'client' + File.separator + 'HelloService.java' );
assert content.contains( 'org' + File.separator + 'codehaus' + File.separator + 'mojo' + File.separator + 'gwt' + File.separator + 'test' + File.separator + 'client' + File.separator + 'HelloServiceAsync.java' );

// From the dependencies
assert content.contains( 'com' + File.separator + 'google' + File.separator + 'gwt' + File.separator + 'core' + File.separator + 'client' + File.separator + 'GWT.java' );

assert new File(basedir, 'build.log').exists();

content = new File(basedir, 'build.log').text;
assert content.contains( '-Dgwt.coverage=' );
assert content.contains( 'target' + File.separator + 'gwt-coverage.in' );

return true;
