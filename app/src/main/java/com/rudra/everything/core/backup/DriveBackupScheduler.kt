package com.rudra.everything.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DriveBackupScheduler(
    private val context: Context,
) {
    fun applySchedule(schedule: DriveBackupSchedule) {
        val workManager = WorkManager.getInstance(context)
        if (schedule == DriveBackupSchedule.Off) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val days = when (schedule) {
            DriveBackupSchedule.Daily -> 1L
            DriveBackupSchedule.Weekly -> 7L
            DriveBackupSchedule.Off -> return
        }
        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "everything_drive_backup"
    }
}
