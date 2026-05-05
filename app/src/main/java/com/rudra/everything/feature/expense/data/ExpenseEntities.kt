package com.rudra.everything.feature.expense.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_entries",
    indices = [
        Index(value = ["entryId"], unique = true),
        Index(value = ["monthKey"]),
        Index(value = ["monthKey", "sourceBillId"], unique = true),
    ],
)
data class ExpenseEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: String,
    val monthKey: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val kind: String,
    val sourceBillId: String?,
    val expenseDate: String,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "monthly_bills",
    indices = [Index(value = ["billId"], unique = true)],
)
data class MonthlyBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: String,
    val title: String,
    val category: String,
    val amountMinor: Long,
    val active: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "expense_months")
data class ExpenseMonthEntity(
    @PrimaryKey
    val monthKey: String,
    val limitMinor: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
