package com.sym.bookkeeping.data

data class StrategyGuide(
    val id: String,
    val name: String,
    val tagline: String,
    val style: String,
    val watchIntensity: String,
    val workerFriendly: Boolean,
    val difficulty: String,
    val holdingDaysShort: String,
    val holdingDaysDetail: String,
    val winRateLevel: String,
    val highWinRatePick: Boolean,
    val winRateAnalysis: String,
    /** 好操作 / 一般 / 难操作 */
    val operability: String,
    /** 可操作性评估 */
    val operabilityNote: String,
    val buyRules: List<String>,
    val sellRules: List<String>,
    val stopLossRules: List<String>,
    val afterWorkSteps: List<String>,
    val summary: String,
    val howTo: List<String>,
    val pros: List<String>,
    val cons: List<String>,
    val tips: List<String>,
    val aShareNotes: List<String>
)

data class WorkerPickRank(
    val rank: Int,
    val strategyId: String,
    val title: String,
    val reason: String
)

object StrategyCatalog {
    @Volatile
    private var imported: List<StrategyGuide> = emptyList()

    fun setImported(list: List<StrategyGuide>) {
        imported = list
    }

    /** 内置 + 导入战法（同 id 时导入覆盖内置文案） */
    fun allGuides(): List<StrategyGuide> {
        val byId = LinkedHashMap<String, StrategyGuide>()
        all.forEach { byId[it.id] = it }
        imported.forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    fun findById(id: String): StrategyGuide? =
        imported.find { it.id == id }
            ?: all.find { it.id == id }
            ?: EmotionPlaybooks.findById(id)

    val workerHighWinRatePicks: List<WorkerPickRank> = listOf(
        WorkerPickRank(
            rank = 1,
            strategyId = "pullback_confirm",
            title = "突破回踩确认法",
            reason = "买卖点可量化；假突破少，晚间决策即可。"
        ),
        WorkerPickRank(
            rank = 2,
            strategyId = "weekly_trend_daily",
            title = "周线定方向 + 日线找买点",
            reason = "周线 MA20 定多空，日线 MA10/20 回踩买；止损清晰，工薪好执行。"
        ),
        WorkerPickRank(
            rank = 3,
            strategyId = "etf_trend",
            title = "宽基/行业 ETF 趋势持仓",
            reason = "按月检查周/月均线，规则少、最好坚持。"
        )
    )

    private val fiveDayMaTrend = StrategyGuide(
        id = "ma5_trend",
        name = "5日均线顺势法",
        tagline = "5日线拐头向上买入，向下卖出",
        style = "短线",
        watchIntensity = "中",
        workerFriendly = false,
        difficulty = "入门",
        holdingDaysShort = "2～10天",
        holdingDaysDetail = "常见持股约 2～10 天；顺畅趋势段偶可到 10～15 天。",
        winRateLevel = "中等",
        highWinRatePick = false,
        winRateAnalysis = "趋势段尚可，震荡市胜率下降。工薪若改成只看收盘价，可操作性会好一些。",
        operability = "一般",
        operabilityNote = "规则简单，但震荡市信号多；上班时段难盯盘中拐头，建议强制「只按收盘价」。",
        buyRules = listOf(
            "日线收盘价站上 MA5，且 MA5 较前一日拐头向上（或走平转上）。",
            "最好同时收盘价 > 日线 MA20（过滤弱市）。",
            "不在涨停追高；收盘后或次日开盘附近买入。"
        ),
        sellRules = listOf(
            "日线收盘价跌破 MA5，且 MA5 拐头向下 → 卖出。",
            "或连续 2 日收盘在 MA5 下方 → 卖出。"
        ),
        stopLossRules = listOf(
            "买入当日低点下方 2%～3% 设止损。",
            "或单笔亏损达到 -5% 无条件止损。",
            "T+1：当日买次日才能卖，仓位不宜过重。"
        ),
        afterWorkSteps = listOf(
            "只看收盘价是否站上/跌破 MA5，决定次日买卖。",
            "必须叠加 MA20 多头过滤，否则震荡损耗大。"
        ),
        summary = "用日线 MA5 方向做短线跟随。规则易懂，但对工薪盯盘不友好，震荡市容易反复。",
        howTo = listOf(
            "大盘与板块偏强时才做。",
            "收盘站上 MA5 且拐头向上考虑买。",
            "收盘跌破并拐头向下卖出。"
        ),
        pros = listOf("规则简单", "止损清晰", "易复盘统计"),
        cons = listOf("震荡打脸", "盘中难执行", "弱市连续止损"),
        tips = listOf("强制收盘价决策", "满 20～30 笔再看胜率"),
        aShareNotes = listOf("涨跌停可能无法理想成交", "除权日均线会跳")
    )

    private val dragonHead = StrategyGuide(
        id = "dragon_head",
        name = "龙头战法",
        tagline = "只做主线最强标的，弱转强与分歧转一致",
        style = "短线",
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "1～5天",
        holdingDaysDetail = "主流 1～5 天，强势偶到 7～10 天。",
        winRateLevel = "偏低（搏盈亏比）",
        highWinRatePick = false,
        winRateAnalysis = "胜率通常不高，靠盈亏比。主观判断多，不适合工薪主策略。",
        operability = "难操作",
        operabilityNote = "要盯竞价、盘口、情绪与板块联动，上班几乎无法稳定执行。",
        buyRules = listOf(
            "先确认当日主线题材仍在，且标的为空间/辨识度龙头。",
            "常见买点：分歧转一致（断板回封、换手上升）、强势首阴低吸（需次日确认）。",
            "不买龙二龙三替补，不买退潮期接力。"
        ),
        sellRules = listOf(
            "龙头断板无法回封 → 卖。",
            "高位放量长上影/烂板 → 卖。",
            "主线切换、小弟补涨龙头走弱 → 卖。"
        ),
        stopLossRules = listOf(
            "开板当日无法回封，按收盘或盘中规则止损。",
            "单笔亏损建议不超过 -7%～-8%（短线波动大，仍要有硬顶）。",
            "总短线仓位设上限，避免单日情绪崩溃。"
        ),
        afterWorkSteps = emptyList(),
        summary = "A 股短线核心之一：只做主线龙头。盈亏比高、胜率与执行难度也高。",
        howTo = listOf("定主线", "认龙头", "等分歧转一致", "退潮即走"),
        pros = listOf("盈亏比高", "跟主线资金"),
        cons = listOf("需全日盯盘", "主观强", "容错低"),
        tips = listOf("先模拟盘练识别龙头"),
        aShareNotes = listOf("打板不适合上班操作")
    )

    private val weeklyTrendDailyEntry = StrategyGuide(
        id = "weekly_trend_daily",
        name = "周线定方向 + 日线找买点",
        tagline = "周线MA20定多空，日线MA10/20回踩买",
        style = "波段",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "10～60天",
        holdingDaysDetail = "典型 10～60 天。周线 MA20 完好可更长；有效跌破周线 MA20 则结束。",
        winRateLevel = "中等偏高",
        highWinRatePick = true,
        winRateAnalysis = "先用周线 MA20 过滤空头，再只做日线回踩，假信号少。胜率相对更高，但交易次数少。",
        operability = "好操作",
        operabilityNote = "工薪友好：周末看一次周线选股，工作日只需收盘后看日线是否回踩，不必盘中盯。参数固定（周MA20、日MA10/20），不容易执行漂移。唯一要求是忍住不追突破当天。",
        buyRules = listOf(
            "【方向条件】切换到周线：收盘价 > 周线 MA20，且本周 MA20 ≥ 上周 MA20（向上或走平）。",
            "【增强可选】同时收盘价 > 周线 MA10；更稳再要求 > 周线 MA60。",
            "【买点条件】切回日线：股价回踩日线 MA10 或 MA20（最低价触及均线，或距均线 ≤ 约 1.5%）。",
            "【确认条件】回踩当日或次日，收盘价重新站上该均线（收盘 > MA10/MA20）。",
            "【执行】收盘后挂单或次日限价买，不盘中追高；单笔仓位建议总资金的 10%～20%。",
            "【不做】周线空头、刚放量长阴破位、一字涨停追高，均不买。"
        ),
        sellRules = listOf(
            "【减仓】日线收盘跌破日线 MA20 → 先卖出一半。",
            "【清仓主规则】周线收盘价跌破周线 MA20 → 剩余仓位清掉。",
            "【清仓加强】若已跌破周线 MA20，且下一周仍收在其下方 → 即使反弹也不再扛。",
            "【止盈可选】浮盈达到预先设定目标（如 +10%～+15%）可先减半，剩余用周线 MA20 管。"
        ),
        stopLossRules = listOf(
            "【价止损】买入后，止损价 = 回踩低点 × (1 - 2.5%)；跌破则卖。",
            "【均线止损】若回踩的是日线 MA20，也可用「收盘跌破 MA20 且亏超 -3%」作为止损触发。",
            "【硬止损】单笔最大亏损 -5%：无论是否还想等，到了就走（防阴跌）。",
            "【时间止损】买入后 10 个交易日仍弱于大盘且未创新高，可减仓观望。",
            "【注意】A 股 T+1，止损按「次日及以后」执行；仓位不要大到止损睡不着。"
        ),
        afterWorkSteps = listOf(
            "周六日：周线筛选「收盘>MA20 且 MA20 不向下」的池子 3～8 只。",
            "每个交易日 15:05～15:20：看池子有没有回踩日线 MA10/20 且收盘站稳。",
            "有则挂次日买；持仓股按卖出/止损规则检查一遍即可收工。"
        ),
        summary = "「周线」= 周K周期。主规则用周线 MA20 定多空；买点用日线回踩 MA10/MA20 并收盘确认。适合工薪：标准具体、好执行、止损可量化。",
        howTo = listOf(
            "软件切到周线，叠加 MA10/MA20/MA60。",
            "多头：收盘 > 周MA20，且周MA20 不向下。",
            "日线等回踩 MA10 或 MA20，收盘站稳再买。",
            "破周MA20 清仓；破日MA20 先减仓；单笔亏 -5% 硬止损。"
        ),
        pros = listOf(
            "买卖与止损都能写成清单，执行不易飘。",
            "下班后 15～20 分钟可完成决策。",
            "过滤大量逆势与追高交易。"
        ),
        cons = listOf(
            "强势单边可能回踩很浅，买不到。",
            "周线滞后，顶部会吐回部分利润。",
            "需要耐心，不能每天都想动手。"
        ),
        tips = listOf(
            "周线 MA20 ≈ 约 100 个交易日量级，别和日线 MA20 搞混。",
            "优先 ETF/行业龙头，减少个股雷。",
            "先用小仓位按清单做满 20 笔，再放大。"
        ),
        aShareNotes = listOf(
            "务必先把周期切到「周」，再读均线 10/20/60。",
            "涨跌停、停牌可能导致止损失效，选流动性好的标的。",
            "财报季、解禁日尽量不新开仓。"
        )
    )

    private val etfTrendHold = StrategyGuide(
        id = "etf_trend",
        name = "宽基/行业 ETF 趋势持仓",
        tagline = "少选股、跟指数，适合上班族底仓",
        style = "中长线",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "90天～数年",
        holdingDaysDetail = "单次趋势仓常见 3 个月以上，可持有数月至数年。",
        winRateLevel = "中等偏高",
        highWinRatePick = true,
        winRateAnalysis = "胜在少犯大错、少踩个股雷。不是追求高笔数胜率，而是账户长期存活。",
        operability = "好操作",
        operabilityNote = "最好操作的一类：每月看一次即可。买卖点少，适合完全没时间盯盘的人。",
        buyRules = listOf(
            "标的：1～2 只宽基 ETF（如沪深300/中证500）± 最多 1 只行业 ETF。",
            "周线或月线收盘价在长期均线上方（常用周线 MA20 或月线 MA10/MA20）。",
            "趋势完好时买入/加到目标仓位；不要在极端暴跌当天冲动满仓（可分 2～3 次）。"
        ),
        sellRules = listOf(
            "周线收盘有效跌破周线 MA20（或你选定的长期均线）→ 降仓到防守仓（如半仓）。",
            "继续走坏、均线拐头向下 → 再降或清到底仓。",
            "行业 ETF 相对宽基连续大幅走弱 → 先减行业。"
        ),
        stopLossRules = listOf(
            "中长线用「结构止损」为主：破长期均线减仓，而不是日线毛刺止损。",
            "账户层面：总回撤达到你预设阈值（如 -15%～-20%）启动全面降仓。",
            "单只行业 ETF 从买入点回撤 -12%～-15% 可先减半。"
        ),
        afterWorkSteps = listOf(
            "每月最后一个交易日晚：检查周/月均线是否仍多头。",
            "按规则调一次仓，其余时间不看盘。"
        ),
        summary = "用 ETF 跟趋势，按月执行。操作极简，适合工薪底仓。",
        howTo = listOf("选定 ETF", "均线上方持有", "跌破降仓", "按月复查"),
        pros = listOf("几乎不盯盘", "避开个股雷", "易坚持"),
        cons = listOf("弹性弱于强势股", "熊市仍回撤"),
        tips = listOf("先写清最大回撤承受度"),
        aShareNotes = listOf("选流动性好的 ETF，注意溢价")
    )

    private val pullbackConfirm = StrategyGuide(
        id = "pullback_confirm",
        name = "突破回踩确认法",
        tagline = "不追突破当天，等回踩站稳再上车",
        style = "波段",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "进阶",
        holdingDaysShort = "5～30天",
        holdingDaysDetail = "常见 5～30 天；假突破止损可能只有几天。",
        winRateLevel = "相对较高",
        highWinRatePick = true,
        winRateAnalysis = "用回踩确认过滤假突破，胜率相对高；代价是可能买不到最强票。",
        operability = "好操作",
        operabilityNote = "很好操作：突破日只记观察，买点出现在之后几天的收盘确认。关键是忍住不追涨。建议与周线 MA20 多头叠加。",
        buyRules = listOf(
            "先标出关键阻力：前高 / 箱体上沿 / 颈线。",
            "突破日：收盘站上阻力且最好放量 → 只观察，不买。",
            "其后 1～5 日：回踩至突破位附近（可距突破价 ±1%～2% 内）。",
            "回踩缩量或收出下影，且收盘再次站上突破位 → 买入。",
            "可选增强：仅当周线也在 MA20 上方时才做。"
        ),
        sellRules = listOf(
            "到达下一阻力或浮盈达到目标（如 +8%～+12%）→ 减仓/止盈。",
            "收盘重新跌破突破位且无法收回 → 卖出。"
        ),
        stopLossRules = listOf(
            "止损放在回踩最低点下方 2%，或突破位下方 2%（取你事先写死的一个）。",
            "买入后若收盘跌破突破位 → 视为假突破，按收盘卖（不等更深）。",
            "硬止损：单笔 -5%。"
        ),
        afterWorkSteps = listOf(
            "晚间标阻力、记突破观察票。",
            "之后每天收盘看是否回踩站稳；符合再挂次日买。"
        ),
        summary = "不追突破当天，等回踩确认。买卖标准清楚，工薪可执行，相对胜率较高。",
        howTo = listOf("标阻力", "突破只观察", "回踩站稳买", "破位止损"),
        pros = listOf("少追高", "晚间可决策", "假突破过滤好"),
        cons = listOf("可能买不到", "要耐心", "频率低"),
        tips = listOf("没买到不算失败", "叠加周线多头更好"),
        aShareNotes = listOf("一字板突破更要等回踩", "消息股勿机械套用")
    )

    /**
     * 快验证版（3～10 交易日）：小涨小跌常见、偶尔大涨、硬止损无大跌。
     * 用时间止损把持股压在可快速复盘的周期内。
     */
    private val asymmetricTrendPullback = StrategyGuide(
        id = "asymmetric_trend_pullback",
        name = "趋势回踩·硬止损法",
        tagline = "3～10 日快验证：小涨小跌 + 偶尔大涨；硬止损杜绝大跌",
        style = "短线",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "3～10天",
        holdingDaysDetail = "目标持股 3～10 个交易日：多数 2～6 天结束；到第 10 日强制清仓便于快速验证。不再故意拿到 20 天长趋势。",
        winRateLevel = "中等（不追求高胜率）",
        highWinRatePick = true,
        winRateAnalysis = "快验证版胜率仍约四五成，单笔多为 ±2%～±6%。偶尔大涨看 +6%～+12% 的短脉冲；大亏被 -4%～-5% 硬止损截断。用 10～15 笔就能初步看期望是否为正。",
        operability = "好操作",
        operabilityNote = "收盘价决策、一次进场。核心纪律：硬止损 + 第 10 日时间止损必须执行，方便短期复盘迭代。",
        buyRules = listOf(
            "【趋势过滤·可衡量】日线收盘价 > MA20，且今日 MA20 ≥ 昨日 MA20。不满足不做。",
            "【可选加强】收盘价 > MA60；优先成交活跃、波动适中的中军（快验证不宜专做超慢牛超大票）。",
            "【回踩区间·可衡量】近 3 日内最低价触及 MA10，或距 MA10 ≤ 1.5%，或落在 MA10～MA20 之间。",
            "【确认买点·可衡量】回踩当日或次日：收盘重新站上 MA10，且仍 > MA20 → 一次买入。",
            "【不做】距 MA10 > 3% 的追高、MA20 下行、跌停次日反抽、一字连板情绪票。"
        ),
        sellRules = listOf(
            "【主卖点】收盘跌破 MA10 → 次日开盘或收盘清仓（多数小涨小跌在此结束）。",
            "【偶尔大涨】浮盈 ≥ +6% 后，改为「收盘跌破 MA10 或触及 +12% 止盈」谁先到谁卖（短周期内锁定大涨，不改盯 MA20 久拖）。",
            "【时间止损·硬性】买入计入第 1 日，满 10 个交易日无论盈亏 → 第 10 日收盘清仓。专为快验证。",
            "【加速确认】若第 5 日仍未到 +3% 且收盘跌破 MA10 → 当日/次日走，避免磨满 10 天。",
            "【一次出手】不加仓；不盘中情绪单。"
        ),
        stopLossRules = listOf(
            "【结构止损】止损价 = 回踩最低价 × 0.98。收盘跌破（默认按收盘）→ 卖。",
            "【硬止损】单笔最大亏损 -4%（快验证略严于 -5%，更快结束错误单）。",
            "【假信号】买入次日收盘跌破 MA20 → 按收盘走。"
        ),
        afterWorkSteps = listOf(
            "收盘筛：>MA20 且 MA20 未下行；近 3 日回踩后站回 MA10；距 MA10 ≤ 3%。",
            "持仓每日只看四件事：硬止损 / 破 MA10 / +6%～+12% 止盈带 / 是否到第 10 日。",
            "推荐标的写入「日志·跟踪日志」（名称写清股票全称）；满足买点并成交后，再到「记录」记账。"
        ),
        summary = "快验证版不对称手法：硬止损封左尾，破 MA10 消化噪声，+6% 后短止盈博偶尔大涨，第 10 日强制清仓。专为 3～10 日周期快速验证期望值。策略全文在本战法页；推荐观察进跟踪日志，已成交进记录。",
        howTo = listOf(
            "MA20 多头 + 回踩站回 MA10 买",
            "破 MA10 或 -4% 硬止损卖",
            "+6% 后盯 MA10/ +12% 止盈",
            "满 10 个交易日必须清"
        ),
        pros = listOf(
            "周期短，10～15 笔即可初步验证",
            "买卖点可量化",
            "硬止损 + 时间止损，避免大跌和长期套牢",
            "一次进场，收盘决策"
        ),
        cons = listOf(
            "会卖飞更长趋势（这是快验证的代价）",
            "胜率未必高，要能接受连亏小单",
            "震荡市摩擦仍在，需 MA20 过滤"
        ),
        tips = listOf(
            "【快验证目标】先证明：最大单亏可控、整体期望不为负；再考虑是否放宽到更长持仓。",
            "【标的】优先招行/平安/海康等活跃中军；茅台类慢牛均持过长，不适合本快验证版。",
            "【记账】手法统一填「趋势回踩·硬止损法」。跟踪日志只记推荐观察标的；已成交进「记录」，两处勿混。"
        ),
        aShareNotes = listOf(
            "T+1：最早次日才能卖；第 10 日按交易日计，不含停牌。",
            "与断板反包/打板分账统计，不要混在一个胜率里。"
        )
    )

    private val gridInRange = StrategyGuide(
        id = "grid_range",
        name = "箱体网格法",
        tagline = "震荡市高抛低吸，预设买卖网格",
        style = "波段",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "进阶",
        holdingDaysShort = "3～20天/格",
        holdingDaysDetail = "单格约 3～20 天；箱体内可运行数月。",
        winRateLevel = "中等（仅限箱体）",
        highWinRatePick = false,
        winRateAnalysis = "箱体成立时单格胜率尚可；单边市会失效。",
        operability = "一般",
        operabilityNote = "设好条件单后较省心；难点是判断箱体是否还成立，以及破位后要敢于全停。",
        buyRules = listOf(
            "箱体至少被测试 2～3 次上下沿。",
            "价格跌到预设买格（如每降 3%～5% 买一格）→ 买。",
            "单格仓位固定，总网格仓位设上限。"
        ),
        sellRules = listOf(
            "涨到上一卖格 → 卖对应仓位。",
            "放量突破上沿 → 停止网格，或改为趋势持有。"
        ),
        stopLossRules = listOf(
            "跌破箱体下沿 2%～3% → 停止网格并处理底仓。",
            "不要在破位后继续向下加格。"
        ),
        afterWorkSteps = listOf(
            "事先设网格与条件单。",
            "每天收盘确认是否破下沿。"
        ),
        summary = "只在清晰箱体用网格。好条件单执行，坏在破位处理。",
        howTo = listOf("确认箱体", "设网格", "破位全停"),
        pros = listOf("震荡有现金流", "可条件单"),
        cons = listOf("单边接刀", "卖飞趋势", "手续费"),
        tips = listOf("先用 ETF 练", "网格别太密"),
        aShareNotes = listOf("税费会吃掉过密网格利润")
    )

    private val fixedInvestDca = StrategyGuide(
        id = "dca_plan",
        name = "纪律定投法",
        tagline = "固定日期定额买，用时间淡化择时",
        style = "中长线",
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "1年～数年",
        holdingDaysDetail = "通常持有 1 年～数年。",
        winRateLevel = "中等（淡化择时）",
        highWinRatePick = false,
        winRateAnalysis = "不保证每笔都赚，但降低一次性买在高点的概率。适合当底座。",
        operability = "好操作",
        operabilityNote = "执行最简单：发薪日买一次。难在标的选择与长期坚持，不在买卖点判断。",
        buyRules = listOf(
            "固定日期（如发薪日）定额买入既定 ETF。",
            "不因当天涨跌取消；金额不超过不影响生活的闲钱。"
        ),
        sellRules = listOf(
            "按人生用钱节点或年度再平衡卖出。",
            "可与趋势策略结合：长期均线大破位时暂停/减额定投。"
        ),
        stopLossRules = listOf(
            "定投一般不做日线止损；用「标的逻辑失效」或「最大回撤预算」管理。",
            "若单标的相对基准长期大幅跑输，更换标的而不是无限加仓摊平。"
        ),
        afterWorkSteps = listOf("发薪日买入", "每年复查一次"),
        summary = "固定日期定额买，最省心。更适合底仓，不是短线胜率工具。",
        howTo = listOf("定标的与金额", "固定日买入", "年度复查"),
        pros = listOf("零盯盘", "平滑成本", "贴合工资节奏"),
        cons = listOf("牛市后期仍买", "标的错了会长期拖"),
        tips = listOf("可与 ETF 趋势策略叠加"),
        aShareNotes = listOf("不用生活费/借贷定投")
    )

    /**
     * 回封确认：涨停打开后再封住才进。专为「一次出手、不能分仓」设计。
     */
    private val resealConfirm = StrategyGuide(
        id = "reseal_confirm",
        name = "回封确认法",
        tagline = "打开后再封住才买：用一次子弹换确认，少做脉冲半路",
        style = "短线",
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "进阶",
        holdingDaysShort = "1～2天",
        holdingDaysDetail = "回封当日进，次日冲高兑现为主；晋级算惊喜，不按连板剧本拿。",
        winRateLevel = "中等偏高（挑票后）",
        highWinRatePick = false,
        winRateAnalysis = "比早半路假突破少，比盲打板稳健；代价是成本偏高、有时封死买不进。胜率靠「只做辨识度票」。",
        operability = "难操作",
        operabilityNote = "要盯打开与回封节奏。适合能看盘的时段；不能盯盘则改收盘后复盘、次日开盘决策，但会漏掉盘中回封。",
        buyRules = listOf(
            "标的当日或前日有涨停基因（首板/连板/反包板），不是无量阴跌票。",
            "盘中打开（断板）后，分时再度拉升并回封；封单回补、打开次数收敛。",
            "板块仍有跟风或指数未崩；跌停潮中不做回封。",
            "一次进场：回封确认后买；不买第一次脉冲半路。",
            "单票仓位 ≤12%～15%，按最坏次日低开算承受力。"
        ),
        sellRules = listOf(
            "次日高开弱走 / 分时翻绿 → 开盘走。",
            "次日冲高无力 → 冲高兑现。",
            "回封后再开且无法回封 → 止损或次日冲高走。"
        ),
        stopLossRules = listOf(
            "跌破回封前分时启动点或明显低点 → 止损。",
            "单笔最大亏损 -5%～-7%。",
            "市场转冰点 → 停做该手法。"
        ),
        afterWorkSteps = listOf(
            "复盘当日打开又回封的票，记辨识度与板块。",
            "统计：回封进场 vs 打板进场的次日溢价差异。"
        ),
        summary = "最适合「只能一枪」的短线模板：牺牲一点成本，换确认度。与断板反包是一家人，本法则把买点钉死在回封上。",
        howTo = listOf("有板基因 → 等打开 → 回封稳住 → 一枪进 → 次日兑现"),
        pros = listOf("确认度高", "假突破少", "一次出手友好", "规则清晰"),
        cons = listOf("成本偏高", "封死买不进", "仍需盯盘", "烂板回封次日仍可能弱"),
        tips = listOf(
            "回封秒开、封单虚、板块死 = 假回封，宁空过。",
            "与断板反包搭配：反包情境下你的默认买点就是回封。"
        ),
        aShareNotes = listOf("T+1：回封日买进，风险在次日低开", "一字回封常买不进，不要追高排板")
    )

    /**
     * 一进二：首板次日，分歧转一致或回踩确认后再做晋级。
     */
    private val oneToTwo = StrategyGuide(
        id = "one_to_two",
        name = "一进二",
        tagline = "首板次日做晋级：等分歧转一致，不追高开一字",
        style = "短线",
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "进阶",
        holdingDaysShort = "1～3天",
        holdingDaysDetail = "二板当日或次日兑现为主；能上三板再看空间，但不预设。",
        winRateLevel = "中等（挑龙头后）",
        highWinRatePick = false,
        winRateAnalysis = "一进二是经典短线，但跟风首板很多；只做「卡位/辨识度/板块中军」胜率才够用。",
        operability = "难操作",
        operabilityNote = "竞价与午前分歧最关键，上班难。可缩小为：只做收盘强势首板的观察池，次日开盘后满足回踩/回封再决策。",
        buyRules = listOf(
            "昨日首板（最好是卡位或板块先锋），换手充分，非一字无量。",
            "今日情绪未冰点，题材仍有延续预期。",
            "【一次出手买点】竞价后分歧（低开/分时回踩）再弱转强，或断板后回封；不追高开一字排板。",
            "晋级过程带量，最好有小弟跟风。",
            "单票 ≤12%～15%。"
        ),
        sellRules = listOf(
            "二板次日冲高走，不默认拿三板。",
            "二板打开不回 → 走。",
            "高开低走、板块熄火 → 开盘减/清。"
        ),
        stopLossRules = listOf(
            "跌破首板当日低点或二板分时启动点 → 止损。",
            "单笔 -5%～-8% 硬止损。",
            "情绪退潮/冰点 → 停做晋级。"
        ),
        afterWorkSteps = listOf(
            "收盘筛强势首板入池（换手、题材、辨识度）。",
            "次日只盯池内，不盘中抓陌生首板。"
        ),
        summary = "一进二吃「首板后的共识强化」。一次出手时，重点等分歧后的确认，而不是开盘追高。",
        howTo = listOf("收盘定首板池", "次日等分歧", "弱转强/回封一枪", "二板附近兑现"),
        pros = listOf("模式经典好复盘", "空间想象大", "确认买点清晰"),
        cons = listOf("跟风首板多", "退潮期杀伤大", "需盯竞价/午前", "排板买不进"),
        tips = listOf(
            "只做有辨识度的首板；杂毛一进二胜率很差。",
            "一次出手：宁可等回封，不追开盘就加速的一字。"
        ),
        aShareNotes = listOf("首板次日低开是常态，先看能否弱转强", "二板溢价高时更要控仓")
    )

    /**
     * 断板反包：前日（或当日）涨停打开/大面后，次日或盘中弱转强再度逼近/封死涨停。
     * 结合实盘：好当家、华胜天成等短持反包案例复盘。
     */
    private val brokenBoardReversal = StrategyGuide(
        id = "broken_board_reversal",
        name = "断板反包",
        tagline = "断板大面后弱转强再包回去：吃情绪修复，不赌无限连板",
        style = "短线",
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "进阶",
        holdingDaysShort = "1～3天",
        holdingDaysDetail = "多数 1～2 天兑现；反包次日冲高走或缩量走弱即减。极少拿到 3 天以上，除非主线仍强且继续晋级。",
        winRateLevel = "中等（挑票后偏高）",
        highWinRatePick = false,
        winRateAnalysis = "乱做假反包胜率很低；只做「有辨识度、前日有资金、板块未死」的断板票，胜率可抬到中等以上，靠的是筛选而不是打板本身。",
        operability = "难操作",
        operabilityNote = "要盯分时弱转强、量能与板块跟风，上班难稳定执行。若只做收盘强势反包板、次日开盘决策，可操作性略好，但会错过盘中黄金买点。",
        buyRules = listOf(
            "【资格】标的刚经历涨停后断板/大面（换手充分），不是无量阴跌杂毛。",
            "【情绪】大盘与题材未全面冰点；最好仍处偏强/分化/退潮前段，忌跌停潮里硬反包。",
            "【约束】若只能一次出手（不能分仓加仓）：默认放弃「早半路试错」，改做「确认买点」——回封确认或强势打板二选一。",
            "【可分仓时】买点优先半路弱转强（成本优）；先看带量、回踩不破、板块同步，再考虑补仓。",
            "【一次出手·首选】回封确认：打开后再回封、封单稳住再进——用一次子弹换确认，降低冲高回落。",
            "【一次出手·次选】打板：仅龙头/中军 + 带量 + 板块跟风；非一字、非尾盘偷袭。接受溢价，换更高确认。",
            "【一次出手·慎做】刚破分时均线的「早半路」：假突破概率高，一枪打空后没有补救仓，胜率不划算。",
            "【仓位】单票 ≤12%～15%（一次进场更要控仓）；总短线仓设上限。宁可空过，不拿唯一子弹赌脉冲。"
        ),
        sellRules = listOf(
            "一次进场后结构破坏（跌破启动点/回封失败）：直接止损，没有「留一半等补仓」剧本。",
            "反包涨停次日：高开弱走 / 分时翻绿 → 开盘减或清。",
            "反包次日冲高无力、量能萎缩 → 冲高兑现，不赌连板。",
            "反包板盘中打开且无法回封 → 按规则止损或次日冲高走。",
            "板块熄火、指数破位与个股同步转弱 → 优先走，不讲故事。"
        ),
        stopLossRules = listOf(
            "跌破反包启动点（弱转强分时低点）或断板当日显著低点 → 止损。",
            "一次出手者：止损必须预设且更干脆（建议亏到 -4%～-5% 或结构坏就走），因为不能靠补仓摊成本。",
            "单笔最大亏损建议 -5%～-7%（打板溢价后更要硬止损）。",
            "市场转冰点（跌停扩散）→ 暂停该手法，已有仓位优先降风险。"
        ),
        afterWorkSteps = listOf(
            "复盘：哪些断板票次日具备反包资格（换手、辨识度、板块）。",
            "列出观察池，次日只盯池内分时，不盘中临时抓陌生票。",
            "统计：半路 vs 打板的盈亏比差异，强制用数据纠正手痒打板。"
        ),
        summary = "断板反包吃的是「恐慌后的情绪修复与筹码交换」，不是无限空间幻想。能分仓时半路成本更优；只能一次出手时，应优先回封确认或强势打板，放弃早半路试错，避免冲高回落打空唯一子弹。",
        howTo = listOf(
            "断板大面 → 入观察池",
            "等弱转强/回封确认",
            "一次出手：回封或严筛打板",
            "次日冲高兑现，不恋战"
        ),
        pros = listOf(
            "买点优于追连板：断板后抛压释放，反包方多空转换更干净",
            "盈亏比潜力好：半路成本低，成功后靠近涨停仍有空间",
            "情绪与资金验真：能反包说明仍有主力/游资愿意接，比无量阴跌可信",
            "心理优势：从「怕断板」切换到「等修复」，减少追高焦虑与后悔盘"
        ),
        cons = listOf(
            "假反包极多：无量拉升、板块死、指数崩时最容易接飞刀",
            "打板溢价伤盈亏比：封死才进 → 次日稍弱就被动",
            "需要盯盘与情绪判断，工薪难稳定执行",
            "成功一次容易上头，把所有断板都当反包做"
        ),
        tips = listOf(
            "【心理】断板当日的疼痛与羞辱感，会逼出「必须马上扳回」——这是假反包温床。规则先于情绪：无带量弱转强就不进。",
            "【心理】反包成功后的得意，容易改成「拿连板」。默认持仓剧本只有「次日兑现」，晋级当惊喜。",
            "【一次出手】不能分仓时，半路的「冲高回落」没有补救：唯一子弹打在假突破上 = 整笔作废。因此默认改为：回封确认优先，强势打板次之；早半路基本不做。",
            "【半路 vs 确认】能分仓：半路试错 + 确认补仓。不能分仓：直接买确认（回封/打板），用略差成本换更高胜率。",
            "【真确认】回封后封单不掉、分时回踩不破、板块有跟风；假确认：回封秒开、无量偷袭、板块熄火——宁可不打。",
            "【打板适不适合】适合：龙头/辨识度龙 + 板块跟风 + 分时强势台阶。不适合：杂毛、退潮末/冰点、尾盘偷袭、一字排板。",
            "【默认答案·一次出手】反包当天优先「回封确认」；够强才打板；不做早半路。空过假突破，比打飞一次更重要。",
            "结合你的案例：好当家、华胜天成均短持兑现——继续按「确认进、次日兑现」统计，不要硬学需要分仓的半路模板。"
        ),
        aShareNotes = listOf(
            "T+1：反包日打板买进，次日才能卖；次日低开是最大风险，仓位必须先按最坏情况算。",
            "涨停封单与打开次数比「是否涨停」更重要；烂板反包次日分歧概率更高。",
            "税费对超短影响大：小仓试错可以，频繁打板摩擦成本会吞掉微利。"
        )
    )

    val emotionPlaybooks: List<StrategyGuide> = EmotionPlaybooks.all

    val all: List<StrategyGuide> = listOf(
        fiveDayMaTrend,
        dragonHead,
        resealConfirm,
        oneToTwo,
        brokenBoardReversal,
        asymmetricTrendPullback,
        weeklyTrendDailyEntry,
        etfTrendHold,
        pullbackConfirm,
        gridInRange,
        fixedInvestDca
    ) + emotionPlaybooks
}
