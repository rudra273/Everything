package com.everything.app.feature.expense.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class ExpenseRepository(
    private val dao: ExpenseDao,
) {
    fun observeMonth(monthKey: String): Flow<ExpenseMonthSummary> {
        return combine(
            dao.observeEntriesForMonth(monthKey),
            dao.observeActiveBills(),
            dao.observeMonth(monthKey),
        ) { entries, bills, month ->
            ExpenseMonthSummary(
                monthKey = monthKey,
                limitMinor = month?.limitMinor ?: 0L,
                entries = entries.map { it.toDomain() },
                monthlyBills = bills.map { it.toDomain() },
            )
        }
    }

    suspend fun ensureMonth(monthKey: String) {
        val now = System.currentTimeMillis()
        if (dao.getMonth(monthKey) == null) {
            dao.upsertMonth(
                ExpenseMonthEntity(
                    monthKey = monthKey,
                    limitMinor = 0L,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
        }
        dao.getActiveBills().forEach { bill ->
            if (dao.getBillOccurrence(monthKey, bill.billId) == null) {
                dao.upsertEntry(bill.toOccurrence(monthKey, now))
            }
        }
    }

    suspend fun setMonthlyLimit(monthKey: String, amountMinor: Long) {
        val now = System.currentTimeMillis()
        val existing = dao.getMonth(monthKey)
        dao.upsertMonth(
            ExpenseMonthEntity(
                monthKey = monthKey,
                limitMinor = amountMinor.coerceAtLeast(0L),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
    }

    suspend fun addDailyExpense(monthKey: String, title: String, category: String, amountMinor: Long, note: String) {
        require(title.trim().isNotBlank()) { "Expense title cannot be empty" }
        require(amountMinor > 0L) { "Amount must be greater than zero" }
        ensureMonth(monthKey)
        val now = System.currentTimeMillis()
        dao.upsertEntry(
            ExpenseEntryEntity(
                entryId = UUID.randomUUID().toString(),
                monthKey = monthKey,
                title = title.trim(),
                category = category.trim().ifBlank { "General" },
                amountMinor = amountMinor,
                kind = KIND_DAILY,
                sourceBillId = null,
                note = note.trim(),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    suspend fun addMonthlyBill(currentMonthKey: String, title: String, category: String, amountMinor: Long) {
        require(title.trim().isNotBlank()) { "Bill title cannot be empty" }
        require(amountMinor > 0L) { "Amount must be greater than zero" }
        val now = System.currentTimeMillis()
        val bill = MonthlyBillEntity(
            billId = UUID.randomUUID().toString(),
            title = title.trim(),
            category = category.trim().ifBlank { "Bills" },
            amountMinor = amountMinor,
            active = true,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        dao.upsertBill(bill)
        ensureMonth(currentMonthKey)
    }

    suspend fun deleteEntry(entryId: String) {
        dao.deleteEntry(entryId)
    }

    suspend fun updateEntry(entryId: String, title: String, category: String, amountMinor: Long, note: String) {
        require(title.trim().isNotBlank()) { "Expense title cannot be empty" }
        require(amountMinor > 0L) { "Amount must be greater than zero" }
        val existing = dao.getEntry(entryId) ?: return
        dao.upsertEntry(
            existing.copy(
                title = title.trim(),
                category = category.trim().ifBlank { "General" },
                amountMinor = amountMinor,
                note = note.trim(),
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun stopMonthlyBill(billId: String) {
        val bill = dao.getBill(billId) ?: return
        dao.upsertBill(bill.copy(active = false, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun exportRecords(): ExpenseBackupRecord {
        return ExpenseBackupRecord(
            entries = dao.getAllEntries().map { it.toDomain() },
            bills = dao.getAllBills().map { it.toDomain() },
            months = dao.getAllMonths(),
        )
    }

    suspend fun importRecords(record: ExpenseBackupRecord) {
        record.months.forEach { dao.upsertMonth(it) }
        record.bills.forEach { bill ->
            dao.upsertBill(
                MonthlyBillEntity(
                    billId = bill.billId,
                    title = bill.title,
                    category = bill.category,
                    amountMinor = bill.amountMinor,
                    active = bill.active,
                    createdAtMillis = bill.createdAtMillis,
                    updatedAtMillis = bill.updatedAtMillis,
                ),
            )
        }
        record.entries.forEach { entry ->
            dao.upsertEntry(entry.toEntity())
        }
    }

    private fun MonthlyBillEntity.toOccurrence(monthKey: String, now: Long): ExpenseEntryEntity {
        return ExpenseEntryEntity(
            entryId = UUID.randomUUID().toString(),
            monthKey = monthKey,
            title = title,
            category = category,
            amountMinor = amountMinor,
            kind = KIND_MONTHLY_BILL,
            sourceBillId = billId,
            note = "Static monthly bill",
            createdAtMillis = now,
            updatedAtMillis = now,
        )
    }

    private fun ExpenseEntryEntity.toDomain(): ExpenseEntry {
        return ExpenseEntry(
            entryId = entryId,
            monthKey = monthKey,
            title = title,
            category = category,
            amountMinor = amountMinor,
            kind = if (kind == KIND_MONTHLY_BILL) ExpenseEntryKind.MonthlyBill else ExpenseEntryKind.Daily,
            sourceBillId = sourceBillId,
            note = note,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun MonthlyBillEntity.toDomain(): MonthlyBill {
        return MonthlyBill(
            billId = billId,
            title = title,
            category = category,
            amountMinor = amountMinor,
            active = active,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun ExpenseEntry.toEntity(): ExpenseEntryEntity {
        return ExpenseEntryEntity(
            entryId = entryId,
            monthKey = monthKey,
            title = title,
            category = category,
            amountMinor = amountMinor,
            kind = if (kind == ExpenseEntryKind.MonthlyBill) KIND_MONTHLY_BILL else KIND_DAILY,
            sourceBillId = sourceBillId,
            note = note,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private companion object {
        const val KIND_DAILY = "daily"
        const val KIND_MONTHLY_BILL = "monthly_bill"
    }
}

data class ExpenseBackupRecord(
    val entries: List<ExpenseEntry>,
    val bills: List<MonthlyBill>,
    val months: List<ExpenseMonthEntity>,
)
