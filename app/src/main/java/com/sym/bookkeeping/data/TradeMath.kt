package com.sym.bookkeeping.data

/**
 * 交易口径计算。
 *
 * 盈亏比一律按「去税费」价差算：
 * (卖出成交金额 - 买入成交金额) / 买入成交金额 × 100
 * 不用券商/同花顺图上的含税收益率。
 */
object TradeMath {

    /** 去税费盈亏比（百分比数值，如 -7.42 表示 -7.42%） */
    fun riskRewardRatioWithoutFee(buyAmount: Double, sellAmount: Double): Double {
        if (buyAmount <= 0.0) return 0.0
        return (sellAmount - buyAmount) / buyAmount * 100.0
    }

    /**
     * 若已知净盈亏与税费合计，可用毛利反推：
     * 毛利 ≈ profit + fee；再除以买入成交金额。
     */
    fun riskRewardRatioFromNet(profit: Double, fee: Double, buyAmount: Double): Double {
        if (buyAmount <= 0.0) return 0.0
        return (profit + fee) / buyAmount * 100.0
    }

    fun roundRatio(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}
