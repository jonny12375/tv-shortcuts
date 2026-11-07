package dev.jonathanclark.tvshortcuts.hub

import android.content.Context
import org.json.JSONArray

/** The remote-control buttons we can remap, and where their choice is stored. */
object Targets {
    /**
     * @property standInPackage package name Sony's firmware looks for when this key is pressed.
     */
    class Button(val id: String, val label: String, val standInPackage: String)

    private var cached: List<Button>? = null

    /** Loaded from the same config/buttons.json the stand-in flavors are generated from. */
    @Synchronized
    fun buttons(context: Context): List<Button> = cached ?: run {
        val json = context.assets.open("buttons.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Button(o.getString("id"), o.getString("label"), o.getString("package"))
        }.also { cached = it }
    }

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
