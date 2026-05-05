package com.rudra.everything.feature.settings.ui

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"

private enum class BackupSheet {
    Password,
    BackupList,
    RestorePassword,
    GoogleAccount,
    Schedule,
}

private enum class BackupDriveAuthorizationAction {
    Refresh,
    BackupNow,
    Restore,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val googleDriveBackupClient = container.googleDriveBackupClient
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var savedBackupPassword by remember { mutableStateOf<String?>(null) }
    var backupPasswordLoaded by remember { mutableStateOf(false) }
    val driveScheduleValue by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE)
        .collectAsStateWithLifecycle(initialValue = DriveBackupSchedule.Weekly.value)
    val selectedDriveAccount by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_ACCOUNT_EMAIL)
        .collectAsStateWithLifecycle(initialValue = null)
    val driveLastBackupAt by container.secureSettingRepository
        .observeString(SecureSettingRepository.KEY_DRIVE_LAST_BACKUP_AT)
        .collectAsStateWithLifecycle(initialValue = null)

    val driveSchedule = DriveBackupSchedule.fromValue(driveScheduleValue)
    var activeSheet by remember { mutableStateOf<BackupSheet?>(null) }
    var driveBackups by remember { mutableStateOf<List<DriveBackupFile>>(emptyList()) }
    var driveBusy by remember { mutableStateOf(false) }
    var pendingDriveAction by remember { mutableStateOf<BackupDriveAuthorizationAction?>(null) }
    var pendingDriveRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var passwordDraft by remember { mutableStateOf("") }
    var passwordConfirmDraft by remember { mutableStateOf("") }
    var oldPasswordDraft by remember { mutableStateOf("") }
    var passwordSheetMode by remember { mutableStateOf(if (savedBackupPassword.isNullOrBlank()) "set" else "options") }
    var showRemovePasswordWarning by remember { mutableStateOf(false) }
    var googleAccounts by remember { mutableStateOf<List<String>>(emptyList()) }
    var accountDraft by remember { mutableStateOf<String?>(null) }

    val passwordSet = !savedBackupPassword.isNullOrBlank()
    val latestBackup = driveBackups.firstOrNull()
    val lastBackupText = latestBackup?.createdAtDisplay
        ?: driveLastBackupAt?.toLongOrNull()?.let(BackupFileNames::displayDate)
        ?: "Never"
    val lastBackupSizeText = latestBackup?.sizeBytes.displaySize()
    val accountSubtitle = selectedDriveAccount ?: "Select Google account"

    LaunchedEffect(Unit) {
        container.secureSettingRepository
            .observeString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
            .collect { password ->
                savedBackupPassword = password
                backupPasswordLoaded = true
                if (password.isNullOrBlank()) {
                    passwordSheetMode = "set"
                }
            }
    }

    fun showSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun clearPasswordDrafts() {
        passwordDraft = ""
        passwordConfirmDraft = ""
        oldPasswordDraft = ""
        showRemovePasswordWarning = false
    }

    fun restartApp() {
        val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (restartIntent != null) {
            context.startActivity(restartIntent)
            activity.finishAffinity()
        }
    }

    fun loadGoogleAccounts() {
        val accounts = runCatching {
            AccountManager.get(context)
                .getAccountsByType(GOOGLE_ACCOUNT_TYPE)
                .map { it.name }
                .distinct()
                .sorted()
        }.getOrElse {
            showSnackbar("Could not read Google accounts")
            emptyList()
        }
        googleAccounts = accounts
        accountDraft = selectedDriveAccount ?: accounts.firstOrNull()
    }

    fun openGoogleAccountSheet() {
        loadGoogleAccounts()
        activeSheet = BackupSheet.GoogleAccount
    }

    val accountPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            openGoogleAccountSheet()
        } else {
            showSnackbar("Allow account access to choose a Google account")
        }
    }

    fun requestGoogleAccountSheet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.checkSelfPermission(Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED
        ) {
            accountPermissionLauncher.launch(Manifest.permission.GET_ACCOUNTS)
        } else {
            openGoogleAccountSheet()
        }
    }

    fun refreshDriveBackups(accessToken: String) {
        scope.launch {
            driveBusy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    googleDriveBackupClient.listBackups(accessToken)
                }
            }.onSuccess { backups ->
                driveBackups = backups
                container.secureSettingRepository.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, false)
                container.secureSettingRepository.delete(SecureSettingRepository.KEY_DRIVE_LAST_ERROR)
            }.onFailure { error ->
                showSnackbar("Could not load backups: ${error.message ?: "unknown error"}")
            }
            driveBusy = false
        }
    }

    fun performDriveBackup(accessToken: String) {
        scope.launch {
            val password = container.secureSettingRepository.getString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
            if (password.isNullOrBlank()) {
                showSnackbar("Set a backup password first")
                return@launch
            }
            driveBusy = true
            val passwordChars = password.toCharArray()
            runCatching {
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
                showSnackbar("Backup complete")
            }.onFailure { error ->
                passwordChars.fill('\u0000')
                showSnackbar("Backup failed: ${error.message ?: "unknown error"}")
            }
            driveBusy = false
        }
    }

    fun restoreDriveBackup(accessToken: String) {
        val selectedBackup = pendingDriveRestore ?: return
        if (selectedBackup.payloadVersion > EverythingBackupService.PAYLOAD_VERSION) {
            restoreError = "Update the app to restore this backup."
            return
        }
        val passwordChars = restorePassword.toCharArray()
        scope.launch {
            driveBusy = true
            restoreError = null
            runCatching {
                val encryptedBackup = withContext(Dispatchers.IO) {
                    googleDriveBackupClient.downloadBackup(accessToken, selectedBackup.id)
                }
                withContext(Dispatchers.Default) {
                    container.backupService.importEncrypted(encryptedBackup, passwordChars)
                }
            }.onSuccess {
                restorePassword = ""
                activeSheet = null
                restartApp()
            }.onFailure {
                passwordChars.fill('\u0000')
                restoreError = "Incorrect password"
            }
            driveBusy = false
        }
    }

    fun handleDriveAuthorization(authorizationResult: AuthorizationResult) {
        val action = pendingDriveAction ?: BackupDriveAuthorizationAction.Refresh
        val accessToken = authorizationResult.accessToken
        pendingDriveAction = null
        if (accessToken.isNullOrBlank()) {
            showSnackbar("Google did not return an access token")
            return
        }
        when (action) {
            BackupDriveAuthorizationAction.Refresh -> refreshDriveBackups(accessToken)
            BackupDriveAuthorizationAction.BackupNow -> performDriveBackup(accessToken)
            BackupDriveAuthorizationAction.Restore -> restoreDriveBackup(accessToken)
        }
    }

    val driveAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val data = result.data
        if (data == null) {
            pendingDriveAction = null
            showSnackbar("Google Drive action canceled")
            return@rememberLauncherForActivityResult
        }
        runCatching {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        }.onSuccess(::handleDriveAuthorization)
            .onFailure { error ->
                pendingDriveAction = null
                val message = (error as? ApiException)?.statusCode?.let { "Google authorization failed ($it)" }
                    ?: "Google authorization failed: ${error.message ?: "unknown error"}"
                showSnackbar(message)
            }
    }

    fun requestDriveAuthorization(action: BackupDriveAuthorizationAction) {
        pendingDriveAction = action
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
        selectedDriveAccount?.takeIf(String::isNotBlank)?.let { email ->
            builder.setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
        }
        Identity.getAuthorizationClient(activity)
            .authorize(builder.build())
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent == null) {
                        pendingDriveAction = null
                        showSnackbar("Google authorization failed")
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
                showSnackbar("Google authorization failed: ${error.message ?: "unknown error"}")
            }
    }

    fun setSchedule(schedule: DriveBackupSchedule) {
        scope.launch {
            container.secureSettingRepository.putString(SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE, schedule.value)
            container.driveBackupScheduler.applySchedule(schedule)
            activeSheet = null
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .glassSurface(RoundedCornerShape(23.dp), selected = false),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "BACKUP SETTINGS",
                    color = SoftText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Back up app data to your Google account and restore it on a new device. Create and remember a backup password to keep your data safe.",
                    color = SoftText.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (!backupPasswordLoaded) {
                    LoadingPanel()
                } else {
                    SettingsActionRow(
                        icon = Icons.Rounded.Lock,
                        title = "Backup password",
                        subtitle = if (passwordSet) "Password set" else "Not set",
                        onClick = {
                            clearPasswordDrafts()
                            passwordSheetMode = if (passwordSet) "options" else "set"
                            activeSheet = BackupSheet.Password
                        },
                    )

                    BackupStatusCard(
                        lastBackup = lastBackupText,
                        size = lastBackupSizeText,
                        backingUp = driveBusy && pendingDriveAction == null,
                        onBackupNow = {
                            if (!passwordSet) {
                                showSnackbar("Set a backup password first")
                            } else {
                                requestDriveAuthorization(BackupDriveAuthorizationAction.BackupNow)
                            }
                        },
                        onShowAll = {
                            activeSheet = BackupSheet.BackupList
                            requestDriveAuthorization(BackupDriveAuthorizationAction.Refresh)
                        },
                    )

                    Text(
                        "GOOGLE ACCOUNT",
                        color = SoftText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.AccountCircle,
                        title = accountSubtitle,
                        subtitle = null,
                        onClick = ::requestGoogleAccountSheet,
                    )
                    SettingsActionRow(
                        icon = Icons.Rounded.AccessTime,
                        title = "Automatic backups",
                        subtitle = driveSchedule.shortLabel(),
                        onClick = { activeSheet = BackupSheet.Schedule },
                    )
                    WarningCallout()
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = {
                activeSheet = null
                restoreError = null
                clearPasswordDrafts()
            },
            sheetState = sheetState,
            containerColor = Color(0xFF202220),
            contentColor = SoftText,
        ) {
            when (sheet) {
                BackupSheet.Password -> PasswordSheet(
                    passwordSet = passwordSet,
                    mode = passwordSheetMode,
                    oldPassword = oldPasswordDraft,
                    password = passwordDraft,
                    confirmPassword = passwordConfirmDraft,
                    showRemoveWarning = showRemovePasswordWarning,
                    onModeChange = {
                        passwordSheetMode = it
                        clearPasswordDrafts()
                    },
                    onOldPasswordChange = { oldPasswordDraft = it },
                    onPasswordChange = { passwordDraft = it },
                    onConfirmPasswordChange = { passwordConfirmDraft = it },
                    onRemoveClick = { showRemovePasswordWarning = true },
                    onCancel = { activeSheet = null },
                    onSave = {
                        val saved = savedBackupPassword.orEmpty()
                        when {
                            passwordSheetMode == "change" && oldPasswordDraft != saved -> showSnackbar("Old password is incorrect")
                            passwordDraft.length < 8 -> showSnackbar("Use at least 8 characters")
                            passwordDraft != passwordConfirmDraft -> showSnackbar("Passwords do not match")
                            else -> scope.launch {
                                container.secureSettingRepository.putString(SecureSettingRepository.KEY_BACKUP_PASSWORD, passwordDraft)
                                clearPasswordDrafts()
                                activeSheet = null
                                showSnackbar("Backup password saved")
                            }
                        }
                    },
                    onConfirmRemove = {
                        scope.launch {
                            container.secureSettingRepository.delete(SecureSettingRepository.KEY_BACKUP_PASSWORD)
                            container.secureSettingRepository.putString(
                                SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE,
                                DriveBackupSchedule.Off.value,
                            )
                            container.driveBackupScheduler.applySchedule(DriveBackupSchedule.Off)
                            clearPasswordDrafts()
                            activeSheet = null
                            showSnackbar("Backup password removed")
                        }
                    },
                )
                BackupSheet.BackupList -> BackupListSheet(
                    backups = driveBackups.take(3),
                    loading = driveBusy,
                    onRestore = { backup ->
                        pendingDriveRestore = backup
                        restorePassword = ""
                        restoreError = null
                        activeSheet = BackupSheet.RestorePassword
                    },
                )
                BackupSheet.RestorePassword -> RestorePasswordSheet(
                    backup = pendingDriveRestore,
                    password = restorePassword,
                    error = restoreError,
                    restoring = driveBusy,
                    onPasswordChange = {
                        restorePassword = it
                        restoreError = null
                    },
                    onRestore = {
                        if (restorePassword.length < 8) {
                            restoreError = "Incorrect password"
                        } else {
                            requestDriveAuthorization(BackupDriveAuthorizationAction.Restore)
                        }
                    },
                )
                BackupSheet.GoogleAccount -> GoogleAccountSheet(
                    accounts = googleAccounts,
                    selected = accountDraft,
                    onSelect = { accountDraft = it },
                    onCancel = { activeSheet = null },
                    onConfirm = {
                        val account = accountDraft
                        if (account.isNullOrBlank()) {
                            showSnackbar("No Google account selected")
                        } else {
                            scope.launch {
                                container.secureSettingRepository.putString(
                                    SecureSettingRepository.KEY_DRIVE_ACCOUNT_EMAIL,
                                    account,
                                )
                                activeSheet = null
                                showSnackbar("Backup account updated")
                            }
                        }
                    },
                )
                BackupSheet.Schedule -> ScheduleSheet(
                    selected = driveSchedule,
                    onSelect = ::setSchedule,
                )
            }
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp))
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false, tintStrength = 0.08f)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = MutedText, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BackupStatusCard(
    lastBackup: String,
    size: String,
    backingUp: Boolean,
    onBackupNow: () -> Unit,
    onShowAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false, tintStrength = 0.08f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Last backup  $lastBackup", color = SoftText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Size  $size", color = SoftText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackupButton(
                text = if (backingUp) "Backing up..." else "Back up now",
                modifier = Modifier.weight(1f),
                enabled = !backingUp,
                icon = Icons.Rounded.CloudUpload,
                onClick = onBackupNow,
            )
            BackupButton(
                text = "Show all",
                modifier = Modifier.weight(1f),
                enabled = !backingUp,
                icon = null,
                onClick = onShowAll,
            )
        }
    }
}

