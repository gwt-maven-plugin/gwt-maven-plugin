/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.codehaus.mojo.gwt.test.client;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class Hello
    implements EntryPoint
{

    public void onModuleLoad()
    {

        RootPanel.get().add( new Label( "GWT is running :D" ) );
        HelloServiceAsync service = HelloServiceAsync.Util.getInstance();

        service.exit( new VoidAsyncCallBack() );

        /**
         *  Asycn method invocations to check generated code matches the expected signatures.
         *  Generation mismatch will be detected during compile phase by the java compiler
         */
        
        service.returnsVoid( "test", new VoidAsyncCallBack() );
        
        service.returnsPrimitive( new String[0], new IntegerAsyncCallBack() );
        
        service.returnsGenerics( new ArrayList<String>(), new CollectionAsynCallBack() );
    }

    private final class CollectionAsynCallBack
        implements AsyncCallback<Collection<Integer>>
    {
        public void onFailure( Throwable caught )
        {
            // TODO Auto-generated method stub

        }

        public void onSuccess( java.util.Collection<Integer> result )
        {
        }
    }

    private final class IntegerAsyncCallBack
        implements AsyncCallback<Integer>
    {
        public void onFailure( Throwable caught )
        {
            // TODO Auto-generated method stub

        }

        public void onSuccess( Integer result )
        {
        }
    }

    private final class VoidAsyncCallBack
        implements AsyncCallback<Void>
    {
        public void onFailure( Throwable caught )
        {

        }

        public void onSuccess( Void result )
        {

        }
    }
}
