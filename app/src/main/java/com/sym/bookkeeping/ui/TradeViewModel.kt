package com.sym.bookkeeping.ui

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sym.bookkeeping.BookkeepingApp
import com.sym.bookkeeping.data.AnalysisCalculator
import com.sym.bookkeeping.data.ImportExportHelper
import com.sym.bookkeeping.data.MethodStats
import com.sym.bookkeeping.data.ReviewLog
import com.sym.bookkeeping.data.StrategyCatalog
import com.sym.bookkeeping.data.TrackLog
import com.sym.bookkeeping.data.TradeRecord
import com.sym.bookkeeping.data.TradeRepository
import com.sym.bookkeeping.data.UserStrategyStore
import com.sym.bookkeeping.data.market.LimitSectionReady
import com.sym.bookkeeping.data.market.LimitUpCount
import com.sym.bookkeeping.data.market.LimitUpPoolResult
import com.sym.bookkeeping.data.market.LimitUpUiState
import com.sym.bookkeeping.data.market.ThsLimitUpApi
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TradeSort(val label: String) {
    DateDesc("最新"),
    RatioDesc("盈亏比↓"),
    RatioAsc("盈亏比↑"),
    ProfitDesc("金额↓"),
    ProfitAsc("金额↑")
}

data class TradeListUiState(
    val records: List<TradeRecord> = emptyList(),
    val query: String = "",
    val selectedMethod: String? = null,
    val methods: List<String> = emptyList(),
    val sort: TradeSort = TradeSort.DateDesc
) {
    /** 当前手法/搜索筛选后的集合（未排序） */
    val matched: List<TradeRecord>
        get() = records.filter { record ->
            val matchMethod = selectedMethod == null || record.method == selectedMethod
            val q = query.trim()
            val matchQuery = q.isEmpty() ||
                record.symbol.contains(q, ignoreCase = true) ||
                record.method.contains(q, ignoreCase = true) ||
                record.journal.contains(q, ignoreCase = true)
            matchMethod && matchQuery
        }

    /** 顶部汇总：按当前筛选范围统计 */
    val summary: MethodStats
        get() = AnalysisCalculator.overall(matched)

    val filtered: List<TradeRecord>
        get() {
            val list = matched
            return when (sort) {
                TradeSort.DateDesc -> list.sortedByDescending { it.tradeDate }
                TradeSort.RatioDesc -> list.sortedByDescending { it.riskRewardRatio }
                TradeSort.RatioAsc -> list.sortedBy { it.riskRewardRatio }
                TradeSort.ProfitDesc -> list.sortedByDescending { it.profit }
                TradeSort.ProfitAsc -> list.sortedBy { it.profit }
            }
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
        avgRiskRewardRatio = 0.0,
        totalRiskRewardRatio = 0.0
    ),
    val byMethod: List<MethodStats> = emptyList()
)

sealed interface UiMessage {
    data class Text(val value: String) : UiMessage
}

