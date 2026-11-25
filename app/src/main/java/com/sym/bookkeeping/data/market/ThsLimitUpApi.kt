package com.sym.bookkeeping.data.market

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 同花顺「涨停聚焦」相关接口（data.10jqka.com.cn）
 *
 * - 涨停池 / 跌停池
 * - 连板天梯 continuous_limit_up
 * - 按日期回看近 N 日涨跌停家数与天梯
 * - 涨跌家数 market_state/overview.rise_fall
 */
class ThsLimitUpApi {

    /** 简易 Cookie 罐（雪球等站点需要） */
    private val cookieStore = ConcurrentHashMap<String, String>()

    suspend fun fetchLimitUpPool(
        page: Int = 1,
        limit: Int = 100,
        date: String? = null
    ): LimitUpPoolResult = withContext(Dispatchers.IO) {
        val body = httpGet(poolUrl("limit_up_pool", page, limit, date))
        parsePool(body).also { /* keep legacy shape */ }
    }

    /**
     * 涨停聚焦懒加载：核心池/天梯先出，指数、涨跌家数、近二十日、天梯历史陆续补齐。
     * [onPartial] 每完成一块回调一次；情绪判定应等 [LimitSectionReady.allReady]。
     */
    suspend fun fetchLimitFocusProgressive(
        historyDays: Int = 20,
        onPartial: (LimitFocusResult, LimitSectionReady) -> Unit
    ): LimitFocusResult =
        withContext(Dispatchers.IO) {
            try {
                fetchLimitFocusProgressiveInternal(historyDays, onPartial)
            } catch (e: Exception) {
                Log.e(TAG, "fetchLimitFocus failed", e)
                throw e
            }
        }

    private suspend fun fetchLimitFocusProgressiveInternal(
        historyDays: Int,
        onPartial: (LimitFocusResult, LimitSectionReady) -> Unit
    ): LimitFocusResult = coroutineScope {
        val lock = Any()
        var focus = LimitFocusResult()
        var ready = LimitSectionReady()

        fun publish(
            patchFocus: (LimitFocusResult) -> LimitFocusResult,
            patchReady: (LimitSectionReady) -> LimitSectionReady
        ) {
            val snap: Pair<LimitFocusResult, LimitSectionReady>
            synchronized(lock) {
                focus = patchFocus(focus)
                ready = patchReady(ready)
                snap = focus to ready
            }
            onPartial(snap.first, snap.second)
        }

        val upDeferred = async { httpGet(poolUrl("limit_up_pool", 1, 100, null)) }
        val downDeferred = async { httpGet(poolUrl("lower_limit_pool", 1, 100, null)) }
        val ladderDeferred = async {
            httpGet("$BASE/continuous_limit_up?filter=$FILTER")
        }
        val statusDeferred = async {
            runCatching { httpGet("$BASE/trade_status") }.getOrNull()
        }
        // 指数与核心池并行请求，但等核心先推送后再发指数，避免空白页闪一下
        val indicesDeferred = async {
            runCatching { fetchMajorIndices(want = 10) }.getOrElse { emptyList() }
        }

        val upJson = upDeferred.await()
        val downJson = downDeferred.await()
        val ladderJson = ladderDeferred.await()
        val statusJson = statusDeferred.await()

        val upPool = parsePool(upJson)
        val downPool = parsePool(downJson)
        val ladder = parseLadder(ladderJson)
        val tradeStatus = parseTradeStatus(statusJson)
        val date = upPool.date.ifBlank { downPool.date }.ifBlank { todayYmd() }
        val downCount = parseSideCount(
            JsonParser.parseString(downJson).asJsonObject
                .getAsJsonObject("data")
                ?.getAsJsonObject("limit_down_count")
                ?.getAsJsonObject("today")
        )
        val yesterdayDown = parseSideCount(
            JsonParser.parseString(downJson).asJsonObject
                .getAsJsonObject("data")
                ?.getAsJsonObject("limit_down_count")
                ?.getAsJsonObject("yesterday")
        ).num

        publish(
            patchFocus = {
                it.copy(
                    date = date,
                    tradeStatus = tradeStatus,
                    upCount = LimitSideCount(
                        num = upPool.count.todayNum,
                        historyNum = upPool.count.todayHistoryNum,
                        rate = upPool.count.todayRate,
                        openNum = upPool.count.todayOpenNum
                    ),
                    downCount = downCount,
                    yesterdayUpNum = upPool.count.yesterdayNum,
                    yesterdayDownNum = yesterdayDown,
                    upStocks = upPool.stocks,
                    downStocks = downPool.stocks,
                    ladder = ladder
                )
            },
            patchReady = { it.copy(core = true) }
        )

        val indicesJob = launch {
            val indices = indicesDeferred.await()
            publish(
                patchFocus = { it.copy(indices = indices) },
                patchReady = { it.copy(indices = true) }
            )
        }

        // 龙头：人气榜 + 板块龙 + 天梯（不阻塞情绪判定）
        val leadersJob = launch {
            val leaders = runCatching {
                fetchDragonLeaders(ladder = ladder, upStocks = upPool.stocks)
            }.onFailure { Log.w(TAG, "leaders failed", it) }
                .getOrElse { emptyList() }
            publish(
                patchFocus = { it.copy(leaders = leaders) },
                patchReady = { it.copy(leaders = true) }
            )
        }

        // 概念 / 行业最强 + 龙一龙二
        val conceptsJob = launch {
            val pair = runCatching { fetchConceptAndIndustryBoards() }
                .onFailure { Log.w(TAG, "concepts failed", it) }
                .getOrElse { emptyList<ConceptBoard>() to emptyList() }
            publish(
                patchFocus = {
                    it.copy(conceptBoards = pair.first, industryBoards = pair.second)
                },
                patchReady = { it.copy(concepts = true) }
            )
        }

        // 外盘全球指数
        val globalJob = launch {
            val globals = runCatching { fetchGlobalIndices() }
                .onFailure { Log.w(TAG, "global indices failed", it) }
                .getOrElse { emptyList() }
            publish(
                patchFocus = { it.copy(globalIndices = globals) },
                patchReady = { it.copy(globalIndices = true) }
            )
        }

        // 涨跌家数+成交额、近二十日并行；天梯历史依赖近二十日日期
        val breadthJob = launch {
            val breadth = runCatching {
                fetchMarketBreadth(anchorDate = date, want = 10)
            }.getOrElse { emptyList() }
            val todayBreadth = breadth.firstOrNull()
            publish(
                patchFocus = {
                    it.copy(
                        breadthDays = breadth,
                        todayRise = todayBreadth?.rise ?: 0,
                        todayFall = todayBreadth?.fall ?: 0,
                        todayFlat = todayBreadth?.flat ?: 0
                    )
                },
                patchReady = { it.copy(breadth = true) }
            )
        }

        val recentJob = launch {
            val recent = runCatching {
                fetchRecentDayStats(anchorDate = date, want = historyDays)
            }.getOrElse { emptyList() }
            publish(
                patchFocus = { it.copy(recentDays = recent) },
                patchReady = { it.copy(recentDays = true) }
            )
            val ladderHistory = runCatching {
                fetchLadderHistory(recent.map { d -> d.date })
            }.getOrElse { emptyList() }
            publish(
                patchFocus = { it.copy(ladderHistory = ladderHistory) },
                patchReady = { it.copy(ladderHistory = true) }
            )
        }

        indicesJob.join()
        leadersJob.join()
        conceptsJob.join()
        globalJob.join()
        breadthJob.join()
        recentJob.join()

        synchronized(lock) { focus }
    }

