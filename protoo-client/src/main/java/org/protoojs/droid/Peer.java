package org.protoojs.droid;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.protoojs.droid.transports.WebSocketTransport;

import java.util.HashMap;
import java.util.Map;

public class Peer implements WebSocketTransport.Listener {

  private static final String TAG = "Peer";

  public interface Listener {

    void onOpen();

    void onFail();

    void onRequest(@NonNull Message.Request request, @NonNull ServerRequestHandler handler);

    void onNotification(@NonNull Message.Notification notification);

    void onDisconnected();

    void onClose();
  }

  public interface ServerRequestHandler {

    void accept(String data);

    void reject(long code, String errorReason);
  }

  public interface ClientRequestHandler {

    void resolve(String data);

    void reject(long error, String errorReason);
  }

  class ClientRequestHandlerProxy implements ClientRequestHandler, Runnable {

    long mRequestId;
    String mMethod;
    ClientRequestHandler mClientRequestHandler;

    ClientRequestHandlerProxy(
        long requestId,
        String method,
        long timeoutDelayMillis,
        ClientRequestHandler clientRequestHandler) {
      mRequestId = requestId;
      mMethod = method;
      mClientRequestHandler = clientRequestHandler;
      mTimerCheckHandler.postDelayed(this, timeoutDelayMillis);
    }

    @Override
    public void run() {
      mSends.remove(mRequestId);
      // TODO: error code redefine. use http timeout.
      if (mClientRequestHandler != null) {
        mClientRequestHandler.reject(408, "request timeout");
      }
    }

    @Override
    public void resolve(String data) {
      if (mClientRequestHandler != null) {
        mClientRequestHandler.resolve(data);
      }
    }

    @Override
    public void reject(long error, String errorReason) {
      if (mClientRequestHandler != null) {
        mClientRequestHandler.reject(error, errorReason);
      }
    }

    void close() {
      // stop timeout check.
      mTimerCheckHandler.removeCallbacks(this);
    }
  }

  // Closed flag.
  private boolean mClosed = false;
  // Transport.
  @NonNull private final WebSocketTransport mTransport;
  // Listener.
  @NonNull private final Listener mListener;
  // Handler for timeout check.
  @NonNull private final Handler mTimerCheckHandler;
  // Connected flag.
  private boolean mConnected;
  // Custom data object.
  private JSONObject mData;
  // Map of pending sent request objects indexed by request id.
  private Map<Long, ClientRequestHandlerProxy> mSends = new HashMap<>();

  public Peer(@NonNull WebSocketTransport transport, @NonNull Listener listener) {
    mTransport = transport;
    mListener = listener;
    mTimerCheckHandler = new Handler(Looper.getMainLooper());
    handleTransport();
  }

  public boolean isClosed() {
    return mClosed;
  }

  public boolean isConnected() {
    return mConnected;
  }

  public JSONObject getData() {
    return mData;
  }

  public void close() {
    if (mClosed) {
      return;
    }

    Logger.d(TAG, "close()");
    mClosed = true;
    mConnected = false;

    // Close Transport.
    mTransport.close();

    // Close every pending sent.
    for (ClientRequestHandlerProxy proxy : mSends.values()) {
      proxy.close();
    }
    mSends.clear();

    // Emit 'close' event.
    mListener.onClose();
  }

  public void request(String method, String data, ClientRequestHandler clientRequestHandler) {
    try {
      request(method, new JSONObject(data), clientRequestHandler);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void request(String method, JSONObject data, ClientRequestHandler clientRequestHandler) {
    JSONObject request = Message.createRequest(method, data);
    long requestId = request.optLong("id");
    Logger.d(TAG, String.format("request() [method:%s]", method));
    String payload = mTransport.sendMessage(request);

    long timeout = (long) (1500 * (15 + (0.1 * payload.length())));
    mSends.put(
        requestId, new ClientRequestHandlerProxy(requestId, method, timeout, clientRequestHandler));
  }

  public void notify(String method, String data) {
    try {
      notify(method, new JSONObject(data));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void notify(String method, JSONObject data) {
    JSONObject notification = Message.createNotification(method, data);
    Logger.d(TAG, String.format("notify() [method:%s]", method));
    mTransport.sendMessage(notification);
  }

  private void handleTransport() {
    if (mTransport.isClosed()) {
      if (mClosed) {
        return;
      }

      mConnected = false;
      mListener.onClose();
      return;
    }

    mTransport.connect(this);
  }

  private void handleRequest(Message.Request request) {
    mListener.onRequest(
        request,
        new ServerRequestHandler() {
          @Override
          public void accept(String data) {
            JSONObject response = Message.createSuccessResponse(request, data);
            try {
              mTransport.sendMessage(response);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void reject(long code, String errorReason) {
            JSONObject response = Message.createErrorResponse(request, code, errorReason);
            try {
              mTransport.sendMessage(response);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }

  private void handleResponse(Message.Response response) {
    ClientRequestHandlerProxy sent = mSends.remove(response.getId());
    if (sent == null) {
      Logger.e(
          TAG, "received response does not match any sent request [id:" + response.getId() + "]");
      return;
    }

    sent.close();
    if (response.isOK()) {
      sent.resolve(response.getData().toString());
    } else {
      sent.reject(response.getErrorCode(), response.getErrorReason());
    }
  }

  private void handleNotification(Message.Notification notification) {
    mListener.onNotification(notification);
  }

  // implement WebSocketTransport$Listener
  @Override
  public void onOpen() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onOpen()");
    mConnected = true;
    mListener.onOpen();
  }

  @Override
  public void onFail() {
    if (mClosed) {
      return;
    }
    Logger.e(TAG, "onFail()");
    mConnected = false;
    mListener.onFail();
  }

  @Override
  public void onMessage(Message message) {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onMessage()");
    if (message instanceof Message.Request) {
      handleRequest((Message.Request) message);
    } else if (message instanceof Message.Response) {
      handleResponse((Message.Response) message);
    } else if (message instanceof Message.Notification) {
      handleNotification((Message.Notification) message);
    }
  }

  @Override
  public void onDisconnected() {
    if (mClosed) {
      return;
    }
    Logger.w(TAG, "onDisconnected()");
    mConnected = false;
    mListener.onDisconnected();
  }

  @Override
  public void onClose() {
    if (mClosed) {
      return;
    }
    Logger.w(TAG, "onClose()");
    mClosed = true;
    mConnected = false;
    mListener.onClose();
  }
}
