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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import com.sym.bookkeeping.ui.TradeSort
import com.sym.bookkeeping.ui.TradeTimeRange
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import com.sym.bookkeeping.ui.components.SummaryStrip
import com.sym.bookkeeping.ui.components.TradeRecordCard

private const val PAGE_SIZE = 15

@Composable
fun TradeListScreen(
    viewModel: TradeViewModel,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    var overflowMenu by remember { mutableStateOf(false) }
    val filterActive = state.selectedMethod != null ||
        state.timeRange != TradeTimeRange.All ||
        state.sort != TradeSort.DateDesc
    val summary = state.summary
    val listState = rememberLazyListState()
    var shown by remember(state.selectedMethod, state.timeRange, state.sort, state.query) {
        mutableIntStateOf(PAGE_SIZE)
    }
    val total = state.filtered.size
    val visible = if (shown >= total) state.filtered else state.filtered.take(shown)

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
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        onClick = { overflowMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "筛选和排序",
                            tint = if (filterActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenu,
                        onDismissRequest = { overflowMenu = false }
                    ) {
                        MenuSectionLabel("手法")
                        DropdownMenuItem(
                            text = { Text("全部手法") },
                            onClick = {
                                viewModel.setSelectedMethod(null)
                                overflowMenu = false
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
                                    overflowMenu = false
                                },
                                trailingIcon = {
                                    if (state.selectedMethod == method) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        MenuSectionLabel("时间")
                        TradeTimeRange.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setTimeRange(option)
                                    overflowMenu = false
                                },
                                trailingIcon = {
                                    if (state.timeRange == option) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        MenuSectionLabel("排序")
                        TradeSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setSort(option)
                                    overflowMenu = false
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

            LaunchedEffect(state.selectedMethod, state.timeRange, state.sort, state.query) {
                if (total > 0) listState.scrollToItem(0)
            }
            LaunchedEffect(listState, shown, total) {
                snapshotFlow {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 1
                }.distinctUntilChanged().collect { atEnd ->
                    if (atEnd && shown < total) {
                        shown = (shown + PAGE_SIZE).coerceAtMost(total)
                    }
                }
            }

            if (state.filtered.isEmpty()) {
                EmptyState(
                    title = if (state.records.isEmpty()) "还没有交易记录" else "没有匹配结果",
                    subtitle = if (state.records.isEmpty()) {
                        "点击右下角加号，记录一笔交易"
                    } else {
                        "试试换个手法或时间筛选"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.id }) { record ->
                        TradeRecordCard(
                            record = record,
                            onClick = { onItemClick(record.id) }
                        )
                    }
                    if (visible.size < total) {
                        item(key = "page-more") {
                            Text(
                                text = "已显示 ${visible.size} / $total，上滑继续加载",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp)
    )
}
