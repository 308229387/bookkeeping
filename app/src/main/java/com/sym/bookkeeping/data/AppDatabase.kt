package com.sym.bookkeeping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TradeRecord::class, ReviewLog::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trade_records ADD COLUMN fee REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        reviewDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // 把误记在交易表里的盘面复盘迁出
                db.execSQL(
                    """
                    INSERT INTO review_logs (title, content, reviewDate, createdAt, updatedAt)
                    SELECT
                        CASE
                            WHEN TRIM(symbol) = '' THEN '盘面复盘'
                            ELSE symbol
                        END,
                        journal,
                        tradeDate,
                        createdAt,
                        updatedAt
                    FROM trade_records
                    WHERE method = '盘面复盘' OR symbol = 'A股盘面'
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    DELETE FROM trade_records
                    WHERE method = '盘面复盘' OR symbol = 'A股盘面'
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookkeeping.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
