@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)

package dev.jonathanclark.tvshortcuts.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val tabTitles = mapOf(
    Category.SHORTCUTS to "Shortcuts",
    Category.NOT_SET_UP to "Not set up",
    Category.LOCKED to "Can't be changed",
)

@Composable
fun HubScreen(
    items: List<ButtonItem>?,
    showInstallPermissionHelp: Boolean,
    onDismissInstallHelp: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onInstall: (Targets.Button) -> Unit,
    onUninstall: (List<String>) -> Unit,
    onChoose: (Targets.Button, String?) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    var tabName by rememberSaveable { mutableStateOf(Category.SHORTCUTS.name) }
    val tab = Category.valueOf(tabName)
    var showAddHelp by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<ButtonItem?>(null) }
    var picking by remember { mutableStateOf<ButtonItem?>(null) }
    var explain by remember { mutableStateOf<ButtonItem?>(null) }
    var confirmRemoveAll by remember { mutableStateOf(false) }
    // Tabs follow focus, so to switch tabs from inside a list we move focus to the tab itself.
    val tabFocus = remember { Category.entries.associateWith { FocusRequester() } }

    val byCategory = items.orEmpty().groupBy { it.category }
    val shortcuts = byCategory[Category.SHORTCUTS].orEmpty()

    val picked = picking
    run {
        if (picked != null) {
            PickerScreen(
                item = picked,
                onPick = { pkg -> onChoose(picked.button, pkg); picking = null },
                onClose = { picking = null },
            )
        } else {
            Column(Modifier.fillMaxSize().background(Palette.Dusk).padding(start = 48.dp, top = 36.dp, end = 48.dp)) {
                Text("TV Shortcuts", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose what each remote app button opens. Other remote buttons can't be remapped.",
                    fontSize = 20.sp, color = Palette.Muted,
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    // Focus entering the tab row (from a list below, say) must land on the current
                    // tab. Otherwise the geometrically nearest chip wins, and since tabs follow
                    // focus, that silently switches category.
                    modifier = Modifier
                        .focusProperties { enter = { tabFocus.getValue(tab) } }
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Category.entries.forEach { c ->
                        TabChip(
                            text = "${tabTitles.getValue(c)}  ${byCategory[c].orEmpty().size}",
                            selected = c == tab,
                            onFocus = { tabName = c.name },
                            modifier = Modifier.focusRequester(tabFocus.getValue(c)),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    Category.SHORTCUTS -> ShortcutsTab(shortcuts, loaded = items != null, onOpen = { menu = it }, onRemoveAll = { confirmRemoveAll = true })
                    Category.NOT_SET_UP -> InfoList(
                        header = "Install a stand-in to use one of these buttons. If your remote doesn't have " +
                            "the button, installing its shortcut won't do anything.",
                        entries = byCategory[Category.NOT_SET_UP].orEmpty(),
                        footer = {
                            NoteRow(
                                title = "Can't find the app you're looking for?",
                                subtitle = "It may be one of the buttons that can't be changed.",
                                actionLabel = "Check Can't be changed",
                                onClick = { tabFocus.getValue(Category.LOCKED).requestFocus() },
                            )
                        },
                    ) { item -> NotSetUpRow(item, onInstall, onUninstall) }
                    Category.LOCKED -> InfoList(
                        header = "These buttons open apps built into the TV, so they can't be remapped without root.",
                        entries = byCategory[Category.LOCKED].orEmpty(),
                        footer = {
                            NoteRow(
                                title = "Still can't find your app?",
                                subtitle = "This only remaps the remote's app buttons, not any other buttons.",
                                actionLabel = "Learn how to add it",
                                onClick = { showAddHelp = true },
                            )
                        },
                    ) { item -> LockedRow(item) { explain = item } }
                }
            }
        }
    }

    menu?.let { item ->
        ActionDialog(
            title = "${item.button.label} button",
            message = item.targetLabel?.let { "Opens $it" },
            actions = buildList {
                add(DialogAction("Change app") { picking = item })
                if (item.targetPackage != null) add(DialogAction("Don't open anything") { onChoose(item.button, null) })
                add(DialogAction("Remove shortcut", destructive = true) { onUninstall(listOf(item.button.standInPackage)) })
            },
            onDismiss = { menu = null },
        )
    }
    explain?.let { item ->
        ActionDialog(
            title = item.button.label,
            message = "${item.button.standInPackage} is a system app that ships with the TV. Android keeps its " +
                "signature on record even after it's removed for your user, so a stand-in with that name is " +
                "always rejected. Replacing it needs root or a modified system image.",
            actions = listOf(DialogAction("OK") {}),
            onDismiss = { explain = null },
        )
    }
    if (showAddHelp) {
        ActionDialog(
            title = "Adding an app",
            message = "TV Shortcuts can only remap the app buttons on the remote (Disney+, Crunchyroll and so on), " +
                "not any other buttons.\n\nTo add an app that isn't listed, scan this code with your phone or visit " +
                "github.com/jonny12375/tv-shortcuts.",
            actions = listOf(DialogAction("Close") {}, DialogAction("Open link") { onOpenLink(PROJECT_URL) }),
            aside = { QrCode(PROJECT_URL) },
            width = 760.dp,
            onDismiss = { showAddHelp = false },
        )
    }
    if (confirmRemoveAll) {
        ActionDialog(
            title = "Remove all shortcuts?",
            message = "Android asks you to confirm each one. The apps you chose are remembered if you install them again.",
            actions = listOf(
                DialogAction("Remove ${shortcuts.size} shortcuts", destructive = true) {
                    onUninstall(shortcuts.map { it.button.standInPackage })
                },
                DialogAction("Cancel") {},
            ),
            onDismiss = { confirmRemoveAll = false },
        )
    }
    if (showInstallPermissionHelp) {
        ActionDialog(
            title = "Allow installs from TV Shortcuts",
            message = "Turn on \"Install unknown apps\" for TV Shortcuts, then try again. You only need to do this once.",
            actions = listOf(DialogAction("Open settings") { onOpenInstallSettings() }, DialogAction("Cancel") {}),
            onDismiss = onDismissInstallHelp,
        )
    }
}


@Composable
private fun TabChip(text: String, selected: Boolean, onFocus: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onFocus,
        modifier = modifier.onFocusChanged { if (it.hasFocus) onFocus() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        colors = surfaceColors(if (selected) Palette.SurfaceRaised else Color.Transparent),
        border = focusBorder(24.dp),
    ) {
        Text(
            text,
            fontSize = 20.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Palette.Text else Palette.Muted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ShortcutsTab(shortcuts: List<ButtonItem>, loaded: Boolean, onOpen: (ButtonItem) -> Unit, onRemoveAll: () -> Unit) {
    if (loaded && shortcuts.isEmpty()) {
        Text(
            "No shortcuts yet. Install a stand-in from Not set up, then choose an app for it here.",
            fontSize = 20.sp, color = Palette.Muted, modifier = Modifier.padding(top = 8.dp),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(shortcuts, key = { it.button.id }) { item ->
            ShortcutCard(item, onClick = { onOpen(item) }, modifier = Modifier.aspectRatio(16f / 9f))
        }
        if (shortcuts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    onClick = onRemoveAll,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                    colors = surfaceColors(Color.Transparent),
                    border = focusBorder(14.dp),
                ) {
                    Text("Remove all shortcuts", fontSize = 18.sp, color = Palette.Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                }
            }
        }
    }
}

private const val PROJECT_URL = "https://github.com/jonny12375/tv-shortcuts"

@Composable
private fun InfoList(
    header: String,
    entries: List<ButtonItem>,
    footer: @Composable () -> Unit,
    row: @Composable (ButtonItem) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(header, fontSize = 20.sp, color = Palette.Muted, modifier = Modifier.padding(bottom = 4.dp)) }
        items(entries, key = { it.button.id }) { row(it) }
        item { footer() }
    }
}

/** A text-only row for pointing people somewhere else; no art, so it reads as navigation, not as a button. */
@Composable
private fun NoteRow(title: String, subtitle: String, actionLabel: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = surfaceColors(Color.Transparent),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = focusBorder(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 18.sp, color = Palette.Muted)
            }
            Text(actionLabel, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Palette.Muted)
        }
    }
}

