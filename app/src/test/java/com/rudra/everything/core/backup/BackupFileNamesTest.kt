package com.rudra.everything.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BackupFileNamesTest {
    @Test
    fun backupName_usesStableUtcFormat() {
        val name = BackupFileNames.backupName(
            payloadVersion = 1,
            createdAt = Instant.parse("2026-05-05T14:30:12Z"),
        )

        assertEquals("everything-backup-v1-20260505T143012Z.everything", name)
    }

    @Test
    fun parse_readsStableUtcBackupNames() {
        val parsed = BackupFileNames.parse("everything-backup-v2-20260505T143012Z.everything")

        assertEquals(2, parsed?.version)
        assertEquals(Instant.parse("2026-05-05T14:30:12Z").toEpochMilli(), parsed?.exportedAtMillis)
        assertEquals("2026-05-05 20:00", parsed?.exportedAt)
    }

    @Test
    fun parse_keepsLegacyLocalBackupNamesRestorable() {
        val parsed = BackupFileNames.parse("everything-v1-20260505-143012.everything")

        assertEquals(1, parsed?.version)
        assertEquals(Instant.parse("2026-05-05T14:30:12Z").toEpochMilli(), parsed?.exportedAtMillis)
    }

    @Test
    fun parse_ignoresUnknownNames() {
        assertNull(BackupFileNames.parse("notes.json"))
    }
}
