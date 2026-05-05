package com.rudra.everything.core.backup

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object BackupFileNames {
    private val driveBackupRegex = Regex("""everything-backup-v(\d+)-(\d{8})T(\d{6})Z\.everything""")
    private val legacyBackupRegex = Regex("""everything-v(\d+)-(\d{8})-(\d{6})\.everything""")
    private val driveFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
        .withZone(ZoneOffset.UTC)
    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.of("Asia/Kolkata"))

    fun backupName(
        payloadVersion: Int = EverythingBackupService.PAYLOAD_VERSION,
        createdAt: Instant = Instant.now(),
    ): String {
        return "everything-backup-v$payloadVersion-${driveFormatter.format(createdAt)}.everything"
    }

    fun parse(name: String): BackupFileNameInfo? {
        driveBackupRegex.matchEntire(name)?.let { match ->
            val instant = parseUtc(match.groupValues[2], match.groupValues[3])
            return BackupFileNameInfo(
                version = match.groupValues[1].toInt(),
                exportedAtMillis = instant.toEpochMilli(),
                exportedAt = displayFormatter.format(instant),
            )
        }

        legacyBackupRegex.matchEntire(name)?.let { match ->
            val instant = parseUtc(match.groupValues[2], match.groupValues[3])
            return BackupFileNameInfo(
                version = match.groupValues[1].toInt(),
                exportedAtMillis = instant.toEpochMilli(),
                exportedAt = displayFormatter.format(instant),
            )
        }

        return null
    }

    fun displayDate(millis: Long): String {
        return displayFormatter.format(Instant.ofEpochMilli(millis))
    }

    private fun parseUtc(date: String, time: String): Instant {
        val text = "${date}T${time}Z"
        return Instant.from(driveFormatter.parse(text))
    }
}

data class BackupFileNameInfo(
    val version: Int,
    val exportedAtMillis: Long,
    val exportedAt: String,
)
