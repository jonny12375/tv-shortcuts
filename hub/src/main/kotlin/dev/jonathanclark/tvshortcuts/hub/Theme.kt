@file:OptIn(ExperimentalTvMaterial3Api::class)

package dev.jonathanclark.tvshortcuts.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * A dusky blue-grey rather than near-black, with amber reserved for focus only
 * (think of a backlit remote key) so it always means "this is selected".
 */
object Palette {
    val Dusk = Color(0xFF1A2130)
    val Surface = Color(0xFF232C3D)
    val SurfaceRaised = Color(0xFF2E3A50)
    val Text = Color(0xFFE9EDF4)
    val Muted = Color(0xFF9BA7BA)
    val Backlight = Color(0xFFF2B24A)
    val Ok = Color(0xFF63C5A7)
    val Problem = Color(0xFFFF8F7E)
    val Blocked = Color(0xFF7C879A)
}

@Composable
fun HubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Backlight,
            onPrimary = Color(0xFF2A1D00),
            background = Palette.Dusk,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.SurfaceRaised,
            onSurfaceVariant = Palette.Muted,
            error = Palette.Problem,
        ),
    ) {
        // Text outside a Surface otherwise defaults to a dark content colour.
        CompositionLocalProvider(LocalContentColor provides Palette.Text, content = content)
    }
}
