package com.everything.app.feature.expense.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_entries WHERE monthKey = :monthKey ORDER BY expenseDate DESC, createdAtMillis DESC")
    fun observeEntriesForMonth(monthKey: String): Flow<List<ExpenseEntryEntity>>

    @Query("SELECT * FROM monthly_bills WHERE active = 1 ORDER BY title COLLATE NOCASE")
    fun observeActiveBills(): Flow<List<MonthlyBillEntity>>

    @Query("SELECT * FROM expense_months WHERE monthKey = :monthKey LIMIT 1")
    fun observeMonth(monthKey: String): Flow<ExpenseMonthEntity?>

    @Query("SELECT * FROM expense_months WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getMonth(monthKey: String): ExpenseMonthEntity?

    @Query("SELECT * FROM monthly_bills WHERE active = 1 ORDER BY title COLLATE NOCASE")
    suspend fun getActiveBills(): List<MonthlyBillEntity>

    @Query("SELECT * FROM expense_entries WHERE monthKey = :monthKey AND sourceBillId = :billId LIMIT 1")
    suspend fun getBillOccurrence(monthKey: String, billId: String): ExpenseEntryEntity?

    @Query("SELECT * FROM expense_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getEntry(entryId: String): ExpenseEntryEntity?

    @Query("SELECT * FROM monthly_bills")
    suspend fun getAllBills(): List<MonthlyBillEntity>

    @Query("SELECT * FROM expense_entries")
    suspend fun getAllEntries(): List<ExpenseEntryEntity>

    @Query("SELECT * FROM expense_months")
    suspend fun getAllMonths(): List<ExpenseMonthEntity>

    @Upsert
    suspend fun upsertMonth(entity: ExpenseMonthEntity)

    @Upsert
    suspend fun upsertEntry(entity: ExpenseEntryEntity)

    @Upsert
    suspend fun upsertBill(entity: MonthlyBillEntity)

    @Query("DELETE FROM expense_entries WHERE entryId = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("SELECT * FROM monthly_bills WHERE billId = :billId LIMIT 1")
    suspend fun getBill(billId: String): MonthlyBillEntity?
}
