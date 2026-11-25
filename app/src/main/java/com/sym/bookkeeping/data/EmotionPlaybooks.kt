package com.sym.bookkeeping.data

/**
 * 涨停页情绪状态对应的短线手法展开（出手时机 / 仓位 / 卖点）。
 * 与 [MarketToneKind] 标签对齐，供战法页查阅。
 */
object EmotionPlaybooks {

    const val STYLE_TAG = "情绪手法"

    fun findById(id: String): StrategyGuide? = all.find { it.id == id }

    private val climax = StrategyGuide(
        id = "emotion_climax",
        name = "高潮 · 兑现与卡位",
        tagline = "行情过热：兑现浮盈，只卡总龙/中军，防次日核按钮",
        style = STYLE_TAG,
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "半日～2天",
        holdingDaysDetail = "高潮日多为当日或次日兑现；隔夜仓要极轻。",
        winRateLevel = "偏低（防回撤）",
        highWinRatePick = false,
        winRateAnalysis = "高潮后次日分歧概率高，胜率不追求，核心是保住利润、降低隔夜风险。",
        operability = "难操作",
        operabilityNote = "需盘中判断是否过热；工薪若不盯盘，高潮日默认减仓/空仓更安全。",
        buyRules = listOf(
            "出手时机：仅当总龙头或板块中军「分歧低吸后回封」；不在一致加速末端追高。",
            "选股：全市场辨识度最高的 1 只总龙，或主线中军；放弃三板外杂毛、跟风。",
            "仓位：新建仓 ≤ 总资金 10%；已有浮盈以减仓为主，加仓只允许对总龙极小仓。",
            "确认：分时回封要带量，板块内仍有跟风涨停；无量假回封不做。"
        ),
        sellRules = listOf(
            "卖点1：冲高滞涨、封单急剧变薄 → 先减半。",
            "卖点2：尾盘仍一致但高度拥挤 → 兑现大部分，最多留观察仓过夜。",
            "卖点3：次日低开击穿昨收/分时均线且不回 → 剩余清仓，不扛核按钮。"
        ),
        stopLossRules = listOf(
            "隔夜仓若次日开盘跌超 −5% 或直接核按钮预期，开盘先走。",
            "盘中跌破当日回封低点 → 止损，不补仓摊平。",
            "单笔亏损硬止损 −7%（短线高潮波动大，仓位必须先控住）。"
        ),
        afterWorkSteps = listOf(
            "复盘：今日是否高潮（涨停多、板高、放量）。",
            "列出总龙与中军，标记「只观察、不追高」。",
            "计划次日：若低开大面，是否还有反包资格（留给退潮反包手法）。"
        ),
        summary = "高潮不是加仓日，是兑现日。只允许极轻仓卡总龙分歧回封，其余以落袋为安。",
        howTo = listOf(
            "先减仓兑现 → 再决定是否留总龙观察仓",
            "不追远端连板与杂毛",
            "次日核按钮直接走，不做英雄"
        ),
        pros = listOf("保护利润", "降低高潮后回撤", "强制聚焦龙头"),
        cons = listOf("容易卖早", "需盯盘", "错过主升末段加速"),
        tips = listOf(
            "放量高潮更危险；缩量高潮更虚，更不要隔夜重仓",
            "与涨停页「高潮」标签联动：见标签先想兑现，不是开干"
        ),
        aShareNotes = listOf("跌停可能卖不出，所以高潮日仓位必须提前降下来", "T+1：当日新买次日才能卖，高潮日尽量不新开隔夜重仓")
    )

