package com.everything.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.everything.app.feature.applock.data.LockedAppDao
import com.everything.app.feature.applock.data.LockedAppEntity
import com.everything.app.feature.keystore.data.KeyStoreEntryDao
import com.everything.app.feature.keystore.data.KeyStoreEntryEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        LockedAppEntity::class,
        KeyStoreEntryEntity::class,
        SecureSettingEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class EverythingDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun keyStoreEntryDao(): KeyStoreEntryDao
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `key_store_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `nameCiphertext` BLOB NOT NULL,
                        `nameIv` BLOB NOT NULL,
                        `labelCiphertext` BLOB NOT NULL,
                        `labelIv` BLOB NOT NULL,
                        `valueCiphertext` BLOB NOT NULL,
                        `valueIv` BLOB NOT NULL,
                        `version` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_key_store_entries_entryId` ON `key_store_entries` (`entryId`)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `key_store_entries`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `key_store_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryId` TEXT NOT NULL,
                        `nameCiphertext` BLOB NOT NULL,
                        `nameIv` BLOB NOT NULL,
                        `labelCiphertext` BLOB NOT NULL,
                        `labelIv` BLOB NOT NULL,
                        `valueCiphertext` BLOB NOT NULL,
                        `valueIv` BLOB NOT NULL,
                        `version` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_key_store_entries_entryId` ON `key_store_entries` (`entryId`)",
                )
            }
        }
    }
}
