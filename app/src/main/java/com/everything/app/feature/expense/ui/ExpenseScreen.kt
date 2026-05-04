package com.everything.app.feature.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everything.app.AppContainer
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.DangerRed
import com.everything.app.core.ui.DangerRedMuted
import com.everything.app.core.ui.GlassBackground
import com.everything.app.core.ui.MutedText
import com.everything.app.core.ui.PanelAlt
import com.everything.app.core.ui.PrimaryButton
import com.everything.app.core.ui.SecondaryButton
import com.everything.app.core.ui.SoftText
import com.everything.app.core.ui.Stroke
import com.everything.app.core.ui.glassSurface
import com.everything.app.feature.expense.data.ExpenseEntry
import com.everything.app.feature.expense.data.ExpenseEntryKind
import com.everything.app.feature.expense.data.ExpenseMonthSummary
import com.everything.app.feature.expense.data.MonthlyBill
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun ExpenseScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthKey = remember(selectedMonth) { selectedMonth.toString() }
    val summary by container.expenseRepository
        .observeMonth(monthKey)
        .collectAsStateWithLifecycle(initialValue = null)
    var selectedExpenseDate by remember(selectedMonth) { mutableStateOf<String?>(null) }
    var addDailyOpen by remember { mutableStateOf(false) }
    var addBillOpen by remember { mutableStateOf(false) }
    var limitOpen by remember { mutableStateOf(false) }
    var actionEntry by remember { mutableStateOf<ExpenseEntry?>(null) }
    var editEntry by remember { mutableStateOf<ExpenseEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<ExpenseEntry?>(null) }
    var stopBill by remember { mutableStateOf<MonthlyBill?>(null) }

    LaunchedEffect(monthKey) {
        container.expenseRepository.ensureMonth(monthKey)
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        ExpenseTopBar(
            selectedMonth = selectedMonth,
            onBack = onBack,
            onPrevious = { selectedMonth = selectedMonth.minusMonths(1) },
            onNext = { selectedMonth = selectedMonth.plusMonths(1) },
        )

        when (val currentSummary = summary) {
            null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val expenseDates = currentSummary.entries
                    .map { it.expenseDate }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedDescending()
                val visibleEntries = currentSummary.entries.filter { entry ->
                    selectedExpenseDate == null || entry.expenseDate == selectedExpenseDate
                }
                item {
                    MonthTotalCard(
                        summary = currentSummary,
                        onSetLimit = { limitOpen = true },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PrimaryButton(
                            text = "Daily",
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            },
                            onClick = { addDailyOpen = !addDailyOpen },
                        )
                        SecondaryButton(
                            text = "Bill",
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(Icons.Rounded.Subscriptions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            },
                            onClick = { addBillOpen = !addBillOpen },
                        )
                    }
                }
                if (addDailyOpen) {
                    item {
                        ExpenseFormCard(
                            title = "Add Daily Expense",
                            defaultCategory = "General",
                            onCancel = { addDailyOpen = false },
                            onSave = { title, category, amount, note, expenseDate ->
                                scope.launch {
                                    container.expenseRepository.addDailyExpense(expenseDate, title, category, amount, note)
                                    selectedMonth = YearMonth.from(LocalDate.parse(expenseDate))
                                    selectedExpenseDate = expenseDate
                                    addDailyOpen = false
                                }
                            },
                        )
                    }
                }
                if (addBillOpen) {
                    item {
                        BillFormCard(
                            onCancel = { addBillOpen = false },
                            onSave = { title, category, amount ->
                                scope.launch {
                                    container.expenseRepository.addMonthlyBill(monthKey, title, category, amount)
                                    addBillOpen = false
                                }
                            },
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Static Monthly Bills",
                        value = money(currentSummary.billTotalMinor),
                    )
                }
                if (currentSummary.monthlyBills.isEmpty()) {
                    item { EmptyExpenseState("No static monthly bills") }
                } else {
                    items(currentSummary.monthlyBills, key = { it.billId }) { bill ->
                        MonthlyBillRow(
                            bill = bill,
                            onStop = { stopBill = bill },
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "This Month",
                        value = "${visibleEntries.size} items",
                    )
                }
                if (expenseDates.isNotEmpty()) {
                    item {
                        DateFilterRow(
                            dates = expenseDates,
                            selectedDate = selectedExpenseDate,
                            onSelect = { selectedExpenseDate = it },
                        )
                    }
                }
                if (currentSummary.entries.isEmpty()) {
                    item { EmptyExpenseState("No expenses for this month") }
                } else if (visibleEntries.isEmpty()) {
                    item { EmptyExpenseState("No expenses for this date") }
                } else {
                    items(visibleEntries, key = { it.entryId }) { entry ->
                        ExpenseEntryRow(
                            entry = entry,
                            onLongPress = { actionEntry = entry },
                        )
                    }
                }
            }
        }
        }
    }

    actionEntry?.let { entry ->
        ExpenseActionDialog(
            entry = entry,
            onDismiss = { actionEntry = null },
            onUpdate = {
                actionEntry = null
                editEntry = entry
            },
            onDelete = {
                actionEntry = null
                deleteEntry = entry
            },
        )
    }

    editEntry?.let { entry ->
        UpdateExpenseDialog(
            entry = entry,
            onDismiss = { editEntry = null },
            onSave = { title, category, amount, note, expenseDate ->
                scope.launch {
                    container.expenseRepository.updateEntry(entry.entryId, title, category, amount, note, expenseDate)
                    selectedMonth = YearMonth.from(LocalDate.parse(expenseDate))
                    selectedExpenseDate = expenseDate
                    editEntry = null
                }
            },
        )
    }

    if (limitOpen) {
        LimitDialog(
            currentLimit = summary?.limitMinor ?: 0L,
            onDismiss = { limitOpen = false },
            onSave = { amount ->
                scope.launch {
                    container.expenseRepository.setMonthlyLimit(monthKey, amount)
                    limitOpen = false
                }
            },
        )
    }

    deleteEntry?.let { entry ->
        ConfirmDialog(
            title = "Delete Expense",
            message = "Remove ${entry.title} from ${monthLabel(selectedMonth)}?",
            confirmText = "Delete",
            onDismiss = { deleteEntry = null },
            onConfirm = {
                scope.launch {
                    container.expenseRepository.deleteEntry(entry.entryId)
                    deleteEntry = null
                }
            },
        )
    }

    stopBill?.let { bill ->
        ConfirmDialog(
            title = "Stop Monthly Bill",
            message = "${bill.title} will not be added to future months.",
            confirmText = "Stop",
            onDismiss = { stopBill = null },
            onConfirm = {
                scope.launch {
                    container.expenseRepository.stopMonthlyBill(bill.billId)
                    stopBill = null
                }
            },
        )
    }
}