    private val mainUpHeightUp = StrategyGuide(
        id = "emotion_main_up_height",
        name = "主升（板高升）· 跟连板",
        tagline = "连板高度抬升：跟晋级顺畅的连板与空间龙，首板要有辨识度",
        style = STYLE_TAG,
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "1～5天",
        holdingDaysDetail = "连板持仓常见 1～3 板周期；空间龙可顺延到高度停滞日。",
        winRateLevel = "中等（靠盈亏比）",
        highWinRatePick = false,
        winRateAnalysis = "主升期跟对龙头盈亏比好，但追高失败一笔可吞多笔利润，必须控仓与止损。",
        operability = "难操作",
        operabilityNote = "需要看天梯晋级、封板质量与板块承接；更适合能盘中看一眼的人。",
        buyRules = listOf(
            "出手时机：① 晋级日开盘弱转强回封；② 分歧日分时回封且板块跟风仍在；③ 空间龙卡位缩量后再放量。",
            "选股优先级：空间龙 > 主线中军连板 > 高辨识度首板（有板块、有叙事）。",
            "不做：无板块的野路子首板、开板次数过多的弱票、高度已拥挤的末位跟风。",
            "仓位：单票启动 10%～15%，确认晋级后再加到 ≤20%；总短线仓 ≤40%。"
        ),
        sellRules = listOf(
            "卖点1：晋级失败（断板）且午后无法回封 → 走。",
            "卖点2：高度抬升后出现「加速一字」却板块跟风塌了 → 次日冲高减。",
            "卖点3：连板节奏断层、天梯最高板回落 ≥2 → 降期望，先减仓。"
        ),
        stopLossRules = listOf(
            "买入后跌破当日回封最低点，或开板后无法回封。",
            "单笔 −6%～−8% 硬止损。",
            "大盘突然转冰点（跌停潮）→ 短线仓整体降到观察位。"
        ),
        afterWorkSteps = listOf(
            "对照天梯：最高板是否抬升、晋级是否顺畅",
            "圈定次日候选：空间龙 + 1～2 只中军",
            "写下「不追的名单」（杂毛、三线）"
        ),
        summary = "板高上升的主升期，核心是跟辨识度与晋级，不是到处打首板。",
        howTo = listOf("先看天梯高度是否抬升", "只做主线晋级", "断板不扛，晋级再加"),
        pros = listOf("盈亏比好", "趋势明确", "易复盘高度"),
        cons = listOf("追高风险", "一字难买", "情绪切换快"),
        tips = listOf("首板必须能讲清「为什么是它」", "仓位永远给晋级留加仓空间，不要一次性打满"),
        aShareNotes = listOf("打板有打开风险", "科创/创业板波动更大，仓位再降一档")
    )

    private val mainUpHeightDown = StrategyGuide(
        id = "emotion_main_up_fade",
        name = "主升（板高落）· 回封与反包",
        tagline = "仍主升但板高回落：等龙头回封/中军，可看大面后反包，少追高",
        style = STYLE_TAG,
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "1～3天",
        holdingDaysDetail = "以回封当日或反包后 1～2 日兑现为主，不恋战。",
        winRateLevel = "中等",
        highWinRatePick = false,
        winRateAnalysis = "板高回落说明赚钱效应开始挑食；做对回封/反包可以，追高胜率下降。",
        operability = "一般",
        operabilityNote = "比纯跟连板稍好执行：更强调等回封与大面后再动手，减少追高。",
        buyRules = listOf(
            "出手时机：前日龙头大面后，次日分时弱转强并回封（或大幅反包接近涨停）。",
            "中军补涨：主线仍在、高标分歧时，中军缩量回调再拉升可低吸（非打板）。",
            "仓位：单票 ≤12%；反包确认后再加，总仓 ≤30%。",
            "前提：涨停页仍偏「主升/偏强」，不是退潮/冰点。"
        ),
        sellRules = listOf(
            "卖点1：反包涨停打开且无法回封 → 次日冲高走。",
            "卖点2：回封后次日无法晋级、又变大面 → 减仓。",
            "卖点3：板高继续塌、跌停增多 → 按退潮处理，清空接力思维。"
        ),
        stopLossRules = listOf(
            "反包失败跌破分时启动点或昨收关键支撑。",
            "单笔 −5%～−7%。",
            "大盘转弱且上涨家数塌方 → 不补仓。"
        ),
        afterWorkSteps = listOf(
            "标记「大面龙头」观察池",
            "看最高板是否只是回落 1 档还是塌方 ≥2",
            "制定：只做回封/反包，不做新高追涨"
        ),
        summary = "主升未死但高度退坡：从「追晋级」切换到「等回封/反包」，少追高。",
        howTo = listOf("确认仍是主升而非退潮", "等大面或分歧", "回封确认再动手"),
        pros = listOf("买点更优", "避免高度拥挤", "衔接退潮反包"),
        cons = listOf("容易抄在半山腰", "假反包多", "需要耐心"),
        tips = listOf("假反包特征：无量、板块死、指数崩", "真反包特征：带量、板块有跟风、跌停减少"),
        aShareNotes = listOf("反包板常伴随巨量，注意次日分歧")
    )

