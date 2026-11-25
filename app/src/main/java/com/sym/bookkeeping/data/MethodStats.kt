package com.sym.bookkeeping.data

data class MethodStats(
    val method: String,
    val totalCount: Int,
    val winCount: Int,
    val lossCount: Int,
    val breakEvenCount: Int,
    val totalProfit: Double,
    val avgProfit: Double,
    val avgRiskRewardRatio: Double,
    /** 各笔盈亏比之和（总点位） */
    val totalRiskRewardRatio: Double = 0.0
) {
    val winRate: Double
        get() = if (totalCount == 0) 0.0 else winCount.toDouble() / totalCount

    val winRatePercent: Double
        get() = winRate * 100
}

object AnalysisCalculator {
    fun byMethod(records: List<TradeRecord>): List<MethodStats> {
        return records
            .groupBy { it.method.ifBlank { "未分类" } }
            .map { (method, list) -> statsFor(method, list) }
            .sortedByDescending { it.winRate }
    }

    fun overall(records: List<TradeRecord>): MethodStats = statsFor("全部", records)

    private fun statsFor(method: String, list: List<TradeRecord>): MethodStats {
        val wins = list.count { it.isWin }
        val losses = list.count { it.isLoss }
        val breakEven = list.size - wins - losses
        val totalProfit = list.sumOf { it.profit }
        val totalRatio = list.sumOf { it.riskRewardRatio }
        return MethodStats(
            method = method,
            totalCount = list.size,
            winCount = wins,
            lossCount = losses,
            breakEvenCount = breakEven,
            totalProfit = totalProfit,
            avgProfit = if (list.isEmpty()) 0.0 else totalProfit / list.size,
            avgRiskRewardRatio = if (list.isEmpty()) 0.0 else totalRatio / list.size,
            totalRiskRewardRatio = totalRatio
        )
    }
}