@Composable
private fun ExpenseTopBar(
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(monthLabel(selectedMonth), color = Cyan, style = MaterialTheme.typography.bodySmall)
        }
        MonthIconButton(Icons.Rounded.KeyboardArrowLeft, "Previous month", onPrevious)
        MonthIconButton(Icons.Rounded.KeyboardArrowRight, "Next month", onNext)
    }
}

@Composable
private fun MonthIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f)),
    ) {
        Icon(icon, contentDescription = description, tint = SoftText)
    }
}

@Composable
private fun MonthTotalCard(
    summary: ExpenseMonthSummary,
    onSetLimit: () -> Unit,
) {
    val isOverLimit = summary.limitMinor > 0L && summary.totalMinor > summary.limitMinor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Payments, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Monthly Total", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Text(money(summary.totalMinor), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                }
                TextButton(onClick = onSetLimit) {
                    Text(if (summary.limitMinor > 0L) "Edit limit" else "Set limit", color = Cyan)
                }
            }
            if (summary.limitMinor > 0L) {
                LinearProgressIndicator(
                    progress = { summary.limitProgress.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (isOverLimit) DangerRed else Cyan,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
                Text(
                    text = if (isOverLimit) {
                        "${money(-summary.remainingMinor)} over ${money(summary.limitMinor)} limit"
                    } else {
                        "${money(summary.remainingMinor)} left from ${money(summary.limitMinor)}"
                    },
                    color = if (isOverLimit) DangerRed else MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill("Bills", money(summary.billTotalMinor), Icons.Rounded.Subscriptions, Modifier.weight(1f))
                StatPill("Daily", money(summary.dailyTotalMinor), Icons.Rounded.ReceiptLong, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .glassSurface(RoundedCornerShape(14.dp), selected = false, tintStrength = 0.06f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
            Text(value, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExpenseFormCard(
    title: String,
    defaultCategory: String,
    onCancel: () -> Unit,
    onSave: (String, String, Long, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var note by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val amountMinor = amount.toMinorOrNull()
    val dateValid = expenseDate.toLocalDateOrNull() != null
    val canSave = name.isNotBlank() && amountMinor != null && amountMinor > 0L && dateValid

    FormCard(title = title, icon = Icons.Rounded.ReceiptLong) {
        ExpenseTextField(value = name, onValueChange = { name = it }, label = "Name")
        ExpenseTextField(value = expenseDate, onValueChange = { expenseDate = it }, label = "Date (YYYY-MM-DD)")
        ExpenseTextField(
            value = amount,
            onValueChange = { amount = it.cleanAmountInput() },
            label = "Amount",
            keyboardType = KeyboardType.Decimal,
        )
        ExpenseTextField(value = category, onValueChange = { category = it }, label = "Category")
        ExpenseTextField(value = note, onValueChange = { note = it }, label = "Note")
        if (expenseDate.isNotBlank() && !dateValid) {
            Text("Use YYYY-MM-DD date", color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
        FormActions(canSave = canSave, onCancel = onCancel, onSave = { onSave(name, category, amountMinor ?: 0L, note, expenseDate) })
    }
}

@Composable
private fun BillFormCard(
    onCancel: () -> Unit,
    onSave: (String, String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bills") }
    val amountMinor = amount.toMinorOrNull()
    val canSave = name.isNotBlank() && amountMinor != null && amountMinor > 0L

    FormCard(title = "Add Static Monthly Bill", icon = Icons.Rounded.Subscriptions) {
        ExpenseTextField(value = name, onValueChange = { name = it }, label = "Bill name")
        ExpenseTextField(
            value = amount,
            onValueChange = { amount = it.cleanAmountInput() },
            label = "Monthly amount",
            keyboardType = KeyboardType.Decimal,
        )
        ExpenseTextField(value = category, onValueChange = { category = it }, label = "Category")
        FormActions(canSave = canSave, onCancel = onCancel, onSave = { onSave(name, category, amountMinor ?: 0L) })
    }
}

@Composable
private fun FormCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = Cyan)
            }
            content()
        }
    }
}

@Composable
private fun ExpenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = KeyboardCapitalization.Words,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Stroke,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            cursorColor = Cyan,
            focusedTextColor = SoftText,
            unfocusedTextColor = SoftText,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FormActions(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SecondaryButton(text = "Cancel", modifier = Modifier.weight(1f), onClick = onCancel)
        PrimaryButton(text = "Save", enabled = canSave, modifier = Modifier.weight(1f), onClick = onSave)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MutedText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DateFilterRow(
    dates: List<String>,
    selectedDate: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            DateFilterChip(
                text = "All",
                selected = selectedDate == null,
                onClick = { onSelect(null) },
            )
        }
        items(dates) { date ->
            DateFilterChip(
                text = date,
                selected = selectedDate == date,
                onClick = { onSelect(date) },
            )
        }
    }
}

@Composable
private fun DateFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .glassSurface(RoundedCornerShape(12.dp), selected = selected, tintStrength = 0.08f)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF001716) else SoftText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MonthlyBillRow(
    bill: MonthlyBill,
    onStop: () -> Unit,
) {
    ExpenseBaseRow(
        icon = Icons.Rounded.Subscriptions,
        title = bill.title,
        category = bill.category,
        amount = money(bill.amountMinor),
        accent = Cyan,
        onLongPress = onStop,
    )
}

@Composable
private fun ExpenseEntryRow(
    entry: ExpenseEntry,
    onLongPress: () -> Unit,
) {
    val accent = if (entry.kind == ExpenseEntryKind.MonthlyBill) Cyan else SoftText
    ExpenseBaseRow(
        icon = if (entry.kind == ExpenseEntryKind.MonthlyBill) Icons.Rounded.CalendarMonth else Icons.Rounded.ReceiptLong,
        title = entry.title,
        category = if (entry.kind == ExpenseEntryKind.MonthlyBill) {
            "${entry.category} monthly · ${entry.expenseDate}"
        } else {
            "${entry.category} · ${entry.expenseDate}"
        },
        note = entry.note.takeIf { entry.kind == ExpenseEntryKind.Daily && it.isNotBlank() },
        amount = money(entry.amountMinor),
        accent = accent,
        onLongPress = onLongPress,
    )
}

@Composable
private fun ExpenseBaseRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    category: String,
    note: String? = null,
    amount: String,
    accent: Color,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = false)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.055f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MutedText, style = MaterialTheme.typography.bodySmall)
                note?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, color = SoftText.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(amount, color = SoftText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyExpenseState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .glassSurface(RoundedCornerShape(18.dp), selected = false),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MutedText)
    }
}

