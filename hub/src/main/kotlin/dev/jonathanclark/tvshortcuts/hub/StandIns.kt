package dev.jonathanclark.tvshortcuts.hub

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** What currently owns the package name Sony's firmware looks for. */
enum class StandInState {
    /** Nothing there: the stand-in APK just needs installing. */
    NOT_INSTALLED,

    /** Our stand-in (same signing certificate as the hub). */
    STAND_IN,

    /** The genuine app, installed by the user. Uninstalling it frees the name. */
    REAL_APP,

    /**
     * A system app (possibly already removed for this user). Android keeps its
     * signature on record, so a stand-in can never be installed over it.
     */
    SYSTEM_APP,
}

class StandInStatus(val state: StandInState, val appLabel: String?, val installedForUser: Boolean)

object StandIns {
    @Suppress("DEPRECATION")
    fun check(context: Context, pkg: String): StandInStatus {
        val pm = context.packageManager
        val info = try {
            // MATCH_UNINSTALLED_PACKAGES so system apps removed for this user still show up.
            pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: PackageManager.NameNotFoundException) {
            return StandInStatus(StandInState.NOT_INSTALLED, null, false)
        }
        val installed = info.flags and ApplicationInfo.FLAG_INSTALLED != 0
        val label = pm.getApplicationLabel(info).toString()
        val state = when {
            info.flags and ApplicationInfo.FLAG_SYSTEM != 0 -> StandInState.SYSTEM_APP
            !installed -> StandInState.NOT_INSTALLED
            pm.checkSignatures(context.packageName, pkg) == PackageManager.SIGNATURE_MATCH -> StandInState.STAND_IN
            else -> StandInState.REAL_APP
        }
        return StandInStatus(state, label, installed)
    }
}
