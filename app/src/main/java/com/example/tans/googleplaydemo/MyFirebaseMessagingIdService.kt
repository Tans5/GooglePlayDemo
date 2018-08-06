package com.example.tans.googleplaydemo

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class MyFirebaseMessagingIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        Log.i(this::class.java.simpleName, "here is a new firebase messaging token: ${FirebaseInstanceId.getInstance().token}")
    }
}