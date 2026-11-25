package com.sym.bookkeeping.ui.strategy

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.data.EmotionPlaybooks
import com.sym.bookkeeping.data.StrategyCatalog
import com.sym.bookkeeping.data.StrategyGuide
import com.sym.bookkeeping.data.UserStrategyStore
import com.sym.bookkeeping.data.WorkerPickRank

private enum class StrategyFilter(val label: String) {
    Emotion("情绪手法"),
    HighWin("高胜率优选"),
    Diagrams("样图"),
    All("全部"),
    Worker("适合工薪"),
    Short("短线"),
    Swing("波段/中长线")
}

@Composable
fun StrategyListScreen(
    onOpenDetail: (String) -> Unit,
    onOpenDiagram: (String) -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf(StrategyFilter.Emotion) }
    val strategyRevision by UserStrategyStore.revision.collectAsStateWithLifecycle()
    val strategies = remember(filter, strategyRevision) {
        StrategyCatalog.allGuides().filter { guide ->
            when (filter) {
                StrategyFilter.All -> true
                StrategyFilter.Emotion -> guide.style == EmotionPlaybooks.STYLE_TAG
                StrategyFilter.HighWin -> guide.highWinRatePick
                StrategyFilter.Diagrams -> false
                StrategyFilter.Worker -> guide.workerFriendly
                StrategyFilter.Short -> guide.style == "短线"
                StrategyFilter.Swing ->
                    guide.style != "短线" && guide.style != EmotionPlaybooks.STYLE_TAG
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("战法手册", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (filter == StrategyFilter.Emotion) {
                "对应涨停页情绪：出手时机、仓位、卖点与止损"
            } else {
                "可看「样图」理解均线/箱体；高胜率优选方便工薪执行"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrategyFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (filter == StrategyFilter.Diagrams) {
            DiagramGalleryScreen(
                onOpenDiagram = onOpenDiagram,
                onOpenStrategy = onOpenDetail,
                embedded = true
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filter == StrategyFilter.Emotion) {
                    item { EmotionPlaybooksIntroCard() }
                }
                if (filter == StrategyFilter.HighWin || filter == StrategyFilter.All) {
                    item {
                        WorkerPicksCard(
                            picks = StrategyCatalog.workerHighWinRatePicks,
                            onOpenDetail = onOpenDetail
                        )
                    }
                }
                items(strategies, key = { it.id }) { guide ->
                    StrategyListCard(guide = guide, onClick = { onOpenDetail(guide.id) })
                }
            }
        }
    }
}

@Composable
private fun EmotionPlaybooksIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "情绪手法手册",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "与涨停页「当日情绪」一一对应。点开每条看：出手时机、仓位、卖点、止损。仅供复盘，非投资建议。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun WorkerPicksCard(
    picks: List<WorkerPickRank>,
    onOpenDetail: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "工薪族高胜率优选（实操排序）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "原则：少盯盘、收盘决策、宁可少做也不追高。胜率是相对概念，需配合仓位与止损。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            picks.forEachIndexed { index, pick ->
                Card(
                    onClick = { onOpenDetail(pick.strategyId) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "No.${pick.rank}  ${pick.title}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = pick.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index != picks.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StrategyListCard(guide: StrategyGuide, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guide.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = guide.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (guide.highWinRatePick) {
                        TagChip(text = "高胜率", emphasized = true)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (guide.workerFriendly) {
                        TagChip(text = "工薪友好", emphasized = true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("胜率${guide.winRateLevel}")
                TagChip("操作${guide.operability}")
                TagChip(guide.style)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("持股${guide.holdingDaysShort}")
                TagChip("盯盘${guide.watchIntensity}")
                TagChip(guide.difficulty)
            }
        }
    }
}

@Composable
fun TagChip(text: String, emphasized: Boolean = false) {
    val bg = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
