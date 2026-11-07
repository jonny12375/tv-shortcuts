package dev.jonathanclark.tvshortcuts.hub

import android.content.Context
import android.content.Intent

/** Where a button lives in the UI, derived from who owns its package name. */
enum class Category { SHORTCUTS, NOT_SET_UP, LOCKED }

class ButtonItem(
    val button: Targets.Button,
    val status: StandInStatus,
    val targetPackage: String?,
    /** Label of the chosen app, or null if none is chosen or it's no longer installed. */
    val targetLabel: String?,
) {
    val category: Category
        get() = when (status.state) {
            StandInState.STAND_IN -> Category.SHORTCUTS
            StandInState.NOT_INSTALLED, StandInState.REAL_APP -> Category.NOT_SET_UP
            StandInState.SYSTEM_APP -> Category.LOCKED
        }
}

class AppEntry(val pkg: String, val label: String)

fun loadItems(context: Context): List<ButtonItem> {
    val pm = context.packageManager
    return Targets.buttons(context).map { b ->
        val target = Targets.get(context, b.id)
        val label = target?.let {
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrNull()
        }
        ButtonItem(b, StandIns.check(context, b.standInPackage), target, label)
    }
}

/** Apps a button can open: everything launchable, minus the stand-ins and the hub itself. */
fun loadLaunchableApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val exclude = Targets.buttons(context).map { it.standInPackage }.toSet() + context.packageName
    val seen = HashSet<String>()
    return listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        .flatMap { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
        .filter { it.activityInfo.packageName !in exclude && seen.add(it.activityInfo.packageName) }
        .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .sortedBy { it.label.lowercase() }
}
