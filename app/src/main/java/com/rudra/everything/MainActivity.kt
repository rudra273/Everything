package com.rudra.everything

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
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
import androidx.fragment.app.FragmentActivity
import com.rudra.everything.core.data.SecureSettingRepository
import com.rudra.everything.core.permissions.AppLockPermissionChecker
import com.rudra.everything.core.permissions.AppLockPermissionState
import com.rudra.everything.core.security.BiometricAuthenticator
import com.rudra.everything.core.ui.Cyan
import com.rudra.everything.core.ui.EverythingTheme
import com.rudra.everything.feature.applock.data.LockedApp
import com.rudra.everything.feature.applock.service.AppMonitorService
import com.rudra.everything.feature.applock.ui.AppLockScreen
import com.rudra.everything.feature.applock.ui.BiometricSetupScreen
import com.rudra.everything.feature.applock.ui.DashboardScreen
import com.rudra.everything.feature.applock.ui.PermissionGrantScreen
import com.rudra.everything.feature.applock.ui.SetupCredentialScreen
import com.rudra.everything.feature.expense.ui.ExpenseScreen
import com.rudra.everything.feature.keystore.ui.KeyStoreScreen
import com.rudra.everything.feature.notes.ui.SecureNotesScreen
import com.rudra.everything.feature.settings.ui.BackupRestoreScreen
import com.rudra.everything.feature.settings.ui.SettingsScreen
import com.rudra.everything.feature.settings.ui.ThemeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    KeyStore,
    Notes,
    Expenses,
    BackupRestore,
    Settings,
    Theme,
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
    var lockedApps by remember { mutableStateOf(emptyList<LockedApp>()) }

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
        val repository = withContext(Dispatchers.IO) {
            container.secureSettingRepository
        }
        repository
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

    LaunchedEffect(Unit) {
        val repository = withContext(Dispatchers.IO) {
            container.appLockRepository
        }
        repository
            .observeLockedApps()
            .catch { lockedApps = emptyList() }
            .collect { apps -> lockedApps = apps }
    }

    LaunchedEffect(permissions.allGranted, credentialReady, lockedApps.size) {
        if (permissions.allGranted && credentialReady) {
            AppMonitorService.start(context)
        }
    }

    when {
        !credentialReady -> SetupCredentialScreen(
            onCredentialReady = { pin ->
                scope.launch {
                    withContext(Dispatchers.Default) {
                        container.credentialRepository.saveCredential(pin.toCharArray())
                    }
                    credentialReady = true
                }
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
        )

        route == MainRoute.Dashboard -> DashboardScreen(
            lockedCount = lockedApps.size,
            onOpenAppLock = { route = MainRoute.AppLock },
            onOpenKeyStore = { route = MainRoute.KeyStore },
            onOpenNotes = { route = MainRoute.Notes },
            onOpenExpenses = { route = MainRoute.Expenses },
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

        route == MainRoute.KeyStore -> KeyStoreScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
        )

        route == MainRoute.Notes -> SecureNotesScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
        )

        route == MainRoute.Expenses -> ExpenseScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
        )

        route == MainRoute.BackupRestore -> BackupRestoreScreen(
            container = container,
            onBack = { route = MainRoute.Settings },
        )

        route == MainRoute.Theme -> ThemeScreen(
            onBack = { route = MainRoute.Settings },
        )

        route == MainRoute.Settings -> SettingsScreen(
            container = container,
            onBack = { route = MainRoute.Dashboard },
            onOpenBackupRestore = { route = MainRoute.BackupRestore },
            onOpenTheme = { route = MainRoute.Theme },
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
