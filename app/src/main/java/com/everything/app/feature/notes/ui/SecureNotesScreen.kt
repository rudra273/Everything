package com.everything.app.feature.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everything.app.AppContainer
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.security.BiometricAuthenticator
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.MutedText
import com.everything.app.core.ui.Panel
import com.everything.app.core.ui.PanelAlt
import com.everything.app.core.ui.PrimaryButton
import com.everything.app.core.ui.SecondaryButton
import com.everything.app.core.ui.SoftText
import com.everything.app.core.ui.Stroke
import com.everything.app.feature.notes.data.SecureNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface NoteEditorState {
    data object Add : NoteEditorState
    data class Edit(val note: SecureNote) : NoteEditorState
}

@Composable
fun SecureNotesScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val biometricAuthenticator = remember(activity) { BiometricAuthenticator(activity) }
    var unlocked by remember { mutableStateOf(false) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var editorState by remember { mutableStateOf<NoteEditorState?>(null) }
    var actionNote by remember { mutableStateOf<SecureNote?>(null) }
    var confirmDeleteNote by remember { mutableStateOf<SecureNote?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    val notes by container.secureNoteRepository
        .observeNotes()
        .collectAsStateWithLifecycle(initialValue = null)

    fun tryBiometricUnlock() {
        if (!biometricEnabled || !biometricAuthenticator.canAuthenticate()) return
        biometricAuthenticator.authenticate(
            title = "Unlock Notes",
            subtitle = "Everything secure notes",
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

    BackHandler {
        when {
            query.isNotBlank() || selectedLabel != null -> {
                query = ""
                selectedLabel = null
            }
            else -> onBack()
        }
    }

    if (!unlocked) {
        NotesUnlockScreen(
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
                    val valid = withContext(Dispatchers.Default) {
                        container.credentialRepository.verify(unlockPin.toCharArray())
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

    val filteredNotes = remember(notes, query, selectedLabel) {
        notes.orEmpty().filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.labels.contains(query, ignoreCase = true)
            val labels = note.labelList()
            val matchesLabel = selectedLabel == null || selectedLabel in labels
            matchesQuery && matchesLabel
        }
    }
    val uniqueLabels = remember(notes) {
        notes.orEmpty().flatMap { it.labelList() }.distinct().sorted()
    }

    editorState?.let { currentEditor ->
        NoteEditorPage(
            state = currentEditor,
            existingLabels = uniqueLabels,
            onBack = { title, content, labels ->
                scope.launch {
                    when (currentEditor) {
                        NoteEditorState.Add -> {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                container.secureNoteRepository.addNote(
                                    title = title.ifBlank { "Untitled" },
                                    content = content.ifBlank { " " },
                                    labels = labels,
                                )
                            }
                        }
                        is NoteEditorState.Edit -> {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                container.secureNoteRepository.updateNote(
                                    noteId = currentEditor.note.noteId,
                                    title = title.ifBlank { "Untitled" },
                                    content = content.ifBlank { " " },
                                    labels = labels,
                                )
                            }
                        }
                    }
                    editorState = null
                }
            },
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Secure Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${filteredNotes.size} of ${notes?.size ?: 0} notes", color = Cyan, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = { editorState = NoteEditorState.Add },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan.copy(alpha = 0.16f)),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add note", tint = Cyan)
            }
        }

        SearchField(query = query, onQueryChange = { query = it })

        if (uniqueLabels.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(uniqueLabels) { label ->
                    LabelChip(
                        label = label,
                        selected = selectedLabel == label,
                        onClick = { selectedLabel = if (selectedLabel == label) null else label },
                    )
                }
            }
        }

        when (val currentNotes = notes) {
            null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                if (currentNotes.isEmpty()) {
                    item { EmptyNotesState("No secure notes saved") }
                } else if (filteredNotes.isEmpty()) {
                    item { EmptyNotesState("No matching notes") }
                }
                items(filteredNotes, key = { it.noteId }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { editorState = NoteEditorState.Edit(note) },
                        onLongPress = { actionNote = note },
                    )
                }
            }
        }
    }

    actionNote?.let { note ->
        NoteActionsDialog(
            note = note,
            onDismiss = { actionNote = null },
            onDelete = {
                actionNote = null
                confirmDeleteNote = note
            },
        )
    }

    confirmDeleteNote?.let { note ->
        ConfirmDeleteDialog(
            note = note,
            onDismiss = { confirmDeleteNote = null },
            onConfirm = {
                scope.launch {
                    container.secureNoteRepository.deleteNote(note.noteId)
                    confirmDeleteNote = null
                }
            },
        )
    }
}

@Composable
private fun NotesUnlockScreen(
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Secure Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Unlock private notes", color = Cyan, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = BorderStroke(1.dp, Stroke),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Master PIN", fontWeight = FontWeight.SemiBold)
                }
                SecureTextField(value = pin, onValueChange = onPinChange, label = "Enter PIN", secure = true, keyboardType = KeyboardType.NumberPassword)
                error?.let { Text(it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (biometricEnabled) {
                        SecondaryButton(
                            text = "Fingerprint",
                            leadingIcon = {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            },
                            modifier = Modifier.weight(1f),
                            onClick = onBiometric,
                        )
                    }
                    PrimaryButton(text = "Unlock", enabled = pin.length >= 4, modifier = Modifier.weight(1f), onClick = onUnlock)
                }
            }
        }
    }
}

