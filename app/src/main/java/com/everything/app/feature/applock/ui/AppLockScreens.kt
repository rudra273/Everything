package com.everything.app.feature.applock.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everything.app.AppContainer
import com.everything.app.core.data.SecureSettingRepository
import com.everything.app.core.permissions.AppLockPermissionState
import com.everything.app.core.permissions.PermissionIntents
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.GlassBackground
import com.everything.app.core.ui.PrimaryButton
import com.everything.app.core.ui.MutedText
import com.everything.app.core.ui.SecondaryButton
import com.everything.app.core.ui.SoftText
import com.everything.app.core.ui.Stroke
import com.everything.app.core.ui.glassSurface
import com.everything.app.feature.applock.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupCredentialScreen(
    onCredentialReady: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val canContinue = pin.length >= 4 && pin == confirmPin

    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                BrandHeader(
                    icon = Icons.Rounded.Security,
                    title = "Everything",
                    subtitle = "Create master PIN",
                )

                SecureTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(12) },
                    label = "Master PIN",
                )
                SecureTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter(Char::isDigit).take(12) },
                    label = "Confirm PIN",
                )
                if (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin) {
                    Text(
                        text = "PINs do not match",
                        color = Color(0xFFFFA8A8),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            PrimaryButton(
                text = "Continue",
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                },
                onClick = { onCredentialReady(pin) },
            )
        }
    }
}

@Composable
fun BiometricSetupScreen(
    canUseBiometric: Boolean,
    message: String?,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                BrandHeader(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric",
                    subtitle = if (canUseBiometric) "Fast unlock" else "PIN unlock",
                )
                StatusPanel(
                    icon = Icons.Rounded.Shield,
                    title = if (canUseBiometric) "Ready" else "Unavailable",
                    value = if (canUseBiometric) "Strong biometric detected" else "Use the master PIN on this device",
                )
                message?.let {
                    Text(text = it, color = Color(0xFFFFD28F), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canUseBiometric) {
                    PrimaryButton(
                        text = "Enable",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        },
                        onClick = onEnable,
                    )
                }
                SecondaryButton(
                    text = if (canUseBiometric) "Skip" else "Continue",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSkip,
                )
            }
        }
    }
}

@Composable
fun PermissionGrantScreen(
    permissions: AppLockPermissionState,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current

    fun openSettings(block: () -> android.content.Intent) {
        try {
            context.startActivity(block())
        } catch (_: ActivityNotFoundException) {
            onRefresh()
        }
    }

    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BrandHeader(
                    icon = Icons.Rounded.Settings,
                    title = "Permissions",
                    subtitle = "Local App Lock access",
                )

                PermissionRow(
                    icon = Icons.Rounded.Apps,
                    title = "Usage access",
                    granted = permissions.usageAccess,
                    onClick = { openSettings { PermissionIntents.usageAccessSettings() } },
                )
                PermissionRow(
                    icon = Icons.Rounded.Security,
                    title = "Overlay",
                    granted = permissions.overlay,
                    onClick = { openSettings { PermissionIntents.overlaySettings(context) } },
                )
            }

            PrimaryButton(
                text = "Check",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                },
                onClick = onRefresh,
            )
        }
    }
}

