package com.rudra.everything

import android.app.Application
import com.rudra.everything.core.session.AppLockSessionManager

class EverythingApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppLockSessionManager.clearExpired()
    }
}