@Composable
private fun NoteEditorPage(
    state: NoteEditorState,
    existingLabels: List<String>,
    onBack: (String, String, String) -> Unit,
) {
    val editingNote = (state as? NoteEditorState.Edit)?.note
    var title by remember(editingNote?.noteId) { mutableStateOf(editingNote?.title.orEmpty()) }
    var content by remember(editingNote?.noteId) { mutableStateOf(editingNote?.content.orEmpty()) }
    var selectedLabels by remember(editingNote?.noteId) {
        mutableStateOf(editingNote?.labelList()?.toSet().orEmpty())
    }
    var labelDialogOpen by remember { mutableStateOf(false) }
    val noteFocusRequester = remember { FocusRequester() }

    fun finish() {
        onBack(title, content, selectedLabels.sorted().joinToString(", "))
    }

    BackHandler { finish() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { finish() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { labelDialogOpen = true }) {
                Icon(Icons.Rounded.Label, contentDescription = null, tint = Cyan, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add label", color = Cyan, fontWeight = FontWeight.SemiBold)
            }
        }

        if (selectedLabels.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(selectedLabels.sorted()) { label ->
                    LabelChip(
                        label = label,
                        selected = true,
                        onClick = { selectedLabels = selectedLabels - label },
                    )
                }
            }
        }

        PlainNoteTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "Title",
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = SoftText,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            imeAction = ImeAction.Next,
            onNext = { noteFocusRequester.requestFocus() },
        )

        PlainNoteTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = "Note",
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SoftText),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(noteFocusRequester),
            singleLine = false,
        )

    }

    if (labelDialogOpen) {
        LabelPickerDialog(
            existingLabels = existingLabels,
            selectedLabels = selectedLabels,
            onToggleLabel = { label ->
                selectedLabels = if (label in selectedLabels) {
                    selectedLabels - label
                } else {
                    selectedLabels + label
                }
            },
            onCreateLabel = { label ->
                selectedLabels = selectedLabels + label.trim()
            },
            onDismiss = { labelDialogOpen = false },
        )
    }
}

@Composable
private fun PlainNoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onNext: () -> Unit = {},
) {
    Box(modifier = modifier.padding(horizontal = 8.dp)) {
        if (value.isEmpty()) {
            Text(placeholder, color = MutedText, style = textStyle)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = if (singleLine) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
        )
    }
}

@Composable
private fun LabelPickerDialog(
    existingLabels: List<String>,
    selectedLabels: Set<String>,
    onToggleLabel: (String) -> Unit,
    onCreateLabel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }
    val cleanNewLabel = newLabel.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Labels", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existingLabels.isNotEmpty()) {
                    Text("Existing", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(existingLabels) { label ->
                            LabelChip(
                                label = label,
                                selected = label in selectedLabels,
                                onClick = { onToggleLabel(label) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("Create label", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Stroke,
                        focusedLabelColor = Cyan,
                        unfocusedLabelColor = MutedText,
                        cursorColor = Cyan,
                        focusedTextColor = SoftText,
                        unfocusedTextColor = SoftText,
                        focusedContainerColor = PanelAlt,
                        unfocusedContainerColor = PanelAlt,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cleanNewLabel.isNotBlank(),
                onClick = {
                    onCreateLabel(cleanNewLabel)
                    newLabel = ""
                },
            ) {
                Text("Add", color = if (cleanNewLabel.isNotBlank()) Cyan else MutedText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MutedText)
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search title, content or label", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Stroke,
            focusedTextColor = SoftText,
            unfocusedTextColor = SoftText,
            cursorColor = Cyan,
            focusedContainerColor = PanelAlt,
            unfocusedContainerColor = PanelAlt,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secure: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        visualTransformation = if (secure && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (secure) {
            {
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(28.dp)) {
                    Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Stroke,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            cursorColor = Cyan,
            focusedTextColor = SoftText,
            unfocusedTextColor = SoftText,
            focusedContainerColor = PanelAlt,
            unfocusedContainerColor = PanelAlt,
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LabelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Cyan else PanelAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color(0xFF001716) else SoftText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NoteRow(
    note: SecureNote,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Stroke),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                if (note.labels.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        note.labels,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Cyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(note.content, maxLines = 3, overflow = TextOverflow.Ellipsis, color = SoftText.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NoteActionsDialog(
    note: SecureNote,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
        text = { Text("Tap a note to open it. Long press is for delete.", color = MutedText) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFFA8A8), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", color = Color(0xFFFFA8A8))
            }
        },
        containerColor = PanelAlt,
        titleContentColor = SoftText,
        textContentColor = SoftText,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun ConfirmDeleteDialog(
    note: SecureNote,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Note", fontWeight = FontWeight.Bold) },
        text = { Text("Remove '${note.title}'?", color = MutedText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFFFA8A8), fontWeight = FontWeight.SemiBold)
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
private fun EmptyNotesState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelAlt),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MutedText)
    }
}

private fun SecureNote.labelList(): List<String> {
    return labels.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
