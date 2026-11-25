package com.sym.bookkeeping.ui.strategy

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sym.bookkeeping.data.EmotionPlaybooks
import com.sym.bookkeeping.data.StrategyCatalog
import com.sym.bookkeeping.data.StrategyDiagramCatalog
import com.sym.bookkeeping.ui.components.AssetImage
import com.sym.bookkeeping.ui.theme.LossRed
import com.sym.bookkeeping.ui.theme.ProfitGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyDetailScreen(
    strategyId: String,
    onBack: () -> Unit
) {
    val guide = StrategyCatalog.findById(strategyId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.name ?: "战法详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (guide == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("未找到该战法")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = guide.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("胜率${guide.winRateLevel}")
                TagChip(guide.style)
                TagChip("持股${guide.holdingDaysShort}")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("可操作性${guide.operability}")
                TagChip("盯盘${guide.watchIntensity}")
                TagChip(guide.difficulty)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (guide.highWinRatePick) TagChip("高胜率优选", emphasized = true)
                if (guide.workerFriendly) TagChip("工薪友好", emphasized = true)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = guide.summary,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(20.dp))

            val diagrams = StrategyDiagramCatalog.forStrategy(guide.id)
            if (diagrams.isNotEmpty()) {
                SectionCard(title = "样图理解") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "示意教学图，帮助对照均线/箱体/止损位置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        diagrams.forEach { diagram ->
                            Text(diagram.title, fontWeight = FontWeight.SemiBold)
                            AssetImage(
                                assetPath = diagram.assetPath,
                                contentDescription = diagram.title
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val emotionStyle = guide.style == EmotionPlaybooks.STYLE_TAG
            SectionCard(title = "可操作性评估") {
                Text(
                    text = guide.operabilityNote,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = if (emotionStyle) "出手时机与仓位" else "买入标准") {
                NumberedList(guide.buyRules)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = if (emotionStyle) "卖点" else "卖出标准") {
                NumberedList(guide.sellRules)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = if (emotionStyle) "止损与风控" else "止损点") {
                NumberedList(guide.stopLossRules)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "胜率分析") {
                Text(
                    text = guide.winRateAnalysis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (guide.afterWorkSteps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard(title = "工薪实操（下班后）") {
                    NumberedList(guide.afterWorkSteps)
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "持股天数范围") {
                Text(
                    text = guide.holdingDaysDetail,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "如何操作") {
                NumberedList(guide.howTo)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "好处") {
                BulletList(items = guide.pros, markerColor = ProfitGreen, marker = "+")
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "缺点") {
                BulletList(items = guide.cons, markerColor = LossRed, marker = "−")
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "实战提醒") {
                NumberedList(guide.tips)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "A股注意") {
                NumberedList(guide.aShareNotes)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "内容仅供学习复盘参考，不构成投资建议。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun NumberedList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, text ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun BulletList(
    items: List<String>,
    markerColor: androidx.compose.ui.graphics.Color,
    marker: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, text ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = marker,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = markerColor,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