    /**
     * 概念：同花顺 block_top（涨停多 + 龙一龙二）+ 热榜补热度；
     * 行业：东财行业涨幅榜领涨股 + 同花顺行业热榜。
     */
    private suspend fun fetchConceptAndIndustryBoards(): Pair<List<ConceptBoard>, List<ConceptBoard>> =
        coroutineScope {
            val blockDeferred = async {
                runCatching { fetchConceptBoardsFromBlockTop() }.getOrElse { emptyList() }
            }
            val hotConceptDeferred = async {
                runCatching { fetchThsHotPlates(type = "concept") }.getOrElse { emptyList() }
            }
            val hotIndustryDeferred = async {
                runCatching { fetchThsHotPlates(type = "industry") }.getOrElse { emptyList() }
            }
            val emIndustryDeferred = async {
                runCatching { fetchEmIndustryBoards(limit = 12) }.getOrElse { emptyList() }
            }

            val blockBoards = blockDeferred.await()
            val hotConcepts = hotConceptDeferred.await().associateBy { it.name }
            val concepts = blockBoards.map { board ->
                val hot = hotConcepts[board.name]
                board.copy(
                    hotRate = hot?.hotRate ?: board.hotRate,
                    hotTag = hot?.hotTag.orEmpty().ifBlank { board.hotTag },
                    tag = board.tag.ifBlank { hot?.tag.orEmpty() }
                )
            }.sortedWith(
                compareByDescending<ConceptBoard> { it.limitUpNum }
                    .thenByDescending { it.changePct }
            ).take(12)

            val hotIndustries = hotIndustryDeferred.await().associateBy { it.name }
            val industries = emIndustryDeferred.await().map { board ->
                val hot = hotIndustries[board.name]
                board.copy(
                    hotRate = hot?.hotRate ?: board.hotRate,
                    hotTag = hot?.hotTag.orEmpty().ifBlank { board.hotTag },
                    tag = board.tag.ifBlank { hot?.tag.orEmpty() }
                )
            }.sortedByDescending { it.changePct }.take(12)

            concepts to industries
        }

    private fun fetchConceptBoardsFromBlockTop(): List<ConceptBoard> {
        val json = httpGet("$BASE/block_top?filter=$FILTER")
        val root = JsonParser.parseString(json).asJsonObject
        if ((root.intOrNull("status_code") ?: -1) != 0) return emptyList()
        val out = mutableListOf<ConceptBoard>()
        root.getAsJsonArray("data")?.forEach { bel ->
            val b = bel.asJsonObject
            val name = b.stringOrNull("name").orEmpty()
            if (name.isBlank()) return@forEach
            val stocks = b.getAsJsonArray("stock_list") ?: return@forEach
            val leaders = stocks.take(2).mapIndexed { idx, sel ->
                val s = sel.asJsonObject
                PlateLeaderStock(
                    code = s.stringOrNull("code").orEmpty(),
                    name = s.stringOrNull("name").orEmpty(),
                    changePct = s.doubleOrNull("change_rate") ?: 0.0,
                    continueNum = s.intOrNull("continue_num") ?: 0,
                    highLabel = s.stringOrNull("high").orEmpty(),
                    order = idx + 1
                )
            }.filter { it.code.isNotBlank() }
            out += ConceptBoard(
                code = b.stringOrNull("code").orEmpty(),
                name = name,
                kind = "concept",
                changePct = b.doubleOrNull("change") ?: 0.0,
                limitUpNum = b.intOrNull("limit_up_num") ?: 0,
                highLabel = b.stringOrNull("high").orEmpty(),
                tag = when (val n = b.intOrNull("limit_up_num") ?: 0) {
                    0 -> ""
                    else -> "${n}家涨停"
                },
                leaders = leaders
            )
        }
        return out
    }

