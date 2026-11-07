package dev.jonathanclark.tvshortcuts.hub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** An app's TV banner if it has one (what Google TV's home row shows), otherwise its icon. */
class AppArt(val image: ImageBitmap, val isBanner: Boolean)

object AppIcons {
    private val cache = ConcurrentHashMap<String, AppArt>()

    suspend fun load(context: Context, pkg: String): AppArt? {
        cache[pkg]?.let { return it }
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val banner = runCatching { pm.getApplicationBanner(pkg) }.getOrNull()
            val art = when {
                banner != null -> AppArt(banner.toBitmap().asImageBitmap(), isBanner = true)
                else -> runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                    ?.let { AppArt(it.toBitmap().asImageBitmap(), isBanner = false) }
            }
            art?.also { cache[pkg] = it }
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        val w = intrinsicWidth.takeIf { it > 0 } ?: 320
        val h = intrinsicHeight.takeIf { it > 0 } ?: 180
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            setBounds(0, 0, w, h)
            draw(Canvas(it))
        }
    }
}

@Composable
fun rememberAppArt(pkg: String?): State<AppArt?> {
    val context = LocalContext.current
    return produceState<AppArt?>(null, pkg) { value = pkg?.let { AppIcons.load(context, it) } }
}
