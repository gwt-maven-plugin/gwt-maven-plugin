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
assert new File(basedir, 'target').exists();
assert new File(basedir, 'target/web.xml').exists();


content = new File(basedir, 'target/web.xml').text;
assert content.contains( '<servlet-name>org.codehaus.mojo.gwt.test.server.HelloRemoteServlet/hello/HelloService</servlet-name>' );
assert content.contains( '<servlet-class>org.codehaus.mojo.gwt.test.server.HelloRemoteServlet</servlet-class>' );
assert content.contains( '<url-pattern>/hello/HelloService</url-pattern>' );
  
assert content.contains( '<servlet-name>org.codehaus.mojo.foo.NiceServlet/Name</servlet-name>' );
assert content.contains( '<servlet-class>org.codehaus.mojo.foo.NiceServlet</servlet-class>' );
assert content.contains( '<url-pattern>/NiceServlet</url-pattern>' );

return true;