    private val strong = StrategyGuide(
        id = "emotion_strong",
        name = "偏强 · 低吸卡位",
        tagline = "情绪偏暖未到主升：低吸卡位，首板→二板，控制仓位",
        style = STYLE_TAG,
        watchIntensity = "中高",
        workerFriendly = false,
        difficulty = "进阶",
        holdingDaysShort = "1～3天",
        holdingDaysDetail = "以首板次日晋级或短持 1～2 日为主。",
        winRateLevel = "中等",
        highWinRatePick = false,
        winRateAnalysis = "偏强期容错尚可，但强度不够时连板晋级率一般，需严选与轻仓。",
        operability = "一般",
        operabilityNote = "可用「收盘强势首板 + 次日开盘弱转强」规则降低盯盘强度。",
        buyRules = listOf(
            "出手时机：强势首板次日开盘回踩分时均线后拉升；或盘中卡位接近涨停前的回流买点（非盲目打板）。",
            "选股：换手充分、板块有 2 只以上跟风、题材当日有热度。",
            "仓位：试错 8%～12%；晋级确认可加到 15%，总短线仓 ≤30%。",
            "涨停家数若较昨明显下降：频率减半，只做最强 1 只。"
        ),
        sellRules = listOf(
            "卖点1：次日无法接近涨停、走势变弱 → 午后减仓。",
            "卖点2：二板打开不回 → 走。",
            "卖点3：冲高到预期空间（如 +7%～涨停）分批兑现。"
        ),
        stopLossRules = listOf(
            "跌破买入日低点或开盘后半小时低点。",
            "单笔 −5%～−6%。",
            "情绪掉到震荡/退潮 → 停止新开仓。"
        ),
        afterWorkSteps = listOf(
            "筛 2～3 只强势首板做次日观察",
            "写下晋级条件：开盘不弱、分时站上均线",
            "不满足则空过"
        ),
        summary = "偏强是「小仓试错晋级」阶段，不是重仓连板阶段。",
        howTo = listOf("轻仓卡位", "晋级再加", "不加就走"),
        pros = listOf("节奏适中", "亏损可控", "可衔接主升"),
        cons = listOf("晋级失败多", "题材轮动快"),
        tips = listOf("回避一字无换手", "首板质量 > 数量"),
        aShareNotes = listOf("注意监管与情绪周期，偏强也可能一日转弱")
    )

    private val split = StrategyGuide(
        id = "emotion_split",
        name = "分化 · 只做最强一条线",
        tagline = "结构分化：只做最强主线龙头/中军，放弃杂毛",
        style = STYLE_TAG,
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "1～4天",
        holdingDaysDetail = "持有到主线分歧或龙头断板为止。",
        winRateLevel = "中偏低",
        highWinRatePick = false,
        winRateAnalysis = "分化市大部分票是陷阱；只做最强一条线可提高盈亏比，但选错线会连续止损。",
        operability = "难操作",
        operabilityNote = "关键是识别「唯一主线」；认错要快。",
        buyRules = listOf(
            "出手时机：主线龙头分歧转一致、或中军回踩后再攻。",
            "先问：今天赚钱效应在哪一条线？说不清就不做。",
            "仓位：单线总仓 ≤25%，单票 ≤15%；禁多线开花。",
            "指数与情绪背离时，以涨停质量/主线强度为准，不看大盘硬做。"
        ),
        sellRules = listOf(
            "卖点1：龙头断板且中军跟风熄火 → 主线结束，清仓该线。",
            "卖点2：主线切换到新题材，旧线只剩杂毛上涨 → 换线或空仓。",
            "卖点3：到达预设目标（如一段主升后的加速日）减仓。"
        ),
        stopLossRules = listOf(
            "认错线：开仓后半天无跟风、分时比大盘弱 → 撤。",
            "单笔 −5%～−7%。",
            "高标断板后不补仓「幻想回封」。"
        ),
        afterWorkSteps = listOf(
            "用涨停页对比：涨停多但下跌家数也多？→ 分化",
            "只保留一条主线的观察池",
            "杂毛全部划掉"
        ),
        summary = "分化市输在「什么都做」。赢面来自聚焦最强一条线。",
        howTo = listOf("先定主线", "只做龙头/中军", "断板即止"),
        pros = listOf("思路清晰", "避免杂毛损耗", "盈亏比尚可"),
        cons = listOf("选线主观", "容易踏空支线爆发"),
        tips = listOf("一天只允许一个主线仓位预算", "说不出龙头名字就空仓"),
        aShareNotes = listOf("分化时常伴随指数红、个股绿，别被指数骗进去")
    )

