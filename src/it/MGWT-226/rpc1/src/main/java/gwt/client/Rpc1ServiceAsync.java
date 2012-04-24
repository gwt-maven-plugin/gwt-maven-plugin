package gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface Rpc1ServiceAsync
{
  void getString(int index, AsyncCallback<String> callback);
}
