package dev.jonathanclark.tvshortcuts.hub

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Read-only provider the stand-in apps query:
 *   content://dev.jonathanclark.tvshortcuts.hub.targets/{buttonId}  ->  one row, column "package"
 * Returns zero rows when nothing is configured.
 */
class TargetProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("package"))
        val buttonId = uri.lastPathSegment
        val ctx = context
        if (buttonId != null && ctx != null) {
            Targets.get(ctx, buttonId)?.let { cursor.addRow(arrayOf<Any>(it)) }
        }
        return cursor
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/vnd.dev.jonathanclark.tvshortcuts.target"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException()
}