    private val range = StrategyGuide(
        id = "emotion_range",
        name = "震荡 · 精做反包/首板",
        tagline = "多空胶着：降频降仓，精做反包板与高质量首板",
        style = STYLE_TAG,
        watchIntensity = "中",
        workerFriendly = true,
        difficulty = "进阶",
        holdingDaysShort = "1～2天",
        holdingDaysDetail = "超短为主，隔夜仓极轻。",
        winRateLevel = "中等（少做）",
        highWinRatePick = true,
        winRateAnalysis = "少做本身提高有效胜率；震荡市频繁出手会磨损。高胜率来自「空过」。",
        operability = "好操作",
        operabilityNote = "规则可写成：每天最多 1 笔；不满足形态空过。适合约束冲动。",
        buyRules = listOf(
            "出手时机：① 大面后缩量再带量反包；② 收盘强势首板且板块有跟风（次日再决定）。",
            "仓位：单笔 ≤8%～10%，当天总仓 ≤20%。",
            "封板率偏弱时：更要等尾盘确认，不早盘追。",
            "一天最多交易 1～2 笔，做完就收工。"
        ),
        sellRules = listOf(
            "卖点1：反包次日冲高乏力 → 兑现。",
            "卖点2：首板次日低开弱 → 开盘走或反弹走。",
            "卖点3：到达 +5%～+8% 先减，留仓看晋级。"
        ),
        stopLossRules = listOf(
            "跌破反包启动点或首板当日低点。",
            "单笔 −4%～−5%（震荡市必须小亏）。",
            "连续两笔止损 → 当日停手。"
        ),
        afterWorkSteps = listOf(
            "只留 1 个次日计划",
            "写明买点与止损价",
            "不满足就空仓"
        ),
        summary = "震荡市核心是降频。精做反包/首板，把「空过」当成策略的一部分。",
        howTo = listOf("降仓降频", "等确认", "小亏快走"),
        pros = listOf("亏损可控", "规则清晰", "工薪相对可执行"),
        cons = listOf("利润空间有限", "假突破多"),
        tips = listOf("宁可错过主升第一天，也不在震荡里打满", "与涨停页「震荡」联动：见标签先降期望"),
        aShareNotes = listOf("震荡末段可能突转高潮或冰点，次日按新情绪切换手法")
    )

    private val ebbFanbao = StrategyGuide(
        id = "emotion_ebb_fanbao",
        name = "退潮（高度塌）· 反包板",
        tagline = "高潮后高度塌方、尚未冰点：做前日大面龙头反包/回封，不接力高位",
        style = STYLE_TAG,
        watchIntensity = "高",
        workerFriendly = false,
        difficulty = "高阶",
        holdingDaysShort = "1～2天",
        holdingDaysDetail = "反包成功后多为短持；晋级罕见，不按主升拿。",
        winRateLevel = "中偏低（精选后可过）",
        highWinRatePick = false,
        winRateAnalysis = "退潮反包是经典窗口，但假反包极多；只做「前高潮龙头」可提高质量。",
        operability = "难操作",
        operabilityNote = "需要对昨日大面票有清单，并盘中确认带量弱转强。",
        buyRules = listOf(
            "出手时机：前日高潮龙头大面后，次日探底回升、带量突破分时均线并冲击涨停/大幅反包。",
            "选股：优先前总龙、前中军；不做退潮期新冒出来的杂毛首板接力。",
            "仓位：试错 8%～12%；回封确认可到 15%，禁止重仓。",
            "否决：无量假拉、板块冰死、跌停家数仍在扩散。"
        ),
        sellRules = listOf(
            "卖点1：反包涨停打开不回 → 次日冲高清。",
            "卖点2：反包次日高开弱走 → 开盘减/走。",
            "卖点3：达到预期（回补大面大部分空间）分批兑现，不赌新一轮主升。"
        ),
        stopLossRules = listOf(
            "跌破反包当日最低点。",
            "单笔 −5%～−6%。",
            "市场转冰点（跌停潮）→ 立即停止该手法。"
        ),
        afterWorkSteps = listOf(
            "从高潮日名单里筛「大面龙头」",
            "看最高板是否塌 ≥2",
            "次日只盯清单内的弱转强，不扫板"
        ),
        summary = "高潮之后、冰点之前：反包窗口。只做前龙头回马枪，不接力新高标。",
        howTo = listOf("列大面龙头池", "等带量弱转强", "成功兑现，失败快认"),
        pros = listOf("买点相对低", "赔率不错", "逻辑清晰"),
        cons = listOf("假反包多", "容易变成接飞刀", "需要强纪律"),
        tips = listOf("高度塌方日不做高位接力", "反包成功也不代表新主升开始"),
        aShareNotes = listOf("大面次日可能直接跌停，买不进也别追", "注意情绪周期：反包后常再分歧")
    )

