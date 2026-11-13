package com.sym.bookkeeping

import android.app.Application
import com.sym.bookkeeping.data.AppDatabase
import com.sym.bookkeeping.data.ImportExportHelper
import com.sym.bookkeeping.data.TradeRepository

class BookkeepingApp : Application() {
    lateinit var repository: TradeRepository
        private set
    lateinit var importExportHelper: ImportExportHelper
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = TradeRepository(db.tradeDao(), db.reviewLogDao())
        importExportHelper = ImportExportHelper(this)
    }
}
