@file:OptIn(ExperimentalTvMaterial3Api::class)

package dev.jonathanclark.tvshortcuts.hub

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

private const val TAG = "TvShortcuts"

@Composable
fun AboutScreen(items: List<ButtonItem>?, onClose: () -> Unit, onOpenLink: (String) -> Unit) {
    var showDebug by remember { mutableStateOf(false) }
    if (showDebug) {
        DebugScreen(items, onClose = { showDebug = false })
        return
    }

    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val first = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(Palette.Dusk).padding(start = 48.dp, top = 36.dp, end = 48.dp)) {
        Text("About TV Shortcuts", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Text("Version ${appVersion(context)}", fontSize = 20.sp, color = Palette.Muted)
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("App button remapper for Google TV", fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Column {
                    Text("Visit the project on GitHub:", fontSize = 20.sp, color = Palette.Muted)
                    Text(PROJECT_URL.removePrefix("https://"), fontSize = 20.sp)
                }
                Text(
                    "If you run into an issue, be sure to attach the information on the debug info screen " +
                        "to any GitHub issues.",
                    fontSize = 20.sp, color = Palette.Muted,
                )
                Spacer(Modifier.height(8.dp))
                AboutButton("Show debug info", Modifier.focusRequester(first)) { showDebug = true }
                AboutButton("Open project page", Modifier) { onOpenLink(PROJECT_URL) }
            }
            QrCode(PROJECT_URL, size = 220.dp)
        }
    }
    LaunchedEffect(Unit) { first.requestFocus() }
}

@Composable
private fun AboutButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(360.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = surfaceColors(Palette.SurfaceRaised),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = focusBorder(14.dp),
    ) {
        Text(label, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
    }
}

/** Everything someone might paste into a bug report, in two columns so it fits one TV screen. */
@Composable
private fun DebugScreen(items: List<ButtonItem>?, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val info = remember(items) { collectDebugInfo(context, items) }
    // Also goes to the log, since typing a screenful off a TV is nobody's idea of fun.
    LaunchedEffect(info) { Log.i(TAG, "debug info\n" + info.asText()) }

    Column(Modifier.fillMaxSize().background(Palette.Dusk).padding(start = 48.dp, top = 36.dp, end = 48.dp)) {
        Text("Debug info", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Include this in a bug report. It's also written to the log: adb logcat -s $TAG",
            fontSize = 20.sp, color = Palette.Muted,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            DebugColumn("Device", info.device, Modifier.weight(1f))
            DebugColumn("Buttons", info.buttons, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DebugColumn(title: String, rows: List<Pair<String, String>>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        rows.forEach { (key, value) ->
            Row {
                Text(key, fontSize = 16.sp, color = Palette.Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.width(200.dp))
                Text(value, fontSize = 16.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            }
        }
    }
}

class DebugInfo(val device: List<Pair<String, String>>, val buttons: List<Pair<String, String>>) {
    fun asText() = buildString {
        appendLine("-- Device")
        device.forEach { (k, v) -> appendLine("$k: $v") }
        appendLine("-- Buttons")
        buttons.forEach { (k, v) -> appendLine("$k: $v") }
    }
}

fun appVersion(context: Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "${info.versionName} (${info.longVersionCode})"
} catch (e: PackageManager.NameNotFoundException) {
    "unknown"
}

private fun packageVersion(pm: PackageManager, pkg: String): String = try {
    pm.getPackageInfo(pkg, 0).versionName ?: "installed"
} catch (e: PackageManager.NameNotFoundException) {
    "not found"
}

fun collectDebugInfo(context: Context, items: List<ButtonItem>?): DebugInfo {
    val pm = context.packageManager
    val device = listOf(
        "TV Shortcuts" to appVersion(context),
        "Stand-in ver." to BuildConfig.STANDIN_VERSION.toString(),
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Security patch" to (Build.VERSION.SECURITY_PATCH.ifEmpty { "unknown" }),
        "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Codename" to "${Build.DEVICE} / ${Build.PRODUCT}",
        "Build" to Build.DISPLAY,
        "Leanback" to pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK).toString(),
        "Google TV" to pm.hasSystemFeature("com.google.android.feature.AMATI_EXPERIENCE").toString(),
        "TV launcher" to packageVersion(pm, "com.google.android.apps.tv.launcherx"),
        "Sony service" to packageVersion(pm, "com.sony.dtv.sonysystemservice"),
        "Install apps" to if (StandInInstaller.canInstall(context)) "allowed" else "not allowed",
    )

    val buttons = buildList {
        if (items == null) {
            add("status" to "loading")
        } else {
            val (present, absent) = items.partition { it.status.state != StandInState.NOT_INSTALLED }
            present.forEach { add(it.button.label to describe(it.status)) }
            if (absent.isNotEmpty()) add("not set up" to "${absent.size} buttons")
        }
    }
    return DebugInfo(device, buttons)
}

private fun describe(status: StandInStatus) = when (status.state) {
    StandInState.STAND_IN ->
        "stand-in v${status.version}" + if (status.needsUpdate) " (update needed)" else ""
    StandInState.REAL_APP -> "real app installed"
    StandInState.SYSTEM_APP -> if (status.installedForUser) "system app" else "system app (removed)"
    StandInState.NOT_INSTALLED -> "not installed"
}