@Composable
fun DashboardScreen(
    lockedCount: Int,
    onOpenAppLock: () -> Unit,
    onOpenKeyStore: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Everything",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = SoftText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tools...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Stroke.copy(alpha = 0.9f),
                    focusedTextColor = SoftText,
                    unfocusedTextColor = SoftText,
                    cursorColor = Cyan,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(20.dp)
            )

            // ── Tool grid items ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // SECURITY TOOLS
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Security",
                        color = SoftText,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_app_lock,
                        title = "App Lock",
                        onClick = onOpenAppLock,
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_key_store,
                        title = "Key Store",
                        onClick = onOpenKeyStore,
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_secure_notes,
                        title = "Notes",
                        onClick = onOpenNotes,
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_file_vault,
                        title = "File Vault",
                        onClick = { /* Placeholder */ },
                    )
                }

                // PRODUCTIVITY & TOOLS
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Productivity",
                        color = SoftText,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_todo_tracker,
                        title = "Habit",
                        onClick = { /* Placeholder */ },
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_expense_tracker,
                        title = "Expenses",
                        onClick = onOpenExpenses,
                    )
                }
                item {
                    ToolGridItem(
                        iconResId = com.everything.app.R.drawable.ic_notes_editor,
                        title = "Editor",
                        onClick = { /* Placeholder */ },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolGridItem(
    iconResId: Int,
    title: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .glassSurface(
                    shape = RoundedCornerShape(18.dp),
                    selected = false,
                    tintStrength = 0.16f,
                    shadowElevation = 2f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = SoftText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AppLockScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSelectionChanged: () -> Unit,
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var unlocked by remember { mutableStateOf(false) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    val toolLocked by container.secureSettingRepository
        .observeBoolean(SecureSettingRepository.KEY_TOOL_LOCK_APP_LOCK)
        .collectAsStateWithLifecycle(initialValue = true)
    val isToolLocked = toolLocked != false
    val lockedApps by container.appLockRepository
        .observeLockedApps()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val lockedPackages = remember(lockedApps) { lockedApps.map { it.packageName }.toSet() }

    LaunchedEffect(Unit) {
        installedApps = container.installedAppProvider.loadLaunchableApps()
    }

    LaunchedEffect(isToolLocked) {
        if (!isToolLocked) {
            unlocked = true
            unlockPin = ""
            unlockError = null
        } else {
            unlocked = false
        }
    }

    if (isToolLocked && !unlocked) {
        UtilityUnlockScreen(
            title = "App Lock",
            subtitle = "Enter master PIN to manage locked apps",
            pin = unlockPin,
            error = unlockError,
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
        )
        return
    }

    val filteredApps = remember(installedApps, query, lockedPackages) {
        installedApps.orEmpty().filter { app ->
            query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }.sortedWith(
            compareByDescending<InstalledApp> { it.packageName in lockedPackages }
                .thenBy { it.label.lowercase() }
        )
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Lock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${lockedApps.size} apps protected",
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
        ) { padding ->
            if (installedApps == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Cyan.copy(alpha = 0.85f)) },
                                placeholder = { Text("Search apps") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Cyan.copy(alpha = 0.8f),
                                    unfocusedBorderColor = SoftText.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Cyan,
                                    focusedPlaceholderColor = MutedText,
                                    unfocusedPlaceholderColor = MutedText,
                                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text = "Apps",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                        }
                    }
                }

                    items(filteredApps, key = { it.packageName }) { app ->
                        AppSelectionRow(
                            app = app,
                            checked = app.packageName in lockedPackages,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    container.appLockRepository.setLocked(
                                        packageName = app.packageName,
                                        label = app.label,
                                        locked = checked,
                                    )
                                    onSelectionChanged()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UtilityUnlockScreen(
    title: String,
    subtitle: String,
    pin: String,
    error: String?,
    onBack: () -> Unit,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
) {
    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                BrandHeader(
                    icon = Icons.Rounded.Lock,
                    title = title,
                    subtitle = subtitle,
                )
                SecureTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = "Master PIN",
                )
                error?.let {
                    Text(text = it, color = Color(0xFFFFA8A8), style = MaterialTheme.typography.bodyMedium)
                }
            }

            PrimaryButton(
                text = "Unlock",
                enabled = pin.length >= 4,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                },
                onClick = onUnlock,
            )
        }
    }
}

@Composable
private fun AppSurface(content: @Composable () -> Unit) {
    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun BrandHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = SoftText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Cyan.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = SoftText,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = SoftText.copy(alpha = 0.18f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Cyan,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        ),
    )
}

@Composable
private fun StatusPanel(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(20.dp), selected = false, shadowElevation = 2f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(20.dp), selected = granted, shadowElevation = 2f)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (granted) Cyan else SoftText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (granted) "Granted" else "Open",
            color = if (granted) Cyan else SoftText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AppSelectionRow(
    app: InstalledApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), selected = checked, shadowElevation = 2f)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.label.firstOrNull()?.uppercase() ?: "#",
            color = if (checked) Cyan else SoftText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (checked) "Locked app" else "Unlocked",
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable { onCheckedChange(!checked) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (checked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = if (checked) "Unlock ${app.label}" else "Lock ${app.label}",
                tint = Cyan,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}
