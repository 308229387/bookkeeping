package com.sym.bookkeeping

import android.app.Application
import com.sym.bookkeeping.data.AppDatabase
import com.sym.bookkeeping.data.ImportExportHelper
import com.sym.bookkeeping.data.TradeRepository
import com.sym.bookkeeping.data.UserStrategyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BookkeepingApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: TradeRepository
        private set
    lateinit var importExportHelper: ImportExportHelper
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = TradeRepository(db.tradeDao(), db.reviewLogDao(), db.trackLogDao())
        importExportHelper = ImportExportHelper(this)
        UserStrategyStore.load(this)
        appScope.launch {
            repository.seedRecommendTracksIfEmpty()
        }
    }
}
