package com.sym.bookkeeping.ui.trades

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val analysis by viewModel.analysisState.collectAsStateWithLifecycle()

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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "交易记录",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            SummaryStrip(
                totalCount = analysis.overall.totalCount,
                totalProfit = analysis.overall.totalProfit,
                winRate = analysis.overall.winRate
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("搜索标的 / 手法 / 日志") }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedMethod == null,
                    onClick = { viewModel.setSelectedMethod(null) },
                    label = { Text("全部手法") }
                )
                state.methods.forEach { method ->
                    FilterChip(
                        selected = state.selectedMethod == method,
                        onClick = {
                            viewModel.setSelectedMethod(
                                if (state.selectedMethod == method) null else method
                            )
                        },
                        label = { Text(method) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.filtered.isEmpty()) {
                EmptyState(
                    title = if (state.records.isEmpty()) "还没有交易记录" else "没有匹配结果",
                    subtitle = if (state.records.isEmpty()) {
                        "点击右下角加号，记录一笔交易"
                    } else {
                        "试试换个关键词或手法筛选"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