@Composable
private fun NotSetUpRow(item: ButtonItem, onInstall: (Targets.Button) -> Unit, onUninstall: (List<String>) -> Unit) {
    val realApp = item.status.state == StandInState.REAL_APP
    val art by rememberAppArt(if (realApp) item.button.standInPackage else null)
    InfoRow(
        art = art,
        placeholder = "?",
        title = item.button.label,
        subtitle = if (realApp) "The real ${item.status.appLabel} app is installed. Uninstall it, then install the stand-in."
        else "No stand-in installed",
        subtitleColor = if (realApp) Palette.Problem else Palette.Muted,
        actionLabel = if (realApp) "Uninstall" else "Install",
        onClick = { if (realApp) onUninstall(listOf(item.button.standInPackage)) else onInstall(item.button) },
    )
}

@Composable
private fun LockedRow(item: ButtonItem, onClick: () -> Unit) {
    val art by rememberAppArt(item.button.standInPackage)
    InfoRow(
        art = art,
        placeholder = "?",
        title = item.button.label,
        subtitle = "Built into the TV. Android won't accept a stand-in for it.",
        subtitleColor = Palette.Blocked,
        actionLabel = "Why?",
        onClick = onClick,
        dim = true,
    )
}

@Composable
private fun PickerScreen(item: ButtonItem, onPick: (String?) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val apps by produceState<List<AppEntry>?>(null) { value = withContext(Dispatchers.IO) { loadLaunchableApps(context) } }
    val first = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(Palette.Dusk).padding(start = 48.dp, top = 36.dp, end = 48.dp)) {
        Text("${item.button.label} button opens", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Text("Choose an app.", fontSize = 20.sp, color = Palette.Muted)
        Spacer(Modifier.height(16.dp))
        apps?.let { list ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { PickTile(null, "Don't open anything", item.targetPackage == null, { onPick(null) }, Modifier.focusRequester(first)) }
                items(list, key = { it.pkg }) { app ->
                    PickTile(app.pkg, app.label, app.pkg == item.targetPackage, { onPick(app.pkg) })
                }
            }
            LaunchedEffect(Unit) { first.requestFocus() }
        }
    }
}

@Composable
private fun PickTile(pkg: String?, label: String, current: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val art by rememberAppArt(pkg)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
            colors = surfaceColors(),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            border = focusBorder(16.dp),
        ) {
            ArtBox(art, placeholder = "–", modifier = Modifier.fillMaxSize())
        }
        Text(label, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (current) Text("Current choice", fontSize = 16.sp, color = Palette.Ok)
    }
}
