package com.rudra.everything.feature.applock.data

import com.rudra.everything.core.security.CipherPayload
import com.rudra.everything.core.security.SensitiveValueCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLockRepository(
    private val dao: LockedAppDao,
    private val cipher: SensitiveValueCipher,
) {
    fun observeLockedApps(): Flow<List<LockedApp>> {
        return dao.observeEnabled().map { entities ->
            entities.mapNotNull { entity -> entity.toDomainOrNull() }
                .sortedBy { it.label.lowercase() }
        }
    }

    fun observeAllSelections(): Flow<List<LockedApp>> {
        return dao.observeAll().map { entities ->
            entities.mapNotNull { entity -> entity.toDomainOrNull() }
        }
    }

    suspend fun getLockedApps(): List<LockedApp> {
        return dao.getAll()
            .filter { it.enabled }
            .mapNotNull { it.toDomainOrNull() }
    }

    suspend fun setLocked(packageName: String, label: String, locked: Boolean) {
        val existing = dao.getAll()
            .mapNotNull { entity -> entity.toDomainOrNull()?.let { it to entity } }
            .firstOrNull { (app, _) -> app.packageName == packageName }

        if (!locked) {
            existing?.second?.id?.let { dao.deleteById(it) }
            return
        }

        val packagePayload = cipher.encryptString(packageName, aad = AAD_PACKAGE)
        val labelPayload = cipher.encryptString(label, aad = AAD_LABEL)
        dao.upsert(
            LockedAppEntity(
                id = existing?.second?.id ?: 0,
                packageNameCiphertext = packagePayload.ciphertext,
                packageNameIv = packagePayload.iv,
                labelCiphertext = labelPayload.ciphertext,
                labelIv = labelPayload.iv,
                enabled = true,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun exportRecords(): List<AppLockBackupRecord> {
        return dao.getAll().mapNotNull { entity ->
            entity.toDomainOrNull()?.let { app ->
                AppLockBackupRecord(
                    packageName = app.packageName,
                    label = app.label,
                    enabled = app.enabled,
                    updatedAtMillis = entity.updatedAtMillis,
                )
            }
        }
    }

    suspend fun importRecords(records: List<AppLockBackupRecord>) {
        records.forEach { record ->
            setLocked(
                packageName = record.packageName,
                label = record.label,
                locked = record.enabled,
            )
        }
    }

    private fun LockedAppEntity.toDomainOrNull(): LockedApp? {
        return runCatching {
            LockedApp(
                id = id,
                packageName = cipher.decryptString(
                    payload = CipherPayload(packageNameCiphertext, packageNameIv),
                    aad = AAD_PACKAGE,
                ),
                label = cipher.decryptString(
                    payload = CipherPayload(labelCiphertext, labelIv),
                    aad = AAD_LABEL,
                ),
                enabled = enabled,
            )
        }.getOrNull()
    }

    private companion object {
        const val AAD_PACKAGE = "app_lock.package_name"
        const val AAD_LABEL = "app_lock.label"
    }
}

data class AppLockBackupRecord(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val updatedAtMillis: Long,
)
