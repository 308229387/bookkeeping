package com.sym.bookkeeping.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sym.bookkeeping.BookkeepingApp
import com.sym.bookkeeping.data.AnalysisCalculator
import com.sym.bookkeeping.data.ImportExportHelper
import com.sym.bookkeeping.data.MethodStats
import com.sym.bookkeeping.data.ReviewLog
import com.sym.bookkeeping.data.TradeRecord
import com.sym.bookkeeping.data.TradeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeListUiState(
    val records: List<TradeRecord> = emptyList(),
    val query: String = "",
    val selectedMethod: String? = null,
    val methods: List<String> = emptyList()
) {
    val filtered: List<TradeRecord>
        get() = records.filter { record ->
            val matchMethod = selectedMethod == null || record.method == selectedMethod
            val q = query.trim()
            val matchQuery = q.isEmpty() ||
                record.symbol.contains(q, ignoreCase = true) ||
                record.method.contains(q, ignoreCase = true) ||
                record.journal.contains(q, ignoreCase = true)
            matchMethod && matchQuery
        }
}

data class AnalysisUiState(
    val overall: MethodStats = MethodStats(
        method = "全部",
        totalCount = 0,
        winCount = 0,
        lossCount = 0,
        breakEvenCount = 0,
        totalProfit = 0.0,
        avgProfit = 0.0,
        avgRiskRewardRatio = 0.0
    ),
    val byMethod: List<MethodStats> = emptyList()
)

sealed interface UiMessage {
    data class Text(val value: String) : UiMessage
}

class TradeViewModel(
    application: Application,
    private val repository: TradeRepository,
    private val importExportHelper: ImportExportHelper
) : AndroidViewModel(application) {

    private val query = MutableStateFlow("")
    private val selectedMethod = MutableStateFlow<String?>(null)

    val listState: StateFlow<TradeListUiState> = combine(
        repository.observeAll(),
        repository.observeMethods(),
        query,
        selectedMethod
    ) { records, methods, q, method ->
        val trades = records.filter {
            it.method != "盘面复盘" && it.symbol != "A股盘面"
        }
        TradeListUiState(
            records = trades,
            query = q,
            selectedMethod = method,
            methods = methods.filter { it != "盘面复盘" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TradeListUiState())

    val journalRecords: StateFlow<List<TradeRecord>> = repository.observeWithJournal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewLogs: StateFlow<List<ReviewLog>> = repository.observeReviewLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analysisState: StateFlow<AnalysisUiState> = repository.observeAll()
        .map { records ->
            val trades = records.filter {
                it.method != "盘面复盘" && it.symbol != "A股盘面"
            }
            AnalysisUiState(
                overall = AnalysisCalculator.overall(trades),
                byMethod = AnalysisCalculator.byMethod(trades)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisUiState())

    private val _messages = MutableSharedFlow<UiMessage>()
    val messages = _messages.asSharedFlow()

    private val _editing = MutableStateFlow(TradeRecord())
    val editing: StateFlow<TradeRecord> = _editing.asStateFlow()

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSelectedMethod(method: String?) {
        selectedMethod.value = method
    }

    fun startCreate() {
        _editing.value = TradeRecord()
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val record = repository.getById(id)
            if (record != null) {
                _editing.value = record
            } else {
                _editing.value = TradeRecord()
                _messages.emit(UiMessage.Text("未找到该记录"))
            }
        }
    }

    fun updateEditing(transform: (TradeRecord) -> TradeRecord) {
        _editing.update(transform)
    }

    fun saveEditing(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val record = _editing.value
            if (record.method.isBlank() && record.symbol.isBlank()) {
                _messages.emit(UiMessage.Text("请至少填写标的或手法"))
                return@launch
            }
            val id = repository.save(record)
            _messages.emit(UiMessage.Text("已保存"))
            onSaved(id)
        }
    }

    fun delete(record: TradeRecord) {
        viewModelScope.launch {
            repository.delete(record)
            _messages.emit(UiMessage.Text("已删除"))
        }
    }

    fun saveReviewLog(title: String, content: String, reviewDate: Long, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveReview(
                ReviewLog(
                    title = title,
                    content = content,
                    reviewDate = reviewDate
                )
            )
            _messages.emit(UiMessage.Text("复盘日志已保存"))
            onSaved(id)
        }
    }

    fun deleteReview(log: ReviewLog) {
        viewModelScope.launch {
            repository.deleteReview(log)
            _messages.emit(UiMessage.Text("已删除复盘日志"))
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                importExportHelper.exportToJson(uri, repository.getAll())
            }.onSuccess {
                _messages.emit(UiMessage.Text("JSON 导出成功"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导出失败：${it.message}"))
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                importExportHelper.exportToCsv(uri, repository.getAll())
            }.onSuccess {
                _messages.emit(UiMessage.Text("CSV 导出成功"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导出失败：${it.message}"))
            }
        }
    }

    fun importJson(uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            runCatching {
                val records = importExportHelper.importFromJson(uri)
                if (replace) repository.replaceAll(records) else repository.mergeImport(records)
                records.size
            }.onSuccess { count ->
                _messages.emit(UiMessage.Text("已导入 $count 条 JSON 记录"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导入失败：${it.message}"))
            }
        }
    }

    fun importCsv(uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            runCatching {
                val records = importExportHelper.importFromCsv(uri)
                if (replace) repository.replaceAll(records) else repository.mergeImport(records)
                records.size
            }.onSuccess { count ->
                _messages.emit(UiMessage.Text("已导入 $count 条 CSV 记录"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导入失败：${it.message}"))
            }
        }
    }

    companion object {
        fun factory(app: BookkeepingApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TradeViewModel(app, app.repository, app.importExportHelper) as T
                }
            }
    }
}
