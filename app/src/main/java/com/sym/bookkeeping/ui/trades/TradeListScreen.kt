package com.sym.bookkeeping.ui.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.ui.TradeSort
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import com.sym.bookkeeping.ui.components.SummaryStrip
import com.sym.bookkeeping.ui.components.TradeRecordCard

@Composable
fun TradeListScreen(
    viewModel: TradeViewModel,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    var methodMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val summary = state.summary

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startCreate()
                onAddClick()
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增交易")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SummaryStrip(
                    totalCount = summary.totalCount,
                    totalProfit = summary.totalProfit,
                    winRate = summary.winRate,
                    totalPoints = summary.totalRiskRewardRatio,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { methodMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "手法筛选",
                            tint = if (state.selectedMethod != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = methodMenu,
                        onDismissRequest = { methodMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部手法") },
                            onClick = {
                                viewModel.setSelectedMethod(null)
                                methodMenu = false
                            },
                            trailingIcon = {
                                if (state.selectedMethod == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        state.methods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    viewModel.setSelectedMethod(method)
                                    methodMenu = false
                                },
                                trailingIcon = {
                                    if (state.selectedMethod == method) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
                Box {
                    IconButton(
                        onClick = { sortMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "排序",
                            tint = if (state.sort != TradeSort.DateDesc) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenu,
                        onDismissRequest = { sortMenu = false }
                    ) {
                        TradeSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setSort(option)
                                    sortMenu = false
                                },
                                trailingIcon = {
                                    if (state.sort == option) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            if (state.filtered.isEmpty()) {
                EmptyState(
                    title = if (state.records.isEmpty()) "还没有交易记录" else "没有匹配结果",
                    subtitle = if (state.records.isEmpty()) {
                        "点击右下角加号，记录一笔交易"
                    } else {
                        "试试换个手法筛选"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filtered, key = { it.id }) { record ->
                        TradeRecordCard(
                            record = record,
                            onClick = { onItemClick(record.id) }
                        )
                    }
                }
            }
        }
    }
}
