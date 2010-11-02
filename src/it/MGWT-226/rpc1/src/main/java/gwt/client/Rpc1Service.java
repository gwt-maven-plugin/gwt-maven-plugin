package gwt.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("rpc1")
public interface Rpc1Service extends RemoteService
{
  String getString(int index);
}
