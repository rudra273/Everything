package com.everything.app.feature.keystore.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.everything.app.AppContainer
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.security.BiometricAuthenticator
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.MutedText
import com.everything.app.core.ui.Panel
import com.everything.app.core.ui.PanelAlt
import com.everything.app.core.ui.SoftText
import com.everything.app.core.ui.Stroke
import com.everything.app.core.ui.Teal
import com.everything.app.feature.keystore.data.KeyStoreEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface KeyEditorState {
    data object Add : KeyEditorState
    data class Edit(val entry: KeyStoreEntry) : KeyEditorState
}

private data class KeyEditDraft(
    val name: String,
    val label: String,
    val value: String,
)

@Composable
fun KeyStoreScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val biometricAuthenticator = remember(activity) { BiometricAuthenticator(activity) }
    var unlocked by remember { mutableStateOf(false) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }
    val entries by container.keyStoreRepository
        .observeEntries()
        .collectAsStateWithLifecycle(initialValue = null)
    var editorState by remember { mutableStateOf<KeyEditorState?>(null) }
    var actionEntry by remember { mutableStateOf<KeyStoreEntry?>(null) }
    var confirmDeleteEntry by remember { mutableStateOf<KeyStoreEntry?>(null) }
    var visibleEntries by remember { mutableStateOf(emptySet<String>()) }
    var query by remember { mutableStateOf("") }
    val filteredEntries = remember(entries, query) {
        entries.orEmpty().filter { entry ->
            query.isBlank() ||
                entry.name.contains(query, ignoreCase = true) ||
                entry.label.contains(query, ignoreCase = true) ||
                entry.value.contains(query, ignoreCase = true)
        }
    }

    fun tryBiometricUnlock() {
        if (!biometricEnabled || !biometricAuthenticator.canAuthenticate()) return
        biometricAuthenticator.authenticate(
            title = "Unlock Key Store",
            subtitle = "Everything secure storage",
            onSuccess = {
                unlocked = true
                unlockPin = ""
                unlockError = null
            },
            onError = { unlockError = it },
        )
    }

    LaunchedEffect(Unit) {
        biometricEnabled = container.secureSettingRepository
            .getBoolean(SecureSettingRepository.KEY_BIOMETRIC_ENABLED) == true
        if (biometricEnabled) {
            tryBiometricUnlock()
        }
    }

    if (!unlocked) {
        KeyStoreUnlockScreen(
            pin = unlockPin,
            error = unlockError,
            biometricEnabled = biometricEnabled,
            onBack = onBack,
            onPinChange = {
                unlockPin = it.filter(Char::isDigit).take(12)
                unlockError = null
            },
            onUnlock = {
                scope.launch {
                    val pin = unlockPin
                    val valid = withContext(Dispatchers.Default) {
                        container.credentialRepository.verify(pin.toCharArray())
                    }
                    if (valid) {
                        unlocked = true
                        unlockPin = ""
                    } else {
                        unlockError = "Wrong PIN"
                        unlockPin = ""
                    }
                }
            },
            onBiometric = { tryBiometricUnlock() },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Key Store", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${filteredEntries.size} of ${entries?.size ?: 0} saved", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { editorState = if (editorState is KeyEditorState.Add) null else KeyEditorState.Add }) {
                Icon(if (editorState is KeyEditorState.Add) Icons.Rounded.Close else Icons.Rounded.Add, contentDescription = "Add", tint = Cyan)
            }
        }

        if (editorState is KeyEditorState.Add) {
            AddKeyCard(
                onCancel = { editorState = null },
                onSave = { name, label, value ->
                    scope.launch {
                        container.keyStoreRepository.addEntry(name, label, value)
                        editorState = null
                    }
                },
            )
        }

        SecureTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search name, label, or value",
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
            },
        )

        when (val currentEntries = entries) {
            null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (currentEntries.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }
                if (currentEntries.isNotEmpty() && filteredEntries.isEmpty()) {
                    item {
                        EmptyState(text = "No matching keys")
                    }
                }
                items(filteredEntries, key = { it.entryId }) { entry ->
                    KeyEntryRow(
                        entry = entry,
                        visible = entry.entryId in visibleEntries,
                        onToggleVisible = {
                            visibleEntries = if (entry.entryId in visibleEntries) {
                                visibleEntries - entry.entryId
                            } else {
                                visibleEntries + entry.entryId
                            }
                        },
                        onLongPress = { actionEntry = entry },
                    )
                }
            }
        }
    }

    actionEntry?.let { entry ->
        KeyActionsDialog(
            entry = entry,
            onDismiss = { actionEntry = null },
            onUpdate = {
                actionEntry = null
                editorState = KeyEditorState.Edit(entry)
            },
            onDelete = {
                actionEntry = null
                confirmDeleteEntry = entry
            },
        )
    }

    confirmDeleteEntry?.let { entry ->
        ConfirmDeleteDialog(
            entry = entry,
            onDismiss = { confirmDeleteEntry = null },
            onConfirm = {
                scope.launch {
                    container.keyStoreRepository.deleteEntry(entry.entryId)
                    visibleEntries = visibleEntries - entry.entryId
                    confirmDeleteEntry = null
                }
            },
        )
    }

    (editorState as? KeyEditorState.Edit)?.let { editState ->
        UpdateKeyDialog(
            entry = editState.entry,
            onDismiss = { editorState = null },
            onSave = { draft ->
                scope.launch {
                    container.keyStoreRepository.updateEntry(
                        entryId = editState.entry.entryId,
                        name = draft.name,
                        label = draft.label,
                        value = draft.value,
                    )
                    editorState = null
                }
            },
        )
    }
}

