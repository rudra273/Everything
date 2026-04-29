package com.everything.app.core.session

import java.util.concurrent.ConcurrentHashMap

object AppLockSessionManager {
    private const val DEFAULT_UNLOCK_WINDOW_MILLIS = 2 * 60 * 1000L
    private val temporaryAccess = ConcurrentHashMap<String, Long>()

    fun allow(packageName: String, durationMillis: Long = DEFAULT_UNLOCK_WINDOW_MILLIS) {
        temporaryAccess[packageName] = System.currentTimeMillis() + durationMillis
    }

    fun isAllowed(packageName: String): Boolean {
        clearExpired()
        return temporaryAccess[packageName]?.let { it > System.currentTimeMillis() } == true
    }

    fun keepOnly(packageName: String?) {
        if (packageName == null) {
            clearAll()
            return
        }
        temporaryAccess.keys
            .filterNot { it == packageName }
            .forEach(temporaryAccess::remove)
        clearExpired()
    }

    fun clearAll() {
        temporaryAccess.clear()
    }

    fun clearExpired() {
        val now = System.currentTimeMillis()
        temporaryAccess.entries
            .filter { it.value <= now }
            .forEach { temporaryAccess.remove(it.key) }
    }
}
