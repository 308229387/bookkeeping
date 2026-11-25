package com.sym.bookkeeping.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(
    private val dao: TradeDao,
    private val reviewLogDao: ReviewLogDao,
    private val trackLogDao: TrackLogDao
) {
    fun observeAll(): Flow<List<TradeRecord>> = dao.observeAll()

    fun observeById(id: Long): Flow<TradeRecord?> = dao.observeById(id)

    fun observeWithJournal(): Flow<List<TradeRecord>> = dao.observeWithJournal()

    fun observeMethods(): Flow<List<String>> = dao.observeMethods()

    fun observeReviewLogs(): Flow<List<ReviewLog>> = reviewLogDao.observeAll()

    fun observeReviewById(id: Long): Flow<ReviewLog?> = reviewLogDao.observeById(id)

    fun observeTrackLogs(): Flow<List<TrackLog>> = trackLogDao.observeAll()

    fun observeTrackById(id: Long): Flow<TrackLog?> = trackLogDao.observeById(id)

    suspend fun getAll(): List<TradeRecord> = dao.getAll()

    suspend fun getAllReviews(): List<ReviewLog> = reviewLogDao.getAll()

    suspend fun getAllTracks(): List<TrackLog> = trackLogDao.getAll()

    suspend fun trackCount(): Int = trackLogDao.count()

    suspend fun getById(id: Long): TradeRecord? = dao.getById(id)

    suspend fun getReviewById(id: Long): ReviewLog? = reviewLogDao.getById(id)

    suspend fun getTrackById(id: Long): TrackLog? = trackLogDao.getById(id)

    suspend fun save(record: TradeRecord): Long {
        val now = System.currentTimeMillis()
        return if (record.id == 0L) {
            dao.insert(record.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(record.copy(updatedAt = now))
            record.id
        }
    }

    suspend fun saveReview(log: ReviewLog): Long {
        val now = System.currentTimeMillis()
        return if (log.id == 0L) {
            reviewLogDao.insert(log.copy(createdAt = now, updatedAt = now))
        } else {
            reviewLogDao.update(log.copy(updatedAt = now))
            log.id
        }
    }

    suspend fun saveTrack(log: TrackLog): Long {
        val now = System.currentTimeMillis()
        return if (log.id == 0L) {
            trackLogDao.insert(log.copy(createdAt = now, updatedAt = now))
        } else {
            trackLogDao.update(log.copy(updatedAt = now))
            log.id
        }
    }

    suspend fun delete(record: TradeRecord) = dao.delete(record)

    suspend fun deleteReview(log: ReviewLog) = reviewLogDao.delete(log)

    suspend fun deleteTrack(log: TrackLog) = trackLogDao.delete(log)

    suspend fun replaceAll(records: List<TradeRecord>) {
        dao.deleteAll()
        if (records.isNotEmpty()) {
            dao.insertAll(records.map { it.copy(id = 0) })
        }
    }

    suspend fun mergeImport(records: List<TradeRecord>) {
        if (records.isNotEmpty()) {
            dao.insertAll(records.map { it.copy(id = 0) })
        }
    }

    suspend fun replaceAllReviews(logs: List<ReviewLog>) {
        reviewLogDao.deleteAll()
        if (logs.isNotEmpty()) {
            reviewLogDao.insertAll(logs.map { it.copy(id = 0) })
        }
    }

    suspend fun mergeReviews(logs: List<ReviewLog>) {
        if (logs.isNotEmpty()) {
            reviewLogDao.insertAll(logs.map { it.copy(id = 0) })
        }
    }

    suspend fun replaceAllTracks(logs: List<TrackLog>) {
        trackLogDao.deleteAll()
        if (logs.isNotEmpty()) {
            trackLogDao.insertAll(logs.map { it.copy(id = 0) })
        }
    }

    suspend fun mergeTracks(logs: List<TrackLog>) {
        if (logs.isNotEmpty()) {
            trackLogDao.insertAll(logs.map { it.copy(id = 0) })
        }
    }

    suspend fun replaceFullBackup(
        records: List<TradeRecord>,
        reviews: List<ReviewLog>,
        tracks: List<TrackLog> = emptyList()
    ) {
        replaceAll(records)
        replaceAllReviews(reviews)
        replaceAllTracks(tracks)
    }

    suspend fun mergeFullBackup(
        records: List<TradeRecord>,
        reviews: List<ReviewLog>,
        tracks: List<TrackLog> = emptyList()
    ) {
        mergeImport(records)
        mergeReviews(reviews)
        mergeTracks(tracks)
    }

    /** 首次写入推荐观察池（仅当跟踪表为空） */
    suspend fun seedRecommendTracksIfEmpty() {
        if (trackLogDao.count() > 0) return
        val now = System.currentTimeMillis()
        val method = "趋势回踩·硬止损法"
        val seeds = listOf(
            TrackLog(
                symbol = "招商银行",
                method = method,
                status = "观察中",
                content = """【推荐跟踪·未成交】
代码参考：600036
手法：趋势回踩·硬止损法（3～10日快验证）
理由：活跃中军、波动适中，适合短周期验证；勿用茅台类慢牛。

【观察要点】
1. 收盘 > MA20 且 MA20 未下行
2. 近 3 日回踩 MA10 后站回
3. 距 MA10 ≤ 3%，不追高
4. 触发买点后再记入「记录」；本条只跟踪推荐。

【卖点纪律】破 MA10 / -4% 硬止损 / +6%～+12% / 满 10 日清仓
""".trim(),
                trackDate = now,
                createdAt = now,
                updatedAt = now
            ),
            TrackLog(
                symbol = "中国平安",
                method = method,
                status = "观察中",
                content = """【推荐跟踪·未成交】
代码参考：601318
手法：趋势回踩·硬止损法（3～10日快验证）
理由：流动性好、趋势信号清晰，便于 10～15 笔快速统计期望。

【观察要点】
同招行规则；未满足买点只观察，不提前记入交易记录。

【卖点纪律】破 MA10 / -4% 硬止损 / +6%～+12% / 满 10 日清仓
""".trim(),
                trackDate = now,
                createdAt = now,
                updatedAt = now
            ),
            TrackLog(
                symbol = "海康威视",
                method = method,
                status = "观察中",
                content = """【推荐跟踪·未成交】
代码参考：002415
手法：趋势回踩·硬止损法（3～10日快验证）
理由：科技中军、日波动够用，比超大票更适合 3～10 日验证周期。

【观察要点】
MA20 多头 + 回踩站回 MA10 才考虑出手；出手后到「记录」记账，此处继续可写每日跟踪。

【卖点纪律】破 MA10 / -4% 硬止损 / +6%～+12% / 满 10 日清仓
""".trim(),
                trackDate = now,
                createdAt = now,
                updatedAt = now
            )
        )
        trackLogDao.insertAll(seeds)
    }
}
