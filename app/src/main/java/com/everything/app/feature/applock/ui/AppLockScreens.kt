package com.everything.app.feature.applock.ui

import android.content.ActivityNotFoundException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everything.app.AppContainer
import com.everything.app.core.permissions.AppLockPermissionState
import com.everything.app.core.permissions.PermissionIntents
import com.everything.app.core.ui.Cyan
import com.everything.app.core.ui.FullWidthDivider
import com.everything.app.core.ui.GradientButton
import com.everything.app.core.ui.MutedText
import com.everything.app.core.ui.Panel
import com.everything.app.core.ui.PanelAlt
import com.everything.app.core.ui.QuietButton
import com.everything.app.core.ui.SoftText
import com.everything.app.core.ui.Stroke
import com.everything.app.core.ui.Teal
import com.everything.app.feature.applock.domain.InstalledApp
import kotlinx.coroutines.launch

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

            GradientButton(
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
                    GradientButton(
                        text = "Enable",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        },
                        onClick = onEnable,
                    )
                }
                QuietButton(
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
    onRequestNotifications: () -> Unit,
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
                PermissionRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    granted = permissions.notifications,
                    onClick = onRequestNotifications,
                )
            }

            GradientButton(
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
) {
    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            BrandHeader(
                icon = Icons.Rounded.Shield,
                title = "Everything",
                subtitle = "Private tools",
            )

            Card(
                onClick = onOpenAppLock,
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Stroke),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Rounded.Lock)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Lock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "$lockedCount apps locked",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "Open",
                        color = Cyan,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSelectionChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    val lockedApps by container.appLockRepository
        .observeLockedApps()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val lockedPackages = remember(lockedApps) { lockedApps.map { it.packageName }.toSet() }

    LaunchedEffect(Unit) {
        installedApps = container.installedAppProvider.loadLaunchableApps()
    }

    val filteredApps = remember(installedApps, query) {
        installedApps.orEmpty().filter { app ->
            query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = SoftText)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${lockedApps.size} selected",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedText) },
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Stroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Cyan,
                    focusedPlaceholderColor = MutedText,
                    unfocusedPlaceholderColor = MutedText,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                ),
            )

            Spacer(Modifier.height(12.dp))

            if (installedApps == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
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
private fun AppSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(22.dp),
    ) {
        content()
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
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(Teal, Cyan))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF001716), modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Stroke,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Cyan,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = MutedText,
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel,
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
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Stroke, RoundedCornerShape(8.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Stroke, RoundedCornerShape(8.dp))
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (checked) Cyan.copy(alpha = 0.18f) else PanelAlt),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = app.label.firstOrNull()?.uppercase() ?: "#",
                    color = if (checked) Cyan else SoftText,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = app.packageName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF001716),
                    checkedTrackColor = Cyan,
                    uncheckedThumbColor = SoftText,
                    uncheckedTrackColor = PanelAlt,
                    uncheckedBorderColor = Stroke,
                ),
            )
        }
        FullWidthDivider()
    }
}
