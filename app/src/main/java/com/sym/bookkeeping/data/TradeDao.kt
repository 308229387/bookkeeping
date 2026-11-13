package com.sym.bookkeeping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_records ORDER BY tradeDate DESC, id DESC")
    fun observeAll(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records ORDER BY tradeDate DESC, id DESC")
    suspend fun getAll(): List<TradeRecord>

    @Query("SELECT * FROM trade_records WHERE id = :id")
    fun observeById(id: Long): Flow<TradeRecord?>

    @Query("SELECT * FROM trade_records WHERE id = :id")
    suspend fun getById(id: Long): TradeRecord?

    @Query(
        """
        SELECT * FROM trade_records
        WHERE journal IS NOT NULL AND TRIM(journal) != ''
          AND method != '盘面复盘'
          AND symbol != 'A股盘面'
        ORDER BY tradeDate DESC, id DESC
        """
    )
    fun observeWithJournal(): Flow<List<TradeRecord>>

    @Query("SELECT DISTINCT method FROM trade_records WHERE TRIM(method) != '' ORDER BY method ASC")
    fun observeMethods(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TradeRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TradeRecord>)

    @Update
    suspend fun update(record: TradeRecord)

    @Delete
    suspend fun delete(record: TradeRecord)

    @Query("DELETE FROM trade_records")
    suspend fun deleteAll()
}