@Composable
private fun ExpenseActionDialog(
    entry: ExpenseEntry,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(money(entry.amountMinor), color = Cyan, fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) {
                    Text(entry.note, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Update", color = Cyan, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = DangerRed)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MutedText)
                }
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun UpdateExpenseDialog(
    entry: ExpenseEntry,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String) -> Unit,
) {
    var name by remember(entry.entryId) { mutableStateOf(entry.title) }
    var amount by remember(entry.entryId) { mutableStateOf(minorToInput(entry.amountMinor)) }
    var category by remember(entry.entryId) { mutableStateOf(entry.category) }
    var note by remember(entry.entryId) { mutableStateOf(entry.note) }
    var expenseDate by remember(entry.entryId) { mutableStateOf(entry.expenseDate) }
    val amountMinor = amount.toMinorOrNull()
    val dateValid = expenseDate.toLocalDateOrNull() != null
    val canSave = name.isNotBlank() && amountMinor != null && amountMinor > 0L && dateValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseTextField(value = name, onValueChange = { name = it }, label = "Name")
                ExpenseTextField(value = expenseDate, onValueChange = { expenseDate = it }, label = "Date (YYYY-MM-DD)")
                ExpenseTextField(
                    value = amount,
                    onValueChange = { amount = it.cleanAmountInput() },
                    label = "Amount",
                    keyboardType = KeyboardType.Decimal,
                )
                ExpenseTextField(value = category, onValueChange = { category = it }, label = "Category")
                ExpenseTextField(value = note, onValueChange = { note = it }, label = "Note")
                if (expenseDate.isNotBlank() && !dateValid) {
                    Text("Use YYYY-MM-DD date", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(name, category, amountMinor ?: 0L, note, expenseDate) },
            ) {
                Text("Save", color = if (canSave) Cyan else MutedText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun LimitDialog(
    currentLimit: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amount by remember(currentLimit) { mutableStateOf(if (currentLimit > 0L) minorToInput(currentLimit) else "") }
    val amountMinor = amount.toMinorOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Limit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseTextField(
                    value = amount,
                    onValueChange = { amount = it.cleanAmountInput() },
                    label = "Limit amount",
                    keyboardType = KeyboardType.Decimal,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DangerRedMuted)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use 0 to remove the limit.", color = SoftText, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amountMinor) }) {
                Text("Save", color = Cyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = MutedText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = DangerRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

private fun money(amountMinor: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    return format.format(amountMinor / 100.0)
}

private fun monthLabel(month: YearMonth): String {
    return month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}

private fun String.cleanAmountInput(): String {
    val filtered = filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) {
        filtered.take(9)
    } else {
        filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).filter(Char::isDigit).take(2)
    }
}

private fun String.toMinorOrNull(): Long? {
    val value = toDoubleOrNull() ?: return null
    return (value * 100.0).roundToLong()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this) }.getOrNull()
}

private fun minorToInput(amountMinor: Long): String {
    val whole = amountMinor / 100
    val fraction = amountMinor % 100
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
}
