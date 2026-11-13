package com.sym.bookkeeping.ui.trades

import android.app.DatePickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.util.formatDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeEditScreen(
    tradeId: Long?,
    viewModel: TradeViewModel,
    onBack: () -> Unit
) {
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(tradeId) {
        if (tradeId == null || tradeId == 0L) {
            viewModel.startCreate()
        } else {
            viewModel.loadForEdit(tradeId)
        }
    }

    val isEdit = editing.id != 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑交易" else "新增交易") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = editing.symbol,
                onValueChange = { value -> viewModel.updateEditing { it.copy(symbol = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标的名称 / 代码") },
                singleLine = true
            )
            OutlinedTextField(
                value = editing.method,
                onValueChange = { value -> viewModel.updateEditing { it.copy(method = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("手法") },
                placeholder = { Text("如：突破回踩、趋势跟随") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editing.riskRewardRatio.asEditable(),
                    onValueChange = { text ->
                        viewModel.updateEditing {
                            it.copy(riskRewardRatio = text.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("盈亏比(%)") },
                    placeholder = { Text("如 -2.48") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editing.profit.asEditable(),
                    onValueChange = { text ->
                        viewModel.updateEditing {
                            it.copy(profit = text.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("盈利金额") },
                    placeholder = { Text("亏损填负数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = if (editing.shareCount == 0) "" else editing.shareCount.toString(),
                    onValueChange = { text ->
                        viewModel.updateEditing {
                            it.copy(shareCount = text.toIntOrNull() ?: 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("持股数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editing.fee.asEditable(),
                    onValueChange = { text ->
                        viewModel.updateEditing {
                            it.copy(fee = text.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("税费") },
                    placeholder = { Text("手续费合计") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = editing.tradeDate }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val picked = Calendar.getInstance().apply {
                                set(year, month, day, 12, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            viewModel.updateEditing { it.copy(tradeDate = picked.timeInMillis) }
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("交易日期：${formatDate(editing.tradeDate)}")
            }
            OutlinedTextField(
                value = editing.journal,
                onValueChange = { value -> viewModel.updateEditing { it.copy(journal = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text("交易日志 / 心得") },
                placeholder = { Text("记录当时的判断依据、情绪和复盘心得…") }
            )
            Button(
                onClick = { viewModel.saveEditing { onBack() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除记录") },
            text = { Text("确定删除这笔交易吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(editing)
                    onBack()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

private fun Double.asEditable(): String {
    if (this == 0.0) return ""
    return if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
}
