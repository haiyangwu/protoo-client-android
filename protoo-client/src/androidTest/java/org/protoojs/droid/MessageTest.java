package org.protoojs.droid;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MessageTest {

  private static final String METHOD_TEST = "test";
  private static final long ERROR_CODE_TEST = 1;
  private static final String ERROR_REASON_TEST = "test error code";

  @Test
  public void createRequestResponse() {
    // test createRequest and parse request.
    JSONObject requestObj = Message.createRequest(METHOD_TEST, null);
    Message parsedRequest = Message.parse(requestObj.toString());
    assertTrue(parsedRequest instanceof Message.Request);

    Message.Request request = (Message.Request) parsedRequest;
    assertEquals(METHOD_TEST, request.getMethod());

    // test createSuccessResponse and parse success response.
    JSONObject sucResObj = Message.createSuccessResponse(request, null);
    Message parsedSucRes = Message.parse(sucResObj.toString());
    assertTrue(parsedSucRes instanceof Message.Response);

    Message.Response sucRes = (Message.Response) parsedSucRes;
    assertEquals(request.getId(), sucRes.getId());

    // test createErrorResponse and parse error response.
    JSONObject errResObj = Message.createErrorResponse(request, ERROR_CODE_TEST, ERROR_REASON_TEST);
    Message parsedErrRes = Message.parse(errResObj.toString());
    assertTrue(parsedErrRes instanceof Message.Response);

    Message.Response errRes = (Message.Response) parsedErrRes;
    assertEquals(request.getId(), errRes.getId());
    assertEquals(ERROR_CODE_TEST, errRes.getErrorCode());
    assertEquals(ERROR_REASON_TEST, errRes.getErrorReason());
  }

  @Test
  public void createNotification() {
    JSONObject request = Message.createNotification(METHOD_TEST, null);
    Message parsedMessage = Message.parse(request.toString());
    assertTrue(parsedMessage instanceof Message.Notification);
    assertEquals(METHOD_TEST, ((Message.Notification) parsedMessage).getMethod());
  }
}
