package gwt.server;

import gwt.client.Rpc1Service;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class Rpc1ServiceImpl extends RemoteServiceServlet implements
    Rpc1Service
{
  private static final long     serialVersionUID = 1L;

  private static final String[] SERVER_STRINGS   = new String[] {
      "Hello World", "Bonjour monde"            };

  @Override
  public String getString(final int index)
  {
    return SERVER_STRINGS[index % SERVER_STRINGS.length];
  }
}
