package com.sym.bookkeeping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {
    @Query("SELECT * FROM review_logs ORDER BY reviewDate DESC, id DESC")
    fun observeAll(): Flow<List<ReviewLog>>

    @Query("SELECT * FROM review_logs WHERE id = :id")
    fun observeById(id: Long): Flow<ReviewLog?>

    @Query("SELECT * FROM review_logs WHERE id = :id")
    suspend fun getById(id: Long): ReviewLog?

    @Query("SELECT * FROM review_logs ORDER BY reviewDate DESC, id DESC")
    suspend fun getAll(): List<ReviewLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReviewLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ReviewLog>)

    @Update
    suspend fun update(log: ReviewLog)

    @Delete
    suspend fun delete(log: ReviewLog)

    @Query("DELETE FROM review_logs")
    suspend fun deleteAll()
}
