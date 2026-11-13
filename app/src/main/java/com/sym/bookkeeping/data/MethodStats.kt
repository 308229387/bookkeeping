package com.sym.bookkeeping.data

data class MethodStats(
    val method: String,
    val totalCount: Int,
    val winCount: Int,
    val lossCount: Int,
    val breakEvenCount: Int,
    val totalProfit: Double,
    val avgProfit: Double,
    val avgRiskRewardRatio: Double
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
            .map { (method, list) ->
                val wins = list.count { it.isWin }
                val losses = list.count { it.isLoss }
                val breakEven = list.size - wins - losses
                val totalProfit = list.sumOf { it.profit }
                MethodStats(
                    method = method,
                    totalCount = list.size,
                    winCount = wins,
                    lossCount = losses,
                    breakEvenCount = breakEven,
                    totalProfit = totalProfit,
                    avgProfit = if (list.isEmpty()) 0.0 else totalProfit / list.size,
                    avgRiskRewardRatio = if (list.isEmpty()) {
                        0.0
                    } else {
                        list.map { it.riskRewardRatio }.average()
                    }
                )
            }
            .sortedByDescending { it.winRate }
    }

    fun overall(records: List<TradeRecord>): MethodStats {
        val wins = records.count { it.isWin }
        val losses = records.count { it.isLoss }
        val breakEven = records.size - wins - losses
        val totalProfit = records.sumOf { it.profit }
        return MethodStats(
            method = "全部",
            totalCount = records.size,
            winCount = wins,
            lossCount = losses,
            breakEvenCount = breakEven,
            totalProfit = totalProfit,
            avgProfit = if (records.isEmpty()) 0.0 else totalProfit / records.size,
            avgRiskRewardRatio = if (records.isEmpty()) {
                0.0
            } else {
                records.map { it.riskRewardRatio }.average()
            }
        )
    }
}
