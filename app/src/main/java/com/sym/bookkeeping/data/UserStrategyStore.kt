package com.sym.bookkeeping.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 用户导入的战法快照，落盘到 filesDir，启动与导入后刷新 [StrategyCatalog] 扩展列表。
 */
object UserStrategyStore {
    private const val FILE_NAME = "imported_strategies.json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun load(context: Context): List<StrategyGuide> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile) {
            StrategyCatalog.setImported(emptyList())
            return emptyList()
        }
        return runCatching {
            val text = file.readText(StandardCharsets.UTF_8)
            val type = object : TypeToken<List<StrategyGuide>>() {}.type
            val list: List<StrategyGuide> = gson.fromJson(text, type) ?: emptyList()
            StrategyCatalog.setImported(list)
            list
        }.getOrElse {
            StrategyCatalog.setImported(emptyList())
            emptyList()
        }
    }

    fun save(context: Context, strategies: List<StrategyGuide>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(gson.toJson(strategies), StandardCharsets.UTF_8)
        StrategyCatalog.setImported(strategies)
        _revision.value = _revision.value + 1
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
        StrategyCatalog.setImported(emptyList())
        _revision.value = _revision.value + 1
    }
}
