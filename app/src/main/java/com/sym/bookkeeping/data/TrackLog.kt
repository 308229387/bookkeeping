package com.sym.bookkeeping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 推荐标的跟踪日志（尚未成交 / 观察池）。
 * 已成交交易只进 [TradeRecord]，不进本表。
 */
@Entity(tableName = "track_logs")
data class TrackLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 标的名称（必填，列表标题用） */
    val symbol: String = "",
    /** 推荐手法，如「趋势回踩·硬止损法」 */
    val method: String = "",
    /** 跟踪正文：为何推荐、买点条件、每日观察 */
    val content: String = "",
    /** 观察中 / 已触发待记 / 已放弃 */
    val status: String = "观察中",
    val trackDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
