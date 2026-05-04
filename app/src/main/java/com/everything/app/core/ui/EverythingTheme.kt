package com.everything.app.core.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppTheme {
    SKY_BLUE,
    ZINC_ROSE,
    SPACE_BLACK,
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.SPACE_BLACK }

val Cyan @Composable get() = when (LocalAppTheme.current) {
    AppTheme.SKY_BLUE -> Color(0xFF38BDF8)
    AppTheme.ZINC_ROSE -> Color(0xFFF43F5E)
    AppTheme.SPACE_BLACK -> Color(0xFFE5E7EB)
}
val Teal @Composable get() = when (LocalAppTheme.current) {
    AppTheme.SKY_BLUE -> Color(0xFF7DD3FC)
    AppTheme.ZINC_ROSE -> Color(0xFFFB7185)
    AppTheme.SPACE_BLACK -> Color(0xFF9CA3AF)
}
val DeepBackground @Composable get() = Color(0xFF09090B)
val Panel @Composable get() = Color(0xFF18181B)
val PanelAlt @Composable get() = Color(0xFF27272A)
val SoftText @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFFF1F5F9) else Color(0xFFFAFAFA)
val MutedText @Composable get() = when (LocalAppTheme.current) {
    AppTheme.SKY_BLUE -> Color(0xFF64748B)
    AppTheme.ZINC_ROSE -> Color(0xFF71717A)
    AppTheme.SPACE_BLACK -> Color(0xFFA1A1AA)
}
val Stroke @Composable get() = Color(0xFF27272A)
val Amber @Composable get() = Cyan
val DangerRed @Composable get() = if (LocalAppTheme.current == AppTheme.SKY_BLUE) Color(0xFFF87171) else Color(0xFFF43F5E)

val AmberMuted @Composable get() = Cyan.copy(alpha = 0.15f)
val DangerRedMuted @Composable get() = DangerRed.copy(alpha = 0.2f)
val Indigo @Composable get() = Color(0xFF3F3F46)
val NavyBlue @Composable get() = PanelAlt
val CardGlow @Composable get() = Teal.copy(alpha = 0.5f)

@Composable
fun Modifier.glassSurface(
    shape: RoundedCornerShape,
    selected: Boolean = false,
    tintStrength: Float = 0.10f,
    @Suppress("UNUSED_PARAMETER") shadowElevation: Float = 4f,
): Modifier {
    val tint = if (selected) Cyan else Teal
    
    // Smooth gradient for light reflection (glass shine)
    val topColor = if (selected) tint.copy(alpha = tintStrength + 0.12f) else Color.White.copy(alpha = 0.10f)
    val bottomColor = if (selected) tint.copy(alpha = tintStrength + 0.02f) else Color.White.copy(alpha = 0.02f)
    
    // Subtle edge highlight (rim lighting)
    val edgeColor = if (selected) tint.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)

    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(topColor, bottomColor)
            )
        )
        .border(0.5.dp, edgeColor, shape)
}

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelAlt.copy(alpha = 0.14f),
                        DeepBackground.copy(alpha = 0.98f),
                        DeepBackground,
                    )
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Teal.copy(alpha = 0.045f),
                        Color.Transparent,
                        Cyan.copy(alpha = 0.025f),
                    )
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.018f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.008f),
                    )
                )
            ),
    ) {
        content()
    }
}

@Composable
fun EverythingTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var themeName by remember { mutableStateOf(sharedPrefs.getString("app_theme", AppTheme.SPACE_BLACK.name) ?: AppTheme.SPACE_BLACK.name) }
    
    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "app_theme") {
                themeName = prefs.getString("app_theme", AppTheme.SPACE_BLACK.name) ?: AppTheme.SPACE_BLACK.name
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val currentTheme = try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SPACE_BLACK }
    
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
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cyan,
            disabledContainerColor = PanelAlt,
            contentColor = Color(0xFF001716),
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        leadingIcon?.invoke()
        Text(text = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) Stroke else Stroke.copy(alpha = 0.5f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Cyan,
            disabledContentColor = MutedText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        leadingIcon?.invoke()
        Text(text = text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
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
