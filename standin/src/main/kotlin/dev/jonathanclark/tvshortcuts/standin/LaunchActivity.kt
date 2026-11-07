package dev.jonathanclark.tvshortcuts.standin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Invisible trampoline: asks the hub which app this button should open,
 * starts it, and finishes. If nothing is configured, opens the hub on this button's picker.
 */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            launch()
        } catch (e: RuntimeException) {
            Log.e(TAG, "launch failed", e)
            Toast.makeText(this, "Shortcut failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun launch() {
        val target = lookupTarget()
        Log.i(TAG, "button=${BuildConfig.BUTTON_ID} target=$target")

        var intent: Intent? = null
        if (target != null && target != packageName) {
            intent = packageManager.getLeanbackLaunchIntentForPackage(target)
                ?: packageManager.getLaunchIntentForPackage(target)
        }
        if (intent == null) {
            // Nothing usable is set for this button (or the chosen app is gone): open the hub
            // straight at this button's app picker.
            intent = packageManager.getLeanbackLaunchIntentForPackage(HUB_PACKAGE)
            if (intent == null) {
                Toast.makeText(this, "TV Shortcuts hub app is not installed", Toast.LENGTH_LONG).show()
                return
            }
            intent.putExtra(EXTRA_BUTTON_ID, BuildConfig.BUTTON_ID)
        }
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun lookupTarget(): String? = try {
        contentResolver.query(TARGETS.buildUpon().appendPath(BuildConfig.BUTTON_ID).build(), null, null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: RuntimeException) {
        Log.w(TAG, "hub provider unavailable", e)
        null
    }

    private companion object {
        const val TAG = "TvShortcuts"
        const val HUB_PACKAGE = "dev.jonathanclark.tvshortcuts.hub"

        /** Read by the hub's MainActivity; keep the two in sync. */
        const val EXTRA_BUTTON_ID = "dev.jonathanclark.tvshortcuts.EXTRA_BUTTON_ID"
        val TARGETS: Uri = Uri.parse("content://dev.jonathanclark.tvshortcuts.hub.targets/")
    }
}
