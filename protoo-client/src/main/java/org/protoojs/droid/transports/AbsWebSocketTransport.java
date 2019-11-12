package org.protoojs.droid.transports;

import org.json.JSONObject;
import org.protoojs.droid.Message;

public abstract class AbsWebSocketTransport {

  public interface Listener {

    void onOpen();
    /** Connection could not be established in the first place. */
    void onFail();

    /** @param message {@link Message} */
    void onMessage(Message message);

    /** A previously established connection was lost unexpected. */
    void onDisconnected();

    void onClose();
  }

  // WebSocket URL.
  protected String mUrl;

  public AbsWebSocketTransport(String mUrl) {
    this.mUrl = mUrl;
  }

  public abstract void connect(Listener listener);

  public abstract String sendMessage(JSONObject message);

  public abstract void close();

  public abstract boolean isClosed();
}
