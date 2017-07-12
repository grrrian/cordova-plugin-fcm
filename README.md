# Firebase Cloud Messaging and Phone Auth Cordova Plugin
> Use this Cordova plugin to receive push notifications through FCM or to sign in using Phone Auth.

#### Version 1.2.0 (07/11/2017)
- Added Phone Auth support for Android and iOS.

#### Version 1.1.6 (10/31/2016)
- Only request for notification permission on iOS after client calls getToken.
- Clear the badge on app open.

#### Version 1.1.5 (10/10/2016)
- iOS10 ready
- Android and iOS tested.
- Available sdk functions: getToken, subscribeToTopic, unsubscribeFromTopic and JavaScript notification data reception.
- Added data payload parameter to check whether the user tapped on the notification or was received in foreground.
- **Free testing server available for free! https://cordova-plugin-fcm.appspot.com**

## Installation
```Bash
cordova plugin add https://github.com/grrrian/cordova-plugin-fcm.git --save
```

#### Firebase configuration
You will need 2 generated files in the Firebase configuration process (see docs: https://firebase.google.com/docs/).

#### Android compilation details
Put your generated file 'google-services.json' in the project root folder.

You will need to ensure that you have installed the following items through the Android SDK Manager:

- Android Support Library version 23 or greater
- Android Support Repository version 20 or greater
- Google Play Services version 27 or greater
- Google Repository version 22 or greater

:warning: For Android >5.0 status bar icon, you must include transparent solid color icon with name 'fcm_push_icon.png' in the 'res' folder in the same way you add the other application icons.
If you do not set this resource, then the SDK will use the default icon for your app which may not meet the standards for Android 5.0.

#### iOS compilation details
Put your generated file 'GoogleService-Info.plist' in the project root folder.


## FCM

:warning: It's highly recommended to use REST API to send push notifications because Firebase console does not have all the functionalities. **Pay attention to the payload example in order to use the plugin properly**.  
You can also test your notifications with the free testing server: https://cordova-plugin-fcm.appspot.com

#### How it works
Send a push notification to a single device or topic.
- 1.a Application is in foreground:
 - The user receives the notification data in the JavaScript callback without notification alert message (this is the normal behaviour of mobile push notifications).
- 1.b Application is in background:
 - The user receives the notification message in its device notification bar.
 - The user taps the notification and the application is opened.
 - The user receives the notification data in the JavaScript callback.

#### Get token

```javascript
// FCMPlugin.getToken( successCallback(token), errorCallback(err) );
// Keep in mind the function will return null if the token has not been established yet.

// On device ready...
FCMPlugin.getToken(
  function(token){
    alert(token);
  },
  function(err){
    console.log('error retrieving token: ' + err);
  }
)
```

#### Subscribe to topic

```javascript
// FCMPlugin.subscribeToTopic( topic, successCallback(msg), errorCallback(err) );
// Must match the following regular expression: "[a-zA-Z0-9-_.~%]{1,900}".

// On device ready...
FCMPlugin.subscribeToTopic('topicExample');
```

#### Unsubscribe from topic

```javascript
// FCMPlugin.unsubscribeFromTopic( topic, successCallback(msg), errorCallback(err) );

// On device ready...
FCMPlugin.unsubscribeFromTopic('topicExample');
```

#### Receiving push notification data

```javascript
// FCMPlugin.onNotification( onNotificationCallback(data), successCallback(msg), errorCallback(err) )
// Here you define your application behaviour based on the notification data.

// On device ready...
FCMPlugin.onNotification(
  function(data){
    if (data.wasTapped) {
      // Notification was received on device tray (background) and tapped by the user.
      alert( JSON.stringify(data) );
    } else {
      // Notification was received in foreground. Maybe the user needs to be notified.
      alert( JSON.stringify(data) );
    }
  },
  function(msg){
    console.log('onNotification callback successfully registered: ' + msg);
  },
  function(err){
    console.log('Error registering onNotification callback: ' + err);
  }
);
```

#### Send notification. Payload example (REST API)
Full documentation: https://firebase.google.com/docs/cloud-messaging/http-server-ref  
Free testing server: https://cordova-plugin-fcm.appspot.com
```javascript
// POST: https://fcm.googleapis.com/fcm/send
// HEADER: Content-Type: application/json
// HEADER: Authorization: key=AIzaSy*******************
{
  "notification":{
    "title":"Notification title",  // Any value
    "body":"Notification body",  // Any value
    "sound":"default", // If you want notification sound
    "click_action":"FCM_PLUGIN_ACTIVITY",  // Must be present for Android
    "icon":"fcm_push_icon"  // White icon Android resource
  },
  "data":{
    "param1":"value1",  // Any data to be retrieved in the notification callback
    "param2":"value2"
  },
    "to":"/topics/topicExample", // Topic or single device
    "priority":"high", // If not set, notification won't be delivered on completely closed iOS app
    "restricted_package_name":"" // Optional. Set for application filtering
}
```

## Phone Auth

You can use this plugin to request an SMS code to be sent to a given phone number. You can then use this code in conjunction with the verification ID returned from the plugin to authenticate on the javascript side.

In some cases, Android may do instant verification or auto-retrieval by reading the incoming SMS. In those cases, we sign-in natively and return the user token instead of a verification ID.

#### Verifying a phone number
Calling FCMPlugin.getVerificationID will return an object containing either `id` or `token`.

```javascript
// FCMPlugin.getVerificationID( phoneNumber, successCallback(msg), errorCallback(err) )

// On device ready...
FCMPlugin.getVerificationID('+19025551234' // user's phone number
  function(verification) {
    if (verification.id) {
      // An SMS with a 6-digit code is sent to the phone number
      var smsCode = getCodeFromUI(); // code as string
      var credential = firebase.auth.PhoneAuthProvider.credential(verification.id, smsCode);
      firebase.auth().signInWithCredential(credential);
    } else if (verification.token) {
      // The code was automatically processed by Android. Use the token of the natively signed-in user to generate a sign-in token on your server.
      var customToken = generateCustomTokenFromSignedInToken(verification.token);
      firebase.auth().signInWithCustomToken(customToken);
    }
  },
  function(err){
    console.log('Error verifying phone number: ' + err);
  }
);
```

## License
```
The MIT License

Copyright (c) 2016 Felipe Echanique Torres (felipe.echanique in the gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
