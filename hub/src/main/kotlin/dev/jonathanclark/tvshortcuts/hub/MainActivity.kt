package dev.jonathanclark.tvshortcuts.hub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    /** Bumped whenever something may have changed (resume, install result, uninstall dialog closed). */
    private var refresh by mutableIntStateOf(0)
    private var installPermissionHelp by mutableStateOf(false)
    private val uninstallQueue = ArrayDeque<String>()

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { nextUninstall() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            InstallEvents.results.collect {
                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                refresh++
            }
        }

        setContent {
            HubTheme {
                val items by produceState<List<ButtonItem>?>(null, refresh) {
                    value = withContext(Dispatchers.IO) { loadItems(this@MainActivity) }
                }
                HubScreen(
                    items = items,
                    showInstallPermissionHelp = installPermissionHelp,
                    onDismissInstallHelp = { installPermissionHelp = false },
                    onOpenInstallSettings = ::openInstallSettings,
                    onInstall = ::install,
                    onUninstall = ::uninstall,
                    onChoose = { button, pkg ->
                        Targets.set(this, button.id, pkg)
                        refresh++
                    },
                    onOpenLink = ::openLink,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
    }

    private fun install(button: Targets.Button) {
        if (!StandInInstaller.canInstall(this)) {
            installPermissionHelp = true
            return
        }
        try {
            StandInInstaller.install(this, button)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't install the stand-in: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Most TVs have no browser, so fall back to showing the address. */
    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "This TV has no browser. Open $url on another device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInstallSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this, "Open Settings > Apps > Security & restrictions > Unknown sources, and allow TV Shortcuts.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    /** Hands each package to the system's own uninstall confirmation, one after another. */
    private fun uninstall(packages: List<String>) {
        uninstallQueue.addAll(packages)
        nextUninstall()
    }

    private fun nextUninstall() {
        refresh++
        val pkg = uninstallQueue.removeFirstOrNull() ?: return
        deleteLauncher.launch(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }
}
