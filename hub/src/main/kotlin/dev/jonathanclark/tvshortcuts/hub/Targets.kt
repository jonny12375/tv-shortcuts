package dev.jonathanclark.tvshortcuts.hub

import android.content.Context

/** The remote-control buttons we can remap, and where their choice is stored. */
object Targets {
    /**
     * @property standInPackage package name Sony's firmware looks for when this key is pressed.
     */
    class Button(val id: String, val label: String, val standInPackage: String)

    val buttons = listOf(
        Button("disney", "Disney+ button", "com.disney.disneyplus"),
        Button("crunchyroll", "Crunchyroll button", "com.crunchyroll.crunchyroid"),
        Button("prime", "Prime Video button", "com.amazon.amazonvideo.livingroom"),
    )

    private const val PREFS = "targets"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The package to launch for this button, or null if unset. */
    fun get(c: Context, buttonId: String): String? = prefs(c).getString(buttonId, null)

    fun set(c: Context, buttonId: String, pkg: String?) {
        prefs(c).edit().apply {
            if (pkg == null) remove(buttonId) else putString(buttonId, pkg)
        }.apply()
    }
}
