package com.example.workmanagerexercise

import android.app.Application
import timber.log.Timber

class BlurApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }
}