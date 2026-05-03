package com.everything.app.feature.expense.data

import com.everything.app.core.backup.BackupContributor
import org.json.JSONArray
import org.json.JSONObject

class ExpenseBackupContributor(
    private val repository: ExpenseRepository,
) : BackupContributor {
    override val toolKey: String = "expenses"
    override val schemaVersion: Int = 1

    override suspend fun exportJson(): JSONObject {
        val record = repository.exportRecords()
        return JSONObject()
            .put("entries", JSONArray().also { rows ->
                record.entries.forEach { entry ->
                    rows.put(
                        JSONObject()
                            .put("entryId", entry.entryId)
                            .put("monthKey", entry.monthKey)
                            .put("title", entry.title)
                            .put("category", entry.category)
                            .put("amountMinor", entry.amountMinor)
                            .put("kind", entry.kind.name)
                            .put("sourceBillId", entry.sourceBillId)
                            .put("note", entry.note)
                            .put("createdAtMillis", entry.createdAtMillis)
                            .put("updatedAtMillis", entry.updatedAtMillis),
                    )
                }
            })
            .put("bills", JSONArray().also { rows ->
                record.bills.forEach { bill ->
                    rows.put(
                        JSONObject()
                            .put("billId", bill.billId)
                            .put("title", bill.title)
                            .put("category", bill.category)
                            .put("amountMinor", bill.amountMinor)
                            .put("active", bill.active)
                            .put("createdAtMillis", bill.createdAtMillis)
                            .put("updatedAtMillis", bill.updatedAtMillis),
                    )
                }
            })
            .put("months", JSONArray().also { rows ->
                record.months.forEach { month ->
                    rows.put(
                        JSONObject()
                            .put("monthKey", month.monthKey)
                            .put("limitMinor", month.limitMinor)
                            .put("createdAtMillis", month.createdAtMillis)
                            .put("updatedAtMillis", month.updatedAtMillis),
                    )
                }
            })
    }

    override suspend fun importJson(payload: JSONObject) {
        val entries = payload.getJSONArray("entries")
        val bills = payload.getJSONArray("bills")
        val months = payload.getJSONArray("months")
        repository.importRecords(
            ExpenseBackupRecord(
                entries = buildList {
                    for (index in 0 until entries.length()) {
                        val entry = entries.getJSONObject(index)
                        add(
                            ExpenseEntry(
                                entryId = entry.getString("entryId"),
                                monthKey = entry.getString("monthKey"),
                                title = entry.getString("title"),
                                category = entry.getString("category"),
                                amountMinor = entry.getLong("amountMinor"),
                                kind = ExpenseEntryKind.valueOf(entry.getString("kind")),
                                sourceBillId = if (entry.isNull("sourceBillId")) {
                                    null
                                } else {
                                    entry.optString("sourceBillId").takeIf { it.isNotBlank() }
                                },
                                note = entry.getString("note"),
                                createdAtMillis = entry.getLong("createdAtMillis"),
                                updatedAtMillis = entry.getLong("updatedAtMillis"),
                            ),
                        )
                    }
                },
                bills = buildList {
                    for (index in 0 until bills.length()) {
                        val bill = bills.getJSONObject(index)
                        add(
                            MonthlyBill(
                                billId = bill.getString("billId"),
                                title = bill.getString("title"),
                                category = bill.getString("category"),
                                amountMinor = bill.getLong("amountMinor"),
                                active = bill.getBoolean("active"),
                                createdAtMillis = bill.getLong("createdAtMillis"),
                                updatedAtMillis = bill.getLong("updatedAtMillis"),
                            ),
                        )
                    }
                },
                months = buildList {
                    for (index in 0 until months.length()) {
                        val month = months.getJSONObject(index)
                        add(
                            ExpenseMonthEntity(
                                monthKey = month.getString("monthKey"),
                                limitMinor = month.getLong("limitMinor"),
                                createdAtMillis = month.getLong("createdAtMillis"),
                                updatedAtMillis = month.getLong("updatedAtMillis"),
                            ),
                        )
                    }
                },
            ),
        )
    }
}
