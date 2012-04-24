package gwt.server;

import gwt.client.Rpc2Service;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class Rpc2ServiceImpl extends RemoteServiceServlet implements
    Rpc2Service
{
  private static final long     serialVersionUID = 1L;

  private static final String[] SERVER_STRINGS   = new String[] { "Good Bye",
      "Au Revoir"                               };

  @Override
  public String getString(final int index)
  {
    return SERVER_STRINGS[index % SERVER_STRINGS.length];
  }
}
