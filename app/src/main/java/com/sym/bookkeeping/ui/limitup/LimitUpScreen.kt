package com.sym.bookkeeping.ui.limitup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sym.bookkeeping.data.market.ConceptBoard
import com.sym.bookkeeping.data.market.DragonLeader
import com.sym.bookkeeping.data.market.GlobalIndexQuote
import com.sym.bookkeeping.data.market.IndexDay
import com.sym.bookkeeping.data.market.IndexSeries
import com.sym.bookkeeping.data.market.LadderDay
import com.sym.bookkeeping.data.market.LadderLevel
import com.sym.bookkeeping.data.market.LimitDayStat
import com.sym.bookkeeping.data.market.LimitFocusResult
import com.sym.bookkeeping.data.market.LimitSectionReady
import com.sym.bookkeeping.data.market.LimitStock
import com.sym.bookkeeping.data.market.MarketBreadthDay
import com.sym.bookkeeping.data.market.MarketTone
import com.sym.bookkeeping.data.market.MarketToneJudge
import com.sym.bookkeeping.data.market.MarketToneKind
import com.sym.bookkeeping.data.market.PlateLeaderStock
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private enum class MarketTab(val label: String) {
    Dashboard("大盘"),
    Concepts("概念行业"),
    LimitUp("涨停"),
    Overseas("外盘")
}

private enum class PoolTab(val label: String) {
    Up("涨停池"),
    Down("跌停池")
}

private val RiseRed = Color(0xFFD32F2F)
private val FallGreen = Color(0xFF2E7D32)
private val RateBlue = Color(0xFF1565C0)
private val FlatGray = Color(0xFF757575)
private val BarTrack = Color(0xFFE8E8E8)
private val GridLine = Color(0xFFE0E0E0)
private val IndexSh = Color(0xFF1565C0)
private val IndexSz = Color(0xFF00897B)
private val IndexCyb = Color(0xFFEF6C00)

@Composable
fun LimitUpScreen(viewModel: TradeViewModel) {
    val state by viewModel.limitUpState.collectAsStateWithLifecycle()
    var marketTab by rememberSaveable { mutableStateOf(MarketTab.Dashboard) }
    var poolTab by rememberSaveable { mutableStateOf(PoolTab.Up) }

    LaunchedEffect(Unit) {
        if (state.focus == null && !state.loading) {
            viewModel.refreshLimitUpPool()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("行情", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = buildString {
                        val f = state.focus
                        if (f != null) {
                            append(formatDateLabel(f.date))
                            if (f.tradeStatus.isNotBlank()) append(" · ").append(f.tradeStatus)
                        } else {
                            append("大盘 / 概念行业 / 涨停 / 外盘")
                        }
                        if (state.updatedAt > 0L) {
                            append(" · ").append(formatTime(state.updatedAt))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(22.dp).height(22.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            OutlinedButton(
                onClick = { viewModel.refreshLimitUpPool() },
                enabled = !state.loading
            ) { Text("刷新") }
        }

        Spacer(Modifier.height(4.dp))
        val selectedIndex = MarketTab.entries.indexOf(marketTab).coerceAtLeast(0)
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                if (selectedIndex in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = RiseRed
                    )
                }
            }
        ) {
            MarketTab.entries.forEach { tab ->
                Tab(
                    selected = marketTab == tab,
                    onClick = { marketTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (marketTab == tab) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = RiseRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.error != null && state.focus == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(title = "拉取失败", subtitle = state.error.orEmpty())
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.refreshLimitUpPool() }) { Text("重试") }
                }
            }
            state.loading && state.focus == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.focus == null -> {
                EmptyState(
                    title = "暂无数据",
                    subtitle = "点刷新拉取行情数据",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val focus = state.focus!!
                val ready = state.sectionReady
                when (marketTab) {
                    MarketTab.Dashboard -> DashboardTabContent(
                        focus = focus,
                        ready = ready,
                        onSaveReview = { viewModel.saveLimitUpSnapshotToReview() }
                    )
                    MarketTab.Concepts -> ConceptsTabContent(focus = focus, ready = ready)
                    MarketTab.LimitUp -> LimitUpTabContent(
                        focus = focus,
                        ready = ready,
                        poolTab = poolTab,
                        onPoolTabChange = { poolTab = it }
                    )
                    MarketTab.Overseas -> OverseasTabContent(focus = focus, ready = ready)
                }
            }
        }
    }
}

@Composable
private fun DashboardTabContent(
    focus: LimitFocusResult,
    ready: LimitSectionReady,
    onSaveReview: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "save") {
            TextButton(
                onClick = onSaveReview,
                enabled = ready.allReady,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { Text("写入复盘日志") }
        }
        item(key = "tone") {
            if (ready.allReady) MarketToneBanner(MarketToneJudge.judge(focus))
            else MarketToneLoadingBanner()
        }
        item(key = "breadth_summary") {
            if (ready.core || ready.breadth) MarketBreadthSummaryCards(focus)
            else SectionLoadingCard("涨跌家数加载中…")
        }
        item(key = "indices10") {
            if (ready.indices) IndexDaysSection(focus.indices)
            else SectionLoadingCard("三大指数加载中…")
        }
        item(key = "breadth10") {
            if (ready.breadth) BreadthDaysSection(focus.breadthDays)
            else SectionLoadingCard("涨跌家数曲线加载中…")
        }
        item(key = "turnover10") {
            if (ready.breadth) TurnoverDaysSection(focus.breadthDays)
            else SectionLoadingCard("成交额加载中…")
        }
    }
}

