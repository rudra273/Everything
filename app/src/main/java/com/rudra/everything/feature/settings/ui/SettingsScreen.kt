package com.rudra.everything.feature.settings.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.rudra.everything.AppContainer
import com.rudra.everything.core.data.SecureSettingRepository
import com.rudra.everything.core.permissions.AppLockPermissionChecker
import com.rudra.everything.core.permissions.PermissionIntents
import com.rudra.everything.core.security.BiometricAuthenticator
import com.rudra.everything.core.security.EverythingDeviceAdmin
import com.rudra.everything.core.ui.AppBackButton
import com.rudra.everything.core.ui.Cyan
import com.rudra.everything.core.ui.GlassBackground
import com.rudra.everything.core.ui.GlassLoadingIndicator
import com.rudra.everything.core.ui.PrimaryButton
import com.rudra.everything.core.ui.SecondaryButton
import com.rudra.everything.core.ui.MutedText
import com.rudra.everything.core.ui.SoftText
import com.rudra.everything.core.ui.glassSurface
import com.rudra.everything.feature.applock.domain.SettingsPackageResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SETTINGS_LABEL = "Settings"

private data class UtilityLockDisableRequest(
    val key: String,
    val title: String,
)

private fun deviceAdminComponent(context: Context) =
    ComponentName(context, EverythingDeviceAdmin::class.java)

private fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isAdminActive(deviceAdminComponent(context))
}

private fun deviceAdminIntent(context: Context): Intent {
    return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            deviceAdminComponent(context),
        )
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable to protect Everything from being uninstalled without your PIN.",
        )
    }
}

private fun removeDeviceAdmin(context: Context) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val component = deviceAdminComponent(context)
    if (dpm.isAdminActive(component)) {
        dpm.removeActiveAdmin(component)
    }
}

