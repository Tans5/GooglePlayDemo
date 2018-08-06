package com.example.tans.googleplaydemo

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage?) {
        Log.i(this::class.java.simpleName, "Message from firebase cloud messaging: ${p0?.notification?.body ?: "null body"}")
    }

}