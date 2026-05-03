package com.everything.app

import android.content.Context
import com.everything.app.core.backup.BackupCrypto
import com.everything.app.core.backup.EverythingBackupService
import com.everything.app.core.data.DatabasePassphraseProvider
import com.everything.app.core.data.EverythingDatabase
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.security.AndroidKeyStoreCrypto
import com.everything.app.core.security.CredentialRepository
import com.everything.app.core.security.PasswordHasher
import com.everything.app.core.security.SensitiveValueCipher
import com.everything.app.feature.applock.data.AppLockBackupContributor
import com.everything.app.feature.applock.data.AppLockRepository
import com.everything.app.feature.applock.domain.InstalledAppProvider
import com.everything.app.feature.expense.data.ExpenseBackupContributor
import com.everything.app.feature.expense.data.ExpenseRepository
import com.everything.app.feature.keystore.data.KeyStoreBackupContributor
import com.everything.app.feature.keystore.data.KeyStoreRepository
import com.everything.app.feature.notes.data.SecureNoteBackupContributor
import com.everything.app.feature.notes.data.SecureNoteRepository

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

    val keyStoreRepository: KeyStoreRepository by lazy {
        KeyStoreRepository(database.keyStoreEntryDao(), sensitiveValueCipher)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao())
    }

    val secureNoteRepository: SecureNoteRepository by lazy {
        SecureNoteRepository(database.secureNoteDao(), sensitiveValueCipher)
    }

    val backupService: EverythingBackupService by lazy {
        EverythingBackupService(
            crypto = BackupCrypto(PasswordHasher()),
            contributors = listOf(
                AppLockBackupContributor(appLockRepository),
                KeyStoreBackupContributor(keyStoreRepository),
                ExpenseBackupContributor(expenseRepository),
                SecureNoteBackupContributor(secureNoteRepository),
            ),
        )
    }

    val installedAppProvider = InstalledAppProvider(appContext)
}