@Composable
private fun WarningCallout() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false, tintStrength = 0.05f)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFFFD28F), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            "All backups are fully encrypted. If you forget your backup password, there is no way to restore your data - keep it safe.",
            color = SoftText.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .glassSurface(RoundedCornerShape(12.dp), selected = false, tintStrength = 0.04f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = SoftText, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BackupButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            contentColor = SoftText,
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Cyan)
    }
}

@Composable
private fun PasswordSheet(
    passwordSet: Boolean,
    mode: String,
    oldPassword: String,
    password: String,
    confirmPassword: String,
    showRemoveWarning: Boolean,
    onModeChange: (String) -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onConfirmRemove: () -> Unit,
) {
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Backup password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!passwordSet || mode == "set") {
            BackupPasswordField(password, onPasswordChange, "Password", isError = mismatch)
            BackupPasswordField(confirmPassword, onConfirmPasswordChange, "Confirm password", isError = mismatch)
            if (mismatch) Text("Passwords do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
            BackupButton("Set password", Modifier.fillMaxWidth(), password.length >= 8 && password == confirmPassword, Icons.Rounded.Lock, onSave)
        } else if (mode == "change") {
            BackupPasswordField(oldPassword, onOldPasswordChange, "Old password")
            BackupPasswordField(password, onPasswordChange, "New password", isError = mismatch)
            BackupPasswordField(confirmPassword, onConfirmPasswordChange, "Confirm new password", isError = mismatch)
            if (mismatch) Text("Passwords do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
            BackupButton("Save password", Modifier.fillMaxWidth(), password.length >= 8 && password == confirmPassword, Icons.Rounded.Lock, onSave)
        } else {
            BackupButton("Change password", Modifier.fillMaxWidth(), true, Icons.Rounded.Lock) { onModeChange("change") }
            BackupButton("Remove password", Modifier.fillMaxWidth(), true, Icons.Rounded.WarningAmber, onRemoveClick)
            BackupButton("Cancel", Modifier.fillMaxWidth(), true, null, onCancel)
        }
        if (showRemoveWarning) {
            Text(
                "Removing the password disables all backups. Existing encrypted backups still require the old password.",
                color = Color(0xFFFFD28F),
                style = MaterialTheme.typography.bodyMedium,
            )
            BackupButton("Confirm remove password", Modifier.fillMaxWidth(), true, Icons.Rounded.WarningAmber, onConfirmRemove)
        }
    }
}

