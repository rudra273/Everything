package com.everything.app

import android.content.Context
import com.everything.app.core.data.DatabasePassphraseProvider
import com.everything.app.core.data.EverythingDatabase
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.security.AndroidKeyStoreCrypto
import com.everything.app.core.security.CredentialRepository
import com.everything.app.core.security.PasswordHasher
import com.everything.app.core.security.SensitiveValueCipher
import com.everything.app.feature.applock.data.AppLockRepository
import com.everything.app.feature.applock.domain.InstalledAppProvider

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val keyStoreCrypto = AndroidKeyStoreCrypto()
    val sensitiveValueCipher = SensitiveValueCipher(keyStoreCrypto)
    val credentialRepository = CredentialRepository(appContext, PasswordHasher())

    val database: EverythingDatabase by lazy {
        val passphraseProvider = DatabasePassphraseProvider(appContext, keyStoreCrypto)
        EverythingDatabase.create(appContext, passphraseProvider)
    }

    val secureSettingRepository: SecureSettingRepository by lazy {
        SecureSettingRepository(database.secureSettingDao(), sensitiveValueCipher)
    }

    val appLockRepository: AppLockRepository by lazy {
        AppLockRepository(database.lockedAppDao(), sensitiveValueCipher)
    }

    val installedAppProvider = InstalledAppProvider(appContext)
}
