package com.everything.app

import android.app.Application
import com.everything.app.core.session.AppLockSessionManager

class EverythingApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppLockSessionManager.clearExpired()
    }
}
