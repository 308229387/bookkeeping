package com.sym.bookkeeping.ui.journal

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.data.ReviewLog
import com.sym.bookkeeping.data.TrackLog
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import com.sym.bookkeeping.ui.util.formatDate

private enum class JournalTab(val label: String) {
    Track("跟踪日志"),
    Review("复盘日志")
}

@Composable
fun JournalScreen(
    viewModel: TradeViewModel,
    onTrackClick: (Long) -> Unit,
    onTrackAdd: () -> Unit,
    onReviewClick: (Long) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(JournalTab.Track) }
    val trackLogs by viewModel.trackLogs.collectAsStateWithLifecycle()
    val reviewLogs by viewModel.reviewLogs.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("日志", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "跟踪日志 = 推荐标的（未成交）；已成交请看「记录」",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                JournalTab.Track -> TrackJournalList(
                    logs = trackLogs,
                    onItemClick = onTrackClick
                )
                JournalTab.Review -> ReviewJournalList(
                    logs = reviewLogs,
                    onItemClick = onReviewClick
                )
            }
        }

        if (tab == JournalTab.Track) {
            FloatingActionButton(
                onClick = onTrackAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增推荐跟踪")
            }
        }
    }
}

@Composable
private fun TrackJournalList(
    logs: List<TrackLog>,
    onItemClick: (Long) -> Unit
) {
    if (logs.isEmpty()) {
        EmptyState(
            title = "暂无推荐跟踪",
            subtitle = "这里只记推荐观察的标的，名称写清股票全称；成交后去「记录」记账",
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(logs, key = { "tk-${it.id}" }) { log ->
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
                        text = formatDate(log.trackDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = log.symbol.ifBlank { "未命名标的" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            log.status.takeIf { it.isNotBlank() },
                            log.method.takeIf { it.isNotBlank() }?.let { "推荐 · $it" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = log.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "点击查看 / 更新跟踪",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
            subtitle = "大盘情绪、涨跌停分析记在这里；单票推荐请用「跟踪日志」",
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
