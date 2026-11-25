package com.sym.bookkeeping.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 完整备份（交易 + 复盘 + 推荐跟踪 + 战法）；图片在 ZIP 的 images/ 目录 */
data class BookkeepingBackup(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val records: List<TradeRecord> = emptyList(),
    val reviewLogs: List<ReviewLog> = emptyList(),
    val trackLogs: List<TrackLog> = emptyList(),
    val strategies: List<StrategyGuide> = emptyList()
)

/** 旧版仅交易 JSON */
data class TradeExportPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val records: List<TradeRecord> = emptyList()
)

data class BackupImportResult(
    val records: List<TradeRecord>,
    val reviewLogs: List<ReviewLog>,
    val trackLogs: List<TrackLog> = emptyList(),
    val strategies: List<StrategyGuide>,
    /** 相对路径 → 已写入 filesDir 的文件 */
    val imagePaths: List<String> = emptyList()
)

class ImportExportHelper(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportFullZip(
        uri: Uri,
        records: List<TradeRecord>,
        reviewLogs: List<ReviewLog>,
        trackLogs: List<TrackLog> = emptyList(),
        strategies: List<StrategyGuide>
    ) {
        context.contentResolver.openOutputStream(uri)?.use { raw ->
            writeZip(raw, records, reviewLogs, trackLogs, strategies)
        } ?: error("无法写入导出文件")
    }

    /** 写入 cache 目录，供系统分享面板使用 */
    fun exportFullZipToCacheFile(
        fileName: String,
        records: List<TradeRecord>,
        reviewLogs: List<ReviewLog>,
        trackLogs: List<TrackLog> = emptyList(),
        strategies: List<StrategyGuide>
    ): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { raw ->
            writeZip(raw, records, reviewLogs, trackLogs, strategies)
        }
        return file
    }

    private fun writeZip(
        raw: java.io.OutputStream,
        records: List<TradeRecord>,
        reviewLogs: List<ReviewLog>,
        trackLogs: List<TrackLog>,
        strategies: List<StrategyGuide>
    ) {
        val backup = BookkeepingBackup(
            records = records,
            reviewLogs = reviewLogs,
            trackLogs = trackLogs,
            strategies = strategies
        )
        val imagePaths = records.map { it.chartImagePath }.filter { it.isNotBlank() }.distinct()
        ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(gson.toJson(backup).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            for (path in imagePaths) {
                val bytes = readChartBytes(path) ?: continue
                zip.putNextEntry(ZipEntry("images/$path"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    fun importFullZip(uri: Uri): BackupImportResult {
        var backup: BookkeepingBackup? = null
        val writtenImages = mutableListOf<String>()

        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.trimStart('/')
                    when {
                        name == "data.json" || name.endsWith("/data.json") -> {
                            val text = zip.readBytes().toString(StandardCharsets.UTF_8)
                            backup = parseBackupJson(text)
                        }
                        name.startsWith("images/") && !entry.isDirectory -> {
                            val relative = name.removePrefix("images/").trimStart('/')
                            if (relative.isNotBlank()) {
                                writeChartFile(relative, zip.readBytes())
                                writtenImages += relative
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("无法读取导入文件")

        val data = backup ?: error("备份包缺少 data.json")
        return BackupImportResult(
            records = data.records,
            reviewLogs = data.reviewLogs,
            trackLogs = data.trackLogs,
            strategies = data.strategies,
            imagePaths = writtenImages
        )
    }

    /** 兼容：纯 JSON 完整备份或旧版仅交易 */
    fun importFromJsonUri(uri: Uri): BackupImportResult {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        } ?: error("无法读取导入文件")
        return parseFlexibleJson(text)
    }

    fun exportToJson(
        uri: Uri,
        records: List<TradeRecord>,
        reviewLogs: List<ReviewLog> = emptyList(),
        trackLogs: List<TrackLog> = emptyList(),
        strategies: List<StrategyGuide> = emptyList()
    ) {
        val backup = BookkeepingBackup(
            records = records,
            reviewLogs = reviewLogs,
            trackLogs = trackLogs,
            strategies = strategies
        )
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(gson.toJson(backup).toByteArray(StandardCharsets.UTF_8))
        } ?: error("无法写入导出文件")
    }

    fun importFromJson(uri: Uri): List<TradeRecord> = importFromJsonUri(uri).records

    fun exportToCsv(uri: Uri, records: List<TradeRecord>) {
        val header = listOf(
            "id", "symbol", "method", "riskRewardRatio", "profit", "fee",
            "shareCount", "journal", "chartImagePath", "tradeDate", "createdAt", "updatedAt"
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
                csvEscape(r.chartImagePath),
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

    private fun readChartBytes(relativePath: String): ByteArray? {
        runCatching {
            context.assets.open(relativePath).use { it.readBytes() }
        }.getOrNull()?.let { return it }

        val file = File(context.filesDir, relativePath)
        return if (file.isFile) file.readBytes() else null
    }

    private fun writeChartFile(relativePath: String, bytes: ByteArray) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun parseBackupJson(text: String): BookkeepingBackup {
        val result = parseFlexibleJson(text)
        return BookkeepingBackup(
            version = 3,
            records = result.records,
            reviewLogs = result.reviewLogs,
            trackLogs = result.trackLogs,
            strategies = result.strategies
        )
    }

    private fun parseFlexibleJson(text: String): BackupImportResult {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            val type = object : TypeToken<List<TradeRecord>>() {}.type
            val records: List<TradeRecord> = gson.fromJson(trimmed, type) ?: emptyList()
            return BackupImportResult(records, emptyList(), emptyList(), emptyList())
        }
        val root = JsonParser.parseString(trimmed).asJsonObject
        val records = parseRecords(root)
        val reviews = parseReviews(root)
        val tracks = parseTracks(root)
        val strategies = parseStrategies(root)
        return BackupImportResult(records, reviews, tracks, strategies)
    }

    private fun parseRecords(root: JsonObject): List<TradeRecord> {
        if (!root.has("records")) return emptyList()
        val type = object : TypeToken<List<TradeRecord>>() {}.type
        return gson.fromJson(root.get("records"), type) ?: emptyList()
    }

    private fun parseReviews(root: JsonObject): List<ReviewLog> {
        if (!root.has("reviewLogs")) return emptyList()
        val type = object : TypeToken<List<ReviewLog>>() {}.type
        return gson.fromJson(root.get("reviewLogs"), type) ?: emptyList()
    }

    private fun parseTracks(root: JsonObject): List<TrackLog> {
        if (!root.has("trackLogs")) return emptyList()
        val type = object : TypeToken<List<TrackLog>>() {}.type
        return gson.fromJson(root.get("trackLogs"), type) ?: emptyList()
    }

    private fun parseStrategies(root: JsonObject): List<StrategyGuide> {
        if (!root.has("strategies")) return emptyList()
        val type = object : TypeToken<List<StrategyGuide>>() {}.type
        return gson.fromJson(root.get("strategies"), type) ?: emptyList()
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
                chartImagePath = col("chartimagepath", 8),
                tradeDate = col("tradedate", 9).toLongOrNull()
                    ?: col("tradedate", 8).toLongOrNull()
                    ?: System.currentTimeMillis(),
                createdAt = col("createdat", 10).toLongOrNull()
                    ?: col("createdat", 9).toLongOrNull()
                    ?: System.currentTimeMillis(),
                updatedAt = col("updatedat", 11).toLongOrNull()
                    ?: col("updatedat", 10).toLongOrNull()
                    ?: System.currentTimeMillis()
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

    private fun ZipInputStream.readBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = read(buf)
            if (n <= 0) break
            buffer.write(buf, 0, n)
        }
        return buffer.toByteArray()
    }
}
