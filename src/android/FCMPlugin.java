package com.gae.scaffolder.plugin;

import android.os.Bundle;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMPlugin extends CordovaPlugin {

  private static final String TAG = "FCMPlugin";
  private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

  public static CordovaWebView gWebView;
  public static String notificationCallBack = "FCMPlugin.onNotificationReceived";
  public static Boolean notificationCallBackReady = false;
  public static Map<String, Object> lastPush = null;

  public FCMPlugin() {}

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    gWebView = webView;
    Log.d(TAG, "==> FCMPlugin initialize");
    FirebaseMessaging.getInstance().subscribeToTopic("android");
    FirebaseMessaging.getInstance().subscribeToTopic("all");
  }

  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    Log.d(TAG,"==> FCMPlugin execute: "+ action);

    try{
      // READY //
      if (action.equals("ready")) {
        callbackContext.success();
      }
      // GET VERIFICATION ID //
      else if (action.equals("getVerificationID")) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          public void run() {
            try{
              String number = args.getString(0);

              mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                  // This callback will be invoked in two situations:
                  // 1 - Instant verification. In some cases the phone number can be instantly
                  //     verified without needing to send or enter a verification code.
                  // 2 - Auto-retrieval. On some devices Google Play services can automatically
                  //     detect the incoming verification SMS and perform verificaiton without
                  //     user action.
                  Log.d(TAG, "success: verifyPhoneNumber.onVerificationCompleted - doing nothing. sign in with token from onCodeSent");

                  // Sign in natively and give a token back to allow sign in javascript.
				          FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                      @Override
                      public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                          Log.d(TAG, "signInWithCredential:success");

                          FirebaseUser user = task.getResult().getUser();
                          user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                          	@Override
                            public void onComplete(Task<GetTokenResult> tokenTask) {
                              if (tokenTask.isSuccessful()) {
                              	Log.d(TAG, "getIdToken:success");
                                String token = tokenTask.getResult().getToken();
                                JSONObject result = new JSONObject();
                                try {
                                  result.put("token", token);
                                } catch (JSONException e) {
                                  callbackContext.error(e.getMessage());
                                  return;
                                }

                                PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
                                pluginresult.setKeepCallback(true);
                                callbackContext.sendPluginResult(pluginresult);
                              } else {
                                Log.w(TAG, "user.getIdToken:failure", tokenTask.getException());
                                callbackContext.error("Error getting the id token.");
                              }
                            }
                          });
                        } else {
                          Log.w(TAG, "signInWithCredential:failure", task.getException());
                          callbackContext.error("Failed to sign in using credential.");
                        }
                      }
                    });
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                  // This callback is invoked on an invalid verification request,
                  // for instance if the the phone number format is invalid.
                  Log.w(TAG, "failed: verifyPhoneNumber.onVerificationFailed ", e);

                  String errorMsg = "Error verifying number";
                  if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    errorMsg = "Invalid request: " + sw.toString();
                  } else if (e instanceof FirebaseTooManyRequestsException) {
                    errorMsg = "The SMS quota for the project has been exceeded";
                  }

                  callbackContext.error(errorMsg);
                }

                @Override
                public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                  // The SMS verification code has been sent to the provided phone number, we
                  // now need to ask the user to enter the code and then construct a credential
                  // by combining the code with a verification ID.
                  Log.d(TAG, "success: verifyPhoneNumber.onCodeSent");

                  JSONObject result = new JSONObject();
                  try {
                    result.put("id", verificationId);
                  } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                    return;
                  }

                  PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
                  pluginresult.setKeepCallback(true);
                  callbackContext.sendPluginResult(pluginresult);
                }
              };

              PhoneAuthProvider.getInstance().verifyPhoneNumber(
                number,                 // Phone number to verify
                60,                     // Timeout duration
                TimeUnit.SECONDS,       // Unit of timeout
                cordova.getActivity(),  // Activity (for callback binding)
                mCallbacks);            // OnVerificationStateChangedCallbacks
            }catch(Exception e){
              Log.d(TAG,"\tError retrieving token");
            }
          }
        });
      }
      // GET TOKEN //
      else if (action.equals("getToken")) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          public void run() {
            try{
              String token = FirebaseInstanceId.getInstance().getToken();
              callbackContext.success( FirebaseInstanceId.getInstance().getToken() );
              Log.d(TAG,"\tToken: "+ token);
            }catch(Exception e){
              Log.d(TAG,"\tError retrieving token");
            }
          }
        });
      }
      // NOTIFICATION CALLBACK REGISTER //
      else if (action.equals("registerNotification")) {
        notificationCallBackReady = true;
        cordova.getActivity().runOnUiThread(new Runnable() {
          public void run() {
            if(lastPush != null) FCMPlugin.sendPushPayload( lastPush );
            lastPush = null;
            callbackContext.success();
          }
        });
      }
      // UN/SUBSCRIBE TOPICS //
      else if (action.equals("subscribeToTopic")) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try{
              FirebaseMessaging.getInstance().subscribeToTopic( args.getString(0) );
              callbackContext.success();
            }catch(Exception e){
              callbackContext.error(e.getMessage());
            }
          }
        });
      }
      else if (action.equals("unsubscribeFromTopic")) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            try{
              FirebaseMessaging.getInstance().unsubscribeFromTopic( args.getString(0) );
              callbackContext.success();
            }catch(Exception e){
              callbackContext.error(e.getMessage());
            }
          }
        });
      }
      else{
        callbackContext.error("Method not found");
        return false;
      }
    }catch(Exception e){
      Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
      callbackContext.error(e.getMessage());
      return false;
    }

    return true;
  }

  public static void sendPushPayload(Map<String, Object> payload) {
    Log.d(TAG, "==> FCMPlugin sendPushPayload");
    Log.d(TAG, "\tnotificationCallBackReady: " + notificationCallBackReady);
    Log.d(TAG, "\tgWebView: " + gWebView);
      try {
        JSONObject jo = new JSONObject();
      for (String key : payload.keySet()) {
          jo.put(key, payload.get(key));
        Log.d(TAG, "\tpayload: " + key + " => " + payload.get(key));
            }
      String callBack = "javascript:" + notificationCallBack + "(" + jo.toString() + ")";
      if(notificationCallBackReady && gWebView != null){
        Log.d(TAG, "\tSent PUSH to view: " + callBack);
        gWebView.sendJavascript(callBack);
      }else {
        Log.d(TAG, "\tView not ready. SAVED NOTIFICATION: " + callBack);
        lastPush = payload;
      }
    } catch (Exception e) {
      Log.d(TAG, "\tERROR sendPushToView. SAVED NOTIFICATION: " + e.getMessage());
      lastPush = payload;
    }
  }
}