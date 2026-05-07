package com.rudra.everything.core.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class AppTheme {
    SKY_BLUE,
    ZINC_ROSE,
    SPACE_BLACK,
}

const val PREF_APP_THEME = "app_theme"
const val PREF_GLASS_OPACITY = "glass_opacity"
const val PREF_GLASS_BLUR = "glass_blur"

private const val DEFAULT_GLASS_OPACITY = 42f
private const val DEFAULT_GLASS_BLUR = 68f

data class GlassMorphSettings(
    val opacity: Float = DEFAULT_GLASS_OPACITY,
    val blur: Float = DEFAULT_GLASS_BLUR,
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme.SPACE_BLACK }
val LocalGlassMorphSettings = staticCompositionLocalOf { GlassMorphSettings() }

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
    shadowElevation: Float = 4f,
): Modifier {
    val glass = LocalGlassMorphSettings.current
    val opacity = glass.opacity.coerceIn(0f, 100f) / 100f
    val blur = glass.blur.coerceIn(0f, 100f) / 100f
    val elevationWeight = shadowElevation.coerceIn(0f, 8f) / 8f
    val tint = if (selected) Cyan else Teal
    val selectedBoost = if (selected) 0.045f else 0f
    val frostAlpha = (0.035f + opacity * 0.18f + blur * 0.055f + tintStrength * 0.08f + selectedBoost).coerceIn(0.035f, 0.24f)
    val topLightAlpha = (0.10f + blur * 0.16f + selectedBoost).coerceIn(0.10f, 0.30f)
    val middleClearAlpha = (0.014f + opacity * 0.035f + blur * 0.018f).coerceIn(0.014f, 0.08f)
    val bottomShadeAlpha = (0.018f + opacity * 0.04f + elevationWeight * 0.018f).coerceIn(0.018f, 0.08f)
    val rimAlpha = (0.16f + blur * 0.22f + selectedBoost).coerceIn(0.16f, 0.44f)
    val tintAlpha = if (selected) {
        (0.035f + tintStrength * 0.45f).coerceIn(0.035f, 0.09f)
    } else {
        (tintStrength * 0.018f * opacity).coerceIn(0f, 0.016f)
    }
    val shineAlpha = (0.035f + blur * 0.15f).coerceIn(0.035f, 0.19f)

    return this
        .clip(shape)
        .drawWithCache {
            val verticalGlass = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = topLightAlpha),
                    Color.White.copy(alpha = frostAlpha),
                    Color.White.copy(alpha = middleClearAlpha),
                    Color.Black.copy(alpha = bottomShadeAlpha),
                )
            )
            val topShine = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = shineAlpha),
                    Color.Transparent,
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, 0f),
                radius = size.width * 0.95f,
            )
            onDrawBehind {
                drawRect(verticalGlass)
                drawRect(tint.copy(alpha = tintAlpha))
                drawRect(topShine)
            }
        }
        .border(0.7.dp, Color.White.copy(alpha = rimAlpha), shape)
}

@Composable
fun GlassFilterButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .glassSurface(shape = shape, selected = selected, tintStrength = 0.08f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) SoftText else MutedText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
                        PanelAlt.copy(alpha = 0.18f),
                        DeepBackground.copy(alpha = 0.88f),
                        DeepBackground,
                    )
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF22D3EE).copy(alpha = 0.075f),
                        Color.Transparent,
                        Color(0xFFF43F5E).copy(alpha = 0.052f),
                    )
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFA78BFA).copy(alpha = 0.040f),
                        Color.Transparent,
                        Color(0xFF34D399).copy(alpha = 0.030f),
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
    var themeName by remember { mutableStateOf(sharedPrefs.getString(PREF_APP_THEME, AppTheme.SPACE_BLACK.name) ?: AppTheme.SPACE_BLACK.name) }
    var glassOpacity by remember { mutableStateOf(sharedPrefs.getFloat(PREF_GLASS_OPACITY, DEFAULT_GLASS_OPACITY)) }
    var glassBlur by remember { mutableStateOf(sharedPrefs.getFloat(PREF_GLASS_BLUR, DEFAULT_GLASS_BLUR)) }
    
    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                PREF_APP_THEME -> themeName = prefs.getString(PREF_APP_THEME, AppTheme.SPACE_BLACK.name) ?: AppTheme.SPACE_BLACK.name
                PREF_GLASS_OPACITY -> glassOpacity = prefs.getFloat(PREF_GLASS_OPACITY, DEFAULT_GLASS_OPACITY)
                PREF_GLASS_BLUR -> glassBlur = prefs.getFloat(PREF_GLASS_BLUR, DEFAULT_GLASS_BLUR)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val currentTheme = try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SPACE_BLACK }
    val glassSettings = GlassMorphSettings(
        opacity = glassOpacity,
        blur = glassBlur,
    )
    
    CompositionLocalProvider(
        LocalAppTheme provides currentTheme,
        LocalGlassMorphSettings provides glassSettings,
    ) {
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
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
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
        Text(text = text, fontWeight = FontWeight.SemiBold, style = textStyle)
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
