package com.everything.app.core.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DeepBackground = Color(0xFF050914)
val Panel = Color(0xFF0C1326)
val PanelAlt = Color(0xFF111A33)
val Indigo = Color(0xFF101A4D)
val Cyan = Color(0xFF18E0D2)
val Teal = Color(0xFF17BFAE)
val SoftText = Color(0xFFC8D1E8)
val MutedText = Color(0xFF7D88A8)
val Stroke = Color(0xFF263152)

private val EverythingColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF001716),
    background = DeepBackground,
    onBackground = Color.White,
    surface = Panel,
    onSurface = Color.White,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = SoftText,
    outline = Stroke,
)

@Composable
fun EverythingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EverythingColors,
        content = {
            Surface(color = DeepBackground, content = content)
        },
    )
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
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
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
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
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
            .background(Stroke.copy(alpha = 0.7f)),
    )
}
