package com.rudra.everything

import android.content.Context
import com.rudra.everything.core.backup.BackupCrypto
import com.rudra.everything.core.backup.EverythingBackupService
import com.rudra.everything.core.data.DatabasePassphraseProvider
import com.rudra.everything.core.data.EverythingDatabase
import com.rudra.everything.core.data.SecureSettingRepository
import com.rudra.everything.core.security.AndroidKeyStoreCrypto
import com.rudra.everything.core.security.CredentialRepository
import com.rudra.everything.core.security.PasswordHasher
import com.rudra.everything.core.security.SensitiveValueCipher
import com.rudra.everything.feature.applock.data.AppLockBackupContributor
import com.rudra.everything.feature.applock.data.AppLockRepository
import com.rudra.everything.feature.applock.domain.InstalledAppProvider
import com.rudra.everything.feature.expense.data.ExpenseBackupContributor
import com.rudra.everything.feature.expense.data.ExpenseRepository
import com.rudra.everything.feature.keystore.data.KeyStoreBackupContributor
import com.rudra.everything.feature.keystore.data.KeyStoreRepository
import com.rudra.everything.feature.notes.data.SecureNoteBackupContributor
import com.rudra.everything.feature.notes.data.SecureNoteRepository

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
