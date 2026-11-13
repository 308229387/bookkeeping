package com.sym.bookkeeping.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(
    private val dao: TradeDao,
    private val reviewLogDao: ReviewLogDao
) {
    fun observeAll(): Flow<List<TradeRecord>> = dao.observeAll()

    fun observeById(id: Long): Flow<TradeRecord?> = dao.observeById(id)

    fun observeWithJournal(): Flow<List<TradeRecord>> = dao.observeWithJournal()

    fun observeMethods(): Flow<List<String>> = dao.observeMethods()

    fun observeReviewLogs(): Flow<List<ReviewLog>> = reviewLogDao.observeAll()

    fun observeReviewById(id: Long): Flow<ReviewLog?> = reviewLogDao.observeById(id)

    suspend fun getAll(): List<TradeRecord> = dao.getAll()

    suspend fun getById(id: Long): TradeRecord? = dao.getById(id)

    suspend fun getReviewById(id: Long): ReviewLog? = reviewLogDao.getById(id)

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

    suspend fun delete(record: TradeRecord) = dao.delete(record)

    suspend fun deleteReview(log: ReviewLog) = reviewLogDao.delete(log)

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
}
