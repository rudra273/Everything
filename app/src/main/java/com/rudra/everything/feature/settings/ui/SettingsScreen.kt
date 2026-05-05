package com.rudra.everything.feature.settings.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.rudra.everything.AppContainer
import com.rudra.everything.core.backup.EverythingBackupService
import com.rudra.everything.core.data.SecureSettingRepository
import com.rudra.everything.core.security.BiometricAuthenticator
import com.rudra.everything.core.security.EverythingDeviceAdmin
import com.rudra.everything.core.ui.Cyan
import com.rudra.everything.core.ui.GlassBackground
import com.rudra.everything.core.ui.PrimaryButton
import com.rudra.everything.core.ui.SecondaryButton
import com.rudra.everything.core.ui.MutedText
import com.rudra.everything.core.ui.SoftText
import com.rudra.everything.core.ui.AppTheme
import com.rudra.everything.core.ui.glassSurface
import com.rudra.everything.feature.applock.domain.SettingsPackageResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val SETTINGS_LABEL = "Settings"
private const val BACKUP_MIME_TYPE = "application/vnd.everything.backup+json"

private enum class BackupAction {
    Export,
    Import,
}

private fun AppTheme.displayName(): String = when (this) {
    AppTheme.SPACE_BLACK -> "space dark"
    AppTheme.SKY_BLUE -> "Sky Blue"
    AppTheme.ZINC_ROSE -> "Zinc Rose"
}

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
    settingsPackages: Iterable<String>,
    locked: Boolean,
) {
    settingsPackages.forEach { packageName ->
        container.appLockRepository.setLocked(
            packageName = packageName,
            label = SETTINGS_LABEL,
            locked = locked,
        )
    }
}

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometricAuthenticator = remember(activity) { BiometricAuthenticator(activity) }
    val biometricEnabled by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_BIOMETRIC_ENABLED)
        .collectAsStateWithLifecycle(initialValue = false)
    val appLockToolLocked by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_APP_LOCK)
        .collectAsStateWithLifecycle(initialValue = true)
    val keyStoreToolLocked by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_KEY_STORE)
        .collectAsStateWithLifecycle(initialValue = true)
    val notesToolLocked by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_NOTES)
        .collectAsStateWithLifecycle(initialValue = true)

    val settingsPackages = remember(context) { SettingsPackageResolver.resolve(context) }
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
    var backupAction by remember { mutableStateOf<BackupAction?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var backupConfirmPassword by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val sharedPrefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var currentTheme by remember { mutableStateOf(sharedPrefs.getString("app_theme", AppTheme.SPACE_BLACK.name) ?: AppTheme.SPACE_BLACK.name) }

    fun resetBackupForm() {
        backupPassword = ""
        backupConfirmPassword = ""
        backupAction = null
        pendingImportUri = null
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri: Uri? ->
        val password = backupPassword.toCharArray()
        resetBackupForm()
        if (uri == null) {
            password.fill('\u0000')
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            backupMessage = runCatching {
                val encryptedBackup = withContext(Dispatchers.Default) {
                    container.backupService.exportEncrypted(password)
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encryptedBackup.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open export location")
                }
                "Export complete"
            }.getOrElse { error ->
                password.fill('\u0000')
                "Export failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            backupAction = BackupAction.Import
            backupPassword = ""
            backupConfirmPassword = ""
            backupMessage = selectedBackupMessage(context, uri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasAdmin = isAdminActive
                isAdminActive = isDeviceAdminActive(context)
                if (!wasAdmin && isAdminActive) {
                    scope.launch {
                        setSettingsLocked(container, settingsPackages, locked = true)
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
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
                    }
                    Spacer(Modifier.width(4.dp))
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
            SettingsSectionTitle("Appearance")

            GlassSettingsBlock(selected = true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(Icons.Rounded.Palette, selected = true)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Theme",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val selectedTheme = runCatching { AppTheme.valueOf(currentTheme) }.getOrDefault(AppTheme.SPACE_BLACK)
                            Text(selectedTheme.displayName(), color = MutedText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(AppTheme.SPACE_BLACK, AppTheme.SKY_BLUE, AppTheme.ZINC_ROSE).forEach { theme ->
                            val selected = currentTheme == theme.name
                            Button(
                                onClick = {
                                    sharedPrefs.edit().putString("app_theme", theme.name).apply()
                                    currentTheme = theme.name
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Cyan.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.06f),
                                    contentColor = if (selected) Color(0xFF001716) else SoftText
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(40.dp),
                            ) {
                                Text(
                                    text = theme.displayName(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            SettingsSectionTitle("Protection")

            GlassSettingsBlock(
                selected = appLockToolLocked != false ||
                    keyStoreToolLocked != false ||
                    notesToolLocked != false,
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
                        locked = appLockToolLocked != false,
                        onLockedChange = { locked ->
                            scope.launch {
                                container.secureSettingRepository.putBoolean(
                                    SecureSettingRepository.KEY_TOOL_LOCK_APP_LOCK,
                                    locked,
                                )
                            }
                        },
                    )
                    UtilityToolLockRow(
                        icon = Icons.Rounded.Key,
                        title = "Key Store",
                        locked = keyStoreToolLocked != false,
                        onLockedChange = { locked ->
                            scope.launch {
                                container.secureSettingRepository.putBoolean(
                                    SecureSettingRepository.KEY_TOOL_LOCK_KEY_STORE,
                                    locked,
                                )
                            }
                        },
                    )
                    UtilityToolLockRow(
                        icon = Icons.Rounded.NoteAlt,
                        title = "Notes",
                        locked = notesToolLocked != false,
                        onLockedChange = { locked ->
                            scope.launch {
                                container.secureSettingRepository.putBoolean(
                                    SecureSettingRepository.KEY_TOOL_LOCK_NOTES,
                                    locked,
                                )
                            }
                        },
                    )
                }
            }

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
                                scope.launch {
                                    container.secureSettingRepository.putBoolean(
                                        SecureSettingRepository.KEY_BIOMETRIC_ENABLED,
                                        false,
                                    )
                                }
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
                                                setSettingsLocked(container, settingsPackages, locked = false)
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

            SettingsSectionTitle("Backup")

            GlassSettingsBlock {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "App Data",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Exports an encrypted Everything backup file. Store it locally or choose Drive from the file picker.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "Export",
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            },
                            onClick = {
                                backupAction = BackupAction.Export
                                backupPassword = ""
                                backupConfirmPassword = ""
                                backupMessage = null
                            },
                        )
                        PrimaryButton(
                            text = "Import",
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            },
                            onClick = {
                                backupPassword = ""
                                backupConfirmPassword = ""
                                backupMessage = null
                                pendingImportUri = null
                                importLauncher.launch(arrayOf("*/*"))
                            },
                        )
                    }

                    backupMessage?.let {
                        Text(
                            text = it,
                            color = if (it.contains("failed", ignoreCase = true)) Color(0xFFFFA8A8) else Cyan,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    val currentBackupAction = backupAction
                    if (currentBackupAction != null) {
                        BackupPasswordForm(
                            action = currentBackupAction,
                            password = backupPassword,
                            confirmPassword = backupConfirmPassword,
                            onPasswordChange = { backupPassword = it },
                            onConfirmPasswordChange = { backupConfirmPassword = it },
                            onCancel = { resetBackupForm() },
                            onContinue = {
                                when (currentBackupAction) {
                                    BackupAction.Export -> exportLauncher.launch(defaultBackupName())
                                    BackupAction.Import -> {
                                        val uri = pendingImportUri
                                        val password = backupPassword.toCharArray()
                                        resetBackupForm()
                                        if (uri == null) {
                                            password.fill('\u0000')
                                            backupMessage = "Select a backup file first"
                                        } else {
                                            scope.launch {
                                                backupMessage = runCatching {
                                                    val encryptedBackup = withContext(Dispatchers.IO) {
                                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                                            input.readBytes().toString(Charsets.UTF_8)
                                                        } ?: error("Could not open backup file")
                                                    }
                                                    withContext(Dispatchers.Default) {
                                                        container.backupService.importEncrypted(encryptedBackup, password)
                                                    }
                                                    "Import complete"
                                                }.getOrElse { error ->
                                                    password.fill('\u0000')
                                                    "Import failed: ${error.message ?: "unknown error"}"
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }

                Spacer(Modifier.height(24.dp))
            }
        }
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

@Composable
private fun BackupPasswordForm(
    action: BackupAction,
    password: String,
    confirmPassword: String,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    val canContinue = when (action) {
        BackupAction.Export -> password.length >= 8 && password == confirmPassword
        BackupAction.Import -> password.length >= 8
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BackupPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Backup password",
        )
        if (action == BackupAction.Export) {
            BackupPasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm backup password",
            )
            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text("Passwords do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "Cancel", onClick = onCancel)
            PrimaryButton(
                text = "Continue",
                enabled = canContinue,
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun BackupPasswordField(
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

private fun defaultBackupName(): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "everything-v${EverythingBackupService.PAYLOAD_VERSION}-$timestamp.everything"
}

private fun selectedBackupMessage(context: Context, uri: Uri): String {
    val name = displayName(context, uri) ?: "Selected backup"
    val parsed = parseBackupFileName(name)
    return if (parsed != null) {
        "Backup selected: v${parsed.version}, exported ${parsed.exportedAt}. Enter its password."
    } else {
        "Backup selected: $name. Enter its password."
    }
}

private fun displayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
}

private fun parseBackupFileName(name: String): BackupFileNameInfo? {
    val match = Regex("""everything-v(\d+)-(\d{8})-(\d{6})\.everything""")
        .matchEntire(name)
        ?: return null
    val date = match.groupValues[2]
    val time = match.groupValues[3]
    val exportedAt = "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)} " +
        "${time.substring(0, 2)}:${time.substring(2, 4)}:${time.substring(4, 6)}"
    return BackupFileNameInfo(
        version = match.groupValues[1],
        exportedAt = exportedAt,
    )
}

private data class BackupFileNameInfo(
    val version: String,
    val exportedAt: String,
)
