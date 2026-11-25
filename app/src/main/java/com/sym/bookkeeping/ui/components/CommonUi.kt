package com.sym.bookkeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sym.bookkeeping.data.MethodStats
import com.sym.bookkeeping.data.TradeRecord
import com.sym.bookkeeping.ui.theme.LossRed
import com.sym.bookkeeping.ui.theme.ProfitGreen
import com.sym.bookkeeping.ui.util.formatDate
import com.sym.bookkeeping.ui.util.formatMoney
import com.sym.bookkeeping.ui.util.formatPercent
import com.sym.bookkeeping.ui.util.formatRatio
import com.sym.bookkeeping.ui.util.formatSignedMoney

/** A 股习惯：盈利/上涨红，亏损/下跌绿 */
fun profitLossColor(value: Double): Color = when {
    value > 0 -> LossRed
    value < 0 -> ProfitGreen
    else -> Color.Unspecified
}

@Composable
fun ProfitText(value: Double, modifier: Modifier = Modifier) {
    val color = when {
        value > 0 -> LossRed
        value < 0 -> ProfitGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = formatSignedMoney(value),
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun TradeRecordCard(
    record: TradeRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.symbol.ifBlank { "未命名标的" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = record.method.ifBlank { "未分类手法" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ProfitText(record.profit)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetaChip(label = "盈亏比", value = formatRatio(record.riskRewardRatio))
                MetaChip(label = "持股", value = "${record.shareCount}")
                MetaChip(label = "税费", value = formatMoney(record.fee))
                MetaChip(label = "日期", value = formatDate(record.tradeDate))
            }
        }
    }
}

@Composable
fun MetaChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MethodStatsCard(stats: MethodStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stats.method,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "胜率 ${formatPercent(stats.winRate)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { stats.winRate.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = ProfitGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("总笔数", stats.totalCount.toString())
                StatItem("盈利", stats.winCount.toString(), LossRed)
                StatItem("亏损", stats.lossCount.toString(), ProfitGreen)
                StatItem(
                    "总盈亏",
                    formatSignedMoney(stats.totalProfit),
                    profitLossColor(stats.totalProfit)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    "平均盈利",
                    formatSignedMoney(stats.avgProfit),
                    profitLossColor(stats.avgProfit)
                )
                StatItem(
                    "平均盈亏比",
                    formatRatio(stats.avgRiskRewardRatio),
                    profitLossColor(stats.avgRiskRewardRatio)
                )
                StatItem(
                    "总点位",
                    formatRatio(stats.totalRiskRewardRatio),
                    profitLossColor(stats.totalRiskRewardRatio)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor == Color.Unspecified) {
                MaterialTheme.colorScheme.onSurface
            } else {
                valueColor
            }
        )
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("记", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SummaryStrip(
    totalCount: Int,
    totalProfit: Double,
    winRate: Double,
    totalPoints: Double = 0.0,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val pad = if (compact) 8.dp else 16.dp
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pad, vertical = if (compact) 6.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("笔数", style = MaterialTheme.typography.labelSmall)
                Text("$totalCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总盈亏", style = MaterialTheme.typography.labelSmall)
                ProfitText(totalProfit)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总胜率", style = MaterialTheme.typography.labelSmall)
                Text(
                    formatPercent(winRate),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总点位", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = formatRatio(totalPoints),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = profitLossColor(totalPoints).takeIf { it != Color.Unspecified }
                        ?: MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
