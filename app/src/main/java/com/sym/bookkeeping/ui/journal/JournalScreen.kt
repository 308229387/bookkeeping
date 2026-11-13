package com.sym.bookkeeping.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.data.ReviewLog
import com.sym.bookkeeping.data.TradeRecord
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import com.sym.bookkeeping.ui.components.ProfitText
import com.sym.bookkeeping.ui.util.formatDate

private enum class JournalTab(val label: String) {
    Trade("交易日志"),
    Review("复盘日志")
}

@Composable
fun JournalScreen(
    viewModel: TradeViewModel,
    onTradeClick: (Long) -> Unit,
    onReviewClick: (Long) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(JournalTab.Review) }
    val tradeJournals by viewModel.journalRecords.collectAsStateWithLifecycle()
    val reviewLogs by viewModel.reviewLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("日志", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "交易心得与大盘复盘分开存放",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JournalTab.entries.forEach { item ->
                FilterChip(
                    selected = tab == item,
                    onClick = { tab = item },
                    label = { Text(item.label) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            JournalTab.Trade -> TradeJournalList(
                records = tradeJournals,
                onItemClick = onTradeClick
            )
            JournalTab.Review -> ReviewJournalList(
                logs = reviewLogs,
                onItemClick = onReviewClick
            )
        }
    }
}

@Composable
private fun TradeJournalList(
    records: List<TradeRecord>,
    onItemClick: (Long) -> Unit
) {
    if (records.isEmpty()) {
        EmptyState(
            title = "暂无交易日志",
            subtitle = "在交易记录里填写心得后，会显示在这里",
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(records, key = { "t-${it.id}" }) { record ->
            Card(
                onClick = { onItemClick(record.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formatDate(record.tradeDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${record.symbol.ifBlank { "未命名标的" }} · ${record.method.ifBlank { "未分类" }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    ProfitText(record.profit)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = record.journal,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (record.journal.length > 80) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "点击查看全文",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewJournalList(
    logs: List<ReviewLog>,
    onItemClick: (Long) -> Unit
) {
    if (logs.isEmpty()) {
        EmptyState(
            title = "暂无复盘日志",
            subtitle = "大盘情绪、涨跌停、龙头分析会记在这里",
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(logs, key = { "r-${it.id}" }) { log ->
            Card(
                onClick = { onItemClick(log.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formatDate(log.reviewDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = log.title.ifBlank { "盘面复盘" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "复盘日志",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = log.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "点击查看全文",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
