package dev.jonathanclark.tvshortcuts.hub

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

/** Two D-pad friendly lists: pick a button, then pick the app it should open. */
class MainActivity : Activity() {
    /** [pkg] == null means "do nothing". */
    private class AppChoice(val label: String, val pkg: String?) {
        override fun toString() = label
    }

    private lateinit var list: ListView
    private var editing: Targets.Button? = null // null while showing the button list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = ListView(this).apply { setPadding(96, 48, 96, 48) }
        setContentView(list)
        showButtons()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (editing != null) showButtons() else super.onBackPressed()
    }

    private fun appLabel(pkg: String?): String {
        if (pkg == null) return "not set"
        return try {
            "${packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))}  ($pkg)"
        } catch (e: PackageManager.NameNotFoundException) {
            "$pkg  (not installed)"
        }
    }

    private fun isInstalled(pkg: String) = try {
        packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun showButtons() {
        editing = null
        title = "TV Shortcuts"
        val rows = Targets.buttons.map { b ->
            val note = if (isInstalled(b.standInPackage)) "" else "   [stand-in APK not installed]"
            "${b.label}\n  opens: ${appLabel(Targets.get(this, b.id))}$note"
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        list.setOnItemClickListener { _, _, position, _ -> showApps(Targets.buttons[position]) }
        list.requestFocus()
    }

    private fun showApps(button: Targets.Button) {
        editing = button
        title = button.label

        // Never point a button at one of the stand-ins (or ourselves).
        val exclude = Targets.buttons.map { it.standInPackage }.toSet() + packageName

        val seen = HashSet<String>()
        val apps = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { category ->
                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
            }
            .filter { it.activityInfo.packageName !in exclude && seen.add(it.activityInfo.packageName) }
            .map { AppChoice("${it.loadLabel(packageManager)}  (${it.activityInfo.packageName})", it.activityInfo.packageName) }
            .sortedBy { it.label.lowercase() }

        val choices = listOf(AppChoice("(do nothing)", null)) + apps
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, choices)
        list.setOnItemClickListener { _, _, position, _ ->
            Targets.set(this, button.id, choices[position].pkg)
            showButtons()
        }
        list.requestFocus()
    }
}
