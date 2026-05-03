package com.everything.app.feature.expense.data

data class ExpenseEntry(
    val entryId: String,
    val monthKey: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val kind: ExpenseEntryKind,
    val sourceBillId: String?,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class ExpenseEntryKind {
    Daily,
    MonthlyBill,
}

data class MonthlyBill(
    val billId: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val active: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class ExpenseMonthSummary(
    val monthKey: String,
    val limitMinor: Long,
    val entries: List<ExpenseEntry>,
    val monthlyBills: List<MonthlyBill>,
) {
    val totalMinor: Long = entries.sumOf { it.amountMinor }
    val dailyTotalMinor: Long = entries.filter { it.kind == ExpenseEntryKind.Daily }.sumOf { it.amountMinor }
    val billTotalMinor: Long = entries.filter { it.kind == ExpenseEntryKind.MonthlyBill }.sumOf { it.amountMinor }
    val remainingMinor: Long = limitMinor - totalMinor
    val limitProgress: Float = if (limitMinor <= 0L) 0f else (totalMinor.toFloat() / limitMinor).coerceAtMost(1.5f)
}
