package com.everything.app.feature.applock.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.everything.app.EverythingApplication
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.permissions.AppLockPermissionChecker
import com.everything.app.core.session.AppLockSessionManager
import com.everything.app.feature.applock.domain.SettingsPackageResolver
import com.everything.app.feature.applock.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AppMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var detector: ForegroundAppDetector
    private lateinit var overlayController: LockOverlayController
    private lateinit var settingsPackages: Set<String>
    private var lockedPackages = emptySet<String>()
    private var biometricEnabled = false
    private var lastForegroundPackage: String? = null
    private var activeActivityLockPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        settingsPackages = SettingsPackageResolver.resolve(this)
        overlayController = LockOverlayController(
            context = this,
            credentialRepository = (application as EverythingApplication).container.credentialRepository,
            onBiometricRequested = { packageName -> launchActivityLockScreen(packageName) },
        )
        observeLockedApps()
        observeBiometricSetting()
        monitorForegroundApps()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AppLockPermissionChecker.hasUsageAccess(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.post { overlayController.dismiss() }
        scope.cancel()
        super.onDestroy()
    }

    private fun observeLockedApps() {
        val repository = (application as EverythingApplication).container.appLockRepository
        scope.launch {
            repository.observeLockedApps()
                .catch { lockedPackages = emptySet() }
                .collect { apps ->
                    lockedPackages = apps.map { it.packageName }.toSet()
                }
        }
    }

    private fun observeBiometricSetting() {
        val repository = (application as EverythingApplication).container.secureSettingRepository
        scope.launch {
            repository.observeBoolean(SecureSettingRepository.KEY_BIOMETRIC_ENABLED)
                .catch { biometricEnabled = false }
                .collect { enabled ->
                    biometricEnabled = enabled == true
                }
        }
    }

    private fun monitorForegroundApps() {
        scope.launch {
            while (true) {
                val foregroundPackage = runCatching { detector.currentForegroundPackage() }.getOrNull()
                if (foregroundPackage != null && foregroundPackage != lastForegroundPackage) {
                    lastForegroundPackage = foregroundPackage
                    if (foregroundPackage == packageName && activeActivityLockPackage != null) {
                        AppLockSessionManager.clearExpired()
                    } else {
                        if (foregroundPackage != packageName) {
                            activeActivityLockPackage = null
                        }
                        AppLockSessionManager.keepOnly(foregroundPackage)
                    }
                } else {
                    AppLockSessionManager.clearExpired()
                }

                val shouldLock = foregroundPackage != null &&
                    foregroundPackage != packageName &&
                    foregroundPackage in lockedPackages &&
                    !AppLockSessionManager.isAllowed(foregroundPackage)

                foregroundPackage?.let { currentPackage ->
                    if (shouldLock) {
                        launchLockScreen(currentPackage)
                    } else if (currentPackage !in lockedPackages) {
                        mainHandler.post { overlayController.dismiss() }
                    }
                }

                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun launchLockScreen(packageName: String) {
        if (biometricEnabled) {
            mainHandler.post { launchActivityLockScreen(packageName) }
            return
        }
        if (packageName !in settingsPackages && Settings.canDrawOverlays(this)) {
            mainHandler.post {
                overlayController.show(
                    packageName = packageName,
                    appLabel = resolveLabel(packageName),
                )
            }
            return
        }
        mainHandler.post { launchActivityLockScreen(packageName) }
    }

    private fun launchActivityLockScreen(packageName: String) {
        if (activeActivityLockPackage == packageName) return
        activeActivityLockPackage = packageName
        val intent = LockActivity.intent(this, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun resolveLabel(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    companion object {
        private const val POLL_INTERVAL_MILLIS = 750L

        fun start(context: Context) {
            runCatching { context.startService(Intent(context, AppMonitorService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }
}
