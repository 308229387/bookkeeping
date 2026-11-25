package com.sym.bookkeeping.data.market

/** 涨停/跌停池单票 */
data class LimitStock(
    val code: String,
    val name: String,
    /** 涨跌幅 % */
    val changeRate: Double = 0.0,
    /** 换手 % */
    val turnoverRate: Double = 0.0,
    /** 几板，如 首板 / 2天2板 */
    val highDays: String = "",
    /** 涨停类型，如 换手板 */
    val limitUpType: String = "",
    /** 概念/原因 */
    val reason: String = "",
    val latest: Double = 0.0,
    /** 开板次数 */
    val openNum: Int = 0,
    /** 流通市值（亿） */
    val marketCapYi: Double = 0.0,
    /** 封单额等（元），辅助参考 */
    val orderAmount: Double = 0.0
)

/** 兼容旧命名 */
typealias LimitUpStock = LimitStock

/** 单日涨/跌停家数统计 */
data class LimitSideCount(
    val num: Int = 0,
    val historyNum: Int = 0,
    val rate: Double = 0.0,
    val openNum: Int = 0
)

data class LimitUpCount(
    val todayNum: Int = 0,
    val todayHistoryNum: Int = 0,
    val todayRate: Double = 0.0,
    val todayOpenNum: Int = 0,
    val yesterdayNum: Int = 0
)

data class LimitUpPoolResult(
    val date: String = "",
    val total: Int = 0,
    val count: LimitUpCount = LimitUpCount(),
    val stocks: List<LimitStock> = emptyList()
)

data class LadderStock(
    val code: String,
    val name: String,
    val continueNum: Int = 0
)

/** 某一板高度的股票列表，height=2 表示 2 板 */
data class LadderLevel(
    val height: Int,
    val stocks: List<LadderStock> = emptyList()
)

data class LadderDay(
    val date: String,
    val levels: List<LadderLevel> = emptyList()
)

/** 近 N 日涨跌停汇总 */
data class LimitDayStat(
    val date: String,
    val up: LimitSideCount = LimitSideCount(),
    val down: LimitSideCount = LimitSideCount()
)

/** 单日全市场涨跌家数（同花顺涨跌家数）+ 成交额 */
data class MarketBreadthDay(
    val date: String,
    val rise: Int = 0,
    val fall: Int = 0,
    val flat: Int = 0,
    val limitUp: Int = 0,
    val limitDown: Int = 0,
    /** 成交额文案，如 1.5万亿 */
    val turnoverText: String = "",
    /** 成交额（亿）便于画图 */
    val turnoverYi: Double = 0.0,
    /** 较昨：1 放量 / -1 缩量 / 0 未知 */
    val turnoverFlag: Int = 0
)

/** 指数单日 K 线摘要 */
data class IndexDay(
    val date: String,
    val close: Double = 0.0,
    /** 涨跌幅 % */
    val changePct: Double = 0.0,
    /** 成交额（亿） */
    val amountYi: Double = 0.0
)

/** 一只指数近 N 日（新→旧） */
data class IndexSeries(
    val code: String,
    val name: String,
    val days: List<IndexDay> = emptyList()
) {
    val latest: IndexDay? get() = days.firstOrNull()
}

/** 概念 / 行业板内龙头股 */
data class PlateLeaderStock(
    val code: String,
    val name: String,
    val changePct: Double = 0.0,
    val continueNum: Int = 0,
    val highLabel: String = "",
    /** 1=龙一 2=龙二 */
    val order: Int = 1
)

/** 当日强势概念或行业 */
data class ConceptBoard(
    val code: String = "",
    val name: String,
    /** concept / industry */
    val kind: String = "concept",
    /** 板块涨跌幅 % */
    val changePct: Double = 0.0,
    /** 涨停家数（概念板更准） */
    val limitUpNum: Int = 0,
    /** 连板高度文案，如 3天3板 */
    val highLabel: String = "",
    /** 人气热度（同花顺热榜） */
    val hotRate: Double = 0.0,
    val hotTag: String = "",
    val tag: String = "",
    val leaders: List<PlateLeaderStock> = emptyList()
)

