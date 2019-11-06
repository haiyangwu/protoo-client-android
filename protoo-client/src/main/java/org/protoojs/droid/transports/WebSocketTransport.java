package org.protoojs.droid.transports;

import org.json.JSONObject;
import org.protoojs.droid.Logger;
import org.protoojs.droid.Message;

import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.exceptions.WebSocketException;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;

public class WebSocketTransport implements IWebSocketConnectionHandler {

  public interface Listener {

    void onOpen();

    /**
     * Connection could not be established in the first place.
     *
     * <p>{@link IWebSocketConnectionHandler#CLOSE_CANNOT_CONNECT}
     */
    void onFail();

    /** @param message {@link Message} */
    void onMessage(Message message);

    /**
     * A previously established connection was lost unexpected.
     *
     * <p>{@link IWebSocketConnectionHandler#CLOSE_CONNECTION_LOST}
     */
    void onDisconnected();

    void onClose();
  }

  // Log tag.
  private static final String TAG = "WebSocketTransport";
  // Closed flag.
  private boolean mClosed;
  // Connected flag.
  private boolean mConnected;
  // WebSocket URL.
  private String mUrl;
  // WebSocketConnection options.
  private WebSocketOptions mOptions;
  // WebSocketConnection instance.
  private WebSocketConnection mWebSocketConnection;
  // Listener.
  private Listener mListener;

  public WebSocketTransport(String url) {
    this(url, null);
  }

  public WebSocketTransport(String url, WebSocketOptions options) {
    mUrl = url;
    if (options == null) {
      mOptions = new WebSocketOptions();
      mOptions.setReconnectInterval(10);
    } else {
      mOptions = options;
    }
    mWebSocketConnection = new WebSocketConnection();
  }

  public void connect(Listener listener) {
    mListener = listener;
    try {
      String[] wsSubprotocols = {"protoo"};
      mWebSocketConnection.connect(mUrl, wsSubprotocols, this, mOptions, null);
    } catch (WebSocketException ex) {
      Logger.e(TAG, "", ex);
    }
  }

  public String sendMessage(JSONObject message) {
    if (mClosed) {
      throw new IllegalStateException("transport closed");
    }
    String payload = message.toString();
    mWebSocketConnection.sendMessage(payload);
    return payload;
  }

  public void close() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "close()");
    mWebSocketConnection.sendClose();
  }

  public boolean isClosed() {
    return mClosed;
  }

  @Override
  public void onConnect(ConnectionResponse response) {
    Logger.d(TAG, "onConnect()");
  }

  @Override
  public void onOpen() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onOpen()");
    mConnected = true;
    if (mListener != null) {
      mListener.onOpen();
    }
  }

  @Override
  public void onClose(int code, String reason) {
    if (mClosed) {
      return;
    }
    Logger.w(TAG, "onClose() " + reason);
    boolean isOnFail =
        (code == IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT)
            || (!mConnected && code == IWebSocketConnectionHandler.CLOSE_RECONNECT);
    boolean isOnDisconnect =
        (code == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST)
            || (mConnected && code == IWebSocketConnectionHandler.CLOSE_RECONNECT);

    if (mListener != null) {
      if (isOnFail) {
        mListener.onFail();
        return;
      } else if (isOnDisconnect) {
        mConnected = false;
        mListener.onDisconnected();
        return;
      }
    }

    mClosed = true;
    if (mListener != null) {
      mListener.onClose();
    }
  }

  @Override
  public void onMessage(String payload) {
    Logger.d(TAG, "onMessage()");
    Message message = Message.parse(payload);
    if (message == null) {
      return;
    }
    if (mListener != null) {
      mListener.onMessage(message);
    }
  }

  @Override
  public void onMessage(byte[] payload, boolean isBinary) {
    Logger.d(TAG, "onMessage()");
  }

  @Override
  public void onPing() {
    Logger.d(TAG, "onPing()");
  }

  @Override
  public void onPing(byte[] payload) {
    Logger.d(TAG, "onPing()");
  }

  @Override
  public void onPong() {
    Logger.d(TAG, "onPong()");
  }

  @Override
  public void onPong(byte[] payload) {
    Logger.d(TAG, "onPong()");
  }

  @Override
  public void setConnection(WebSocketConnection connection) {
    Logger.d(TAG, "setConnection()");
  }
}
