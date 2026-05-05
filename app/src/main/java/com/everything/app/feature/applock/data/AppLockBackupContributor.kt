package com.everything.app.feature.applock.data

import com.everything.app.core.backup.BackupContributor
import org.json.JSONArray
import org.json.JSONObject

class AppLockBackupContributor(
    private val repository: AppLockRepository,
) : BackupContributor {
    override val toolKey: String = "app_lock"
    override val schemaVersion: Int = 1

    override suspend fun exportJson(): JSONObject {
        val records = JSONArray()
        repository.exportRecords().forEach { record ->
            records.put(
                JSONObject()
                    .put("packageName", record.packageName)
                    .put("label", record.label)
                    .put("enabled", record.enabled)
                    .put("updatedAtMillis", record.updatedAtMillis),
            )
        }
        return JSONObject().put("records", records)
    }

    override suspend fun importJson(payload: JSONObject) {
        val records = payload.getJSONArray("records")
        val parsed = buildList {
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                add(
                    AppLockBackupRecord(
                        packageName = record.getString("packageName"),
                        label = record.getString("label"),
                        enabled = record.getBoolean("enabled"),
                        updatedAtMillis = record.getLong("updatedAtMillis"),
                    ),
                )
            }
        }
        repository.importRecords(parsed)
    }
}
