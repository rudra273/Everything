package com.everything.app.core.data

import com.everything.app.core.security.CipherPayload
import com.everything.app.core.security.SensitiveValueCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecureSettingRepository(
    private val dao: SecureSettingDao,
    private val cipher: SensitiveValueCipher,
) {
    suspend fun getBoolean(key: String): Boolean? {
        return dao.get(key)?.decrypt()?.toBooleanStrictOrNull()
    }

    fun observeBoolean(key: String): Flow<Boolean?> {
        return dao.observe(key).map { it?.decrypt()?.toBooleanStrictOrNull() }
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        val payload = cipher.encryptString(value.toString(), aad = key)
        dao.upsert(
            SecureSettingEntity(
                key = key,
                valueCiphertext = payload.ciphertext,
                valueIv = payload.iv,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun SecureSettingEntity.decrypt(): String {
        return cipher.decryptString(
            payload = CipherPayload(valueCiphertext, valueIv),
            aad = key,
        )
    }

    companion object {
        const val KEY_BIOMETRIC_ENABLED = "app_lock.biometric_enabled"
    }
}