    private val ebbDefend = StrategyGuide(
        id = "emotion_ebb_defend",
        name = "退潮（其它）· 防守",
        tagline = "赚钱效应退潮：防守、等企稳，不追连板",
        style = STYLE_TAG,
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "观望为主",
        holdingDaysDetail = "默认空仓或极轻观察仓；有仓也以当日或次日了结。",
        winRateLevel = "高（靠空仓）",
        highWinRatePick = true,
        winRateAnalysis = "退潮期最好的「胜率」是不亏损。空仓也是交易。",
        operability = "好操作",
        operabilityNote = "执行最简单：停止新开连板类仓位。工薪友好。",
        buyRules = listOf(
            "默认不出手。",
            "仅当出现明确弱转强首板且跌停减少、指数不崩时，可 ≤5%～8% 试错。",
            "禁止：追连板、接力高位、摊平亏损。"
        ),
        sellRules = listOf(
            "有仓先降到观察位；反弹减仓。",
            "情绪未修复前，盈利也以兑现为主。"
        ),
        stopLossRules = listOf(
            "任何新建试错仓，−3%～−4% 即走。",
            "跌停风险票不隔夜。"
        ),
        afterWorkSteps = listOf(
            "只做复盘与观察池",
            "等待冰点信号或情绪企稳（涨停回升、跌停收敛）",
            "企稳后再切回偏强/反包手法"
        ),
        summary = "退潮防守的核心是停止伤害。等企稳，不追连板。",
        howTo = listOf("停手", "降仓", "等信号"),
        pros = listOf("保本", "心理压力小", "易执行"),
        cons = listOf("可能踏空反抽", "需要忍住出手欲"),
        tips = listOf("把「今天不做」写进日志", "指数不弱也不加仓追高"),
        aShareNotes = listOf("退潮末期常有脉冲反弹诱多，仓位仍要克制")
    )

    private val ice = StrategyGuide(
        id = "emotion_ice",
        name = "冰点 · 空仓观察",
        tagline = "冰点杀跌：空仓观察，记跌停名单留给后面反包",
        style = STYLE_TAG,
        watchIntensity = "低",
        workerFriendly = true,
        difficulty = "入门",
        holdingDaysShort = "空仓",
        holdingDaysDetail = "冰点日与其后 1～2 日以空仓为主，除非极致分歧后出现明确脉冲且你能接受试错。",
        winRateLevel = "高（空仓）",
        highWinRatePick = true,
        winRateAnalysis = "冰点抄底胜率很差；空仓等待是统计上更优的选择。",
        operability = "好操作",
        operabilityNote = "最简单：不买。把精力放在名单与次日计划。",
        buyRules = listOf(
            "默认不买。",
            "若尾盘跌停打开、情绪脉冲，也只当信号，不当买入理由；次日验证再说。",
            "试错仓（极少数）≤5%，且必须是超跌龙头而非杂毛。"
        ),
        sellRules = listOf(
            "若仍持仓：能卖先卖，不幻想V反。",
            "反弹到成本或小亏即处理，不为「回本」硬扛。"
        ),
        stopLossRules = listOf(
            "冰点日持仓按「生存」处理：开盘能走则走。",
            "不补仓、不摊平、不加仓。"
        ),
        afterWorkSteps = listOf(
            "记录跌停与断板名单（次日反包观察池）",
            "记录指数与跌停家数，判断是否仍在冰点",
            "制定：冰点解除条件（跌停收敛、涨停回升、板高止跌）"
        ),
        summary = "冰点是休息日。空仓、记名单，把子弹留给后面的反包与偏强。",
        howTo = listOf("空仓", "记名单", "等解除信号"),
        pros = listOf("避免深套", "积累观察池", "纪律训练"),
        cons = listOf("可能踏空冰点反抽第一下"),
        tips = listOf("反抽第一下往往给退潮反包手法，而不是冰点当日猛冲", "与涨停页「冰点」标签联动：见标签先放下鼠标"),
        aShareNotes = listOf("跌停潮中流动性差，更不能重仓幻想")
    )

    val all: List<StrategyGuide> = listOf(
        climax,
        mainUpHeightUp,
        mainUpHeightDown,
        strong,
        split,
        range,
        ebbFanbao,
        ebbDefend,
        ice
    )
}
