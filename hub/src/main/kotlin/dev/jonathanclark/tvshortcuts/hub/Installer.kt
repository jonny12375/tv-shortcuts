package dev.jonathanclark.tvshortcuts.hub

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.flow.MutableSharedFlow

class InstallResult(val buttonId: String, val success: Boolean, val message: String)

/** Process-wide channel from the install broadcast back to the UI. */
object InstallEvents {
    val results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)
}

/** Installs the stand-in APKs bundled in assets/standins/<buttonId>.apk. */
object StandInInstaller {
    const val ACTION_RESULT = "dev.jonathanclark.tvshortcuts.hub.INSTALL_RESULT"
    const val EXTRA_BUTTON_ID = "buttonId"

    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun install(context: Context, button: Targets.Button) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(button.standInPackage) }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            context.assets.open("standins/${button.id}.apk").use { input ->
                session.openWrite("standin", 0, -1).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val intent = Intent(context, InstallResultReceiver::class.java)
                .setAction(ACTION_RESULT)
                .putExtra(EXTRA_BUTTON_ID, button.id)
            // Mutable: the installer adds status extras to this intent.
            val pending = PendingIntent.getBroadcast(
                context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val buttonId = intent.getStringExtra(StandInInstaller.EXTRA_BUTTON_ID) ?: return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // The system installer wants the user to confirm.
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS ->
                InstallEvents.results.tryEmit(InstallResult(buttonId, true, "Stand-in installed"))
            else -> {
                val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                InstallEvents.results.tryEmit(InstallResult(buttonId, false, "Couldn't install the stand-in: $detail"))
            }
        }
    }
}
