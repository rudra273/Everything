package com.everything.app.core.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppTheme {
    SKY_BLUE,
    ZINC_ROSE
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.SKY_BLUE }

val Cyan @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF38BDF8) else Color(0xFFF43F5E)
val Teal @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF7DD3FC) else Color(0xFFFB7185)
val DeepBackground @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF0B0F19) else Color(0xFF09090B)
val Panel @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF111827) else Color(0xFF18181B)
val PanelAlt @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF1A2234) else Color(0xFF27272A)
val SoftText @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFFF1F5F9) else Color(0xFFFAFAFA)
val MutedText @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF64748B) else Color(0xFF71717A)
val Stroke @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF1E293B) else Color(0xFF27272A)
val Amber @Composable get() = Cyan
val DangerRed @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFFF87171) else Color(0xFFF43F5E)

val AmberMuted @Composable get() = Cyan.copy(alpha = 0.15f)
val DangerRedMuted @Composable get() = DangerRed.copy(alpha = 0.2f)
val Indigo @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFF243044) else Color(0xFF3F3F46)
val NavyBlue @Composable get() = PanelAlt
val CardGlow @Composable get() = Teal.copy(alpha = 0.5f)

@Composable
fun EverythingTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var themeName by remember { mutableStateOf(sharedPrefs.getString("app_theme", AppTheme.SKY_BLUE.name) ?: AppTheme.SKY_BLUE.name) }
    
    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "app_theme") {
                themeName = prefs.getString("app_theme", AppTheme.SKY_BLUE.name) ?: AppTheme.SKY_BLUE.name
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val currentTheme = try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SKY_BLUE }
    
    CompositionLocalProvider(LocalAppTheme provides currentTheme) {
        val colors = darkColorScheme(
            primary = Cyan,
            onPrimary = Color.White,
            secondary = Teal,
            onSecondary = Color(0xFF001716),
            background = DeepBackground,
            onBackground = SoftText,
            surface = Panel,
            onSurface = SoftText,
            surfaceVariant = PanelAlt,
            onSurfaceVariant = SoftText,
            outline = Stroke,
        )

        MaterialTheme(
            colorScheme = colors,
            content = {
                Surface(color = DeepBackground, content = content)
            },
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(Teal, Cyan))),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Stroke,
            contentColor = Color(0xFF001716),
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        leadingIcon?.invoke()
        Text(text = text)
    }
}

@Composable
fun QuietButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Stroke),
        colors = ButtonDefaults.buttonColors(
            containerColor = PanelAlt,
            contentColor = SoftText,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        leadingIcon?.invoke()
        Text(text = text)
    }
}

@Composable
fun FullWidthDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Stroke.copy(alpha = 0.5f)),
    )
}