@Composable
private fun ConceptsTabContent(focus: LimitFocusResult, ready: LimitSectionReady) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!ready.concepts) {
            item(key = "concepts_loading") {
                SectionLoadingCard("概念 / 行业加载中…")
            }
        } else {
            item(key = "concepts_strong") {
                ConceptBoardsSection(
                    title = "当日最强概念",
                    subtitle = "按涨停家数 · 同花顺板块龙一龙二",
                    boards = focus.conceptBoards
                )
            }
            item(key = "industry_strong") {
                ConceptBoardsSection(
                    title = "当日最强行业",
                    subtitle = "按涨幅 · 东财行业指数领涨股",
                    boards = focus.industryBoards,
                    showDragonTwo = false
                )
            }
        }
    }
}

@Composable
private fun LimitUpTabContent(
    focus: LimitFocusResult,
    ready: LimitSectionReady,
    poolTab: PoolTab,
    onPoolTabChange: (PoolTab) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "limit_summary") {
            if (ready.core) LimitSummaryCards(focus)
            else SectionLoadingCard("涨停摘要加载中…")
        }
        item(key = "history20") {
            if (ready.recentDays) RecentDaysSection(focus.recentDays)
            else SectionLoadingCard("近二十日涨跌停加载中…")
        }
        item(key = "leaders") {
            if (ready.leaders) DragonLeadersSection(focus.leaders)
            else SectionLoadingCard("龙头股（人气+连板）加载中…")
        }
        item(key = "ladder") {
            if (ready.core) {
                LadderSection(
                    date = focus.date,
                    today = focus.ladder,
                    history = if (ready.ladderHistory) focus.ladderHistory else emptyList(),
                    historyLoading = !ready.ladderHistory
                )
            } else {
                SectionLoadingCard("连板天梯加载中…")
            }
        }
        if (!ready.core) {
            item(key = "pool_loading") {
                SectionLoadingCard("涨停 / 跌停池加载中…")
            }
        } else {
            item(key = "pool_tabs") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoolTab.entries.forEach { tab ->
                        FilterChip(
                            selected = poolTab == tab,
                            onClick = { onPoolTabChange(tab) },
                            label = {
                                val n = when (tab) {
                                    PoolTab.Up -> focus.upStocks.size
                                    PoolTab.Down -> focus.downStocks.size
                                }
                                Text("${tab.label} $n")
                            }
                        )
                    }
                }
            }
            val stocks = when (poolTab) {
                PoolTab.Up -> focus.upStocks
                PoolTab.Down -> focus.downStocks
            }
            if (stocks.isEmpty()) {
                item(key = "pool_empty") {
                    EmptyState(
                        title = if (poolTab == PoolTab.Up) "暂无涨停" else "暂无跌停",
                        subtitle = "非交易日或当日无数据"
                    )
                }
            } else {
                items(stocks, key = { "${poolTab.name}-${it.code}" }) { stock ->
                    LimitStockCard(stock = stock, down = poolTab == PoolTab.Down)
                }
            }
        }
    }
}

@Composable
private fun OverseasTabContent(focus: LimitFocusResult, ready: LimitSectionReady) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!ready.globalIndices) {
            item(key = "global_loading") {
                SectionLoadingCard("全球指数加载中…")
            }
        } else if (focus.globalIndices.isEmpty()) {
            item(key = "global_empty") {
                EmptyState(title = "暂无外盘数据", subtitle = "稍后下拉刷新重试")
            }
        } else {
            item(key = "global_hint") {
                Text(
                    text = "对齐同花顺全球指数 · 亚太 / 欧美 / 商品货币",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val regions = listOf("亚太", "欧美", "美洲", "商品货币")
            regions.forEach { region ->
                val quotes = focus.globalIndices.filter { it.region == region }
                if (quotes.isNotEmpty()) {
                    item(key = "global_$region") {
                        GlobalIndicesSection(region = region, quotes = quotes)
                    }
                }
            }
            val rest = focus.globalIndices.filter { it.region !in regions }
            if (rest.isNotEmpty()) {
                item(key = "global_other") {
                    GlobalIndicesSection(region = "其他", quotes = rest)
                }
            }
        }
    }
}