    private fun fetchThsHotPlates(type: String): List<ConceptBoard> {
        val url =
            "https://dq.10jqka.com.cn/fuyao/hot_list_data/out/hot_list/v1/plate" +
                "?type=$type&list_type=normal"
        val json = httpGet(url = url, referer = "https://eq.10jqka.com.cn/")
        val root = JsonParser.parseString(json).asJsonObject
        val arr = root.getAsJsonObject("data")?.getAsJsonArray("plate_list")
            ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val name = o.stringOrNull("name").orEmpty()
            if (name.isBlank()) return@mapNotNull null
            ConceptBoard(
                code = o.stringOrNull("code").orEmpty(),
                name = name,
                kind = type,
                changePct = o.doubleOrNull("rise_and_fall") ?: 0.0,
                hotRate = o.stringOrNull("rate")?.toDoubleOrNull()
                    ?: o.doubleOrNull("rate")
                    ?: 0.0,
                hotTag = o.stringOrNull("hot_tag").orEmpty(),
                tag = o.stringOrNull("tag").orEmpty()
            )
        }
    }

    /** 东财行业涨幅榜：领涨股作龙一 */
    private fun fetchEmIndustryBoards(limit: Int): List<ConceptBoard> {
        val url = buildString {
            append("https://push2.eastmoney.com/api/qt/clist/get")
            append("?pn=1&pz=").append(limit.coerceIn(5, 30))
            append("&po=1&np=1&fltt=2&invt=2&fid=f3")
            append("&fs=m:90+t:2+f:!50")
            append("&fields=f12,f14,f2,f3,f104,f105,f128,f136,f140,f141")
        }
        val json = httpGet(url = url, referer = "https://quote.eastmoney.com/")
        val root = JsonParser.parseString(json).asJsonObject
        val arr = root.getAsJsonObject("data")?.getAsJsonArray("diff") ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val name = o.stringOrNull("f14").orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val leadCode = o.stringOrNull("f140").orEmpty()
            val leadName = o.stringOrNull("f128").orEmpty()
            val leaders = if (leadCode.isNotBlank()) {
                listOf(
                    PlateLeaderStock(
                        code = leadCode,
                        name = leadName,
                        changePct = o.doubleOrNull("f136") ?: 0.0,
                        order = 1
                    )
                )
            } else emptyList()
            ConceptBoard(
                code = o.stringOrNull("f12").orEmpty(),
                name = name,
                kind = "industry",
                changePct = o.doubleOrNull("f3") ?: 0.0,
                leaders = leaders
            )
        }
    }

    /**
     * 全球指数：腾讯（美/港更准）+ 新浪（欧日韩/黄金/美元）。
     * 分组对齐同花顺全球页：亚太 / 欧美 / 商品货币。
     */
    private fun fetchGlobalIndices(): List<GlobalIndexQuote> {
        val out = linkedMapOf<String, GlobalIndexQuote>()
        runCatching { fetchTencentGlobalIndices() }.getOrElse { emptyList() }
            .forEach { out[it.code] = it }
        runCatching { fetchSinaGlobalIndices() }.getOrElse { emptyList() }
            .forEach { q -> if (!out.containsKey(q.code)) out[q.code] = q }
        val order = listOf(
            "HSI", "HSCEI", "N225", "KOSPI",
            "FTSE", "DAX", "CAC",
            "DJI", "IXIC", "INX",
            "XAU", "DXY"
        )
        return order.mapNotNull { out[it] } + out.values.filter { it.code !in order }
    }

    private fun fetchTencentGlobalIndices(): List<GlobalIndexQuote> {
        val codes = listOf(
            "usDJI" to ("DJI" to "美洲"),
            "usIXIC" to ("IXIC" to "美洲"),
            "usINX" to ("INX" to "美洲"),
            "hkHSI" to ("HSI" to "亚太"),
            "hkHSCEI" to ("HSCEI" to "亚太")
        )
        val url = "https://qt.gtimg.cn/q=" + codes.joinToString(",") { it.first }
        val text = httpGet(
            url = url,
            referer = "https://finance.qq.com/",
            charset = "GBK"
        )
        val bySym = codes.associate { it.first to it.second }
        val out = mutableListOf<GlobalIndexQuote>()
        Regex("""v_(\w+)="([^"]*)"""").findAll(text).forEach { m ->
            val sym = m.groupValues[1]
            val meta = bySym[sym] ?: return@forEach
            val p = m.groupValues[2].split('~')
            if (p.size < 33) return@forEach
            val price = p[3].toDoubleOrNull() ?: return@forEach
            val change = p[31].toDoubleOrNull() ?: 0.0
            val changePct = p[32].toDoubleOrNull() ?: 0.0
            out += GlobalIndexQuote(
                code = meta.first,
                name = p[1].ifBlank { meta.first },
                region = meta.second,
                price = price,
                change = change,
                changePct = changePct,
                timeText = p.getOrNull(30).orEmpty()
            )
        }
        return out
    }

    private fun fetchSinaGlobalIndices(): List<GlobalIndexQuote> {
        // 美股用腾讯；新浪补欧日韩、黄金、美元
        data class Spec(val sina: String, val code: String, val name: String, val region: String)
        val specs = listOf(
            Spec("int_nikkei", "N225", "日经指数", "亚太"),
            Spec("b_KOSPI", "KOSPI", "韩国KOSPI", "亚太"),
            Spec("b_FTSE", "FTSE", "英国富时", "欧美"),
            Spec("b_DAX", "DAX", "德国DAX", "欧美"),
            Spec("b_CAC", "CAC", "法国CAC", "欧美"),
            Spec("hf_GC", "XAU", "纽约黄金", "商品货币"),
            Spec("DINIW", "DXY", "美元指数", "商品货币")
        )
        val url = "https://hq.sinajs.cn/list=" + specs.joinToString(",") { it.sina }
        val text = httpGet(
            url = url,
            referer = "https://finance.sina.com.cn/",
            charset = "GBK"
        )
        val bySina = specs.associateBy { it.sina }
        val out = mutableListOf<GlobalIndexQuote>()
        Regex("""hq_str_([^=]+)="([^"]*)"""").findAll(text).forEach { m ->
            val key = m.groupValues[1]
            val spec = bySina[key] ?: return@forEach
            val p = m.groupValues[2].split(',')
            if (p.size < 2 || p[0].isBlank()) return@forEach
            when (key) {
                "hf_GC" -> {
                    // name at index 13 when long enough; price often p[0] empty → p[2]
                    val price = listOf(p.getOrNull(0), p.getOrNull(2), p.getOrNull(3))
                        .firstNotNullOfOrNull { it?.toDoubleOrNull() } ?: return@forEach
                    val prev = p.getOrNull(7)?.toDoubleOrNull() ?: 0.0
                    val change = if (prev > 0) price - prev else 0.0
                    val pct = if (prev > 0) change / prev * 100.0 else 0.0
                    out += GlobalIndexQuote(
                        code = spec.code,
                        name = p.getOrNull(13)?.ifBlank { spec.name } ?: spec.name,
                        region = spec.region,
                        price = price,
                        change = change,
                        changePct = pct,
                        timeText = p.getOrNull(6).orEmpty()
                    )
                }
                "DINIW" -> {
                    val price = p.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
                    val open = p.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                    val change = if (open > 0) price - open else 0.0
                    val pct = if (open > 0) change / open * 100.0 else 0.0
                    out += GlobalIndexQuote(
                        code = spec.code,
                        name = p.getOrNull(9)?.ifBlank { spec.name } ?: spec.name,
                        region = spec.region,
                        price = price,
                        change = change,
                        changePct = pct,
                        timeText = p.getOrNull(0).orEmpty()
                    )
                }
                else -> {
                    // int_*: name,price,change,pct
                    // b_*: name,price,change,pct,...
                    val price = p.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
                    val change = p.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                    val pct = p.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    out += GlobalIndexQuote(
                        code = spec.code,
                        name = p.getOrNull(0)?.ifBlank { spec.name } ?: spec.name,
                        region = spec.region,
                        price = price,
                        change = change,
                        changePct = pct
                    )
                }
            }
        }
        return out
    }

    /**
     * 人气龙头：东财人气 + 雪球热股 + 同花顺板块龙/天梯 + 成交额/市值/换手。
     * 偏向「能带动板块」的辨识度龙，而非纯杂毛人气。
     */
    private suspend fun fetchDragonLeaders(
        ladder: List<LadderLevel>,
        upStocks: List<LimitStock>
    ): List<DragonLeader> = coroutineScope {
        val emDeferred = async {
            runCatching { fetchEmHotRank(limit = 40) }.getOrElse { emptyList() }
        }
        val xqDeferred = async {
            runCatching { fetchXueqiuHotA(limit = 30) }.getOrElse { emptyList() }
        }
        val thsDeferred = async {
            runCatching { fetchThsHotRank(limit = 40) }.getOrElse { emptyList() }
        }
        val blockDeferred = async {
            runCatching { fetchBlockTop() }.getOrElse { emptyList() }
        }
        rankDragonLeaders(
            ladder = ladder,
            upStocks = upStocks,
            emHot = emDeferred.await(),
            xqHot = xqDeferred.await(),
            thsHot = thsDeferred.await(),
            blocks = blockDeferred.await()
        )
    }

    private data class HotQuote(
        val code: String,
        val name: String,
        val changePct: Double = 0.0,
        val price: Double = 0.0,
        /** 成交额（亿） */
        val amountYi: Double = 0.0,
        /** 流通市值（亿） */
        val marketCapYi: Double = 0.0,
        /** 换手 % */
        val turnoverRate: Double = 0.0
    )

    private data class EmHotItem(
        val code: String,
        val name: String,
        val rank: Int,
        val rankDelta: Int,
        val quote: HotQuote
    )

    private data class XueqiuHotItem(
        val code: String,
        val name: String,
        val rank: Int,
        val changePct: Double,
        val hotValue: Double
    )

    private data class ThsHotItem(
        val code: String,
        val name: String,
        val rank: Int,
        val hotRate: Double,
        val changePct: Double,
        val rankChg: Int,
        val popTag: String,
        val concepts: List<String>
    )

    private data class BlockTopStock(
        val code: String,
        val name: String,
        val continueNum: Int,
        val highLabel: String,
        val changePct: Double,
        val reason: String,
        val concept: String,
        val conceptLimitUp: Int,
        /** 在该概念 stock_list 中的次序，0=最前 */
        val orderInConcept: Int
    )

    private fun fetchEmHotRank(limit: Int): List<EmHotItem> {
        val body = """{"appId":"appId01","globalId":"786e4c21-70dc-435a-93bb-38","pageNo":1,"pageSize":$limit}"""
        val json = httpPost(
            url = "https://emappdata.eastmoney.com/stockrank/getAllCurrentList",
            body = body,
            referer = "https://guba.eastmoney.com/"
        )
        val root = JsonParser.parseString(json).asJsonObject
        val arr = root.getAsJsonArray("data") ?: return emptyList()
        val codes = mutableListOf<Pair<String, JsonObject>>()
        arr.forEach { el ->
            val o = el.asJsonObject
            val sc = o.stringOrNull("sc").orEmpty()
            if (sc.length < 8) return@forEach
            codes += sc.substring(2) to o
        }
        if (codes.isEmpty()) return emptyList()
        val quotes = fetchEmQuotes(codes.map { it.first })
        return codes.mapNotNull { (code, o) ->
            val q = quotes[code] ?: HotQuote(code = code, name = "")
            EmHotItem(
                code = code,
                name = q.name,
                rank = o.intOrNull("rk") ?: 0,
                rankDelta = o.intOrNull("rc") ?: 0,
                quote = q
            )
        }.filter { it.code.isNotBlank() && it.rank > 0 }
    }

    /**
     * 同花顺热榜（小时·大家都在看）
     * https://dq.10jqka.com.cn/fuyao/hot_list_data/out/hot_list/v1/stock
     */
    private fun fetchThsHotRank(limit: Int): List<ThsHotItem> {
        val url =
            "https://dq.10jqka.com.cn/fuyao/hot_list_data/out/hot_list/v1/stock" +
                "?stock_type=a&type=hour&list_type=normal"
        val json = httpGet(
            url = url,
            referer = "https://eq.10jqka.com.cn/"
        )
        val root = JsonParser.parseString(json).asJsonObject
        // 该接口可能无 status_code，直接看 data.stock_list
        val arr = root.getAsJsonObject("data")?.getAsJsonArray("stock_list")
            ?: return emptyList()
        val out = mutableListOf<ThsHotItem>()
        arr.take(limit.coerceAtLeast(1)).forEach { el ->
            val o = el.asJsonObject
            val code = o.stringOrNull("code").orEmpty()
            if (code.length != 6) return@forEach
            val tag = o.getAsJsonObject("tag")
            val concepts = mutableListOf<String>()
            tag?.getAsJsonArray("concept_tag")?.forEach { c ->
                runCatching { c.asString }.getOrNull()?.let { concepts += it }
            }
            out += ThsHotItem(
                code = code,
                name = o.stringOrNull("name").orEmpty(),
                rank = o.intOrNull("order") ?: (out.size + 1),
                hotRate = o.doubleOrNull("rate") ?: 0.0,
                changePct = o.doubleOrNull("rise_and_fall") ?: 0.0,
                rankChg = o.intOrNull("hot_rank_chg") ?: 0,
                popTag = tag?.stringOrNull("popularity_tag").orEmpty().replace("\n", ""),
                concepts = concepts
            )
        }
        return out
    }

    /** 雪球 A 股热股榜 type=12 */
    private fun fetchXueqiuHotA(limit: Int): List<XueqiuHotItem> {
        // 先拿 cookie，再拉热股
        httpGetWithCookies(
            url = "https://xueqiu.com/",
            referer = "https://xueqiu.com/",
            cookieJar = true
        )
        val json = httpGetWithCookies(
            url = "https://stock.xueqiu.com/v5/stock/hot_stock/list.json?size=$limit&type=12&_type=12",
            referer = "https://xueqiu.com/",
            cookieJar = true
        )
        val root = JsonParser.parseString(json).asJsonObject
        val items = root.getAsJsonObject("data")?.getAsJsonArray("items") ?: return emptyList()
        val out = mutableListOf<XueqiuHotItem>()
        items.forEachIndexed { idx, el ->
            val o = el.asJsonObject
            val symbol = o.stringOrNull("symbol").orEmpty() // SZ300308
            val code = when {
                symbol.length >= 8 && (symbol.startsWith("SZ") || symbol.startsWith("SH")) ->
                    symbol.substring(2)
                else -> o.stringOrNull("code")?.takeLast(6).orEmpty()
            }
            if (code.length != 6 || !code.all { it.isDigit() }) return@forEachIndexed
            out += XueqiuHotItem(
                code = code,
                name = o.stringOrNull("name").orEmpty(),
                rank = idx + 1,
                changePct = o.doubleOrNull("percent") ?: 0.0,
                hotValue = o.doubleOrNull("value") ?: 0.0
            )
        }
        return out
    }

    private fun fetchEmQuotes(codes: List<String>): Map<String, HotQuote> {
        if (codes.isEmpty()) return emptyMap()
        val secids = codes.joinToString(",") { code ->
            val mkt = if (code.startsWith("6")) "1" else "0"
            "$mkt.$code"
        }
        // f5量 f6额 f8换手 f12代码 f14名 f20总市值 f21流通市值
        val quoteJson = httpGet(
            url = "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&fields=f2,f3,f5,f6,f8,f12,f14,f20,f21&secids=$secids",
            referer = "https://quote.eastmoney.com/"
        )
        val root = JsonParser.parseString(quoteJson).asJsonObject
        val map = mutableMapOf<String, HotQuote>()
        root.getAsJsonObject("data")?.getAsJsonArray("diff")?.forEach { el ->
            val o = el.asJsonObject
            val code = o.stringOrNull("f12") ?: return@forEach
            val amount = o.doubleOrNull("f6") ?: 0.0
            val circ = o.doubleOrNull("f21") ?: o.doubleOrNull("f20") ?: 0.0
            map[code] = HotQuote(
                code = code,
                name = o.stringOrNull("f14").orEmpty(),
                changePct = o.doubleOrNull("f3") ?: 0.0,
                price = o.doubleOrNull("f2") ?: 0.0,
                amountYi = amount / 1e8,
                marketCapYi = circ / 1e8,
                turnoverRate = o.doubleOrNull("f8") ?: 0.0
            )
        }
        return map
    }

    private fun fetchBlockTop(): List<BlockTopStock> {
        val json = httpGet("$BASE/block_top?filter=$FILTER")
        val root = JsonParser.parseString(json).asJsonObject
        if ((root.intOrNull("status_code") ?: -1) != 0) return emptyList()
        val out = mutableListOf<BlockTopStock>()
        root.getAsJsonArray("data")?.forEach { bel ->
            val b = bel.asJsonObject
            val concept = b.stringOrNull("name").orEmpty()
            val lu = b.intOrNull("limit_up_num") ?: 0
            b.getAsJsonArray("stock_list")?.forEachIndexed { idx, sel ->
                val s = sel.asJsonObject
                val code = s.stringOrNull("code").orEmpty()
                if (code.isBlank()) return@forEachIndexed
                out += BlockTopStock(
                    code = code,
                    name = s.stringOrNull("name").orEmpty(),
                    continueNum = s.intOrNull("continue_num") ?: 1,
                    highLabel = s.stringOrNull("high").orEmpty(),
                    changePct = s.doubleOrNull("change_rate") ?: 0.0,
                    reason = s.stringOrNull("reason_type").orEmpty(),
                    concept = concept,
                    conceptLimitUp = lu,
                    orderInConcept = idx
                )
            }
        }
        return out
    }

    private fun rankDragonLeaders(
        ladder: List<LadderLevel>,
        upStocks: List<LimitStock>,
        emHot: List<EmHotItem>,
        xqHot: List<XueqiuHotItem>,
        thsHot: List<ThsHotItem>,
        blocks: List<BlockTopStock>
    ): List<DragonLeader> {
        val ladderByCode = mutableMapOf<String, Pair<Int, String>>()
        ladder.forEach { level ->
            level.stocks.forEach { s ->
                ladderByCode[s.code] = level.height to "${level.height}板"
            }
        }
        // 同票可属多概念：取「板块涨停多 + 排序靠前」者，代表更能带板块
        val blockByCode = blocks
            .groupBy { it.code }
            .mapValues { (_, list) ->
                list.maxBy {
                    it.conceptLimitUp * 20 - it.orderInConcept * 3 + it.continueNum
                }
            }
        val emByCode = emHot.associateBy { it.code }
        val xqByCode = xqHot.associateBy { it.code }
        val thsByCode = thsHot.associateBy { it.code }
        val upByCode = upStocks.associateBy { it.code }

        // 补行情：候选里没有完整量价的再拉一遍
        val needQuote = LinkedHashSet<String>().apply {
            addAll(emHot.map { it.code })
            addAll(xqHot.take(20).map { it.code })
            addAll(thsHot.take(25).map { it.code })
            addAll(blockByCode.keys)
            addAll(ladderByCode.keys)
            addAll(upStocks.take(40).map { it.code })
        }
        val quotes = fetchEmQuotes(needQuote.toList())

        val codes = LinkedHashSet<String>().apply {
            addAll(emHot.take(20).map { it.code })
            addAll(xqHot.take(20).map { it.code })
            addAll(thsHot.take(25).map { it.code })
            addAll(ladderByCode.keys)
            // 强势概念前列（更能带板块）
            addAll(blocks.filter { it.conceptLimitUp >= 5 && it.orderInConcept <= 2 }.map { it.code })
        }

        data class Cand(val score: Double, val leader: DragonLeader)

        val cands = codes.mapNotNull { code ->
            val em = emByCode[code]
            val xq = xqByCode[code]
            val ths = thsByCode[code]
            val block = blockByCode[code]
            val ladderPair = ladderByCode[code]
            val up = upByCode[code]
            val q = quotes[code] ?: em?.quote

            val continueNum = maxOf(
                ladderPair?.first ?: 0,
                block?.continueNum ?: 0,
                parseContinueFromHighDays(up?.highDays.orEmpty())
            )
            val changePct = when {
                up != null && kotlin.math.abs(up.changeRate) > 0.01 -> up.changeRate
                q != null && kotlin.math.abs(q.changePct) > 0.01 -> q.changePct
                ths != null && kotlin.math.abs(ths.changePct) > 0.01 -> ths.changePct
                block != null -> block.changePct
                xq != null -> xq.changePct
                else -> 0.0
            }
            val amountYi = q?.amountYi?.takeIf { it > 0 }
                ?: 0.0
            val marketCapYi = when {
                (q?.marketCapYi ?: 0.0) > 0 -> q!!.marketCapYi
                up != null && up.marketCapYi > 0 -> up.marketCapYi
                else -> 0.0
            }
            val turnover = when {
                (q?.turnoverRate ?: 0.0) > 0 -> q!!.turnoverRate
                up != null && up.turnoverRate > 0 -> up.turnoverRate
                else -> 0.0
            }
            val name = up?.name?.ifBlank { null }
                ?: q?.name?.ifBlank { null }
                ?: ths?.name?.ifBlank { null }
                ?: block?.name?.ifBlank { null }
                ?: em?.name?.ifBlank { null }
                ?: xq?.name?.ifBlank { null }
                ?: code
            val highLabel = when {
                !up?.highDays.isNullOrBlank() -> up!!.highDays
                ths?.popTag?.contains("板") == true -> ths.popTag
                !block?.highLabel.isNullOrBlank() -> block!!.highLabel
                continueNum > 1 -> "${continueNum}板"
                continueNum == 1 -> "首板"
                else -> ""
            }
            val concept = block?.concept?.ifBlank { null }
                ?: ths?.concepts?.firstOrNull().orEmpty()
            val conceptLu = block?.conceptLimitUp ?: 0
            val reason = block?.reason?.ifBlank { null }
                ?: ths?.popTag?.ifBlank { null }
                ?: up?.reason?.ifBlank { null }
                ?: up?.limitUpType.orEmpty()

            var score = 0.0

            // —— 多平台人气 ——
            em?.let {
                score += (22 - it.rank).coerceAtLeast(0) * 2.5
                if (it.rankDelta > 0) score += 4.0
                if (it.rankDelta < 0) score -= 2.0
            }
            xq?.let {
                score += (22 - it.rank).coerceAtLeast(0) * 2.2
                score += (it.hotValue / 2500.0).coerceIn(0.0, 7.0)
            }
            ths?.let {
                // 同花顺热榜：排名 + 热度值
                score += (22 - it.rank).coerceAtLeast(0) * 2.7
                score += (kotlin.math.ln((it.hotRate + 1.0).coerceAtLeast(1.0)) / 3.0)
                    .coerceIn(0.0, 12.0)
                if (it.rankChg > 0) score += 5.0
                if (it.rankChg < 0) score -= 2.0
                if (it.popTag.contains("板") || it.popTag.contains("涨停")) score += 4.0
            }
            val hotPlatforms = listOfNotNull(em, xq, ths).size
            if (hotPlatforms >= 2) score += 16.0
            if (hotPlatforms >= 3) score += 14.0 // 三平台共振

            // —— 板块带动（核心）——
            block?.let {
                score += 10.0 + it.conceptLimitUp.coerceAtMost(18) * 1.1
                // 概念内越靠前，越像板块龙
                score += when (it.orderInConcept) {
                    0 -> 18.0
                    1 -> 12.0
                    2 -> 7.0
                    else -> 2.0
                }
                if (it.conceptLimitUp >= 8 && it.orderInConcept <= 1) score += 10.0
            }

            // 连板：人气龙加分，但不再压过「带板块」
            score += continueNum * 12.0
            if (ladderPair != null && continueNum >= 2) score += 6.0

            // —— 成交额 / 市值 / 换手（辨识度与承接）——
            score += when {
                amountYi >= 40 -> 16.0
                amountYi >= 15 -> 12.0
                amountYi >= 6 -> 8.0
                amountYi >= 2 -> 3.0
                amountYi > 0 -> 0.0
                else -> -2.0
            }
            // 短线人气龙甜蜜市值：约 40～280 亿流通
            score += when {
                marketCapYi in 40.0..280.0 -> 14.0
                marketCapYi in 25.0..40.0 || marketCapYi in 280.0..450.0 -> 7.0
                marketCapYi in 15.0..25.0 -> 2.0
                marketCapYi > 0 && marketCapYi < 15.0 -> -10.0 // 过小难带板块
                marketCapYi > 800.0 -> -6.0 // 过大弹性差，除非极强人气
                else -> 0.0
            }
            val bestHotRank = listOfNotNull(em?.rank, xq?.rank, ths?.rank).minOrNull() ?: 99
            if (marketCapYi > 800.0 && bestHotRank <= 5) score += 8.0
            score += when {
                turnover in 6.0..28.0 -> 8.0
                turnover in 3.0..6.0 || turnover in 28.0..40.0 -> 3.0
                turnover > 45.0 -> -4.0 // 过热兑现风险
                else -> 0.0
            }

            // 涨势：人气龙最好当日强势
            score += when {
                changePct >= 9.5 -> 14.0
                changePct >= 5.0 -> 8.0
                changePct >= 2.0 -> 3.0
                changePct <= -8.0 -> -14.0
                changePct <= -3.0 -> -7.0
                else -> 0.0
            }

            val structureSources = listOfNotNull(block, ladderPair).size
            if (hotPlatforms >= 1 && structureSources >= 1) score += 16.0
            if (hotPlatforms >= 2 && structureSources >= 1) score += 10.0
            // 纯人气、无板块/连板：降权（容易是杂毛情绪）
            if (hotPlatforms >= 1 && structureSources == 0 && continueNum <= 0) score -= 12.0

            if (score < 12.0) return@mapNotNull null

            val tags = buildList {
                em?.let { add("东财#${it.rank}") }
                xq?.let { add("雪球#${it.rank}") }
                ths?.let { add("同花顺#${it.rank}") }
                when {
                    hotPlatforms >= 3 -> add("三平台共振")
                    hotPlatforms >= 2 -> add("双平台人气")
                }
                if (block != null && block.orderInConcept <= 1 && conceptLu >= 5) add("板块龙")
                if (continueNum >= 2) add("${continueNum}连板")
                else if (continueNum == 1) add("首板")
                ths?.popTag?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (amountYi >= 10) add(String.format("额%.0f亿", amountYi))
                if (marketCapYi >= 1) add(String.format("流值%.0f亿", marketCapYi))
                if (concept.isNotBlank()) add(concept)
            }

            Cand(
                score = score,
                leader = DragonLeader(
                    rank = 0,
                    code = code,
                    name = name,
                    continueNum = continueNum,
                    highLabel = highLabel,
                    changePct = changePct,
                    hotRank = em?.rank,
                    hotDelta = em?.rankDelta,
                    xueqiuRank = xq?.rank,
                    thsRank = ths?.rank,
                    thsHotRate = ths?.hotRate ?: 0.0,
                    thsPopTag = ths?.popTag.orEmpty(),
                    amountYi = amountYi,
                    marketCapYi = marketCapYi,
                    turnoverRate = turnover,
                    concept = concept,
                    conceptLimitUp = conceptLu,
                    reason = reason,
                    score = score.toInt(),
                    tags = tags.distinct()
                )
            )
        }.sortedByDescending { it.score }

        return cands.take(3).mapIndexed { i, c -> c.leader.copy(rank = i + 1) }
    }

    private fun parseContinueFromHighDays(highDays: String): Int {
        if (highDays.contains("首板")) return 1
        val m = Regex("""(\d+)\s*板""").find(highDays) ?: return 0
        return m.groupValues[1].toIntOrNull() ?: 0
    }

    /** 上证 / 深成 / 创业 近 N 日（东财日 K，失败则腾讯当日点兜底） */
    private suspend fun fetchMajorIndices(want: Int): List<IndexSeries> = coroutineScope {
        // 错开并行，减轻东财短连断开
        listOf(
            Triple("1.000001", "000001", "上证指数"),
            Triple("0.399001", "399001", "深证成指"),
            Triple("0.399006", "399006", "创业板指")
        ).mapIndexed { index, (secid, code, name) ->
            async {
                if (index > 0) {
                    runCatching { Thread.sleep(120L * index) }
                }
                runCatching { fetchEmIndexKline(secid, code, name, want) }
                    .onFailure { Log.w(TAG, "index $name failed", it) }
                    .getOrElse {
                        runCatching { fetchSinaIndexKline(code, name, want) }
                            .recoverCatching { fetchTencentIndexLatest(code, name) }
                            .getOrElse { IndexSeries(code = code, name = name, days = emptyList()) }
                    }
            }
        }.awaitAll()
    }

    private fun fetchEmIndexKline(
        secid: String,
        code: String,
        name: String,
        want: Int
    ): IndexSeries {
        var lastError: Exception? = null
        for (host in EM_KLINE_HOSTS) {
            repeat(2) { attempt ->
                try {
                    val series = fetchEmIndexKlineOnce(host, secid, code, name, want)
                    if (series.days.isNotEmpty()) return series
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "index $name via $host attempt=${attempt + 1}: ${e.message}")
                    runCatching { Thread.sleep(160L * (attempt + 1)) }
                }
            }
        }
        // 东财失败：新浪近 N 日 K（可画曲线）；再不行腾讯当日点
        val sina = runCatching { fetchSinaIndexKline(code, name, want) }.getOrNull()
        if (sina != null && sina.days.size >= 2) return sina
        val fallback = runCatching { fetchTencentIndexLatest(code, name) }.getOrNull()
        if (fallback != null && fallback.days.isNotEmpty()) return fallback
        if (sina != null && sina.days.isNotEmpty()) return sina
        throw lastError ?: IllegalStateException("指数 $name 拉取失败")
    }

    private fun fetchEmIndexKlineOnce(
        host: String,
        secid: String,
        code: String,
        name: String,
        want: Int
    ): IndexSeries {
        val url = buildString {
            append("https://").append(host).append("/api/qt/stock/kline/get")
            append("?secid=").append(secid)
            append("&fields1=f1,f2,f3,f4,f5,f6")
            append("&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61")
            append("&klt=101&fqt=1&end=20500101&lmt=").append(want.coerceIn(5, 60))
        }
        val json = httpGet(
            url = url,
            referer = "https://quote.eastmoney.com/"
        )
        val root = JsonParser.parseString(json).asJsonObject
        val data = root.getAsJsonObject("data")
            ?: return IndexSeries(code, name, emptyList())
        val displayName = data.stringOrNull("name")?.ifBlank { name } ?: name
        val klines = data.getAsJsonArray("klines") ?: return IndexSeries(code, displayName, emptyList())
        val days = klines.mapNotNull { el ->
            val line = el.asString
            val p = line.split(',')
            if (p.size < 9) return@mapNotNull null
            val date = p[0].replace("-", "")
            val close = p[2].toDoubleOrNull() ?: return@mapNotNull null
            val changePct = p[8].toDoubleOrNull() ?: 0.0
            val amountYuan = p.getOrNull(6)?.toDoubleOrNull() ?: 0.0
            IndexDay(
                date = date,
                close = close,
                changePct = changePct,
                amountYi = amountYuan / 1e8
            )
        }
        // 接口为旧→新，统一成新→旧
        return IndexSeries(code = code, name = displayName, days = days.asReversed())
    }

    /**
     * 新浪日 K 兜底（东财 push2his 常断连时用）。
     * 返回新→旧；涨跌幅由昨收推算。
     */
    private fun fetchSinaIndexKline(code: String, name: String, want: Int): IndexSeries {
        val symbol = when (code) {
            "000001" -> "sh000001"
            "399001" -> "sz399001"
            "399006" -> "sz399006"
            else -> return IndexSeries(code, name, emptyList())
        }
        val lim = want.coerceIn(5, 60)
        val url = buildString {
            append("https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/")
            append("CN_MarketData.getKLineData")
            append("?symbol=").append(symbol)
            append("&scale=240&ma=no&datalen=").append(lim)
        }
        val json = httpGet(
            url = url,
            referer = "https://finance.sina.com.cn/"
        )
        val arr = runCatching { JsonParser.parseString(json).asJsonArray }.getOrNull()
            ?: return IndexSeries(code, name, emptyList())
        // 新浪为旧→新
        val chrono = arr.mapNotNull { el ->
            val o = el.asJsonObject
            val day = o.stringOrNull("day") ?: return@mapNotNull null
            val close = o.get("close")?.asString?.toDoubleOrNull()
                ?: runCatching { o.get("close")?.asDouble }.getOrNull()
                ?: return@mapNotNull null
            day.replace("-", "") to close
        }
        if (chrono.isEmpty()) return IndexSeries(code, name, emptyList())
        val days = chrono.mapIndexed { i, (date, close) ->
            val prev = chrono.getOrNull(i - 1)?.second
            val changePct = if (prev != null && prev > 0) {
                (close / prev - 1.0) * 100.0
            } else {
                0.0
            }
            IndexDay(
                date = date,
                close = close,
                changePct = changePct,
                amountYi = 0.0
            )
        }
        return IndexSeries(code = code, name = name, days = days.asReversed())
    }

    /** 腾讯简况：s_sh000001 / s_sz399001 / s_sz399006 */
    private fun fetchTencentIndexLatest(code: String, name: String): IndexSeries {
        val symbol = when (code) {
            "000001" -> "s_sh000001"
            "399001" -> "s_sz399001"
            "399006" -> "s_sz399006"
            else -> return IndexSeries(code, name, emptyList())
        }
        val text = httpGet(
            url = "https://qt.gtimg.cn/q=$symbol",
            referer = "https://finance.qq.com/",
            charset = "GBK"
        )
        val m = Regex("""v_${Regex.escape(symbol)}="([^"]*)"""").find(text)
            ?: return IndexSeries(code, name, emptyList())
        val p = m.groupValues[1].split('~')
        // 1~名称~代码~现价~涨跌额~涨跌幅~成交量~成交额...
        if (p.size < 6) return IndexSeries(code, name, emptyList())
        val close = p[3].toDoubleOrNull() ?: return IndexSeries(code, name, emptyList())
        val changePct = p[5].toDoubleOrNull() ?: 0.0
        val amountYi = p.getOrNull(7)?.toDoubleOrNull()?.div(10_000.0) ?: 0.0 // 万元→亿
        val displayName = p.getOrNull(1)?.ifBlank { name } ?: name
        return IndexSeries(
            code = code,
            name = displayName,
            days = listOf(
                IndexDay(
                    date = todayYmd(),
                    close = close,
                    changePct = changePct,
                    amountYi = amountYi
                )
            )
        )
    }

    private suspend fun fetchRecentDayStats(
        anchorDate: String,
        want: Int
    ): List<LimitDayStat> = coroutineScope {
        val candidates = recentYmdCandidates(anchorDate, want * 3)
        val partial = candidates.map { d ->
            async {
                runCatching {
                    val json = httpGet(poolUrl("limit_up_pool", 1, 1, d))
                    val root = JsonParser.parseString(json).asJsonObject
                    if ((root.intOrNull("status_code") ?: -1) != 0) return@runCatching null
                    val data = root.getAsJsonObject("data") ?: return@runCatching null
                    val up = parseSideCount(data.getAsJsonObject("limit_up_count")?.getAsJsonObject("today"))
                    val down = parseSideCount(data.getAsJsonObject("limit_down_count")?.getAsJsonObject("today"))
                    // 非交易日 today 全 0；跳过
                    if (up.num == 0 && up.historyNum == 0 && down.num == 0 && down.historyNum == 0) {
                        null
                    } else {
                        LimitDayStat(date = d, up = up, down = down)
                    }
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
        partial.take(want)
    }

    private fun todayYmd(): String {
        val c = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d%02d%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun recentYmdCandidates(anchorYmd: String, count: Int): List<String> {
        val cal = Calendar.getInstance()
        if (anchorYmd.length == 8) {
            runCatching {
                cal.set(
                    anchorYmd.substring(0, 4).toInt(),
                    anchorYmd.substring(4, 6).toInt() - 1,
                    anchorYmd.substring(6, 8).toInt()
                )
            }
        }
        return (0 until count).map {
            val ymd = String.format(
                Locale.US,
                "%04d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            cal.add(Calendar.DAY_OF_MONTH, -1)
            ymd
        }
    }

    private suspend fun fetchLadderHistory(dates: List<String>): List<LadderDay> =
        coroutineScope {
            dates.map { d ->
                async {
                    runCatching {
                        val json = httpGet("$BASE/continuous_limit_up?date=$d&filter=$FILTER")
                        LadderDay(date = d, levels = parseLadder(json))
                    }.getOrElse { LadderDay(date = d, levels = emptyList()) }
                }
            }.awaitAll()
        }

    /** 近 N 个交易日涨跌家数 + 成交额（同花顺 overview） */
    private suspend fun fetchMarketBreadth(
        anchorDate: String,
        want: Int
    ): List<MarketBreadthDay> = coroutineScope {
        val candidates = recentYmdCandidates(anchorDate, want * 3)
        candidates.map { d ->
            async {
                runCatching {
                    val json = httpGet("$OVERVIEW?date=$d")
                    val root = JsonParser.parseString(json).asJsonObject
                    if ((root.intOrNull("status_code") ?: -1) != 0) return@runCatching null
                    val data = root.getAsJsonObject("data") ?: return@runCatching null
                    val rf = data.getAsJsonObject("rise_fall") ?: return@runCatching null
                    val rise = rf.intOrNull("rise") ?: 0
                    val fall = rf.intOrNull("fall") ?: 0
                    val flat = rf.intOrNull("deuce") ?: 0
                    if (rise == 0 && fall == 0) return@runCatching null
                    val to = data.getAsJsonObject("turnover")
                    val turnoverText = to?.stringOrNull("now").orEmpty()
                    MarketBreadthDay(
                        date = d,
                        rise = rise,
                        fall = fall,
                        flat = flat,
                        limitUp = rf.intOrNull("limit_up") ?: 0,
                        limitDown = rf.intOrNull("limit_down") ?: 0,
                        turnoverText = turnoverText,
                        turnoverYi = parseTurnoverYi(turnoverText),
                        turnoverFlag = to?.intOrNull("flag") ?: 0
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull().take(want)
    }

    /** "1.5万亿" / "8500亿" → 亿 */
    private fun parseTurnoverYi(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val t = text.trim().replace(",", "").replace(" ", "")
        val num = Regex("""[\d.]+""").find(t)?.value?.toDoubleOrNull() ?: return 0.0
        return when {
            "万亿" in t -> num * 10_000.0
            "亿" in t -> num
            "万" in t -> num / 10_000.0
            else -> num
        }
    }

    private fun poolUrl(
        path: String,
        page: Int,
        limit: Int,
        date: String?
    ): String = buildString {
        append("$BASE/$path")
        append("?page=").append(page)
        append("&limit=").append(limit)
        append("&field=").append(FIELDS)
        append("&filter=").append(FILTER)
        append("&order_field=330324&order_type=0")
        if (!date.isNullOrBlank()) append("&date=").append(date)
    }

    private fun httpGet(
        url: String,
        referer: String = "https://data.10jqka.com.cn/mobile/limitup/v2/index.html",
        charset: String = "UTF-8"
    ): String = httpRequest(
        url = url,
        method = "GET",
        body = null,
        referer = referer,
        cookieJar = false,
        charset = charset
    )

    private fun httpGetWithCookies(
        url: String,
        referer: String,
        cookieJar: Boolean
    ): String = httpRequest(
        url = url,
        method = "GET",
        body = null,
        referer = referer,
        cookieJar = cookieJar,
        charset = "UTF-8"
    )

    private fun httpPost(
        url: String,
        body: String,
        referer: String
    ): String = httpRequest(
        url = url,
        method = "POST",
        body = body,
        referer = referer,
        cookieJar = false,
        charset = "UTF-8"
    )

    private fun httpRequest(
        url: String,
        method: String,
        body: String?,
        referer: String,
        cookieJar: Boolean,
        charset: String
    ): String {
        val host = runCatching { URL(url).host }.getOrDefault("")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 15_000
            doInput = true
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", referer)
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Accept-Encoding", "identity")
            // 同花顺热榜等跨域接口需要 Origin
            if (referer.contains("eq.10jqka.com.cn") || referer.contains("10jqka.com.cn")) {
                setRequestProperty("Origin", "https://eq.10jqka.com.cn")
            }
            if (cookieJar) {
                val cookie = cookieHeaderFor(host)
                if (cookie.isNotBlank()) setRequestProperty("Cookie", cookie)
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charset.forName("UTF-8")))
                }
            }
            val code = conn.responseCode
            if (cookieJar) {
                mergeCookies(host, conn.headerFields["Set-Cookie"])
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                ?: throw IllegalStateException("行情接口 HTTP $code")
            val text = BufferedReader(InputStreamReader(stream, Charset.forName(charset))).use {
                it.readText()
            }
            if (code !in 200..299) {
                throw IllegalStateException("行情接口 HTTP $code: ${text.take(200)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun cookieHeaderFor(host: String): String {
        cookieStore[host]?.takeIf { it.isNotBlank() }?.let { return it }
        // stock.xueqiu.com 需带上 xueqiu.com 域 cookie
        if (host.contains("xueqiu")) {
            return cookieStore
                .filterKeys { it.contains("xueqiu") }
                .values
                .filter { it.isNotBlank() }
                .joinToString("; ")
        }
        return ""
    }

    private fun mergeCookies(host: String, setCookies: List<String>?) {
        if (host.isBlank() || setCookies.isNullOrEmpty()) return
        val map = cookieStore[host]
            ?.split(';')
            ?.mapNotNull {
                val p = it.trim().split('=', limit = 2)
                if (p.size == 2) p[0].trim() to p[1].trim() else null
            }
            ?.toMap()
            ?.toMutableMap()
            ?: mutableMapOf()
        setCookies.forEach { raw ->
            val first = raw.substringBefore(';').trim()
            val p = first.split('=', limit = 2)
            if (p.size == 2 && p[0].isNotBlank()) map[p[0].trim()] = p[1].trim()
        }
        if (map.isNotEmpty()) {
            val joined = map.entries.joinToString("; ") { "${it.key}=${it.value}" }
            cookieStore[host] = joined
            if (host.contains("xueqiu") && host != "xueqiu.com") {
                cookieStore["xueqiu.com"] = joined
            }
        }
    }

    private fun parsePool(json: String): LimitUpPoolResult {
        val root = JsonParser.parseString(json).asJsonObject
        val status = root.intOrNull("status_code") ?: -1
        if (status != 0) {
            val msg = root.stringOrNull("status_msg") ?: "未知错误"
            throw IllegalStateException("同花顺返回异常($status): $msg")
        }
        val data = root.getAsJsonObject("data")
            ?: throw IllegalStateException("同花顺返回无 data")

        val page = data.getAsJsonObject("page")
        val total = page?.intOrNull("total")
            ?: data.getAsJsonArray("info")?.size()
            ?: 0

        val today = data.getAsJsonObject("limit_up_count")?.getAsJsonObject("today")
        val yesterday = data.getAsJsonObject("limit_up_count")?.getAsJsonObject("yesterday")
        val count = LimitUpCount(
            todayNum = today?.intOrNull("num") ?: total,
            todayHistoryNum = today?.intOrNull("history_num") ?: 0,
            todayRate = today?.doubleOrNull("rate") ?: 0.0,
            todayOpenNum = today?.intOrNull("open_num") ?: 0,
            yesterdayNum = yesterday?.intOrNull("num") ?: 0
        )

        val stocks = mutableListOf<LimitStock>()
        val info = data.getAsJsonArray("info")
        if (info != null) {
            for (el in info) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val currency = o.doubleOrNull("currency_value") ?: 0.0
                stocks += LimitStock(
                    code = o.stringOrNull("code").orEmpty(),
                    name = o.stringOrNull("name").orEmpty(),
                    changeRate = o.doubleOrNull("change_rate") ?: 0.0,
                    turnoverRate = o.doubleOrNull("turnover_rate") ?: 0.0,
                    highDays = o.stringOrNull("high_days").orEmpty(),
                    limitUpType = o.stringOrNull("limit_up_type").orEmpty(),
                    reason = o.stringOrNull("reason_type").orEmpty(),
                    latest = o.doubleOrNull("latest") ?: 0.0,
                    openNum = o.intOrNull("open_num") ?: 0,
                    marketCapYi = if (currency > 0) currency / 1e8 else 0.0,
                    orderAmount = o.doubleOrNull("order_amount") ?: 0.0
                )
            }
        }

        return LimitUpPoolResult(
            date = data.stringOrNull("date").orEmpty(),
            total = total,
            count = count,
            stocks = stocks
        )
    }

    private fun parseLadder(json: String): List<LadderLevel> {
        val root = JsonParser.parseString(json).asJsonObject
        val status = root.intOrNull("status_code") ?: -1
        if (status != 0) return emptyList()
        val arr = root.getAsJsonArray("data") ?: return emptyList()
        val levels = mutableListOf<LadderLevel>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val height = o.intOrNull("height") ?: continue
            val stocks = mutableListOf<LadderStock>()
            val list = o.getAsJsonArray("code_list")
            if (list != null) {
                for (s in list) {
                    if (!s.isJsonObject) continue
                    val so = s.asJsonObject
                    stocks += LadderStock(
                        code = so.stringOrNull("code").orEmpty(),
                        name = so.stringOrNull("name").orEmpty(),
                        continueNum = so.intOrNull("continue_num") ?: height
                    )
                }
            }
            levels += LadderLevel(height = height, stocks = stocks)
        }
        return levels.sortedByDescending { it.height }
    }

    private fun parseTradeStatus(json: String?): String {
        if (json.isNullOrBlank()) return ""
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            root.getAsJsonObject("data")?.stringOrNull("stat").orEmpty()
        }.getOrDefault("")
    }

    private fun parseSideCount(obj: JsonObject?): LimitSideCount {
        if (obj == null) return LimitSideCount()
        return LimitSideCount(
            num = obj.intOrNull("num") ?: 0,
            historyNum = obj.intOrNull("history_num") ?: 0,
            rate = obj.doubleOrNull("rate") ?: 0.0,
            openNum = obj.intOrNull("open_num") ?: 0
        )
    }

    companion object {
        private const val TAG = "ThsLimitUpApi"
        private const val BASE = "https://data.10jqka.com.cn/dataapi/limit_up"
        private const val OVERVIEW =
            "https://data.10jqka.com.cn/mobileapi/hotspot_focus/market_state/v1/overview"
        private const val FILTER = "HS,GEM2STAR"
        private const val FIELDS =
            "199112,10,9001,330323,330324,330325,9002,330329,133971,133970,1968584,3475914,9003,9004"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        /** 东财历史 K 多节点，并发时易断，轮询重试 */
        private val EM_KLINE_HOSTS = listOf(
            "push2his.eastmoney.com",
            "72.push2his.eastmoney.com",
            "75.push2his.eastmoney.com",
            "80.push2his.eastmoney.com"
        )

        private fun JsonObject.elem(key: String): JsonElement? {
            val el = get(key) ?: return null
            return if (el.isJsonNull) null else el
        }

        private fun JsonObject.intOrNull(key: String): Int? {
            val el = elem(key) ?: return null
            return runCatching {
                when {
                    el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt
                    el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.toIntOrNull()
                    else -> null
                }
            }.getOrNull()
        }

        private fun JsonObject.doubleOrNull(key: String): Double? {
            val el = elem(key) ?: return null
            return runCatching {
                when {
                    el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asDouble
                    el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.toDoubleOrNull()
                    else -> null
                }
            }.getOrNull()
        }

        private fun JsonObject.stringOrNull(key: String): String? {
            val el = elem(key) ?: return null
            return runCatching {
                when {
                    el.isJsonPrimitive -> el.asString
                    else -> null
                }
            }.getOrNull()
        }
    }
}
