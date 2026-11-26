package com.sym.bookkeeping.data.market

import android.content.Context
import com.google.gson.Gson
import java.io.File

/** 行情页上次成功拉到的完整快照，打开时先显示。 */
object LimitFocusCache {
    private const val FILE_NAME = "limit_focus_cache.json"
    private val gson = Gson()

    data class Snapshot(
        val updatedAt: Long = 0L,
        val focus: LimitFocusResult = LimitFocusResult(),
        val sectionReady: LimitSectionReady = LimitSectionReady()
    )

    fun load(context: Context): Snapshot? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile || file.length() == 0L) return null
        return runCatching {
            gson.fromJson(file.readText(), Snapshot::class.java)
        }.getOrNull()?.takeIf { it.focus.date.isNotBlank() || it.focus.upStocks.isNotEmpty() }
    }

    fun save(context: Context, snapshot: Snapshot) {
        val file = File(context.filesDir, FILE_NAME)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(gson.toJson(snapshot))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
