package com.example.tans.googleplaydemo

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId

class AppApplication : Application() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mGoogleAnalytics: GoogleAnalytics

    override fun onCreate() {
        super.onCreate()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mGoogleAnalytics = GoogleAnalytics.getInstance(this)
        Log.i(AppApplication::class.java.simpleName,  "Firebase messaging Token: ${FirebaseInstanceId.getInstance().token}")
    }

    fun getFireBaseAnalytics() : FirebaseAnalytics = mFirebaseAnalytics

    fun getGoogleAnalyticsTracker(): Tracker = mGoogleAnalytics.newTracker("UA-123154242-1")

    fun sendFirebaseEvent(eventType: String, event: String) {
        mFirebaseAnalytics.logEvent("Action", Bundle().let {
            it.putString(eventType, event)
            it
        })

    }

    fun sendGoogleAnalyticsEvent(eventType: String, event: String) {

        getGoogleAnalyticsTracker().send( HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction(eventType)
                .setLabel(event)
                .build()
        )
    }

}