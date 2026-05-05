package com.rudra.everything.core.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.rudra.everything.AppContainer
import com.rudra.everything.core.data.SecureSettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = AppContainer(applicationContext)
        val settings = container.secureSettingRepository
        val schedule = DriveBackupSchedule.fromValue(settings.getString(SecureSettingRepository.KEY_DRIVE_BACKUP_SCHEDULE))
        if (schedule == DriveBackupSchedule.Off) {
            return@withContext Result.success()
        }

        val password = settings.getString(SecureSettingRepository.KEY_BACKUP_PASSWORD)
        if (password.isNullOrBlank()) {
            settings.putString(SecureSettingRepository.KEY_DRIVE_LAST_ERROR, "Set a backup password")
            return@withContext Result.success()
        }

        val authorizationResult = runCatching {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            Tasks.await(Identity.getAuthorizationClient(applicationContext).authorize(request))
        }.getOrElse { error ->
            settings.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, true)
            settings.putString(
                SecureSettingRepository.KEY_DRIVE_LAST_ERROR,
                "Reconnect Google Drive: ${error.message ?: "authorization failed"}",
            )
            return@withContext Result.success()
        }

        val accessToken = authorizationResult.accessToken
        if (authorizationResult.hasResolution() || accessToken.isNullOrBlank()) {
            settings.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, true)
            settings.putString(SecureSettingRepository.KEY_DRIVE_LAST_ERROR, "Reconnect Google Drive")
            return@withContext Result.success()
        }

        val passwordChars = password.toCharArray()
        runCatching {
            val encryptedBackup = container.backupService.exportEncrypted(passwordChars)
            GoogleDriveBackupClient().uploadBackup(
                accessToken = accessToken,
                encryptedBackup = encryptedBackup,
                source = DriveBackupSource.Automatic,
            )
        }.onSuccess { upload ->
            settings.putBoolean(SecureSettingRepository.KEY_DRIVE_NEEDS_AUTHORIZATION, false)
            settings.putString(
                SecureSettingRepository.KEY_DRIVE_LAST_BACKUP_AT,
                upload.file.createdAtMillis.toString(),
            )
            settings.delete(SecureSettingRepository.KEY_DRIVE_LAST_ERROR)
        }.onFailure { error ->
            passwordChars.fill('\u0000')
            settings.putString(
                SecureSettingRepository.KEY_DRIVE_LAST_ERROR,
                error.message ?: "Automatic backup failed",
            )
        }

        Result.success()
    }

    private companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
