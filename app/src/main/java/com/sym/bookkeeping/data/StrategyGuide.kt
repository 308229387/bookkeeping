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
    fun findById(id: String): StrategyGuide? = all.find { it.id == id }

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

    val all: List<StrategyGuide> = listOf(
        fiveDayMaTrend,
        dragonHead,
        weeklyTrendDailyEntry,
        etfTrendHold,
        pullbackConfirm,
        gridInRange,
        fixedInvestDca
    )
}
