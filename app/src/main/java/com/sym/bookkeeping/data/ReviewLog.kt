package com.sym.bookkeeping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 复盘日志（大盘/情绪复盘，不属于单笔交易） */
@Entity(tableName = "review_logs")
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 标题，如「2026-11-13 盘面复盘」 */
    val title: String = "",
    /** 正文（可较长） */
    val content: String = "",
    /** 复盘日期 */
    val reviewDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
