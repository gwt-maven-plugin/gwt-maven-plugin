package gwt.client;

import gwt.client.Rpc1Service;
import gwt.client.Rpc1ServiceAsync;
import gwt.client.Rpc2Service;
import gwt.client.Rpc2ServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

public class Application implements EntryPoint
{
  private static int index = 0;

  @Override
  public void onModuleLoad()
  {
    final Panel root = RootPanel.get();

    final Button button1 = new Button("RPC 1");
    final Button button2 = new Button("RPC 2");

    button1.addClickHandler(new ClickHandler()
    {
      @Override
      public void onClick(final ClickEvent event)
      {
        GWT.<Rpc1ServiceAsync> create(Rpc1Service.class).getString(index++,
            new WindowAlertAsyncCallback());
      }
    });

    button2.addClickHandler(new ClickHandler()
    {
      @Override
      public void onClick(final ClickEvent event)
      {
        GWT.<Rpc2ServiceAsync> create(Rpc2Service.class).getString(index++,
            new WindowAlertAsyncCallback());
      }
    });

    root.add(button1);
    root.add(button2);
  }

  private class WindowAlertAsyncCallback implements AsyncCallback<String>
  {
    @Override
    public void onFailure(final Throwable thrown)
    {
      Window.alert("Failure: " + thrown.toString());
    }

    @Override
    public void onSuccess(final String success)
    {
      Window.alert("Success: " + success);
    }
  }
}
