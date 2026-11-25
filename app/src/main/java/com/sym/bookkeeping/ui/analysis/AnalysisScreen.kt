package com.sym.bookkeeping.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import com.sym.bookkeeping.ui.components.MethodStatsCard
import com.sym.bookkeeping.ui.components.SectionTitle
import com.sym.bookkeeping.ui.io.DataIoBar

@Composable
fun AnalysisScreen(viewModel: TradeViewModel) {
    val state by viewModel.analysisState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        DataIoBar(viewModel = viewModel)
        Spacer(Modifier.height(16.dp))
        Text("数据分析", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "按交易手法统计胜率、盈亏与笔数",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (state.overall.totalCount == 0) {
            EmptyState(
                title = "暂无分析数据",
                subtitle = "先录入几笔交易，即可查看手法胜率",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionTitle("总览")
                    MethodStatsCard(stats = state.overall)
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionTitle("各手法胜率")
                }
                items(state.byMethod, key = { it.method }) { stats ->
                    MethodStatsCard(stats = stats)
                }
            }
        }
    }
}
