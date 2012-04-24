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
assert new File(basedir, 'target/extra').exists();
assert new File(basedir, 'target/extra/hello/soycReport').exists();
assert new File(basedir, 'target/extra/hello/soycReport/compile-report').exists();
assert new File(basedir, 'target/extra/hello/soycReport/compile-report/index.html').exists();
assert new File(basedir, 'target/site/gwtCompileReports/hello').exists();
assert new File(basedir, 'target/site/gwtCompileReports/hello/index.html').exists();
assert new File(basedir, 'target/site/gwtCompileReports/hello/soyc.css').exists();
assert new File(basedir, 'target/site/gwtCompileReports/hello/goog.css').exists();
assert new File(basedir, 'target/site/gwtCompileReports/hello/goog.css').exists();
assert new File(basedir, 'target/site/gwt-compiler-reports.html').exists();

assert new File(basedir, 'target/site/gwtCompileReports/hello/index.html').exists();
assert new File(basedir, 'target/site/gwtCompileReports/com.google.gwt.sample.hello.Hello/index.html').exists();

content = new File(basedir, 'target/site/gwt-compiler-reports.html').text;
assert content.contains( 'href="./gwtCompileReports/com.google.gwt.sample.hello.Hello/index.html' );
assert content.contains( 'com.google.gwt.sample.hello.Hello</a>' );

assert content.contains( 'href="./gwtCompileReports/hello/index.html' );
assert content.contains( 'org.codehaus.mojo.gwt.test.Hello</a>' );

return true;