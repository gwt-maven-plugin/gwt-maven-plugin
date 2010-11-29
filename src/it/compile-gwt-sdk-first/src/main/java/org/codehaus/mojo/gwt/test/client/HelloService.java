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

package org.codehaus.mojo.gwt.test.client;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath( "Hello" )
public interface HelloService
    extends RemoteService
{
    void exit();

    // expected : void returnsVoid( String value, AsyncCallback<Void> callback );
    void returnsVoid( String value );

    // expected : void returnsPrimitive( String[] values, AsyncCallback<Integer> callback );
    int returnsPrimitive( String[] values );

    // expected : void returnsArray( String[] values, AsyncCallback<String[]> callback );
    String[] returnsArray( String[] values );

    // expected : void returnsGenerics( java.util.List<String> values, AsyncCallback<java.util.Collection<Integer>>
    // callback );
    Collection<Integer> returnsGenerics( List<String> values );
}
