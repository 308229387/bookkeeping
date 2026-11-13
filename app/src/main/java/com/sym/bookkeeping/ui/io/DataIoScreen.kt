package com.sym.bookkeeping.ui.io

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.ui.TradeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ImportMode { Json, Csv }

@Composable
fun DataIoScreen(viewModel: TradeViewModel) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    var pendingImport by remember { mutableStateOf<ImportMode?>(null) }
    var replaceConfirm by remember { mutableStateOf<Pair<ImportMode, Uri>?>(null) }

    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(viewModel::exportJson)
    }
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let(viewModel::exportCsv)
    }
    val openJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val mode = pendingImport
        pendingImport = null
        if (uri != null && mode == ImportMode.Json) {
            replaceConfirm = mode to uri
        }
    }
    val openCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val mode = pendingImport
        pendingImport = null
        if (uri != null && mode == ImportMode.Csv) {
            replaceConfirm = mode to uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("导入导出", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "当前共 ${listState.records.size} 条交易记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IoCard(
            title = "导出数据",
            description = "把本地记录备份为 JSON 或 CSV，方便换机或备份。"
        ) {
            Button(
                onClick = { createJson.launch(defaultFileName("json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出 JSON")
            }
            OutlinedButton(
                onClick = { createCsv.launch(defaultFileName("csv")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出 CSV")
            }
        }

        IoCard(
            title = "导入数据",
            description = "支持本 App 导出的 JSON，以及同字段 CSV。导入时可选择合并或覆盖。"
        ) {
            Button(
                onClick = {
                    pendingImport = ImportMode.Json
                    openJson.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导入 JSON")
            }
            OutlinedButton(
                onClick = {
                    pendingImport = ImportMode.Csv
                    openCsv.launch(arrayOf("text/csv", "text/*", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导入 CSV")
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("字段说明", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "标的、手法、盈亏比、盈利金额、税费、持股数、交易日志、交易日期。\n" +
                        "盈利金额为正表示盈利，负数为亏损；胜率按盈利金额 > 0 统计。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    replaceConfirm?.let { (mode, uri) ->
        AlertDialog(
            onDismissRequest = { replaceConfirm = null },
            title = { Text("导入方式") },
            text = {
                Text("选择「合并」保留现有记录并追加；选择「覆盖」会清空后再导入。")
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        when (mode) {
                            ImportMode.Json -> viewModel.importJson(uri, replace = false)
                            ImportMode.Csv -> viewModel.importCsv(uri, replace = false)
                        }
                        replaceConfirm = null
                    }) { Text("合并") }
                    TextButton(onClick = {
                        when (mode) {
                            ImportMode.Json -> viewModel.importJson(uri, replace = true)
                            ImportMode.Csv -> viewModel.importCsv(uri, replace = true)
                        }
                        replaceConfirm = null
                    }) { Text("覆盖") }
                }
            },
            dismissButton = {
                TextButton(onClick = { replaceConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun IoCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

private fun defaultFileName(ext: String): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
    return "bookkeeping_trades_$stamp.$ext"
}