@Composable
private fun BackupListSheet(
    backups: List<DriveBackupFile>,
    loading: Boolean,
    onRestore: (DriveBackupFile) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recent backups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (loading) {
            LoadingPanel()
        } else if (backups.isEmpty()) {
            Text("No backups found", color = MutedText)
        } else {
            backups.forEach { backup ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(backup.createdAtDisplay, fontWeight = FontWeight.SemiBold)
                        Text(backup.sizeBytes.displaySize(), color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onRestore(backup) }) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore", color = Cyan)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }
    }
}

@Composable
private fun RestorePasswordSheet(
    backup: DriveBackupFile?,
    password: String,
    error: String?,
    restoring: Boolean,
    onPasswordChange: (String) -> Unit,
    onRestore: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Restore backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(backup?.createdAtDisplay.orEmpty(), color = MutedText)
        BackupPasswordField(password, onPasswordChange, "Backup password", isError = error != null)
        error?.let { Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall) }
        BackupButton(
            text = if (restoring) "Restoring..." else "Restore",
            modifier = Modifier.fillMaxWidth(),
            enabled = !restoring && password.length >= 8,
            icon = Icons.Rounded.Restore,
            onClick = onRestore,
        )
    }
}

@Composable
private fun GoogleAccountSheet(
    accounts: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Google account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (accounts.isEmpty()) {
            Text("No Google accounts found on this device.", color = MutedText)
        } else {
            accounts.forEach { account ->
                RadioRow(account, selected == account) { onSelect(account) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BackupButton("Cancel", Modifier.weight(1f), true, null, onCancel)
            BackupButton("Confirm", Modifier.weight(1f), !selected.isNullOrBlank(), null, onConfirm)
        }
    }
}

@Composable
private fun ScheduleSheet(
    selected: DriveBackupSchedule,
    onSelect: (DriveBackupSchedule) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Automatic backups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf(
            DriveBackupSchedule.Daily,
            DriveBackupSchedule.Weekly,
            DriveBackupSchedule.Manual,
            DriveBackupSchedule.Off,
        ).forEach { schedule ->
            RadioRow(schedule.shortLabel(), selected == schedule) { onSelect(schedule) }
        }
    }
}

@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(10.dp))
        Text(text, color = SoftText, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BackupPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
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
            errorBorderColor = Color(0xFFFFA8A8),
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

private fun DriveBackupSchedule.shortLabel(): String = when (this) {
    DriveBackupSchedule.Daily -> "Daily"
    DriveBackupSchedule.Weekly -> "Weekly"
    DriveBackupSchedule.Manual -> "Only when I tap 'Back up now'"
    DriveBackupSchedule.Off -> "Off"
}

private fun Long?.displaySize(): String {
    val bytes = this ?: return "-"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${kb.formatOne()} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${mb.formatOne()} MB"
    return "${(mb / 1024.0).formatOne()} GB"
}

private fun Double.formatOne(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
