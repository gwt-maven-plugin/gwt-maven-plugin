package gwt.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("rpc2")
public interface Rpc2Service extends RemoteService
{
  String getString(int index);
}
