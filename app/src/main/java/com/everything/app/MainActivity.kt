package com.everything.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.permissions.AppLockPermissionChecker
import com.everything.app.core.permissions.AppLockPermissionState
import com.everything.app.core.security.BiometricAuthenticator
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.EverythingTheme
import com.everything.app.feature.applock.service.AppMonitorService
import com.everything.app.feature.applock.ui.AppLockScreen
import com.everything.app.feature.applock.ui.BiometricSetupScreen
import com.everything.app.feature.applock.ui.DashboardScreen
import com.everything.app.feature.applock.ui.PermissionGrantScreen
import com.everything.app.feature.applock.ui.SetupCredentialScreen
import com.everything.app.feature.settings.ui.SettingsScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val container: AppContainer
        get() = (application as EverythingApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView()
    }

    private fun setContentView() {
        setContent {
            EverythingTheme {
                EverythingApp(
                    activity = this,
                    container = container,
                )
            }
        }
    }
}

private enum class MainRoute {
    Dashboard,
    AppLock,
    Settings,
}

@Composable
private fun EverythingApp(
    activity: FragmentActivity,
    container: AppContainer,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val biometricAuthenticator = remember(activity) { BiometricAuthenticator(activity) }

    var credentialReady by remember { mutableStateOf(container.credentialRepository.hasCredential()) }
    var route by remember { mutableStateOf(MainRoute.Dashboard) }
    var permissions by remember { mutableStateOf(AppLockPermissionChecker.check(context)) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    var biometricPreferenceLoaded by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf<Boolean?>(null) }

    val lockedApps by container.appLockRepository
        .observeLockedApps()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        permissions = AppLockPermissionChecker.check(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = AppLockPermissionChecker.check(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeBoolean(SecureSettingRepository.KEY_BIOMETRIC_ENABLED)
            .catch {
                biometricEnabled = false
                biometricPreferenceLoaded = true
            }
            .collect { enabled ->
                biometricEnabled = enabled
                biometricPreferenceLoaded = true
            }
    }

    LaunchedEffect(permissions.allGranted, credentialReady, lockedApps.size) {
        if (permissions.allGranted && credentialReady) {
            AppMonitorService.start(context)
        }
    }

    when {
        !credentialReady -> SetupCredentialScreen(
            onCredentialReady = { pin ->
                container.credentialRepository.saveCredential(pin.toCharArray())
                credentialReady = true
            },
        )

        !biometricPreferenceLoaded -> StartupLoadingScreen()

        biometricEnabled == null -> BiometricSetupScreen(
            canUseBiometric = biometricAuthenticator.canAuthenticate(),
            message = biometricMessage,
            onEnable = {
                biometricAuthenticator.authenticate(
                    title = "Enable biometric",
                    subtitle = "Confirm once to use biometric unlock",
                    onSuccess = {
                        scope.launch {
                            container.secureSettingRepository.putBoolean(
                                SecureSettingRepository.KEY_BIOMETRIC_ENABLED,
                                true,
                            )
                        }
                    },
                    onError = { biometricMessage = it },
                )
            },
            onSkip = {
                scope.launch {
                    container.secureSettingRepository.putBoolean(
                        SecureSettingRepository.KEY_BIOMETRIC_ENABLED,
                        false,
                    )
                }
            },
        )

        !permissions.allGranted -> PermissionGrantScreen(
            permissions = permissions,
            onRefresh = { permissions = AppLockPermissionChecker.check(context) },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )

        route == MainRoute.Dashboard -> DashboardScreen(
            lockedCount = lockedApps.size,
            onOpenAppLock = { route = MainRoute.AppLock },
            onOpenSettings = { route = MainRoute.Settings },
        )

        route == MainRoute.AppLock -> AppLockScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
            onSelectionChanged = {
                permissions = AppLockPermissionChecker.check(context)
                if (permissions.allGranted) {
                    AppMonitorService.start(context)
                }
            },
        )

        route == MainRoute.Settings -> SettingsScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
        )
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Cyan)
    }
}