@Composable
private fun KeyStoreUnlockScreen(
    pin: String,
    error: String?,
    biometricEnabled: Boolean,
    onBack: () -> Unit,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onBiometric: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Key Store", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Unlock secure storage", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = androidx.compose.foundation.BorderStroke(1.dp, Stroke),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Teal, Cyan))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF001716), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Enter master PIN", fontWeight = FontWeight.SemiBold)
                }
                SecureTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = "Master PIN",
                    secure = true,
                )
                error?.let {
                    Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (biometricEnabled) {
                        TextButton(onClick = onBiometric) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fingerprint")
                        }
                    }
                    TextButton(enabled = pin.length >= 4, onClick = onUnlock) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddKeyCard(
    onCancel: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var confirmValue by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && value.isNotBlank() && value == confirmValue

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, Stroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add Key",
                fontWeight = FontWeight.SemiBold,
            )
            SecureTextField(value = name, onValueChange = { name = it }, label = "Key name")
            SecureTextField(value = value, onValueChange = { value = it }, label = "Value", secure = true)
            SecureTextField(value = confirmValue, onValueChange = { confirmValue = it }, label = "Confirm value", secure = true)
            SecureTextField(value = label, onValueChange = { label = it }, label = "Label")
            if (confirmValue.isNotEmpty() && value != confirmValue) {
                Text("Values do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
                TextButton(enabled = canSave, onClick = { onSave(name, label, value) }) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun KeyEntryRow(
    entry: KeyStoreEntry,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    onLongPress: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, Stroke),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Teal, Cyan))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Key, contentDescription = null, tint = Color(0xFF001716), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.label.isNotBlank()) {
                    Text(
                        text = entry.label,
                        color = Cyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = if (visible) entry.value else "Hidden",
                    color = if (visible) SoftText else MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "v${entry.version}",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = "Toggle visibility",
                    tint = SoftText,
                )
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(entry.value)) }) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = "Copy value",
                    tint = SoftText,
                )
            }
        }
    }
}

@Composable
private fun KeyActionsDialog(
    entry: KeyStoreEntry,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Choose an action", color = MutedText)
                if (entry.label.isNotBlank()) {
                    Text(entry.label, color = Cyan, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Update")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFFA8A8), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = Color(0xFFFFA8A8))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MutedText)
                }
            }
        },
        containerColor = Panel,
        titleContentColor = SoftText,
        textContentColor = SoftText,
    )
}

@Composable
private fun ConfirmDeleteDialog(
    entry: KeyStoreEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete key?") },
        text = { Text("This will remove ${entry.name}.", color = MutedText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFFFA8A8))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        containerColor = Panel,
        titleContentColor = SoftText,
        textContentColor = SoftText,
    )
}

@Composable
private fun UpdateKeyDialog(
    entry: KeyStoreEntry,
    onDismiss: () -> Unit,
    onSave: (KeyEditDraft) -> Unit,
) {
    var name by remember(entry.entryId) { mutableStateOf(entry.name) }
    var value by remember(entry.entryId) { mutableStateOf(entry.value) }
    var confirmValue by remember(entry.entryId) { mutableStateOf(entry.value) }
    var label by remember(entry.entryId) { mutableStateOf(entry.label) }
    val canSave = name.isNotBlank() && value.isNotBlank() && value == confirmValue

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecureTextField(value = name, onValueChange = { name = it }, label = "Key name")
                SecureTextField(value = value, onValueChange = { value = it }, label = "Value", secure = true)
                SecureTextField(value = confirmValue, onValueChange = { confirmValue = it }, label = "Confirm value", secure = true)
                SecureTextField(value = label, onValueChange = { label = it }, label = "Label")
                if (confirmValue.isNotEmpty() && value != confirmValue) {
                    Text("Values do not match", color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        KeyEditDraft(
                            name = name,
                            label = label,
                            value = value,
                        ),
                    )
                },
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        containerColor = Panel,
        titleContentColor = SoftText,
        textContentColor = SoftText,
    )
}

@Composable
private fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secure: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        singleLine = !secure,
        visualTransformation = if (secure && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (secure) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide" else "Show",
                        tint = SoftText,
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Stroke,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            cursorColor = Cyan,
            focusedTextColor = SoftText,
            unfocusedTextColor = SoftText,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyState(text: String = "No keys saved") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelAlt),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MutedText)
    }
}