@Composable
private fun MarketToneLoadingBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当日情绪",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "数据加载中，齐了再判定…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionLoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MarketToneBanner(tone: MarketTone) {
    val color = when (tone.kind.bias) {
        1 -> RiseRed
        -1 -> FallGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bg = when (tone.kind.bias) {
        1 -> RiseRed.copy(alpha = 0.08f)
        -1 -> FallGreen.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当日情绪",
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tone.kind.label,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = tone.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (tone.kind == MarketToneKind.Split || tone.kind == MarketToneKind.Range) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "中性 · 分数 ${tone.score}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tone.playbookFocus.isNotBlank() || tone.playbookTips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "手法侧重 · ${tone.playbookFocus.ifBlank { tone.kind.label }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                tone.playbookTips.forEachIndexed { i, tip ->
                    Text(
                        text = "${i + 1}. $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    if (i != tone.playbookTips.lastIndex) Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "仅供复盘参考，非投资建议",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun DragonLeadersSection(leaders: List<DragonLeader>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "龙头股 · 龙一龙二龙三",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "同花顺热榜·东财人气·雪球热股·板块带动·量价市值",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (leaders.isEmpty()) {
                Text("暂未评出龙头", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                leaders.forEach { leader ->
                    DragonLeaderCard(leader)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DragonLeaderCard(leader: DragonLeader) {
    val title = when (leader.rank) {
        1 -> "龙一"
        2 -> "龙二"
        3 -> "龙三"
        else -> "龙${leader.rank}"
    }
    val accent = when (leader.rank) {
        1 -> RiseRed
        2 -> Color(0xFFEF6C00)
        else -> RateBlue
    }
    val chgColor = when {
        leader.changePct > 0 -> RiseRed
        leader.changePct < 0 -> FallGreen
        else -> FlatGray
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leader.name.ifBlank { leader.code },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(leader.code)
                        if (leader.highLabel.isNotBlank()) append(" · ").append(leader.highLabel)
                        leader.thsRank?.let { append(" · 同花顺#").append(it) }
                        leader.hotRank?.let { append(" · 东财#").append(it) }
                        leader.xueqiuRank?.let { append(" · 雪球#").append(it) }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format(Locale.CHINA, "%+.2f%%", leader.changePct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = chgColor
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                if (leader.amountYi > 0) append(String.format(Locale.CHINA, "成交 %.1f亿", leader.amountYi))
                if (leader.marketCapYi > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(String.format(Locale.CHINA, "流值 %.0f亿", leader.marketCapYi))
                }
                if (leader.turnoverRate > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(String.format(Locale.CHINA, "换手 %.1f%%", leader.turnoverRate))
                }
                if (leader.conceptLimitUp > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("板块涨停${leader.conceptLimitUp}")
                }
            }.ifBlank { "量价市值待补全" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (leader.concept.isNotBlank() || leader.reason.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(leader.concept, leader.reason)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (leader.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                leader.tags.take(6).forEach { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketBreadthSummaryCards(focus: LimitFocusResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (focus.todayRise > 0 || focus.todayFall > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "上涨",
                    value = focus.todayRise.toString(),
                    subtitle = "同花顺涨跌家数",
                    accent = RiseRed
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "下跌",
                    value = focus.todayFall.toString(),
                    subtitle = "平盘 ${focus.todayFlat}",
                    accent = FallGreen
                )
            }
        }
        val todayTo = focus.breadthDays.firstOrNull()
        if (todayTo != null && todayTo.turnoverText.isNotBlank()) {
            val flagText = when (todayTo.turnoverFlag) {
                1 -> "较昨放量"
                -1 -> "较昨缩量"
                else -> "成交额"
            }
            StatCard(
                modifier = Modifier.fillMaxWidth(),
                title = "两市成交额",
                value = todayTo.turnoverText,
                subtitle = flagText,
                accent = RateBlue
            )
        }
    }
}

@Composable
private fun LimitSummaryCards(focus: LimitFocusResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "涨停",
            value = focus.upCount.num.toString(),
            subtitle = "昨 ${focus.yesterdayUpNum} · 封板 ${pct(focus.upCount.rate)}",
            accent = RiseRed
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "跌停",
            value = focus.downCount.num.toString(),
            subtitle = "昨 ${focus.yesterdayDownNum} · 封板 ${pct(focus.downCount.rate)}",
            accent = FallGreen
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "开板",
            value = focus.upCount.openNum.toString(),
            subtitle = "触及 ${focus.upCount.historyNum}",
            accent = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ConceptBoardsSection(
    title: String,
    subtitle: String,
    boards: List<ConceptBoard>,
    showDragonTwo: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (boards.isEmpty()) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                boards.forEachIndexed { index, board ->
                    ConceptBoardCard(board = board, showDragonTwo = showDragonTwo)
                    if (index != boards.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ConceptBoardCard(board: ConceptBoard, showDragonTwo: Boolean) {
    val chgColor = when {
        board.changePct > 0 -> RiseRed
        board.changePct < 0 -> FallGreen
        else -> FlatGray
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (board.limitUpNum > 0) append("涨停${board.limitUpNum}")
                        if (board.highLabel.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(board.highLabel)
                        }
                        if (board.tag.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(board.tag)
                        }
                        if (board.hotTag.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(board.hotTag)
                        }
                    }.ifBlank { if (board.kind == "industry") "行业指数" else "概念板块" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = String.format(Locale.CHINA, "%+.2f%%", board.changePct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = chgColor
            )
        }
        val leaders = board.leaders
            .filter { it.code.isNotBlank() }
            .let { if (showDragonTwo) it.take(2) else it.take(1) }
        if (leaders.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            leaders.forEach { stock ->
                PlateLeaderRow(stock)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PlateLeaderRow(stock: PlateLeaderStock) {
    val label = when (stock.order) {
        1 -> "龙一"
        2 -> "龙二"
        else -> "龙${stock.order}"
    }
    val accent = if (stock.order == 1) RiseRed else Color(0xFFEF6C00)
    val chgColor = when {
        stock.changePct > 0 -> RiseRed
        stock.changePct < 0 -> FallGreen
        else -> FlatGray
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stock.name.ifBlank { stock.code },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (stock.highLabel.isNotBlank()) {
            Text(
                text = stock.highLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = String.format(Locale.CHINA, "%+.2f%%", stock.changePct),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = chgColor
        )
    }
}

@Composable
private fun GlobalIndicesSection(region: String, quotes: List<GlobalIndexQuote>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = when (region) {
                    "亚太" -> "亚太市场"
                    "欧美" -> "欧非中东"
                    "美洲" -> "美洲市场"
                    "商品货币" -> "商品 / 货币"
                    else -> region
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            quotes.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { q ->
                        GlobalIndexCell(quote = q, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GlobalIndexCell(quote: GlobalIndexQuote, modifier: Modifier = Modifier) {
    val chgColor = when {
        quote.changePct > 0 -> RiseRed
        quote.changePct < 0 -> FallGreen
        else -> FlatGray
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chgColor.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = quote.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatGlobalPrice(quote.price),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = chgColor,
            maxLines = 1
        )
        Text(
            text = String.format(Locale.CHINA, "%+.2f%%", quote.changePct),
            style = MaterialTheme.typography.labelSmall,
            color = chgColor
        )
    }
}

private fun formatGlobalPrice(price: Double): String = when {
    price >= 1000 -> String.format(Locale.CHINA, "%,.2f", price)
    price >= 100 -> String.format(Locale.CHINA, "%.2f", price)
    else -> String.format(Locale.CHINA, "%.4f", price)
}

@Composable
private fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun indexColor(code: String): Color = when (code) {
    "000001" -> IndexSh
    "399001" -> IndexSz
    "399006" -> IndexCyb
    else -> RateBlue
}

@Composable
private fun IndexDaysSection(indices: List<IndexSeries>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "三大指数 · 近十日",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "上证 / 深成 / 创业 · 曲线为区间涨跌幅% · 可点击",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (indices.all { it.days.isEmpty() }) {
                Text("暂无指数数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                indices.forEach { series ->
                    val latest = series.latest
                    val accent = when {
                        latest == null -> FlatGray
                        latest.changePct > 0 -> RiseRed
                        latest.changePct < 0 -> FallGreen
                        else -> FlatGray
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = accent.copy(alpha = 0.08f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                text = shortIndexName(series),
                                style = MaterialTheme.typography.labelMedium,
                                color = indexColor(series.code),
                                maxLines = 1
                            )
                            if (latest == null) {
                                Text("—", fontWeight = FontWeight.Bold, color = FlatGray)
                            } else {
                                Text(
                                    text = String.format(Locale.CHINA, "%.0f", latest.close),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    maxLines = 1
                                )
                                Text(
                                    text = String.format(
                                        Locale.CHINA,
                                        "%+.2f%%",
                                        latest.changePct
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val dateAxis = remember(indices) {
                indices.map { it.days.map { d -> d.date } }
                    .maxByOrNull { it.size }
                    .orEmpty()
                    .asReversed()
            }
            if (dateAxis.isEmpty()) return@Column
            var selectedIndex by remember(dateAxis) {
                mutableIntStateOf((dateAxis.size - 1).coerceAtLeast(0))
            }
            val selectedDate = dateAxis.getOrNull(selectedIndex) ?: dateAxis.last()
            IndexDayDetail(indices = indices, date = selectedDate)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                indices.forEach { s ->
                    LegendItem(color = indexColor(s.code), label = shortIndexName(s))
                }
            }
            Spacer(Modifier.height(8.dp))
            IndexTrendChart(
                indices = indices,
                dates = dateAxis,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDateShort(dateAxis.first()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "相对区间首日涨跌%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDateShort(dateAxis.last()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun shortIndexName(s: IndexSeries): String = when (s.code) {
    "000001" -> "上证"
    "399001" -> "深成"
    "399006" -> "创业"
    else -> s.name.take(2)
}

@Composable
private fun IndexDayDetail(indices: List<IndexSeries>, date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = formatDateLabel(date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            indices.forEach { series ->
                val day = series.days.find { it.date == date }
                val accent = when {
                    day == null -> FlatGray
                    day.changePct > 0 -> RiseRed
                    day.changePct < 0 -> FallGreen
                    else -> FlatGray
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        shortIndexName(series),
                        style = MaterialTheme.typography.labelSmall,
                        color = indexColor(series.code)
                    )
                    Text(
                        text = if (day == null) "—"
                        else String.format(Locale.CHINA, "%.0f", day.close),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = if (day == null) ""
                        else String.format(Locale.CHINA, "%+.2f%%", day.changePct),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun IndexTrendChart(
    indices: List<IndexSeries>,
    dates: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dates.isEmpty()) return
    // 每只指数：相对区间首日收盘的累计涨跌幅%
    val seriesPct = remember(indices, dates) {
        indices.map { series ->
            val byDate = series.days.associateBy { it.date }
            val base = dates.firstOrNull()?.let { byDate[it]?.close }?.takeIf { it > 0 }
            dates.map { d ->
                val c = byDate[d]?.close
                if (base == null || c == null || base == 0.0) null
                else ((c / base - 1.0) * 100.0).toFloat()
            }
        }
    }
    val allVals = seriesPct.flatten().filterNotNull()
    if (allVals.isEmpty()) return
    val maxV = allVals.max().coerceAtLeast(0.1f)
    val minV = allVals.min().coerceAtMost(-0.1f)
    val lo = minV - (maxV - minV) * 0.08f
    val hi = maxV + (maxV - minV) * 0.08f
    val span = (hi - lo).coerceAtLeast(0.2f)
    val axisLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val tickStyle = TextStyle(color = axisLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    val ticks = listOf(lo, 0f.coerceIn(lo, hi), hi).distinct()

    Canvas(
        modifier = modifier.pointerInput(dates) {
            detectTapGestures { offset ->
                val leftPad = 40.dp.toPx()
                val rightPad = 12.dp.toPx()
                val w = size.width - leftPad - rightPad
                if (w <= 0f) return@detectTapGestures
                val idx = if (dates.size == 1) 0 else dates.indices.minBy { i ->
                    abs(offset.x - (leftPad + w * i / (dates.size - 1).toFloat()))
                }
                onSelectIndex(idx)
            }
        }
    ) {
        val leftPad = 40.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 10.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0f || h <= 0f) return@Canvas

        fun xAt(i: Int): Float {
            if (dates.size == 1) return leftPad + w / 2f
            return leftPad + w * i / (dates.size - 1).toFloat()
        }

        fun yOf(v: Float): Float =
            topPad + h * (1f - ((v - lo) / span).coerceIn(0f, 1f))

        // 零轴
        if (0f in lo..hi) {
            drawLine(
                GridLine.copy(alpha = 0.9f),
                Offset(leftPad, yOf(0f)),
                Offset(leftPad + w, yOf(0f)),
                strokeWidth = 1.5f
            )
        }
        ticks.forEach { tv ->
            val y = yOf(tv)
            drawLine(GridLine, Offset(leftPad, y), Offset(leftPad + w, y), strokeWidth = 1f)
            val label = String.format(Locale.CHINA, "%+.1f%%", tv)
            val layout = textMeasurer.measure(label, style = tickStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (leftPad - layout.size.width - 4.dp.toPx()).coerceAtLeast(0f),
                    y - layout.size.height / 2f
                )
            )
        }

        val sel = selectedIndex.coerceIn(0, dates.lastIndex)
        drawLine(
            selectColor.copy(alpha = 0.35f),
            Offset(xAt(sel), topPad),
            Offset(xAt(sel), topPad + h),
            2.dp.toPx()
        )

        indices.forEachIndexed { si, series ->
            val color = indexColor(series.code)
            val vals = seriesPct[si]
            val path = Path()
            var started = false
            vals.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val p = Offset(xAt(i), yOf(v))
                if (!started) {
                    path.moveTo(p.x, p.y)
                    started = true
                } else path.lineTo(p.x, p.y)
            }
            if (started) {
                drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            vals.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val r = if (i == sel) 5.dp.toPx() else 2.5.dp.toPx()
                drawCircle(color, r, Offset(xAt(i), yOf(v)))
            }
        }
    }
}

@Composable
private fun BreadthDaysSection(days: List<MarketBreadthDay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "近十日涨跌家数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "同花顺口径 · 点击曲线查看当日明细",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (days.isEmpty()) {
                Text("暂无涨跌家数", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val chrono = remember(days) { days.asReversed() }
                var selectedIndex by remember(days) {
                    mutableIntStateOf((chrono.size - 1).coerceAtLeast(0))
                }
                val selected = chrono.getOrNull(selectedIndex) ?: chrono.last()
                BreadthDayDetail(selected)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendItem(color = RiseRed, label = "上涨")
                    LegendItem(color = FallGreen, label = "下跌")
                    LegendItem(color = FlatGray, label = "平盘")
                }
                Spacer(Modifier.height(8.dp))
                BreadthTrendChart(
                    days = chrono,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDateShort(chrono.first().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击节点查看当日",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateShort(chrono.last().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadthDayDetail(day: MarketBreadthDay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = formatDateLabel(day.date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DetailMetric("上涨", day.rise.toString(), RiseRed)
            DetailMetric("下跌", day.fall.toString(), FallGreen)
            DetailMetric("平盘", day.flat.toString(), FlatGray)
            DetailMetric("涨停", day.limitUp.toString(), RiseRed)
            DetailMetric("跌停", day.limitDown.toString(), FallGreen)
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BreadthTrendChart(
    days: List<MarketBreadthDay>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxCount = days.maxOf { max(it.rise, max(it.fall, it.flat)) }.coerceAtLeast(1).toFloat()
    val axisLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier.pointerInput(days) {
            detectTapGestures { offset ->
                val leftPad = 36.dp.toPx()
                val rightPad = 12.dp.toPx()
                val w = size.width - leftPad - rightPad
                if (w <= 0f || days.isEmpty()) return@detectTapGestures
                val idx = if (days.size == 1) {
                    0
                } else {
                    days.indices.minBy { i ->
                        val x = leftPad + w * i / (days.size - 1).toFloat()
                        abs(offset.x - x)
                    }
                }
                onSelectIndex(idx)
            }
        }
    ) {
        val leftPad = 36.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = 8.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0f || h <= 0f) return@Canvas

        fun xAt(i: Int): Float {
            if (days.size == 1) return leftPad + w / 2f
            return leftPad + w * i / (days.size - 1).toFloat()
        }

        fun yCount(v: Float): Float =
            topPad + h * (1f - (v / maxCount).coerceIn(0f, 1f))

        for (i in 0..4) {
            val y = topPad + h * i / 4f
            drawLine(GridLine, Offset(leftPad, y), Offset(leftPad + w, y), strokeWidth = 1f)
        }

        val sel = selectedIndex.coerceIn(0, days.lastIndex)
        val selX = xAt(sel)
        drawLine(
            color = selectColor.copy(alpha = 0.35f),
            start = Offset(selX, topPad),
            end = Offset(selX, topPad + h),
            strokeWidth = 2.dp.toPx()
        )

        fun drawSeries(color: Color, yOf: (MarketBreadthDay) -> Float) {
            if (days.size == 1) {
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(xAt(0), yOf(days[0])))
                return
            }
            val path = Path()
            days.forEachIndexed { i, day ->
                val p = Offset(xAt(i), yOf(day))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            days.forEachIndexed { i, day ->
                val center = Offset(xAt(i), yOf(day))
                val r = if (i == sel) 5.dp.toPx() else 2.5.dp.toPx()
                drawCircle(color, radius = r, center = center)
            }
        }

        drawSeries(RiseRed) { yCount(it.rise.toFloat()) }
        drawSeries(FallGreen) { yCount(it.fall.toFloat()) }
        drawSeries(FlatGray) { yCount(it.flat.toFloat()) }
        drawLine(
            RiseRed.copy(alpha = 0.35f),
            Offset(leftPad, topPad),
            Offset(leftPad, topPad + h),
            2f
        )
    }

    Text(
        text = "0 ~ ${maxCount.toInt()}",
        style = MaterialTheme.typography.labelSmall,
        color = axisLabel
    )
}

@Composable
private fun TurnoverDaysSection(days: List<MarketBreadthDay>) {
    val withTo = remember(days) { days.filter { it.turnoverYi > 0 || it.turnoverText.isNotBlank() } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "近十日成交额",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "同花顺两市成交额 · 点击查看当日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (withTo.isEmpty()) {
                Text("暂无成交额", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val chrono = remember(withTo) { withTo.asReversed() }
                var selectedIndex by remember(withTo) {
                    mutableIntStateOf((chrono.size - 1).coerceAtLeast(0))
                }
                val selected = chrono.getOrNull(selectedIndex) ?: chrono.last()
                val flagLabel = when (selected.turnoverFlag) {
                    1 -> "较昨放量"
                    -1 -> "较昨缩量"
                    else -> "—"
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = formatDateLabel(selected.date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailMetric(
                            "成交额",
                            selected.turnoverText.ifBlank { formatYi(selected.turnoverYi) },
                            RateBlue
                        )
                        DetailMetric("折合", formatYi(selected.turnoverYi), RateBlue)
                        DetailMetric("量能", flagLabel, when (selected.turnoverFlag) {
                            1 -> RiseRed
                            -1 -> FallGreen
                            else -> FlatGray
                        })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LegendItem(color = RateBlue, label = "成交额(亿)")
                }
                Spacer(Modifier.height(8.dp))
                TurnoverTrendChart(
                    days = chrono,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDateShort(chrono.first().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击节点查看当日",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateShort(chrono.last().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnoverTrendChart(
    days: List<MarketBreadthDay>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val values = remember(days) { days.map { it.turnoverYi.toFloat() } }
    val maxV = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val minV = values.minOrNull()?.coerceAtMost(maxV - 1f) ?: 0f
    // 成交额波动区间通常不大，用 min~max 放大走势
    val lo = (minV * 0.92f).coerceAtLeast(0f)
    val hi = (maxV * 1.04f).coerceAtLeast(lo + 1f)
    val span = (hi - lo).coerceAtLeast(1f)
    val axisLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val tickStyle = TextStyle(color = axisLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    val ticks = remember(lo, hi) {
        listOf(lo, lo + span * 0.5f, hi)
    }

    Canvas(
        modifier = modifier.pointerInput(days) {
            detectTapGestures { offset ->
                val leftPad = 48.dp.toPx()
                val rightPad = 12.dp.toPx()
                val w = size.width - leftPad - rightPad
                if (w <= 0f || days.isEmpty()) return@detectTapGestures
                val idx = if (days.size == 1) 0 else days.indices.minBy { i ->
                    val x = leftPad + w * i / (days.size - 1).toFloat()
                    abs(offset.x - x)
                }
                onSelectIndex(idx)
            }
        }
    ) {
        val leftPad = 48.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 10.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0f || h <= 0f) return@Canvas

        fun xAt(i: Int): Float {
            if (days.size == 1) return leftPad + w / 2f
            return leftPad + w * i / (days.size - 1).toFloat()
        }

        fun yOf(v: Float): Float =
            topPad + h * (1f - ((v - lo) / span).coerceIn(0f, 1f))

        ticks.forEach { tv ->
            val y = yOf(tv)
            drawLine(GridLine, Offset(leftPad, y), Offset(leftPad + w, y), strokeWidth = 1f)
            val label = formatYiShort(tv.toDouble())
            val layout = textMeasurer.measure(label, style = tickStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (leftPad - layout.size.width - 4.dp.toPx()).coerceAtLeast(0f),
                    y - layout.size.height / 2f
                )
            )
        }

        val sel = selectedIndex.coerceIn(0, days.lastIndex)
        drawLine(
            color = selectColor.copy(alpha = 0.35f),
            start = Offset(xAt(sel), topPad),
            end = Offset(xAt(sel), topPad + h),
            strokeWidth = 2.dp.toPx()
        )

        if (days.size == 1) {
            drawCircle(RateBlue, 4.dp.toPx(), Offset(xAt(0), yOf(values[0])))
        } else {
            val path = Path()
            values.forEachIndexed { i, v ->
                val p = Offset(xAt(i), yOf(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, RateBlue, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { i, v ->
                val r = if (i == sel) 5.dp.toPx() else 2.5.dp.toPx()
                drawCircle(RateBlue, r, Offset(xAt(i), yOf(v)))
            }
        }

        drawLine(
            RateBlue.copy(alpha = 0.35f),
            Offset(leftPad, topPad),
            Offset(leftPad, topPad + h),
            2f
        )
    }
}

private fun formatYi(yi: Double): String = when {
    yi >= 10_000 -> String.format(Locale.CHINA, "%.2f万亿", yi / 10_000.0)
    yi >= 100 -> String.format(Locale.CHINA, "%.0f亿", yi)
    yi > 0 -> String.format(Locale.CHINA, "%.1f亿", yi)
    else -> "—"
}

private fun formatYiShort(yi: Double): String = when {
    yi >= 10_000 -> String.format(Locale.CHINA, "%.1f万亿", yi / 10_000.0)
    yi >= 100 -> String.format(Locale.CHINA, "%.0f亿", yi)
    yi > 0 -> String.format(Locale.CHINA, "%.0f亿", yi)
    else -> "0"
}

@Composable
private fun RecentDaysSection(days: List<LimitDayStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "近二十日涨跌停",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "涨停家数 / 跌停家数 / 封板率 · 点击曲线查看当日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (days.isEmpty()) {
                Text("暂无历史数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val chrono = remember(days) { days.asReversed() }
                var selectedIndex by remember(days) {
                    mutableIntStateOf((chrono.size - 1).coerceAtLeast(0))
                }
                val selected = chrono.getOrNull(selectedIndex) ?: chrono.last()
                LimitDayDetail(selected)
                Spacer(Modifier.height(10.dp))
                val maxUp = days.maxOf { it.up.num }.coerceAtLeast(1)
                days.forEach { day ->
                    DayStatRow(day = day, maxUp = maxUp)
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "走势曲线",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                ChartLegend()
                Spacer(Modifier.height(8.dp))
                LimitTrendChart(
                    days = chrono,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDateShort(chrono.first().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击节点查看当日",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateShort(chrono.last().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitDayDetail(day: LimitDayStat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = formatDateLabel(day.date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DetailMetric("涨停", day.up.num.toString(), RiseRed)
            DetailMetric("跌停", day.down.num.toString(), FallGreen)
            DetailMetric("封板率", pct(day.up.rate), RateBlue)
            DetailMetric("开板", day.up.openNum.toString(), FlatGray)
            DetailMetric("曾涨停", day.up.historyNum.toString(), RiseRed)
        }
    }
}

@Composable
private fun ChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LegendItem(color = RiseRed, label = "涨停")
        LegendItem(color = FallGreen, label = "跌停")
        LegendItem(color = RateBlue, label = "封板率")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LimitTrendChart(
    days: List<LimitDayStat>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxCount = days.maxOf { max(it.up.num, it.down.num) }.coerceAtLeast(1).toFloat()
    val axisLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier.pointerInput(days) {
            detectTapGestures { offset ->
                val leftPad = 36.dp.toPx()
                val rightPad = 36.dp.toPx()
                val w = size.width - leftPad - rightPad
                if (w <= 0f || days.isEmpty()) return@detectTapGestures
                val idx = if (days.size == 1) {
                    0
                } else {
                    days.indices.minBy { i ->
                        val x = leftPad + w * i / (days.size - 1).toFloat()
                        abs(offset.x - x)
                    }
                }
                onSelectIndex(idx)
            }
        }
    ) {
        val leftPad = 36.dp.toPx()
        val rightPad = 36.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = 8.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0f || h <= 0f) return@Canvas

        fun xAt(i: Int): Float {
            if (days.size == 1) return leftPad + w / 2f
            return leftPad + w * i / (days.size - 1).toFloat()
        }

        fun yCount(v: Float): Float = topPad + h * (1f - (v / maxCount).coerceIn(0f, 1f))
        fun yRate(rate: Double): Float {
            val pct = (rate * 100.0).toFloat().coerceIn(0f, 100f)
            return topPad + h * (1f - pct / 100f)
        }

        for (i in 0..4) {
            val y = topPad + h * i / 4f
            drawLine(GridLine, Offset(leftPad, y), Offset(leftPad + w, y), strokeWidth = 1f)
        }

        val sel = selectedIndex.coerceIn(0, days.lastIndex)
        val selX = xAt(sel)
        drawLine(
            color = selectColor.copy(alpha = 0.35f),
            start = Offset(selX, topPad),
            end = Offset(selX, topPad + h),
            strokeWidth = 2.dp.toPx()
        )

        fun drawSeries(color: Color, yOf: (LimitDayStat) -> Float) {
            if (days.size == 1) {
                val cx = xAt(0)
                val cy = yOf(days[0])
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(cx, cy))
                return
            }
            val path = Path()
            days.forEachIndexed { i, day ->
                val p = Offset(xAt(i), yOf(day))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            days.forEachIndexed { i, day ->
                val r = if (i == sel) 5.dp.toPx() else 2.5.dp.toPx()
                drawCircle(color, radius = r, center = Offset(xAt(i), yOf(day)))
            }
        }

        drawSeries(RiseRed) { yCount(it.up.num.toFloat()) }
        drawSeries(FallGreen) { yCount(it.down.num.toFloat()) }
        drawSeries(RateBlue) { yRate(it.up.rate) }

        drawLine(RiseRed.copy(alpha = 0.4f), Offset(leftPad, topPad), Offset(leftPad, topPad + h), 2f)
        drawLine(RateBlue.copy(alpha = 0.4f), Offset(leftPad + w, topPad), Offset(leftPad + w, topPad + h), 2f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "0~${maxCount.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = axisLabel
        )
        Text(
            text = "0%~100%",
            style = MaterialTheme.typography.labelSmall,
            color = RateBlue
        )
    }
}

@Composable
private fun DayStatRow(day: LimitDayStat, maxUp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateShort(day.date),
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BarTrack)
        ) {
            val frac = (day.up.num.toFloat() / maxUp).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(frac)
                    .background(RiseRed.copy(alpha = 0.85f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${day.up.num}",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = RiseRed,
            textAlign = TextAlign.End
        )
        Text(
            text = "/${day.down.num}",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = FallGreen
        )
        Text(
            text = pct(day.up.rate),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LadderSection(
    date: String,
    today: List<LadderLevel>,
    history: List<LadderDay>,
    historyLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "连板天梯",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatDateLabel(date)} · 按连板高度分层",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            if (today.isEmpty()) {
                Text("今日暂无连板", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                today.forEach { level ->
                    LadderLevelRow(level)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (historyLoading) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "历史天梯 / 高度曲线加载中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LadderHeightSection(history)
            }

            if (history.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "近二十日天梯矩阵",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "横轴日期，纵轴板高（有票标红）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LadderMatrix(history)
            }
        }
    }
}

/** 从天梯取当日最高连板高度，以及该高度上的家数 */
private fun LadderDay.maxBoardHeight(): Int =
    levels.maxOfOrNull { it.height } ?: 0

private fun LadderDay.maxBoardCount(): Int {
    val h = maxBoardHeight()
    if (h <= 0) return 0
    return levels.find { it.height == h }?.stocks?.size ?: 0
}

@Composable
private fun LadderHeightSection(history: List<LadderDay>) {
    val chrono = remember(history) { history.asReversed() }
    var selectedIndex by remember(history) {
        mutableIntStateOf((chrono.size - 1).coerceAtLeast(0))
    }
    val selected = chrono.getOrNull(selectedIndex) ?: chrono.last()
    val maxH = selected.maxBoardHeight()
    val maxCnt = selected.maxBoardCount()

    Text(
        "连板高度曲线",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        "近二十日最高连板高度 · 点击查看当日",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = formatDateLabel(selected.date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DetailMetric("最高板", if (maxH > 0) "${maxH}板" else "—", RiseRed)
            DetailMetric("最高板家数", maxCnt.toString(), RiseRed)
            DetailMetric("有票层级", selected.levels.count { it.stocks.isNotEmpty() }.toString(), FlatGray)
            DetailMetric(
                "连板家数",
                selected.levels.filter { it.height >= 2 }.sumOf { it.stocks.size }.toString(),
                RateBlue
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LegendItem(color = RiseRed, label = "最高连板高度")
    }
    Spacer(Modifier.height(8.dp))
    LadderHeightChart(
        days = chrono,
        selectedIndex = selectedIndex,
        onSelectIndex = { selectedIndex = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDateShort(chrono.first().date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "点击节点查看当日",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDateShort(chrono.last().date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LadderHeightChart(
    days: List<LadderDay>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val heights = remember(days) { days.map { it.maxBoardHeight() } }
    val maxH = heights.maxOrNull()?.coerceAtLeast(1) ?: 1
    val maxCount = maxH.toFloat()
    val axisLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val selectColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val tickStyle = TextStyle(
        color = axisLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
    // 连板高度是整数，Y 轴每板都标（1板、2板、3板…）
    val ticks = remember(maxH) { (0..maxH).toList() }

    Canvas(
        modifier = modifier.pointerInput(days) {
            detectTapGestures { offset ->
                val leftPad = 40.dp.toPx()
                val rightPad = 12.dp.toPx()
                val w = size.width - leftPad - rightPad
                if (w <= 0f || days.isEmpty()) return@detectTapGestures
                val idx = if (days.size == 1) {
                    0
                } else {
                    days.indices.minBy { i ->
                        val x = leftPad + w * i / (days.size - 1).toFloat()
                        abs(offset.x - x)
                    }
                }
                onSelectIndex(idx)
            }
        }
    ) {
        val leftPad = 40.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 10.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0f || h <= 0f) return@Canvas

        fun xAt(i: Int): Float {
            if (days.size == 1) return leftPad + w / 2f
            return leftPad + w * i / (days.size - 1).toFloat()
        }

        fun yHeight(v: Int): Float =
            topPad + h * (1f - (v / maxCount).coerceIn(0f, 1f))

        ticks.forEach { board ->
            val y = yHeight(board)
            drawLine(GridLine, Offset(leftPad, y), Offset(leftPad + w, y), strokeWidth = 1f)
            val label = if (board == 0) "0" else "${board}板"
            val layout = textMeasurer.measure(label, style = tickStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (leftPad - layout.size.width - 4.dp.toPx()).coerceAtLeast(0f),
                    y - layout.size.height / 2f
                )
            )
        }

        val sel = selectedIndex.coerceIn(0, days.lastIndex)
        drawLine(
            color = selectColor.copy(alpha = 0.35f),
            start = Offset(xAt(sel), topPad),
            end = Offset(xAt(sel), topPad + h),
            strokeWidth = 2.dp.toPx()
        )

        if (days.size == 1) {
            drawCircle(
                RiseRed,
                radius = 4.dp.toPx(),
                center = Offset(xAt(0), yHeight(heights[0]))
            )
        } else {
            val path = Path()
            heights.forEachIndexed { i, hv ->
                val p = Offset(xAt(i), yHeight(hv))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path = path,
                color = RiseRed,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            heights.forEachIndexed { i, hv ->
                val r = if (i == sel) 5.dp.toPx() else 2.5.dp.toPx()
                drawCircle(RiseRed, radius = r, center = Offset(xAt(i), yHeight(hv)))
            }
        }

        drawLine(
            RiseRed.copy(alpha = 0.35f),
            Offset(leftPad, topPad),
            Offset(leftPad, topPad + h),
            2f
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LadderLevelRow(level: LadderLevel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${level.height}板",
            modifier = Modifier
                .width(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RiseRed.copy(alpha = 0.12f))
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = RiseRed,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            level.stocks.forEach { s ->
                Text(
                    text = s.name.ifBlank { s.code },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LadderMatrix(history: List<LadderDay>) {
    val heights = history.flatMap { day -> day.levels.map { it.height } }
        .distinct()
        .sortedDescending()
        .ifEmpty { listOf(2) }
    val scroll = rememberScrollState()
    val colW = 72.dp

    Row(Modifier.horizontalScroll(scroll)) {
        Column {
            Row {
                Box(Modifier.width(36.dp))
                history.forEach { day ->
                    Text(
                        text = formatDateShort(day.date),
                        modifier = Modifier.width(colW),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            heights.forEach { h ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${h}板",
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = RiseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    history.forEach { day ->
                        val stocks = day.levels.find { it.height == h }?.stocks.orEmpty()
                        Column(
                            modifier = Modifier
                                .width(colW)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (stocks.isEmpty()) BarTrack.copy(alpha = 0.5f)
                                    else RiseRed.copy(alpha = 0.1f)
                                )
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (stocks.isEmpty()) {
                                Text(
                                    "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                stocks.take(3).forEach { s ->
                                    Text(
                                        text = s.name.ifBlank { s.code }.take(4),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RiseRed,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (stocks.size > 3) {
                                    Text(
                                        "+${stocks.size - 3}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LimitStockCard(stock: LimitStock, down: Boolean) {
    val accent = if (down) FallGreen else RiseRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stock.name.ifBlank { stock.code },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stock.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = String.format(
                        Locale.CHINA,
                        "%+.2f%%",
                        stock.changeRate
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    if (stock.highDays.isNotBlank()) append(stock.highDays)
                    if (stock.limitUpType.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(stock.limitUpType)
                    }
                    if (stock.openNum > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("开板").append(stock.openNum).append("次")
                    }
                    if (stock.turnoverRate > 0) {
                        if (isNotEmpty()) append(" · ")
                        append(String.format(Locale.CHINA, "换手%.1f%%", stock.turnoverRate))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (stock.reason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stock.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun pct(rate: Double): String =
    if (rate <= 0.0) "—" else String.format(Locale.CHINA, "%.0f%%", rate * 100)

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(millis))

private fun formatDateLabel(yyyymmdd: String): String {
    if (yyyymmdd.length != 8) return yyyymmdd.ifBlank { "今日" }
    return "${yyyymmdd.substring(4, 6)}-${yyyymmdd.substring(6, 8)}"
}

private fun formatDateShort(yyyymmdd: String): String {
    if (yyyymmdd.length != 8) return yyyymmdd.takeLast(4)
    return "${yyyymmdd.substring(4, 6)}/${yyyymmdd.substring(6, 8)}"
}
