package org.protoojs.droid;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

  private static final String TAG = "message";

  // message data.
  private JSONObject mData;

  public Message() {}

  public Message(JSONObject data) {
    mData = data;
  }

  public JSONObject getData() {
    return mData;
  }

  public void setData(JSONObject data) {
    mData = data;
  }

  public static class Request extends Message {

    private boolean mRequest = true;
    private String mMethod;
    private long mId;

    public Request(String method, long id, JSONObject data) {
      super(data);
      mMethod = method;
      mId = id;
    }

    public boolean isRequest() {
      return mRequest;
    }

    public void setRequest(boolean request) {
      mRequest = request;
    }

    public long getId() {
      return mId;
    }

    public void setId(long id) {
      mId = id;
    }

    public String getMethod() {
      return mMethod;
    }

    public void setMethod(String method) {
      mMethod = method;
    }
  }

  public static class Response extends Message {

    private boolean mResponse = true;
    private long mId;
    private boolean mOK;
    private long mErrorCode;
    private String mErrorReason;

    public Response(long id, JSONObject data) {
      super(data);
      mId = id;
      mOK = true;
    }

    public Response(long id, long errorCode, String errorReason) {
      mId = id;
      mOK = false;
      mErrorCode = errorCode;
      mErrorReason = errorReason;
    }

    public boolean isResponse() {
      return mResponse;
    }

    public void setResponse(boolean response) {
      mResponse = response;
    }

    public long getId() {
      return mId;
    }

    public void setId(long id) {
      mId = id;
    }

    public boolean isOK() {
      return mOK;
    }

    public void setOK(boolean OK) {
      mOK = OK;
    }

    public long getErrorCode() {
      return mErrorCode;
    }

    public void setErrorCode(long errorCode) {
      mErrorCode = errorCode;
    }

    public String getErrorReason() {
      return mErrorReason;
    }

    public void setErrorReason(String errorReason) {
      mErrorReason = errorReason;
    }
  }

  public static class Notification extends Message {

    private boolean mNotification = true;
    private String mMethod;

    public Notification(String method, JSONObject data) {
      super(data);
      mMethod = method;
    }

    public boolean isNotification() {
      return mNotification;
    }

    public void setNotification(boolean notification) {
      mNotification = notification;
    }

    public String getMethod() {
      return mMethod;
    }

    public void setMethod(String method) {
      mMethod = method;
    }
  }

  public static Message parse(String raw) {
    Logger.d(TAG, "parse() ");
    JSONObject object;
    try {
      object = new JSONObject(raw);
    } catch (JSONException e) {
      Logger.e(TAG, String.format("parse() | invalid JSON: %s", e.getMessage()));
      return null;
    }

    if (object.optBoolean("request")) {
      // Request.
      String method = object.optString("method");
      long id = object.optLong("id");

      if (TextUtils.isEmpty(method)) {
        Logger.e(TAG, "parse() | missing/invalid method field. rawData: " + raw);
        return null;
      }
      if (id == 0) {
        Logger.e(TAG, "parse() | missing/invalid id field. rawData: " + raw);
        return null;
      }

      return new Request(method, id, object.optJSONObject("data"));
    } else if (object.optBoolean("response")) {
      // Response.
      long id = object.optLong("id");

      if (id == 0) {
        Logger.e(TAG, "parse() | missing/invalid id field. rawData: " + raw);
        return null;
      }

      if (object.optBoolean("ok")) {
        return new Response(id, object.optJSONObject("data"));
      } else {
        return new Response(id, object.optLong("errorCode"), object.optString("errorReason"));
      }
    } else if (object.optBoolean("notification")) {
      // Notification.
      String method = object.optString("method");

      if (TextUtils.isEmpty(method)) {
        Logger.e(TAG, "parse() | missing/invalid method field. rawData: " + raw);
        return null;
      }

      return new Notification(method, object.optJSONObject("data"));
    } else {
      // Invalid.
      Logger.e(TAG, "parse() | missing request/response field. rawData: " + raw);
      return null;
    }
  }

  public static JSONObject createRequest(String method, JSONObject data) {
    JSONObject request = new JSONObject();
    try {
      request.put("request", true);
      request.put("method", method);
      request.put("id", Utils.generateRandomNumber());
      request.put("data", data != null ? data : new JSONObject());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return request;
  }

  public static JSONObject createSuccessResponse(@NonNull Request request, String data) {
    try {
      return createSuccessResponse(request, new JSONObject(data));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static JSONObject createSuccessResponse(@NonNull Request request, JSONObject data) {
    JSONObject response = new JSONObject();
    try {
      response.put("response", true);
      response.put("id", request.getId());
      response.put("ok", true);
      response.put("data", data != null ? data : new JSONObject());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return response;
  }

  public static JSONObject createErrorResponse(
      @NonNull Request request, long errorCode, String errorReason) {
    JSONObject response = new JSONObject();
    try {
      response.put("response", true);
      response.put("id", request.getId());
      response.put("ok", false);
      response.put("errorCode", errorCode);
      response.put("errorReason", errorReason);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return response;
  }

  public static JSONObject createNotification(String method, JSONObject data) {
    JSONObject notification = new JSONObject();
    try {
      notification.put("onNotification", true);
      notification.put("method", method);
      notification.put("data", data != null ? data : new JSONObject());
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return notification;
  }
}
