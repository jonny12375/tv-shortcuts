@file:OptIn(ExperimentalTvMaterial3Api::class)

package dev.jonathanclark.tvshortcuts.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun surfaceColors(base: Color = Palette.Surface) = ClickableSurfaceDefaults.colors(
    containerColor = base,
    contentColor = Palette.Text,
    focusedContainerColor = Palette.SurfaceRaised,
    focusedContentColor = Palette.Text,
    pressedContainerColor = Palette.SurfaceRaised,
    pressedContentColor = Palette.Text,
)

@Composable
fun focusBorder(radius: Dp) = ClickableSurfaceDefaults.border(
    focusedBorder = Border(BorderStroke(3.dp, Palette.Backlight), shape = RoundedCornerShape(radius)),
)

/** The remote button a card stands for, drawn like a small keycap. */
@Composable
fun KeyChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Palette.Text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC11161F))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** An app's banner or icon, or a text placeholder ("?" / "+") when there is none. */
@Composable
fun ArtBox(art: AppArt?, placeholder: String, modifier: Modifier = Modifier, dim: Boolean = false) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            art != null && art.isBanner -> Image(
                bitmap = art.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (dim) 0.45f else 1f,
                modifier = Modifier.fillMaxSize(),
            )
            art != null -> Box(Modifier.fillMaxSize().background(Palette.SurfaceRaised), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = art.image,
                    contentDescription = null,
                    alpha = if (dim) 0.45f else 1f,
                    modifier = Modifier.fillMaxSize(0.5f),
                )
            }
            else -> Text(placeholder, fontSize = 40.sp, color = Palette.Muted, fontWeight = FontWeight.Light)
        }
    }
}

/** A live shortcut: the chosen app's banner, with the remote button named on top. */
@Composable
fun ShortcutCard(item: ButtonItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val art by rememberAppArt(item.targetPackage)
    val shape = RoundedCornerShape(20.dp)
    val empty = item.targetLabel == null
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = surfaceColors(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = focusBorder(20.dp),
    ) {
        Box(
            Modifier.fillMaxSize().then(
                if (empty) Modifier.drawBehind {
                    drawRoundRect(
                        color = Palette.Muted,
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))),
                    )
                } else Modifier,
            ),
        ) {
            ArtBox(art, placeholder = if (empty && item.targetPackage == null) "+" else "?", modifier = Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE611161F))))
                    .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 12.dp),
            ) {
                Text(
                    text = when {
                        item.targetPackage == null -> "Choose an app"
                        item.targetLabel == null -> "App not installed"
                        else -> item.targetLabel
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.targetPackage != null && item.targetLabel == null) Palette.Problem else Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            KeyChip(item.button.label, Modifier.align(Alignment.TopStart).padding(12.dp))
        }
    }
}

/** A flat, informational row (buttons that aren't live shortcuts). */
@Composable
fun InfoRow(
    art: AppArt?,
    placeholder: String,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    actionLabel: String,
    onClick: () -> Unit,
    dim: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = surfaceColors(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = focusBorder(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtBox(
                art, placeholder, dim = dim,
                modifier = Modifier.size(width = 112.dp, height = 63.dp).clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceRaised),
            )
            Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 18.sp, color = subtitleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(actionLabel, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Palette.Muted)
        }
    }
}

class DialogAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** A small focus-first menu. Every action closes the dialog before running. */
@Composable
fun ActionDialog(
    title: String,
    message: String?,
    actions: List<DialogAction>,
    onDismiss: () -> Unit,
    /** Optional content shown to the right of the message (for example a QR code). */
    aside: (@Composable () -> Unit)? = null,
    width: Dp = 560.dp,
) {
    // usePlatformDefaultWidth = false so the width we ask for is the width we get.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val first = remember { FocusRequester() }
        Column(
            Modifier.width(width).clip(RoundedCornerShape(28.dp)).background(Palette.Surface).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            if (message != null || aside != null) {
                Row(
                    Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (message != null) Text(message, fontSize = 18.sp, color = Palette.Muted, modifier = Modifier.weight(1f))
                    aside?.invoke()
                }
            }
            actions.forEachIndexed { i, action ->
                Surface(
                    onClick = { onDismiss(); action.onClick() },
                    modifier = Modifier.fillMaxWidth().then(if (i == 0) Modifier.focusRequester(first) else Modifier),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors = surfaceColors(Palette.SurfaceRaised),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                    border = focusBorder(14.dp),
                ) {
                    Text(
                        action.label,
                        fontSize = 20.sp,
                        color = if (action.destructive) Palette.Problem else Palette.Text,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
            }
        }
        LaunchedEffect(Unit) { first.requestFocus() }
    }
}
