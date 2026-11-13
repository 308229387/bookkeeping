package com.sym.bookkeeping.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class TradeExportPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val records: List<TradeRecord> = emptyList()
)

class ImportExportHelper(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToJson(uri: Uri, records: List<TradeRecord>) {
        val payload = TradeExportPayload(records = records)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(gson.toJson(payload).toByteArray(StandardCharsets.UTF_8))
        } ?: error("无法写入导出文件")
    }

    fun importFromJson(uri: Uri): List<TradeRecord> {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        } ?: error("无法读取导入文件")

        return parseJson(text)
    }

    fun exportToCsv(uri: Uri, records: List<TradeRecord>) {
        val header = listOf(
            "id", "symbol", "method", "riskRewardRatio", "profit", "fee",
            "shareCount", "journal", "tradeDate", "createdAt", "updatedAt"
        ).joinToString(",")

        val lines = records.map { r ->
            listOf(
                r.id.toString(),
                csvEscape(r.symbol),
                csvEscape(r.method),
                r.riskRewardRatio.toString(),
                r.profit.toString(),
                r.fee.toString(),
                r.shareCount.toString(),
                csvEscape(r.journal),
                r.tradeDate.toString(),
                r.createdAt.toString(),
                r.updatedAt.toString()
            ).joinToString(",")
        }

        val content = (listOf(header) + lines).joinToString("\n")
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(StandardCharsets.UTF_8))
        } ?: error("无法写入导出文件")
    }

    fun importFromCsv(uri: Uri): List<TradeRecord> {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        } ?: error("无法读取导入文件")

        return parseCsv(text)
    }

    private fun parseJson(text: String): List<TradeRecord> {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val type = object : TypeToken<List<TradeRecord>>() {}.type
            return gson.fromJson(trimmed, type) ?: emptyList()
        }
        val payload = gson.fromJson(trimmed, TradeExportPayload::class.java)
        return payload?.records ?: emptyList()
    }

    private fun parseCsv(text: String): List<TradeRecord> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines.first()).map { it.lowercase() }
        val index = { name: String -> header.indexOf(name).takeIf { it >= 0 } }

        return lines.drop(1).mapNotNull { line ->
            val cols = parseCsvLine(line)
            if (cols.isEmpty()) return@mapNotNull null
            fun col(name: String, fallbackIndex: Int = -1): String {
                val i = index(name) ?: fallbackIndex
                return if (i in cols.indices) cols[i] else ""
            }
            TradeRecord(
                id = 0,
                symbol = col("symbol", 1),
                method = col("method", 2),
                riskRewardRatio = col("riskrewardratio", 3).toDoubleOrNull() ?: 0.0,
                profit = col("profit", 4).toDoubleOrNull() ?: 0.0,
                fee = col("fee", 5).toDoubleOrNull() ?: 0.0,
                shareCount = col("sharecount", 6).toIntOrNull() ?: 0,
                journal = col("journal", 7),
                tradeDate = col("tradedate", 8).toLongOrNull() ?: System.currentTimeMillis(),
                createdAt = col("createdat", 9).toLongOrNull() ?: System.currentTimeMillis(),
                updatedAt = col("updatedat", 10).toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    }

    private fun csvEscape(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
