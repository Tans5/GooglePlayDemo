package com.example.tans.googleplaydemo

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics

class AppApplication : Application() {

    lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    fun getFireBaseAnalytics() : FirebaseAnalytics {
        return mFirebaseAnalytics
    }

}