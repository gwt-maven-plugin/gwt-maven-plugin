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
assert new File(basedir, 'target/generated-sources/gwt').exists();
assert new File(basedir, 'target/generated-sources/gwt/com/google/gwt/sample/hello/client/Bar.java').exists();
assert new File(basedir, 'target/generated-sources/gwt/com/google/gwt/sample/hello/client/Foo.java').exists();
assert new File(basedir, 'target/generated-sources/gwt/com/google/gwt/sample/hello/client/Wine.java').exists();

content = new File(basedir, 'target/generated-sources/gwt/com/google/gwt/sample/hello/client/Bar.java').text;
assert content.contains( 'public interface Bar extends com.google.gwt.i18n.client.Constants' );
assert content.contains( '@DefaultStringValue("somewhere")' );
assert content.contains( '@Key("where")' );
assert content.contains( 'String where();' );

content = new File(basedir, 'target/generated-sources/gwt/com/google/gwt/sample/hello/client/Wine.java').text;
assert content.contains( 'public interface Wine extends com.google.gwt.i18n.client.Messages' );
assert content.contains( '@DefaultMessage("Gevrey Chambertin ?")' );
assert content.contains( '@Key("best.wine")' );
assert content.contains( 'String best_wine();' );

return true;