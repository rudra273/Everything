package com.rudra.everything.feature.applock.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AppLockRepository(
    private val lockedPackageCache: LockedPackageCache,
) {
    private val lockedRecords = MutableStateFlow(lockedPackageCache.getRecords().orEmpty())

    fun observeLockedApps(): Flow<List<LockedApp>> {
        return lockedRecords.map { records -> records.toLockedApps() }
    }

    fun observeAllSelections(): Flow<List<LockedApp>> {
        return observeLockedApps()
    }

    suspend fun getLockedApps(): List<LockedApp> {
        return lockedRecords.value.toLockedApps()
    }

    fun getLockedPackages(): Set<String> {
        return lockedPackageCache.getPackages().orEmpty()
    }

    suspend fun setLocked(packageName: String, label: String, locked: Boolean) {
        lockedRecords.value = lockedPackageCache.updatePackage(
            packageName = packageName,
            label = label,
            locked = locked,
        )
    }

    suspend fun exportRecords(): List<AppLockBackupRecord> {
        return lockedRecords.value.map { record ->
            AppLockBackupRecord(
                packageName = record.packageName,
                label = record.label,
                enabled = true,
                updatedAtMillis = record.updatedAtMillis,
            )
        }
    }

    suspend fun importRecords(records: List<AppLockBackupRecord>) {
        lockedPackageCache.putRecords(
            records
                .filter { it.enabled }
                .map { record ->
                    LockedPackageRecord(
                        packageName = record.packageName,
                        label = record.label,
                        updatedAtMillis = record.updatedAtMillis,
                    )
                },
        )
        lockedRecords.value = lockedPackageCache.getRecords().orEmpty()
    }

    private fun List<LockedPackageRecord>.toLockedApps(): List<LockedApp> {
        return sortedBy { it.label.lowercase() }
            .mapIndexed { index, record ->
                LockedApp(
                    id = index.toLong() + 1L,
                    packageName = record.packageName,
                    label = record.label,
                    enabled = true,
                )
            }
    }
}

data class AppLockBackupRecord(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val updatedAtMillis: Long,
)
