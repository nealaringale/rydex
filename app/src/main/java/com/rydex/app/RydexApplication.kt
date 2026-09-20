package com.rydex.app

import android.app.Application

class RydexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RydexCrashReporter.install(this)
    }
}