/** 外盘 / 全球指数实时点 */
data class GlobalIndexQuote(
    val code: String,
    val name: String,
    /** 亚太 / 欧美 / 商品货币 */
    val region: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePct: Double = 0.0,
    val timeText: String = ""
)

/** 综合多平台人气 + 量价市值 + 板块带动评出的人气龙头 */
data class DragonLeader(
    /** 1=龙一 2=龙二 3=龙三 */
    val rank: Int,
    val code: String,
    val name: String,
    /** 连板高度 */
    val continueNum: Int = 0,
    /** 如 3天3板 / 首板 */
    val highLabel: String = "",
    /** 涨跌幅 % */
    val changePct: Double = 0.0,
    /** 东财人气榜排名 */
    val hotRank: Int? = null,
    /** 东财人气排名变动 */
    val hotDelta: Int? = null,
    /** 雪球热股排名 */
    val xueqiuRank: Int? = null,
    /** 同花顺热榜排名（小时榜） */
    val thsRank: Int? = null,
    /** 同花顺热度值 */
    val thsHotRate: Double = 0.0,
    /** 同花顺人气标签，如 首板涨停 */
    val thsPopTag: String = "",
    /** 成交额（亿） */
    val amountYi: Double = 0.0,
    /** 流通市值（亿） */
    val marketCapYi: Double = 0.0,
    /** 换手 % */
    val turnoverRate: Double = 0.0,
    /** 所属强势概念 */
    val concept: String = "",
    /** 该概念涨停家数（带动能力） */
    val conceptLimitUp: Int = 0,
    /** 短因/标签 */
    val reason: String = "",
    val score: Int = 0,
    val tags: List<String> = emptyList()
)

/**
 * 涨停聚焦页完整快照：涨停/跌停池 + 连板天梯 + 近二十日 + 涨跌家数 + 指数。
 */
data class LimitFocusResult(
    val date: String = "",
    val tradeStatus: String = "",
    val upCount: LimitSideCount = LimitSideCount(),
    val downCount: LimitSideCount = LimitSideCount(),
    val yesterdayUpNum: Int = 0,
    val yesterdayDownNum: Int = 0,
    /** 当日涨跌家数（同花顺口径） */
    val todayRise: Int = 0,
    val todayFall: Int = 0,
    val todayFlat: Int = 0,
    val upStocks: List<LimitStock> = emptyList(),
    val downStocks: List<LimitStock> = emptyList(),
    /** 当日连板天梯（高度从高到低） */
    val ladder: List<LadderLevel> = emptyList(),
    /** 近二十日连板天梯（新→旧） */
    val ladderHistory: List<LadderDay> = emptyList(),
    /** 近二十日涨跌停家数（新→旧） */
    val recentDays: List<LimitDayStat> = emptyList(),
    /** 近十日全市场涨跌家数（新→旧） */
    val breadthDays: List<MarketBreadthDay> = emptyList(),
    /** 上证 / 深成 / 创业 近十日指数（新→旧） */
    val indices: List<IndexSeries> = emptyList(),
    /** 龙一 / 龙二 / 龙三 */
    val leaders: List<DragonLeader> = emptyList(),
    /** 当日最强概念（含龙一龙二） */
    val conceptBoards: List<ConceptBoard> = emptyList(),
    /** 当日最强行业（含领涨/龙） */
    val industryBoards: List<ConceptBoard> = emptyList(),
    /** 外盘全球指数 */
    val globalIndices: List<GlobalIndexQuote> = emptyList()
)

/** 各数据块是否已返回（懒加载进度） */
data class LimitSectionReady(
    val core: Boolean = false,
    val indices: Boolean = false,
    val breadth: Boolean = false,
    val recentDays: Boolean = false,
    val ladderHistory: Boolean = false,
    val leaders: Boolean = false,
    val concepts: Boolean = false,
    val globalIndices: Boolean = false
) {
    /** 情绪状态等全部齐了再算（龙头/概念/外盘单独展示，不阻塞情绪） */
    val allReady: Boolean
        get() = core && indices && breadth && recentDays && ladderHistory
}

data class LimitUpUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: LimitUpPoolResult? = null,
    val focus: LimitFocusResult? = null,
    val sectionReady: LimitSectionReady = LimitSectionReady(),
    val updatedAt: Long = 0L
)
