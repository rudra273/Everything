package com.rudra.everything.feature.settings.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.rudra.everything.AppContainer
import com.rudra.everything.core.backup.BackupFileNames
import com.rudra.everything.core.backup.DriveBackupFile
import com.rudra.everything.core.backup.DriveBackupSchedule
import com.rudra.everything.core.backup.DriveBackupSource
import com.rudra.everything.core.backup.EverythingBackupService
import com.rudra.everything.core.data.SecureSettingRepository
import com.rudra.everything.core.ui.Cyan
import com.rudra.everything.core.ui.GlassBackground
import com.rudra.everything.core.ui.MutedText
import com.rudra.everything.core.ui.SoftText
import com.rudra.everything.core.ui.glassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BACKUP_MIME_TYPE = "application/vnd.everything.backup+json"
private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

private enum class BackupDriveAuthorizationAction {
    Connect,
    Refresh,
    BackupNow,
    Restore,
}

@Composable
fun BackupRestoreScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val googleDriveBackupClient = container.googleDriveBackupClient
    val savedBackupPassword by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
        .collectAsStateWithLifecycle(initialValue = null)
    val driveScheduleValue by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE)
        .collectAsStateWithLifecycle(initialValue = DriveBackupSchedule.Off.value)
    val driveLastBackupAt by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_LAST_BACKUP_AT)
        .collectAsStateWithLifecycle(initialValue = null)
    val driveLastError by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_LAST_ERROR)
        .collectAsStateWithLifecycle(initialValue = null)
    val driveNeedsAuthorization by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION)
        .collectAsStateWithLifecycle(initialValue = true)

    val driveSchedule = DriveBackupSchedule.fromValue(driveScheduleValue)
    var driveBackups by remember { mutableStateOf<List<DriveBackupFile>>(emptyList()) }
    var driveMessage by remember { mutableStateOf<String?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var driveBusy by remember { mutableStateOf(false) }
    var showExistingDriveBackupPrompt by remember { mutableStateOf(false) }
    var pendingDriveAction by remember { mutableStateOf<BackupDriveAuthorizationAction?>(null) }
    var pendingDriveRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var driveRestorePassword by remember { mutableStateOf("") }
    var showDriveRestorePassword by remember { mutableStateOf(false) }
    var localRestorePassword by remember { mutableStateOf("") }
    var pendingLocalRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingLocalBackupPassword by remember { mutableStateOf<CharArray?>(null) }
    var showBackupPasswordForm by remember { mutableStateOf(false) }
    var backupPasswordDraft by remember { mutableStateOf("") }
    var backupPasswordConfirmDraft by remember { mutableStateOf("") }
    var backupPasswordMessage by remember { mutableStateOf<String?>(null) }

    fun clearDriveRestoreForm() {
        driveRestorePassword = ""
        showDriveRestorePassword = false
    }

    fun showDriveRestoreForm(backup: DriveBackupFile?) {
        val selectedBackup = backup ?: driveBackups.firstOrNull()
        if (selectedBackup == null) {
            driveMessage = "No Drive backup available"
            return
        }
        if (selectedBackup.payloadVersion > EverythingBackupService.PAYLOAD_VERSION) {
            driveMessage = "Update the app to restore this backup"
            return
        }
        pendingDriveRestore = selectedBackup
        driveRestorePassword = ""
        showDriveRestorePassword = true
        showExistingDriveBackupPrompt = false
    }

    fun refreshDriveBackups(accessToken: String, showRestorePrompt: Boolean, successMessage: String? = null) {
        scope.launch {
            driveBusy = true
            driveMessage = runCatching {
                val backups = withContext(Dispatchers.IO) {
                    googleDriveBackupClient.listBackups(accessToken)
                }
                driveBackups = backups
                container.secureSettingRepository.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, false)
                container.secureSettingRepository.delete(SecureSettingRepository.KEY_DRIVE_LAST_ERROR)
                if (showRestorePrompt && backups.isNotEmpty()) {
                    showExistingDriveBackupPrompt = true
                    "Backup already available"
                } else {
                    successMessage ?: "Drive backups refreshed"
                }
            }.getOrElse { error ->
                "Drive refresh failed: ${error.message ?: "unknown error"}"
            }
            driveBusy = false
        }
    }

    fun performDriveBackup(accessToken: String) {
        scope.launch {
            val password = container.secureSettingRepository.getString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
            if (password.isNullOrBlank()) {
                driveMessage = "Set a backup password first"
                return@launch
            }
            driveBusy = true
            val passwordChars = password.toCharArray()
            driveMessage = runCatching {
                val encryptedBackup = withContext(Dispatchers.Default) {
                    container.backupService.exportEncrypted(passwordChars)
                }
                val upload = withContext(Dispatchers.IO) {
                    googleDriveBackupClient.uploadBackup(
                        accessToken = accessToken,
                        encryptedBackup = encryptedBackup,
                        source = DriveBackupSource.Manual,
                    )
                }
                container.secureSettingRepository.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, false)
                container.secureSettingRepository.putString(
                    SecureSettingRepository.KEY_DRIVE_LAST_BACKUP_AT,
                    upload.file.createdAtMillis.toString(),
                )
                container.secureSettingRepository.delete(SecureSettingRepository.KEY_DRIVE_LAST_ERROR)
                driveBackups = withContext(Dispatchers.IO) {
                    googleDriveBackupClient.listBackups(accessToken)
                }
                val pruned = if (upload.deletedOldBackups > 0) {
                    " Removed ${upload.deletedOldBackups} old backup(s)."
                } else {
                    ""
                }
                "Drive backup complete: ${upload.file.createdAtDisplay}.$pruned"
            }.getOrElse { error ->
                passwordChars.fill('\u0000')
                "Drive backup failed: ${error.message ?: "unknown error"}"
            }
            showExistingDriveBackupPrompt = false
            driveBusy = false
        }
    }

    fun restoreDriveBackup(accessToken: String, backup: DriveBackupFile?) {
        val selectedBackup = backup ?: pendingDriveRestore ?: driveBackups.firstOrNull()
        if (selectedBackup == null) {
            driveMessage = "No Drive backup available"
            return
        }
        if (selectedBackup.payloadVersion > EverythingBackupService.PAYLOAD_VERSION) {
            driveMessage = "Update the app to restore this backup"
            return
        }
        val passwordChars = driveRestorePassword.toCharArray()
        clearDriveRestoreForm()
        scope.launch {
            driveBusy = true
            driveMessage = runCatching {
                val encryptedBackup = withContext(Dispatchers.IO) {
                    googleDriveBackupClient.downloadBackup(accessToken, selectedBackup.id)
                }
                withContext(Dispatchers.Default) {
                    container.backupService.importEncrypted(encryptedBackup, passwordChars)
                }
                "Restore complete: ${selectedBackup.createdAtDisplay}"
            }.getOrElse { error ->
                passwordChars.fill('\u0000')
                "Restore failed: ${error.message ?: "unknown error"}"
            }
            pendingDriveRestore = null
            driveBusy = false
        }
    }

    fun handleDriveAuthorization(authorizationResult: AuthorizationResult) {
        val action = pendingDriveAction ?: BackupDriveAuthorizationAction.Refresh
        val accessToken = authorizationResult.accessToken
        pendingDriveAction = null
        if (accessToken.isNullOrBlank()) {
            driveMessage = "Google did not return an access token"
            return
        }
        scope.launch {
            container.secureSettingRepository.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, false)
        }
        when (action) {
            BackupDriveAuthorizationAction.Connect -> refreshDriveBackups(accessToken, showRestorePrompt = true, successMessage = "Google Drive connected")
            BackupDriveAuthorizationAction.Refresh -> refreshDriveBackups(accessToken, showRestorePrompt = false)
            BackupDriveAuthorizationAction.BackupNow -> performDriveBackup(accessToken)
            BackupDriveAuthorizationAction.Restore -> restoreDriveBackup(accessToken, pendingDriveRestore)
        }
    }

    val driveAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            pendingDriveAction = null
            driveMessage = "Google Drive action canceled"
            return@rememberLauncherForActivityResult
        }
        runCatching {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        }.onSuccess { authorizationResult ->
            handleDriveAuthorization(authorizationResult)
        }.onFailure { error ->
            pendingDriveAction = null
            val message = (error as? ApiException)?.statusCode?.let { "Google authorization failed ($it)" }
                ?: "Google authorization failed: ${error.message ?: "unknown error"}"
            driveMessage = message
        }
    }

    fun requestDriveAuthorization(action: BackupDriveAuthorizationAction) {
        pendingDriveAction = action
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
                        pendingDriveAction = null
                        driveMessage = "Google authorization failed: no sign-in prompt was available"
                        return@addOnSuccessListener
                    }
                    driveAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                    )
                } else {
                    handleDriveAuthorization(authorizationResult)
                }
            }
            .addOnFailureListener { error ->
                pendingDriveAction = null
                driveMessage = "Google authorization failed: ${error.message ?: "unknown error"}"
            }
    }

    fun updateDriveSchedule(schedule: DriveBackupSchedule) {
        scope.launch {
            if (schedule != DriveBackupSchedule.Off && driveNeedsAuthorization != false) {
                driveMessage = "Connect Google Drive before enabling automatic backup"
                requestDriveAuthorization(BackupDriveAuthorizationAction.Connect)
                return@launch
            }
            container.secureSettingRepository.putString(SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE, schedule.value)
            container.driveBackupScheduler.applySchedule(schedule)
            driveMessage = when (schedule) {
                DriveBackupSchedule.Off -> "Automatic Drive backup is off"
                DriveBackupSchedule.Daily -> "Automatic Drive backup set to daily"
                DriveBackupSchedule.Weekly -> "Automatic Drive backup set to weekly"
            }
        }
    }

    val localBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri: Uri? ->
        val password = pendingLocalBackupPassword
        pendingLocalBackupPassword = null
        if (uri == null) {
            password?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }
        if (password == null) {
            localMessage = "Local backup failed: backup password was not loaded"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            localMessage = runCatching {
                val encryptedBackup = withContext(Dispatchers.Default) {
                    container.backupService.exportEncrypted(password)
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encryptedBackup.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open backup location")
                }
                "Local backup complete"
            }.getOrElse { error ->
                password.fill('\u0000')
                "Local backup failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun startLocalBackup() {
        scope.launch {
            val password = container.secureSettingRepository.getString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
            if (password.isNullOrBlank()) {
                localMessage = "Set a backup password first"
                return@launch
            }
            pendingLocalBackupPassword = password.toCharArray()
            localBackupLauncher.launch(BackupFileNames.backupName())
        }
    }

    val localRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            pendingLocalRestoreUri = uri
            localRestorePassword = ""
            localMessage = selectedBackupMessage(context, uri)
        }
    }

    fun restoreLocalBackup() {
        val uri = pendingLocalRestoreUri
        val passwordChars = localRestorePassword.toCharArray()
        localRestorePassword = ""
        if (uri == null) {
            passwordChars.fill('\u0000')
            localMessage = "Select a backup file first"
            return
        }
        scope.launch {
            localMessage = runCatching {
                val encryptedBackup = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Could not open backup file")
                }
                withContext(Dispatchers.Default) {
                    container.backupService.importEncrypted(encryptedBackup, passwordChars)
                }
                pendingLocalRestoreUri = null
                "Restore complete"
            }.getOrElse { error ->
                passwordChars.fill('\u0000')
                "Restore failed: ${error.message ?: "unknown error"}"
            }
        }
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
                    Text("Backup & restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                val backupPasswordSaved = !savedBackupPassword.isNullOrBlank()

                BackupPasswordSetupCard(
                    passwordSaved = backupPasswordSaved,
                    showForm = showBackupPasswordForm || !backupPasswordSaved,
                    passwordDraft = backupPasswordDraft,
                    confirmPasswordDraft = backupPasswordConfirmDraft,
                    message = backupPasswordMessage,
                    onToggleForm = {
                        showBackupPasswordForm = !showBackupPasswordForm
                        backupPasswordDraft = ""
                        backupPasswordConfirmDraft = ""
                        if (showBackupPasswordForm) {
                            backupPasswordMessage = null
                        }
                    },
                    onPasswordChange = {
                        backupPasswordDraft = it
                        backupPasswordMessage = null
                    },
                    onConfirmPasswordChange = {
                        backupPasswordConfirmDraft = it
                        backupPasswordMessage = null
                    },
                    onCancel = {
                        showBackupPasswordForm = false
                        backupPasswordDraft = ""
                        backupPasswordConfirmDraft = ""
                    },
                    onRemove = {
                        scope.launch {
                            container.secureSettingRepository.delete(SecureSettingRepository.KEY_BACKUP_PASSWORD)
                            container.secureSettingRepository.putString(
                                SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE,
                                DriveBackupSchedule.Off.value,
                            )
                            container.driveBackupScheduler.applySchedule(DriveBackupSchedule.Off)
                            backupPasswordDraft = ""
                            backupPasswordConfirmDraft = ""
                            showBackupPasswordForm = false
                            showDriveRestorePassword = false
                            pendingDriveRestore = null
                            pendingLocalRestoreUri = null
                            localRestorePassword = ""
                            backupPasswordMessage = "Backup password removed"
                        }
                    },
                    onSave = {
                        val passwordToSave = backupPasswordDraft
                        scope.launch {
                            container.secureSettingRepository.putString(
                                SecureSettingRepository.KEY_BACKUP_PASSWORD,
                                passwordToSave,
                            )
                            backupPasswordDraft = ""
                            backupPasswordConfirmDraft = ""
                            showBackupPasswordForm = false
                            backupPasswordMessage = "Backup password saved"
                            driveMessage = null
                            localMessage = null
                        }
                    },
                )

                if (backupPasswordSaved) {
                    GoogleDriveBackupCard(
                        driveNeedsAuthorization = driveNeedsAuthorization,
                        driveBusy = driveBusy,
                        driveSchedule = driveSchedule,
                        driveLastBackupAt = driveLastBackupAt,
                        driveLastError = driveLastError,
                        driveMessage = driveMessage,
                        driveBackups = driveBackups,
                        showExistingDriveBackupPrompt = showExistingDriveBackupPrompt,
                        showDriveRestorePassword = showDriveRestorePassword,
                        driveRestorePassword = driveRestorePassword,
                        pendingDriveRestore = pendingDriveRestore,
                        onConnectOrRefresh = {
                            driveMessage = null
                            requestDriveAuthorization(
                                if (driveNeedsAuthorization == false) BackupDriveAuthorizationAction.Refresh else BackupDriveAuthorizationAction.Connect,
                            )
                        },
                        onBackupNow = {
                            driveMessage = null
                            requestDriveAuthorization(BackupDriveAuthorizationAction.BackupNow)
                        },
                        onScheduleChange = ::updateDriveSchedule,
                        onRestoreSelected = ::showDriveRestoreForm,
                        onCreateNewFromPrompt = {
                            showExistingDriveBackupPrompt = false
                            requestDriveAuthorization(BackupDriveAuthorizationAction.BackupNow)
                        },
                        onLaterFromPrompt = { showExistingDriveBackupPrompt = false },
                        onDriveRestorePasswordChange = {
                            driveRestorePassword = it
                            driveMessage = null
                        },
                        onCancelDriveRestore = {
                            pendingDriveRestore = null
                            clearDriveRestoreForm()
                        },
                        onConfirmDriveRestore = {
                            requestDriveAuthorization(BackupDriveAuthorizationAction.Restore)
                        },
                    )

                    LocalBackupCard(
                        localMessage = localMessage,
                        pendingRestoreUri = pendingLocalRestoreUri,
                        restorePassword = localRestorePassword,
                        onBackupToFile = {
                            localMessage = null
                            startLocalBackup()
                        },
                        onPickRestoreFile = {
                            localMessage = null
                            pendingLocalRestoreUri = null
                            localRestoreLauncher.launch(arrayOf("*/*"))
                        },
                        onRestorePasswordChange = {
                            localRestorePassword = it
                            localMessage = null
                        },
                        onCancelRestore = {
                            pendingLocalRestoreUri = null
                            localRestorePassword = ""
                        },
                        onRestore = ::restoreLocalBackup,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GoogleDriveBackupCard(
    driveNeedsAuthorization: Boolean?,
    driveBusy: Boolean,
    driveSchedule: DriveBackupSchedule,
    driveLastBackupAt: String?,
    driveLastError: String?,
    driveMessage: String?,
    driveBackups: List<DriveBackupFile>,
    showExistingDriveBackupPrompt: Boolean,
    showDriveRestorePassword: Boolean,
    driveRestorePassword: String,
    pendingDriveRestore: DriveBackupFile?,
    onConnectOrRefresh: () -> Unit,
    onBackupNow: () -> Unit,
    onScheduleChange: (DriveBackupSchedule) -> Unit,
    onRestoreSelected: (DriveBackupFile?) -> Unit,
    onCreateNewFromPrompt: () -> Unit,
    onLaterFromPrompt: () -> Unit,
    onDriveRestorePasswordChange: (String) -> Unit,
    onCancelDriveRestore: () -> Unit,
    onConfirmDriveRestore: () -> Unit,
) {
    BackupBlock(selected = driveNeedsAuthorization == false) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Google Drive backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (driveNeedsAuthorization == false) "Connected for encrypted cloud backups" else "Connect your Google account before cloud backup",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )

            if (showExistingDriveBackupPrompt && driveBackups.isNotEmpty()) {
                Text(
                    "Backup already available. Restore it or create a new backup.",
                    color = Color(0xFFFFD28F),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactActionButton("Restore", Modifier.weight(1f), onClick = { onRestoreSelected(driveBackups.firstOrNull()) })
                    CompactSecondaryButton("Create new", Modifier.weight(1f), onClick = onCreateNewFromPrompt)
                }
                CompactSecondaryButton("Later", Modifier.fillMaxWidth(), onClick = onLaterFromPrompt)
            }

            if (driveNeedsAuthorization != false) {
                CompactActionButton(
                    text = "Connect Google Drive",
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = onConnectOrRefresh,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactActionButton("Refresh", Modifier.weight(1f), icon = {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    }, onClick = onConnectOrRefresh)
                    CompactActionButton("Backup now", Modifier.weight(1f), enabled = !driveBusy, icon = {
                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    }, onClick = onBackupNow)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveScheduleButton(DriveBackupSchedule.Off, driveSchedule == DriveBackupSchedule.Off) {
                    onScheduleChange(DriveBackupSchedule.Off)
                }
                DriveScheduleButton(DriveBackupSchedule.Daily, driveSchedule == DriveBackupSchedule.Daily) {
                    onScheduleChange(DriveBackupSchedule.Daily)
                }
                DriveScheduleButton(DriveBackupSchedule.Weekly, driveSchedule == DriveBackupSchedule.Weekly) {
                    onScheduleChange(DriveBackupSchedule.Weekly)
                }
            }
            Text(driveSchedule.statusText(), color = MutedText, style = MaterialTheme.typography.bodySmall)

            driveLastBackupAt?.toLongOrNull()?.let { lastBackupMillis ->
                Text(
                    "Last backup: ${BackupFileNames.displayDate(lastBackupMillis)}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (driveBackups.isNotEmpty()) {
                Text("Available backups", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                driveBackups.take(3).forEach { backup ->
                    DriveBackupRow(backup = backup, onRestore = { onRestoreSelected(backup) })
                }
            }

            if (showDriveRestorePassword) {
                Text(
                    "Enter password to restore ${pendingDriveRestore?.createdAtDisplay.orEmpty()}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                BackupPasswordField(
                    value = driveRestorePassword,
                    onValueChange = onDriveRestorePasswordChange,
                    label = "Backup password",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSecondaryButton("Cancel", Modifier.weight(1f), onClick = onCancelDriveRestore)
                    CompactActionButton(
                        text = "Restore",
                        modifier = Modifier.weight(1f),
                        enabled = driveRestorePassword.length >= 8,
                        onClick = onConfirmDriveRestore,
                    )
                }
            }

            driveLastError?.takeIf(String::isNotBlank)?.let {
                Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
            }
            driveMessage?.let {
                Text(it, color = if (it.isErrorMessage()) Color(0xFFFFA8A8) else Cyan, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LocalBackupCard(
    localMessage: String?,
    pendingRestoreUri: Uri?,
    restorePassword: String,
    onBackupToFile: () -> Unit,
    onPickRestoreFile: () -> Unit,
    onRestorePasswordChange: (String) -> Unit,
    onCancelRestore: () -> Unit,
    onRestore: () -> Unit,
) {
    BackupBlock {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Local backup file", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Create or restore an encrypted backup file on this device.",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )
            CompactActionButton("Backup to file", Modifier.fillMaxWidth(), icon = {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
            }, onClick = onBackupToFile)
            CompactActionButton("Restore from file", Modifier.fillMaxWidth(), icon = {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            }, onClick = onPickRestoreFile)

            if (pendingRestoreUri != null) {
                BackupPasswordField(
                    value = restorePassword,
                    onValueChange = onRestorePasswordChange,
                    label = "Backup password",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSecondaryButton("Cancel", Modifier.weight(1f), onClick = onCancelRestore)
                    CompactActionButton(
                        text = "Restore",
                        modifier = Modifier.weight(1f),
                        enabled = restorePassword.length >= 8,
                        onClick = onRestore,
                    )
                }
            }

            localMessage?.let {
                Text(it, color = if (it.isErrorMessage()) Color(0xFFFFA8A8) else Cyan, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BackupPasswordSetupCard(
    passwordSaved: Boolean,
    showForm: Boolean,
    passwordDraft: String,
    confirmPasswordDraft: String,
    message: String?,
    onToggleForm: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
) {
    BackupBlock(selected = passwordSaved || showForm) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Backup password", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (passwordSaved) "Saved for encrypted backups" else "Set this before backup and restore",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    message?.let {
                        Text(
                            it,
                            color = if (it.contains("removed", ignoreCase = true)) Color(0xFFFFD28F) else Cyan,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (passwordSaved) {
                    CompactSecondaryButton(
                        text = if (showForm) "Close" else "Change",
                        onClick = onToggleForm,
                    )
                }
            }

            if (showForm) {
                val passwordsMatch = passwordDraft == confirmPasswordDraft
                val canSavePassword = passwordDraft.length >= 8 &&
                    confirmPasswordDraft.length >= 8 &&
                    passwordsMatch

                BackupPasswordField(
                    value = passwordDraft,
                    onValueChange = onPasswordChange,
                    label = "Backup password",
                )
                BackupPasswordField(
                    value = confirmPasswordDraft,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm backup password",
                )
                if (confirmPasswordDraft.isNotEmpty() && !passwordsMatch) {
                    Text("Passwords do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (passwordSaved) {
                        CompactSecondaryButton("Cancel", Modifier.weight(1f), onClick = onCancel)
                        CompactSecondaryButton("Remove", Modifier.weight(1f), onClick = onRemove)
                    }
                    CompactActionButton(
                        text = "Save",
                        modifier = Modifier.weight(1f),
                        enabled = canSavePassword,
                        onClick = onSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupBlock(
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
private fun CompactActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cyan,
            disabledContainerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color(0xFF001716),
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(6.dp))
        Text(text = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun CompactSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.06f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            contentColor = Cyan,
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun DriveScheduleButton(
    schedule: DriveBackupSchedule,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Cyan.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.06f),
            contentColor = if (selected) Color(0xFF001716) else SoftText,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(36.dp),
    ) {
        Text(text = schedule.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun DriveBackupRow(
    backup: DriveBackupFile,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(backup.createdAtDisplay, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            Text("v${backup.payloadVersion} · ${backup.source.label}", color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
        CompactSecondaryButton(
            text = "Restore",
            enabled = backup.payloadVersion <= EverythingBackupService.PAYLOAD_VERSION,
            onClick = onRestore,
        )
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

private fun DriveBackupSchedule.statusText(): String = when (this) {
    DriveBackupSchedule.Off -> "Automatic backup is off"
    DriveBackupSchedule.Daily -> "Automatic backup runs daily"
    DriveBackupSchedule.Weekly -> "Automatic backup runs weekly"
}

private fun String.isErrorMessage(): Boolean {
    return contains("failed", ignoreCase = true) ||
        contains("error", ignoreCase = true) ||
        contains("reconnect", ignoreCase = true) ||
        contains("update the app", ignoreCase = true) ||
        contains("set a backup password", ignoreCase = true)
}

private fun selectedBackupMessage(context: Context, uri: Uri): String {
    val name = displayName(context, uri) ?: "Selected backup"
    val parsed = BackupFileNames.parse(name)
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