private suspend fun setSettingsLocked(
    container: AppContainer,
    settingsPackage: String,
    locked: Boolean,
) {
    container.appLockRepository.getLockedApps()
        .filter { app -> app.label == SETTINGS_LABEL && app.packageName != settingsPackage }
        .forEach { app ->
            container.appLockRepository.setLocked(
                packageName = app.packageName,
                label = app.label,
                locked = false,
            )
        }

    container.appLockRepository.setLocked(
        packageName = settingsPackage,
        label = SETTINGS_LABEL,
        locked = locked,
    )
}

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenTheme: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometricAuthenticator = remember(activity) { BiometricAuthenticator(activity) }
    var biometricEnabled by remember { mutableStateOf<Boolean?>(null) }
    var appLockToolLocked by remember { mutableStateOf<Boolean?>(null) }
    var keyStoreToolLocked by remember { mutableStateOf<Boolean?>(null) }
    var notesToolLocked by remember { mutableStateOf<Boolean?>(null) }
    var accessibilityEnabled by remember {
        mutableStateOf(AppLockPermissionChecker.hasAccessibilityService(context))
    }
    val settingsLoaded = biometricEnabled != null &&
        appLockToolLocked != null &&
        keyStoreToolLocked != null &&
        notesToolLocked != null

    val settingsPackage = remember(context) { SettingsPackageResolver.resolve(context) }
    var isAdminActive by remember { mutableStateOf(isDeviceAdminActive(context)) }
    var showDisablePin by remember { mutableStateOf(false) }
    var disablePin by remember { mutableStateOf("") }
    var disablePinError by remember { mutableStateOf<String?>(null) }
    var disablePinVisible by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }
    var changePinMessage by remember { mutableStateOf<String?>(null) }
    var changingPin by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    var showBiometricDisableConfirm by remember { mutableStateOf(false) }
    var biometricDisablePin by remember { mutableStateOf("") }
    var biometricDisableError by remember { mutableStateOf<String?>(null) }
    var pendingUtilityDisable by remember { mutableStateOf<UtilityLockDisableRequest?>(null) }
    var utilityDisablePin by remember { mutableStateOf("") }
    var utilityDisableError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeBoolean(SecureSettingRepository.KEY_BIOMETRIC_ENABLED)
            .collect { enabled -> biometricEnabled = enabled ?: false }
    }

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_APP_LOCK)
            .collect { locked -> appLockToolLocked = locked ?: true }
    }

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_KEY_STORE)
            .collect { locked -> keyStoreToolLocked = locked ?: true }
    }

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_NOTES)
            .collect { locked -> notesToolLocked = locked ?: true }
    }

    fun requestUtilityLockChange(key: String, title: String, locked: Boolean) {
        if (locked) {
            scope.launch {
                container.secureSettingRepository.putBoolean(key, true)
            }
        } else {
            pendingUtilityDisable = UtilityLockDisableRequest(key = key, title = title)
            utilityDisablePin = ""
            utilityDisableError = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasAdmin = isAdminActive
                isAdminActive = isDeviceAdminActive(context)
                accessibilityEnabled = AppLockPermissionChecker.hasAccessibilityService(context)
                if (!wasAdmin && isAdminActive) {
                    scope.launch {
                        setSettingsLocked(container, settingsPackage, locked = true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppBackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            if (!settingsLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    GlassLoadingIndicator()
                }
                return@Column
            }

            SettingsSectionTitle("Data")

            SettingsNavigationRow(
                icon = Icons.Rounded.CloudUpload,
                title = "Backup & Restore",
                subtitle = "Backup password, Google Drive, and local files",
                onClick = onOpenBackupRestore,
            )

            SettingsSectionTitle("Protection")

            GlassSettingsBlock(selected = showChangePin) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsIconBadge(Icons.Rounded.Lock, selected = showChangePin)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Master PIN",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Change the PIN used to unlock Everything",
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            changePinMessage?.let {
                                Text(
                                    text = it,
                                    color = Cyan,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        SecondaryButton(
                            text = if (showChangePin) "Close" else "Change",
                            textStyle = MaterialTheme.typography.labelMedium,
                            onClick = {
                                showChangePin = !showChangePin
                                oldPin = ""
                                newPin = ""
                                confirmNewPin = ""
                                changePinError = null
                                if (showChangePin) {
                                    changePinMessage = null
                                }
                            },
                        )
                    }

                    if (showChangePin) {
                        val pinsMatch = newPin == confirmNewPin
                        val canChangePin = oldPin.length >= 4 &&
                            newPin.length >= 4 &&
                            confirmNewPin.length >= 4 &&
                            pinsMatch &&
                            !changingPin

                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PinTextField(
                                value = oldPin,
                                onValueChange = {
                                    oldPin = it.filter(Char::isDigit).take(12)
                                    changePinError = null
                                },
                                label = "Old PIN",
                            )
                            PinTextField(
                                value = newPin,
                                onValueChange = {
                                    newPin = it.filter(Char::isDigit).take(12)
                                    changePinError = null
                                },
                                label = "New PIN",
                            )
                            PinTextField(
                                value = confirmNewPin,
                                onValueChange = {
                                    confirmNewPin = it.filter(Char::isDigit).take(12)
                                    changePinError = null
                                },
                                label = "Confirm new PIN",
                            )
                            if (newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && !pinsMatch) {
                                Text("PINs do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                            }
                            changePinError?.let {
                                Text(text = it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    text = "Cancel",
                                    enabled = !changingPin,
                                    onClick = {
                                        showChangePin = false
                                        oldPin = ""
                                        newPin = ""
                                        confirmNewPin = ""
                                        changePinError = null
                                    },
                                )
                                PrimaryButton(
                                    text = if (changingPin) "Saving" else "Save",
                                    enabled = canChangePin,
                                    onClick = {
                                        scope.launch {
                                            changingPin = true
                                            changePinError = null
                                            changePinMessage = null
                                            runCatching {
                                                val oldCandidate = oldPin
                                                val newCandidate = newPin
                                                val valid = withContext(Dispatchers.Default) {
                                                    container.credentialRepository.verify(oldCandidate.toCharArray())
                                                }
                                                if (valid) {
                                                    withContext(Dispatchers.Default) {
                                                        container.credentialRepository.saveCredential(newCandidate.toCharArray())
                                                    }
                                                    oldPin = ""
                                                    newPin = ""
                                                    confirmNewPin = ""
                                                    showChangePin = false
                                                    changePinMessage = "PIN updated"
                                                } else {
                                                    oldPin = ""
                                                    changePinError = "Old PIN is incorrect"
                                                }
                                            }.onFailure { error ->
                                                changePinError = error.message ?: "Could not update PIN"
                                            }.also {
                                                changingPin = false
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            GlassSettingsBlock(selected = biometricEnabled == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIconBadge(Icons.Rounded.Fingerprint, selected = biometricEnabled == true)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fingerprint Unlock",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = if (biometricEnabled == true) "Enabled for Everything tools" else "Use master PIN only",
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        biometricMessage?.let {
                            Text(
                                text = it,
                                color = Color(0xFFFFD28F),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Switch(
                        modifier = Modifier.scale(0.78f),
                        checked = biometricEnabled == true,
                        onCheckedChange = { enable ->
                            biometricMessage = null
                            if (enable) {
                                if (!biometricAuthenticator.canAuthenticate()) {
                                    biometricMessage = "Fingerprint is unavailable on this device"
                                } else {
                                    biometricAuthenticator.authenticate(
                                        title = "Enable fingerprint",
                                        subtitle = "Confirm once for Everything tools",
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
                                }
                            } else {
                                showBiometricDisableConfirm = true
                                biometricDisablePin = ""
                                biometricDisableError = null
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cyan,
                            checkedTrackColor = Cyan.copy(alpha = 0.22f),
                            uncheckedThumbColor = SoftText,
                            uncheckedTrackColor = Color.Transparent,
                            uncheckedBorderColor = SoftText.copy(alpha = 0.22f),
                        ),
                    )
                }
            }

            GlassSettingsBlock(selected = isAdminActive) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsIconBadge(Icons.Rounded.Shield, selected = isAdminActive)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Uninstall Protection",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = if (isAdminActive) "PIN required to uninstall"
                                else "Anyone can uninstall",
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            modifier = Modifier.scale(0.78f),
                            checked = isAdminActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    context.startActivity(deviceAdminIntent(context))
                                } else {
                                    showDisablePin = true
                                    disablePin = ""
                                    disablePinError = null
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Cyan,
                                checkedTrackColor = Cyan.copy(alpha = 0.22f),
                                uncheckedThumbColor = SoftText,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = SoftText.copy(alpha = 0.22f),
                            ),
                        )
                    }

                    if (showDisablePin) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = disablePin,
                                onValueChange = {
                                    disablePin = it.filter(Char::isDigit).take(12)
                                    disablePinError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Enter PIN to disable") },
                                singleLine = true,
                                visualTransformation = if (disablePinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { disablePinVisible = !disablePinVisible }) {
                                        Icon(
                                            if (disablePinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (disablePinVisible) "Hide" else "Show",
                                            tint = SoftText,
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Cyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Cyan,
                                    focusedLabelColor = Cyan,
                                    unfocusedLabelColor = MutedText,
                                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                ),
                            )
                            disablePinError?.let {
                                Text(text = it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    text = "Cancel",
                                    onClick = {
                                        showDisablePin = false
                                        disablePin = ""
                                    }
                                )
                                PrimaryButton(
                                    text = "Confirm",
                                    enabled = disablePin.length >= 4,
                                    onClick = {
                                        scope.launch {
                                            val pin = disablePin
                                            val valid = withContext(Dispatchers.Default) {
                                                container.credentialRepository.verify(pin.toCharArray())
                                            }
                                            if (valid) {
                                                setSettingsLocked(container, settingsPackage, locked = false)
                                                removeDeviceAdmin(context)
                                                isAdminActive = false
                                                showDisablePin = false
                                                disablePin = ""
                                            } else {
                                                disablePinError = "Wrong PIN"
                                                disablePin = ""
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (isAdminActive) {
                Text(
                    text = "The Settings app is locked to prevent admin deactivation. " +
                        "Enter your PIN to disable this protection.",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            SettingsNavigationRow(
                icon = Icons.Rounded.AccessibilityNew,
                title = "Reliable App Lock",
                subtitle = if (accessibilityEnabled) {
                    "Accessibility detection is enabled"
                } else {
                    "Enable stronger locked-app detection"
                },
                selected = accessibilityEnabled,
                onClick = {
                    runCatching {
                        context.startActivity(PermissionIntents.accessibilitySettings())
                    }
                },
            )

            GlassSettingsBlock(
                selected = appLockToolLocked == true ||
                    keyStoreToolLocked == true ||
                    notesToolLocked == true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Utility Tool Locks",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Choose which Everything tools need the master PIN before opening.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    UtilityToolLockRow(
                        icon = Icons.Rounded.Apps,
                        title = "App Lock",
                        locked = appLockToolLocked == true,
                        onLockedChange = { locked ->
                            requestUtilityLockChange(
                                key = SecureSettingRepository.KEY_TOOL_LOCK_APP_LOCK,
                                title = "App Lock",
                                locked = locked,
                            )
                        },
                    )
                    UtilityToolLockRow(
                        icon = Icons.Rounded.Key,
                        title = "Key Store",
                        locked = keyStoreToolLocked == true,
                        onLockedChange = { locked ->
                            requestUtilityLockChange(
                                key = SecureSettingRepository.KEY_TOOL_LOCK_KEY_STORE,
                                title = "Key Store",
                                locked = locked,
                            )
                        },
                    )
                    UtilityToolLockRow(
                        icon = Icons.Rounded.NoteAlt,
                        title = "Notes",
                        locked = notesToolLocked == true,
                        onLockedChange = { locked ->
                            requestUtilityLockChange(
                                key = SecureSettingRepository.KEY_TOOL_LOCK_NOTES,
                                title = "Notes",
                                locked = locked,
                            )
                        },
                    )
                }
            }

            SettingsSectionTitle("Personalization")

            SettingsNavigationRow(
                icon = Icons.Rounded.Palette,
                title = "Theme",
                subtitle = "Choose the app theme",
                onClick = onOpenTheme,
            )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showBiometricDisableConfirm) {
        AlertDialog(
            onDismissRequest = {
                showBiometricDisableConfirm = false
                biometricDisablePin = ""
                biometricDisableError = null
            },
            title = { Text("Turn off fingerprint", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your master PIN to turn off fingerprint unlock.", color = MutedText)
                    PinTextField(
                        value = biometricDisablePin,
                        onValueChange = {
                            biometricDisablePin = it.filter(Char::isDigit).take(12)
                            biometricDisableError = null
                        },
                        label = "Master PIN",
                    )
                    biometricDisableError?.let {
                        Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = biometricDisablePin.length >= 4,
                    onClick = {
                        scope.launch {
                            val pin = biometricDisablePin
                            val valid = withContext(Dispatchers.Default) {
                                container.credentialRepository.verify(pin.toCharArray())
                            }
                            if (valid) {
                                container.secureSettingRepository.putBoolean(
                                    SecureSettingRepository.KEY_BIOMETRIC_ENABLED,
                                    false,
                                )
                                showBiometricDisableConfirm = false
                                biometricDisablePin = ""
                                biometricDisableError = null
                                biometricMessage = "Fingerprint unlock turned off"
                            } else {
                                biometricDisablePin = ""
                                biometricDisableError = "Wrong PIN"
                            }
                        }
                    },
                ) {
                    Text("Confirm", color = Cyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBiometricDisableConfirm = false
                        biometricDisablePin = ""
                        biometricDisableError = null
                    },
                ) {
                    Text("Cancel", color = MutedText)
                }
            },
            containerColor = Color(0xFF101417),
            titleContentColor = SoftText,
            textContentColor = SoftText,
            shape = RoundedCornerShape(14.dp),
        )
    }

    pendingUtilityDisable?.let { request ->
        AlertDialog(
            onDismissRequest = {
                pendingUtilityDisable = null
                utilityDisablePin = ""
                utilityDisableError = null
            },
            title = { Text("Turn off ${request.title} lock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Confirm with fingerprint or master PIN.", color = MutedText)
                    PinTextField(
                        value = utilityDisablePin,
                        onValueChange = {
                            utilityDisablePin = it.filter(Char::isDigit).take(12)
                            utilityDisableError = null
                        },
                        label = "Master PIN",
                    )
                    utilityDisableError?.let {
                        Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = utilityDisablePin.length >= 4,
                    onClick = {
                        scope.launch {
                            val pin = utilityDisablePin
                            val valid = withContext(Dispatchers.Default) {
                                container.credentialRepository.verify(pin.toCharArray())
                            }
                            if (valid) {
                                container.secureSettingRepository.putBoolean(request.key, false)
                                pendingUtilityDisable = null
                                utilityDisablePin = ""
                                utilityDisableError = null
                            } else {
                                utilityDisablePin = ""
                                utilityDisableError = "Wrong PIN"
                            }
                        }
                    },
                ) {
                    Text("Confirm", color = Cyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    if (biometricEnabled == true && biometricAuthenticator.canAuthenticate()) {
                        TextButton(
                            onClick = {
                                biometricAuthenticator.authenticate(
                                    title = "Confirm change",
                                    subtitle = "Turn off ${request.title} lock",
                                    onSuccess = {
                                        scope.launch {
                                            container.secureSettingRepository.putBoolean(request.key, false)
                                            pendingUtilityDisable = null
                                            utilityDisablePin = ""
                                            utilityDisableError = null
                                        }
                                    },
                                    onError = { utilityDisableError = it },
                                )
                            },
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fingerprint", color = Cyan)
                        }
                    }
                    TextButton(
                        onClick = {
                            pendingUtilityDisable = null
                            utilityDisablePin = ""
                            utilityDisableError = null
                        },
                    ) {
                        Text("Cancel", color = MutedText)
                    }
                }
            },
            containerColor = Color(0xFF101417),
            titleContentColor = SoftText,
            textContentColor = SoftText,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = SoftText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    GlassSettingsBlock(selected = selected) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(icon, selected = selected)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = subtitle,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GlassSettingsBlock(
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(18.dp),
                selected = selected,
                tintStrength = 0.08f,
                shadowElevation = 2f,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    selected: Boolean,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) Cyan else SoftText,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun UtilityToolLockRow(
    icon: ImageVector,
    title: String,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon, selected = locked)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (locked) "Master PIN required" else "Opens without PIN",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            modifier = Modifier.scale(0.78f),
            checked = locked,
            onCheckedChange = onLockedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Cyan,
                checkedTrackColor = Cyan.copy(alpha = 0.22f),
                uncheckedThumbColor = SoftText,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = SoftText.copy(alpha = 0.22f),
            ),
        )
    }
}

@Composable
private fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = SoftText,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Cyan,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        ),
    )
}
