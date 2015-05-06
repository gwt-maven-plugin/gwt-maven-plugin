package org.codehaus.mojo.gwt.servlets;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.RemoteService;

import java.util.Collection;
import java.util.List;

@RemoteServiceRelativePath(value="/HelloService")
public interface HelloRemoteService extends RemoteService {

    Collection<Integer> returnsGenerics( List<String> values );

    int returnsPrimitive( String[] values );

    void returnsVoid( String value );

    String[] returnsArray( String[] values );

}
