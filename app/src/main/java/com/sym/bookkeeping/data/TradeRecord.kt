package com.sym.bookkeeping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 标的名称或代码 */
    val symbol: String = "",
    /** 交易手法/策略 */
    val method: String = "",
    /** 盈亏比（百分比数值），例如 -2.48 表示 -2.48% */
    val riskRewardRatio: Double = 0.0,
    /** 盈利金额，亏损为负数 */
    val profit: Double = 0.0,
    /** 税费 / 手续费合计 */
    val fee: Double = 0.0,
    /** 持股数 */
    val shareCount: Int = 0,
    /** 交易日志 / 心得 */
    val journal: String = "",
    /** 交易日期（毫秒时间戳） */
    val tradeDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isWin: Boolean get() = profit > 0
    val isLoss: Boolean get() = profit < 0
}
