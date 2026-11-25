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
    /**
     * 盈亏比（百分比数值），例如 -7.42 表示 -7.42%。
     * 口径：去税费后 (卖出成交额 - 买入成交额) / 买入成交额，不用行情软件含税比例。
     */
    val riskRewardRatio: Double = 0.0,
    /** 盈利金额，亏损为负数 */
    val profit: Double = 0.0,
    /** 税费 / 手续费合计 */
    val fee: Double = 0.0,
    /** 持股数 */
    val shareCount: Int = 0,
    /** 交易日志 / 心得 */
    val journal: String = "",
    /**
     * K 线截图相对路径：优先 assets/，否则 filesDir。
     * 单图：`trade_charts/wentou_600715.jpg`
     * 多图：用 `|` 分隔，例 `trade_charts/a.webp|trade_charts/b.webp`
     */
    val chartImagePath: String = "",
    /** 交易日期（毫秒时间戳） */
    val tradeDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 金额未知（持股&lt;0，如 gp 导入）时按盈亏比判定胜负 */
    val isWin: Boolean
        get() = if (shareCount < 0) riskRewardRatio > 0 else profit > 0
    val isLoss: Boolean
        get() = if (shareCount < 0) riskRewardRatio < 0 else profit < 0

    /** 解析 [chartImagePath] 为路径列表（兼容单图与 `|` 多图） */
    fun chartImagePaths(): List<String> =
        chartImagePath.split('|')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    companion object {
        fun joinChartPaths(paths: List<String>): String =
            paths.map { it.trim() }.filter { it.isNotBlank() }.joinToString("|")
    }
}
