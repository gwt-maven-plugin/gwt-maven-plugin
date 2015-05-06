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

h1 = new File(basedir, 'target/generated-sources/gwt/org/codehaus/mojo/gwt/test/client/Hello1ServiceAsync.java');
h2 = new File(basedir, 'target/generated-sources/gwt/org/codehaus/mojo/gwt/test/client/Hello2ServiceAsync.java');

assert h1.exists();
assert h2.exists();

c1 = h1.text;
c2 = h2.text;

assert c1.contains("java.util.Collection<java.lang.Double>");
assert c2.contains("java.util.Collection<java.lang.Double>");

assert !c1.contains("import com.google.gwt.user.client.rpc.ServiceDefTarget;");
assert c2.contains("import com.google.gwt.user.client.rpc.ServiceDefTarget;");

return true;
