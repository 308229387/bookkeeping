package com.sym.bookkeeping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackLogDao {
    @Query("SELECT * FROM track_logs ORDER BY trackDate DESC, id DESC")
    fun observeAll(): Flow<List<TrackLog>>

    @Query("SELECT * FROM track_logs WHERE id = :id")
    fun observeById(id: Long): Flow<TrackLog?>

    @Query("SELECT * FROM track_logs WHERE id = :id")
    suspend fun getById(id: Long): TrackLog?

    @Query("SELECT * FROM track_logs ORDER BY trackDate DESC, id DESC")
    suspend fun getAll(): List<TrackLog>

    @Query("SELECT COUNT(*) FROM track_logs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TrackLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<TrackLog>)

    @Update
    suspend fun update(log: TrackLog)

    @Delete
    suspend fun delete(log: TrackLog)

    @Query("DELETE FROM track_logs")
    suspend fun deleteAll()
}
