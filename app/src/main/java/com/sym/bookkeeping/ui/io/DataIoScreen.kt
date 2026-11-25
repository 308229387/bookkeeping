package com.sym.bookkeeping.ui.io

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

/**
 * 紧凑导入导出条：放在分析页上方，不含独立 Tab。
 * 完整备份 = 交易(含图) + 复盘日志 + 战法。
 */
@Composable
fun DataIoBar(viewModel: TradeViewModel) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val reviewLogs by viewModel.reviewLogs.collectAsStateWithLifecycle()
    val trackLogs by viewModel.trackLogs.collectAsStateWithLifecycle()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingUri = uri
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("数据备份", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "交易 ${listState.records.size} · 跟踪 ${trackLogs.size} · 复盘 ${reviewLogs.size} · 含图与战法",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.shareFullBackup() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("导出")
                }
                OutlinedButton(
                    onClick = {
                        openBackup.launch(
                            arrayOf(
                                "application/zip",
                                "application/json",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("导入")
                }
            }
        }
    }

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text("导入方式") },
            text = {
                Text("合并：保留现有数据并追加。覆盖：清空交易与日志后再导入（战法按备份写入）。")
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.importFullBackup(uri, replace = false)
                        pendingUri = null
                    }) { Text("合并") }
                    TextButton(onClick = {
                        viewModel.importFullBackup(uri, replace = true)
                        pendingUri = null
                    }) { Text("覆盖") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUri = null }) { Text("取消") }
            }
        )
    }
}