class TradeViewModel(
    application: Application,
    private val repository: TradeRepository,
    private val importExportHelper: ImportExportHelper,
    private val thsLimitUpApi: ThsLimitUpApi = ThsLimitUpApi()
) : AndroidViewModel(application) {

    private val query = MutableStateFlow("")
    private val selectedMethod = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(TradeSort.DateDesc)

    private val _limitUpState = MutableStateFlow(LimitUpUiState())
    val limitUpState: StateFlow<LimitUpUiState> = _limitUpState.asStateFlow()

    val listState: StateFlow<TradeListUiState> = combine(
        repository.observeAll(),
        repository.observeMethods(),
        query,
        selectedMethod,
        sort
    ) { records, methods, q, method, sortMode ->
        val trades = records.filter {
            it.method != "盘面复盘" && it.symbol != "A股盘面"
        }
        TradeListUiState(
            records = trades,
            query = q,
            selectedMethod = method,
            methods = methods.filter { it != "盘面复盘" },
            sort = sortMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TradeListUiState())

    val trackLogs: StateFlow<List<TrackLog>> = repository.observeTrackLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewLogs: StateFlow<List<ReviewLog>> = repository.observeReviewLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editingTrack = MutableStateFlow(TrackLog())
    val editingTrack: StateFlow<TrackLog> = _editingTrack.asStateFlow()

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

    fun setSort(value: TradeSort) {
        sort.value = value
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

    fun refreshLimitUpPool() {
        viewModelScope.launch {
            _limitUpState.update {
                it.copy(
                    loading = true,
                    error = null,
                    sectionReady = LimitSectionReady(),
                    // 保留旧 focus 也可；刷新时先清空避免新旧混杂
                    focus = null,
                    result = null
                )
            }
            runCatching {
                thsLimitUpApi.fetchLimitFocusProgressive(historyDays = 20) { focus, ready ->
                    val legacy = LimitUpPoolResult(
                        date = focus.date,
                        total = focus.upStocks.size,
                        count = LimitUpCount(
                            todayNum = focus.upCount.num,
                            todayHistoryNum = focus.upCount.historyNum,
                            todayRate = focus.upCount.rate,
                            todayOpenNum = focus.upCount.openNum,
                            yesterdayNum = focus.yesterdayUpNum
                        ),
                        stocks = focus.upStocks
                    )
                    _limitUpState.update {
                        it.copy(
                            loading = !ready.allReady,
                            error = null,
                            result = legacy,
                            focus = focus,
                            sectionReady = ready,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
            }.onSuccess {
                _limitUpState.update { it.copy(loading = false) }
            }.onFailure { e ->
                _limitUpState.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "拉取涨停聚焦失败"
                    )
                }
                _messages.emit(UiMessage.Text("涨停聚焦拉取失败：${e.message}"))
            }
        }
    }

    /** 把当前涨停聚焦摘要写入一条复盘日志 */
    fun saveLimitUpSnapshotToReview(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val focus = _limitUpState.value.focus
            if (focus == null) {
                _messages.emit(UiMessage.Text("请先刷新涨停页"))
                return@launch
            }
            val body = buildString {
                appendLine("日期 ${focus.date} ${focus.tradeStatus}")
                append("涨停 ").append(focus.upCount.num)
                append("（昨 ").append(focus.yesterdayUpNum).append("）")
                append(" 封板率 ").append(String.format("%.1f%%", focus.upCount.rate * 100))
                append(" 开板 ").append(focus.upCount.openNum)
                appendLine()
                append("跌停 ").append(focus.downCount.num)
                append("（昨 ").append(focus.yesterdayDownNum).append("）")
                appendLine()
                appendLine()
                appendLine("【连板天梯】")
                if (focus.ladder.isEmpty()) {
                    appendLine("暂无连板")
                } else {
                    focus.ladder.forEach { level ->
                        append(level.height).append("板：")
                        append(level.stocks.joinToString("、") { it.name.ifBlank { it.code } })
                        appendLine()
                    }
                }
                appendLine()
                appendLine("【涨跌家数】涨 ${focus.todayRise} 跌 ${focus.todayFall} 平 ${focus.todayFlat}")
                focus.breadthDays.forEach { d ->
                    append(d.date).append(" 涨").append(d.rise)
                    append(" 跌").append(d.fall).append(" 平").append(d.flat)
                    appendLine()
                }
                appendLine()
                appendLine("【近二十日涨跌停】")
                focus.recentDays.forEach { d ->
                    append(d.date).append(" 涨停").append(d.up.num)
                    append(" 跌停").append(d.down.num)
                    append(" 封板").append(String.format("%.0f%%", d.up.rate * 100))
                    appendLine()
                }
                appendLine()
                appendLine("【涨停池】")
                focus.upStocks.take(30).forEach { s ->
                    append(s.code).append(' ').append(s.name)
                    if (s.highDays.isNotBlank()) append(" · ").append(s.highDays)
                    if (s.reason.isNotBlank()) append(" · ").append(s.reason)
                    appendLine()
                }
            }
            val id = repository.saveReview(
                ReviewLog(
                    title = "涨停聚焦 ${focus.date}".trim(),
                    content = body.trim(),
                    reviewDate = System.currentTimeMillis()
                )
            )
            _messages.emit(UiMessage.Text("已写入复盘日志"))
            onSaved(id)
        }
    }

    fun deleteReview(log: ReviewLog) {
        viewModelScope.launch {
            repository.deleteReview(log)
            _messages.emit(UiMessage.Text("已删除复盘日志"))
        }
    }

    fun startCreateTrack() {
        _editingTrack.value = TrackLog(
            method = "趋势回踩·硬止损法",
            status = "观察中"
        )
    }

    fun loadTrackForEdit(id: Long) {
        viewModelScope.launch {
            val log = repository.getTrackById(id)
            if (log != null) {
                _editingTrack.value = log
            } else {
                startCreateTrack()
                _messages.emit(UiMessage.Text("未找到该跟踪日志"))
            }
        }
    }

    fun updateEditingTrack(transform: (TrackLog) -> TrackLog) {
        _editingTrack.update(transform)
    }

    fun saveEditingTrack(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val log = _editingTrack.value
            if (log.symbol.isBlank()) {
                _messages.emit(UiMessage.Text("请填写标的名称"))
                return@launch
            }
            val id = repository.saveTrack(log)
            _messages.emit(UiMessage.Text("跟踪日志已保存"))
            onSaved(id)
        }
    }

    fun deleteTrack(log: TrackLog) {
        viewModelScope.launch {
            repository.deleteTrack(log)
            _messages.emit(UiMessage.Text("已删除跟踪日志"))
        }
    }

    /** 完整 ZIP：交易(含图) + 复盘日志 + 战法 */
    fun exportFullBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    importExportHelper.exportFullZip(
                        uri = uri,
                        records = repository.getAll(),
                        reviewLogs = repository.getAllReviews(),
                        trackLogs = repository.getAllTracks(),
                        strategies = StrategyCatalog.allGuides()
                    )
                }
            }.onSuccess {
                _messages.emit(UiMessage.Text("完整备份已导出（交易+图片+日志+战法）"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导出失败：${it.message}"))
            }
        }
    }

    /** 生成备份并直接打开系统分享面板（微信/文件/邮件等） */
    fun shareFullBackup() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
                    val fileName = "bookkeeping_full_$stamp.zip"
                    val file = importExportHelper.exportFullZipToCacheFile(
                        fileName = fileName,
                        records = repository.getAll(),
                        reviewLogs = repository.getAllReviews(),
                        trackLogs = repository.getAllTracks(),
                        strategies = StrategyCatalog.allGuides()
                    )
                    val app = getApplication<Application>()
                    val uri = FileProvider.getUriForFile(
                        app,
                        "${app.packageName}.fileprovider",
                        file
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, fileName)
                        putExtra(Intent.EXTRA_TEXT, "记账完整备份（交易+图片+跟踪/复盘日志+战法）")
                        clipData = ClipData.newUri(app.contentResolver, fileName, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    Intent.createChooser(send, "分享备份到").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }.onSuccess { chooser ->
                getApplication<Application>().startActivity(chooser)
                _messages.emit(UiMessage.Text("请选择要分享到的应用"))
            }.onFailure {
                _messages.emit(UiMessage.Text("导出失败：${it.message}"))
            }
        }
    }

    fun importFullBackup(uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = uri.lastPathSegment.orEmpty().lowercase()
                    val result = when {
                        name.endsWith(".zip") || name.contains("zip") ->
                            importExportHelper.importFullZip(uri)
                        else -> {
                            // 先试 ZIP，失败再当 JSON（部分文档选择器不带扩展名）
                            runCatching { importExportHelper.importFullZip(uri) }
                                .getOrElse { importExportHelper.importFromJsonUri(uri) }
                        }
                    }
                    if (replace) {
                        repository.replaceFullBackup(
                            result.records,
                            result.reviewLogs,
                            result.trackLogs
                        )
                    } else {
                        repository.mergeFullBackup(
                            result.records,
                            result.reviewLogs,
                            result.trackLogs
                        )
                    }
                    if (result.strategies.isNotEmpty()) {
                        UserStrategyStore.save(getApplication(), result.strategies)
                    } else if (replace) {
                        UserStrategyStore.clear(getApplication())
                    }
                    result
                }
            }.onSuccess { result ->
                _messages.emit(
                    UiMessage.Text(
                        "已导入 交易${result.records.size} / 跟踪${result.trackLogs.size} / " +
                            "复盘${result.reviewLogs.size} / 战法${result.strategies.size} / " +
                            "图${result.imagePaths.size}"
                    )
                )
            }.onFailure {
                _messages.emit(UiMessage.Text("导入失败：${it.message}"))
            }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    importExportHelper.exportToJson(
                        uri,
                        repository.getAll(),
                        repository.getAllReviews(),
                        repository.getAllTracks(),
                        StrategyCatalog.allGuides()
                    )
                }
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
        importFullBackup(uri, replace)
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
