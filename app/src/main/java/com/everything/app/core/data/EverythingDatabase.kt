package com.everything.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.everything.app.feature.applock.data.LockedAppDao
import com.everything.app.feature.applock.data.LockedAppEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        LockedAppEntity::class,
        SecureSettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EverythingDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun secureSettingDao(): SecureSettingDao

    companion object {
        fun create(
            context: Context,
            passphraseProvider: DatabasePassphraseProvider,
        ): EverythingDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphraseProvider.getOrCreatePassphrase())
            return Room.databaseBuilder(
                context.applicationContext,
                EverythingDatabase::class.java,
                "everything_secure.db",
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}
